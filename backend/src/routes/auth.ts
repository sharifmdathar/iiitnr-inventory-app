import type { FastifyPluginAsync } from 'fastify';
import { compare, hash } from 'bcryptjs';
import { OAuth2Client } from 'google-auth-library';
import { eq } from 'drizzle-orm';
import { db } from '../drizzle/db.js';
import { user } from '../drizzle/schema.js';
import { UserRole } from '../utils/enums.js';

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
  const existingUser = await db.query.user.findFirst({
    columns: { id: true, email: true, name: true, imageUrl: true, role: true, googleId: true },
    where: (u, { eq, or }) => or(eq(u.googleId, googleId), eq(u.email, email)),
  });

  if (!existingUser) {
    const now = new Date().toISOString();
    const [created] = await db
      .insert(user)
      .values({
        id: crypto.randomUUID(),
        email,
        name,
        imageUrl,
        googleId,
        role: UserRole.STUDENT,
        createdAt: now,
        updatedAt: now,
      })
      .returning({
        id: user.id,
        email: user.email,
        name: user.name,
        imageUrl: user.imageUrl,
        role: user.role,
        googleId: user.googleId,
      });
    return created!;
  }

  const shouldUpdateProfile =
    !existingUser.googleId ||
    (name != null && existingUser.name !== name) ||
    (imageUrl != null && existingUser.imageUrl !== imageUrl);

  if (shouldUpdateProfile) {
    const [updated] = await db
      .update(user)
      .set({
        googleId: existingUser.googleId || googleId,
        name: name ?? existingUser.name,
        imageUrl: imageUrl ?? existingUser.imageUrl,
      })
      .where(eq(user.id, existingUser.id))
      .returning({
        id: user.id,
        email: user.email,
        name: user.name,
        imageUrl: user.imageUrl,
        role: user.role,
        googleId: user.googleId,
      });
    return updated!;
  }

  return existingUser;
}

function handleGoogleAuthError(err: unknown): { statusCode: number; error: string } {
  if (
    err instanceof Error &&
    (err.message.includes('audience') ||
      (err as Error & { code?: string }).code === 'auth/id-token-audience-mismatch')
  ) {
    return {
      statusCode: 400,
      error: 'Google Sign-In failed: client ID mismatch. Contact the administrator.',
    };
  }

  return { statusCode: 400, error: 'Google Sign-In failed. Please try again.' };
}

const registerSchema = {
  body: {
    type: 'object',
    required: ['email', 'password'],
    properties: {
      email: { type: 'string', maxLength: 254 },
      password: { type: 'string', maxLength: 128 },
      name: { type: 'string', maxLength: 100 },
    },
    additionalProperties: false,
  },
} as const;

const loginSchema = {
  body: {
    type: 'object',
    required: ['email', 'password'],
    properties: {
      email: { type: 'string', maxLength: 254 },
      password: { type: 'string', maxLength: 128 },
    },
    additionalProperties: false,
  },
} as const;

const googleSchema = {
  body: {
    type: 'object',
    required: ['idToken'],
    properties: {
      idToken: { type: 'string', maxLength: 4096 },
    },
    additionalProperties: false,
  },
} as const;

const authRoutes: FastifyPluginAsync = async (app) => {
  app.post(
    '/register',
    {
      schema: registerSchema,
      config: {
        rateLimit: {
          max: 15,
          timeWindow: '1 minute',
        },
      },
    },
    async (request, reply) => {
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
        const now = new Date().toISOString();
        const [created] = await db
          .insert(user)
          .values({
            id: crypto.randomUUID(),
            email,
            name,
            passwordHash,
            role: UserRole.PENDING,
            createdAt: now,
            updatedAt: now,
          })
          .returning({
            id: user.id,
            email: user.email,
            name: user.name,
            imageUrl: user.imageUrl,
            role: user.role,
            createdAt: user.createdAt,
          });

        if (!created) {
          return reply.code(500).send({ error: 'failed to create user' });
        }

        const token = app.jwt.sign({ sub: created.id, role: created.role });

        return reply.code(201).send({ user: created, token });
      } catch (err) {
        app.log.error(err);
        return reply.code(400).send({ error: 'email already in use' });
      }
    },
  );

  app.post(
    '/login',
    {
      schema: loginSchema,
      config: {
        rateLimit: {
          max: 10,
          timeWindow: '1 minute',
        },
      },
    },
    async (request, reply) => {
      const body = request.body as { email?: string; password?: string };
      const email = body?.email?.trim();
      const password = body?.password;

      if (!email || !password) {
        return reply.code(400).send({ error: 'email and password are required' });
      }

      const found = await db.query.user.findFirst({
        columns: {
          id: true,
          email: true,
          name: true,
          imageUrl: true,
          role: true,
          passwordHash: true,
        },
        where: (u, { eq }) => eq(u.email, email),
      });

      if (!found) {
        return reply.code(401).send({ error: 'invalid credentials' });
      }

      if (!found.passwordHash) {
        return reply.code(401).send({ error: 'this account uses Google Sign-In' });
      }

      const matches = await compare(password, found.passwordHash);
      if (!matches) {
        return reply.code(401).send({ error: 'invalid credentials' });
      }

      if (found.role === UserRole.PENDING) {
        return reply.code(403).send({ error: 'account pending approval by admin' });
      }

      const token = app.jwt.sign({ sub: found.id, role: found.role });

      return reply.send({
        user: {
          id: found.id,
          email: found.email,
          name: found.name,
          imageUrl: found.imageUrl,
          role: found.role,
        },
        token,
      });
    },
  );

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

      const found = await db.query.user.findFirst({
        columns: { id: true, email: true, name: true, imageUrl: true, role: true },
        where: (u, { eq }) => eq(u.id, userId),
      });

      if (!found) {
        return reply.code(401).send({ error: 'invalid token' });
      }

      return reply.send({ user: found });
    },
  );

  app.post(
    '/google',
    {
      schema: googleSchema,
      config: {
        rateLimit: {
          max: 100,
          timeWindow: '1 minute',
        },
      },
    },
    async (request, reply) => {
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

        const token = app.jwt.sign({ sub: user.id, role: user.role });

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
    },
  );
};

export default authRoutes;
