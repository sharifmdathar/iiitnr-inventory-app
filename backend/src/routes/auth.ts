import type { FastifyPluginAsync } from 'fastify';
import { compare, hash } from 'bcryptjs';
import { OAuth2Client } from 'google-auth-library';
import { prisma } from '../lib/prisma.js';
import { UserRole } from '../utils/enums.js';
import type { UserRoleValue } from '../utils/enums.js';

function getGoogleClientIds(): { primary: string; all: string[] } | null {
  const primaryClientId = process.env.GOOGLE_CLIENT_ID;
  if (!primaryClientId) {
    return null;
  }

  const extraClientIds = process.env.GOOGLE_CLIENT_EXTRA_IDS
    ? process.env.GOOGLE_CLIENT_EXTRA_IDS.split(',')
        .map((id) => id.trim())
        .filter(Boolean)
    : [];

  return {
    primary: primaryClientId,
    all: [primaryClientId, ...extraClientIds],
  };
}

async function verifyGoogleToken(idToken: string, allowedClientIds: string[]) {
  const client = new OAuth2Client(allowedClientIds[0]);
  const ticket = await client.verifyIdToken({
    idToken,
    audience: allowedClientIds as string[],
  });

  const payload = ticket.getPayload();
  if (!payload) {
    throw new Error('Google token verification returned null payload');
  }

  return {
    ...payload,
    name: payload.name || payload.given_name || undefined,
    imageUrl: typeof payload.picture === 'string' ? payload.picture : undefined,
  };
}

function validateEmailDomain(email: string): { valid: boolean; error?: string } {
  const allowedDomain = process.env.ALLOWED_EMAIL_DOMAIN || '@iiitnr.edu.in';
  if (!email.toLowerCase().endsWith(allowedDomain.toLowerCase())) {
    return {
      valid: false,
      error: `Only ${allowedDomain} email addresses are allowed. Your email (${email}) does not match the required domain.`,
    };
  }
  return { valid: true };
}

function validateEmailVerification(emailVerified?: boolean): { valid: boolean; error?: string } {
  const allowUnverifiedEmail = process.env.ALLOW_UNVERIFIED_EMAIL === 'true';
  if (!emailVerified && !allowUnverifiedEmail) {
    return {
      valid: false,
      error:
        'Google account email is not verified. Please verify your email address in your Google account settings, or set ALLOW_UNVERIFIED_EMAIL=true in .env for development.',
    };
  }
  return { valid: true };
}

async function findOrCreateGoogleUser(
  googleId: string,
  email: string,
  name: string | null,
  imageUrl: string | null,
) {
  const userSelect = {
    id: true,
    email: true,
    name: true,
    imageUrl: true,
    role: true,
    googleId: true,
  } as const;

  let user = await prisma.user.findFirst({
    where: {
      OR: [{ googleId }, { email }],
    },
    select: userSelect,
  });

  const shouldUpdateProfile =
    user &&
    (!user.googleId ||
      (name != null && user.name !== name) ||
      (imageUrl != null && user.imageUrl !== imageUrl));

  if (shouldUpdateProfile) {
    user = await prisma.user.update({
      where: { id: user.id },
      data: {
        googleId: user.googleId || googleId,
        name: name ?? user.name,
        imageUrl: imageUrl ?? user.imageUrl,
      },
      select: userSelect,
    });
  } else if (!user) {
    user = await prisma.user.create({
      data: {
        email,
        name,
        imageUrl,
        googleId,
        role: UserRole.STUDENT,
      },
      select: userSelect,
    });
  }

  return user;
}

function handleGoogleAuthError(err: unknown): { statusCode: number; error: string } {
  type GoogleAuthError = Error & { code?: string };

  if (err instanceof Error) {
    const { message } = err;
    const code = (err as GoogleAuthError).code;

    if (message.includes('audience') || code === 'auth/id-token-audience-mismatch') {
      return {
        statusCode: 400,
        error:
          'Token audience mismatch. The Web application Client ID in the backend must match the serverClientId used in the app.',
      };
    }

    const errorMessage = message || String(err) || 'unknown error';
    return { statusCode: 400, error: `Invalid Google token: ${errorMessage}` };
  }

  const fallbackMessage = String(err) || 'unknown error';
  return { statusCode: 400, error: `Invalid Google token: ${fallbackMessage}` };
}

const authRoutes: FastifyPluginAsync = async (app) => {
  app.post('/register', async (request, reply) => {
    const body = request.body as {
      email?: string;
      password?: string;
      name?: string;
    };

    const email = body?.email?.trim();
    const password = body?.password;
    const name = body?.name?.trim();

    if (!email || !password) {
      return reply.code(400).send({ error: 'email and password are required' });
    }

    if (password.length < 8) {
      return reply.code(400).send({ error: 'password must be at least 8 characters' });
    }

    const passwordHash = await hash(password, 12);

    try {
      const user = await prisma.user.create({
        data: {
          email,
          name,
          passwordHash,
          role: UserRole.PENDING,
        },
        select: {
          id: true,
          email: true,
          name: true,
          imageUrl: true,
          role: true,
          createdAt: true,
        },
      });

      const token = app.jwt.sign({ sub: user.id, role: user.role }, { expiresIn: '1d' });

      return reply.code(201).send({ user, token });
    } catch (err) {
      app.log.error(err);
      return reply.code(400).send({ error: 'email already in use' });
    }
  });

  app.post('/login', async (request, reply) => {
    const body = request.body as { email?: string; password?: string };
    const email = body?.email?.trim();
    const password = body?.password;

    if (!email || !password) {
      return reply.code(400).send({ error: 'email and password are required' });
    }

    const user = await prisma.user.findUnique({
      where: { email },
      select: {
        id: true,
        email: true,
        name: true,
        imageUrl: true,
        role: true,
        passwordHash: true,
      },
    });

    if (!user) {
      return reply.code(401).send({ error: 'invalid credentials' });
    }

    if (!user.passwordHash) {
      return reply.code(401).send({ error: 'this account uses Google Sign-In' });
    }

    const matches = await compare(password, user.passwordHash);
    if (!matches) {
      return reply.code(401).send({ error: 'invalid credentials' });
    }

    const token = app.jwt.sign({ sub: user.id, role: user.role }, { expiresIn: '1d' });

    return reply.send({
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        imageUrl: user.imageUrl,
        role: user.role,
      },
      token,
    });
  });

  app.get(
    '/me',
    {
      preHandler: async (request) => {
        await request.jwtVerify();
      },
    },
    async (request, reply) => {
      const userId = (request.user as { sub?: string })?.sub;
      if (!userId) {
        return reply.code(401).send({ error: 'invalid token' });
      }

      const user = await prisma.user.findUnique({
        where: { id: userId },
        select: {
          id: true,
          email: true,
          name: true,
          imageUrl: true,
          role: true,
        },
      });

      if (!user) {
        return reply.code(401).send({ error: 'invalid token' });
      }

      return reply.send({ user });
    },
  );

  app.post('/google', async (request, reply) => {
    const body = request.body as { idToken?: string };
    const idToken = body?.idToken;

    if (!idToken) {
      return reply.code(400).send({ error: 'idToken is required' });
    }

    const clientIds = getGoogleClientIds();
    if (!clientIds) {
      return reply.code(500).send({ error: 'Google OAuth not configured' });
    }

    try {
      const payload = await verifyGoogleToken(idToken, clientIds.all);

      if (!payload.email) {
        app.log.error('Google Sign-In: No email in token payload');
        return reply.code(400).send({ error: 'No email found in Google account' });
      }

      const domainCheck = validateEmailDomain(payload.email);
      if (!domainCheck.valid) {
        app.log.warn(
          `Google Sign-In: Email domain not allowed - email: ${payload.email}, allowedDomain: ${process.env.ALLOWED_EMAIL_DOMAIN || '@iiitnr.edu.in'}`,
        );
        return reply.code(403).send({ error: domainCheck.error });
      }

      const verificationCheck = validateEmailVerification(payload.email_verified);
      if (!verificationCheck.valid) {
        return reply.code(400).send({ error: verificationCheck.error });
      }

      const name = payload.name || null;
      const imageUrl = payload.imageUrl || null;
      const user = await findOrCreateGoogleUser(payload.sub, payload.email, name, imageUrl);

      const token = app.jwt.sign({ sub: user.id, role: user.role }, { expiresIn: '1d' });

      return reply.send({
        user: {
          id: user.id,
          email: user.email,
          name: user.name,
          imageUrl: user.imageUrl,
          role: user.role,
        },
        token,
      });
    } catch (err: unknown) {
      app.log.error({ err }, 'Google Sign-In error');
      const { statusCode, error } = handleGoogleAuthError(err);
      return reply.code(statusCode).send({ error });
    }
  });
};

export default authRoutes;
