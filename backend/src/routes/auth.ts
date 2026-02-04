import type { FastifyPluginAsync } from 'fastify';
import { compare, hash } from 'bcryptjs';
import { OAuth2Client } from 'google-auth-library';
import { prisma } from '../lib/prisma.js';
import { UserRole } from '../utils/enums.js';
import type { UserRoleValue } from '../utils/enums.js';

const authRoutes: FastifyPluginAsync = async (app) => {
  app.post('/register', async (request, reply) => {
    const body = request.body as {
      email?: string;
      password?: string;
      name?: string;
      role?: UserRoleValue;
    };

    const email = body?.email?.trim();
    const password = body?.password;
    const name = body?.name?.trim();
    const role = body?.role;

    if (!email || !password) {
      return reply.code(400).send({ error: 'email and password are required' });
    }

    if (password.length < 8) {
      return reply.code(400).send({ error: 'password must be at least 8 characters' });
    }

    if (role === UserRole.ADMIN) {
      return reply.code(400).send({ error: 'invalid role' });
    }

    if (role && !Object.values(UserRole).includes(role)) {
      return reply.code(400).send({ error: 'invalid role' });
    }

    const passwordHash = await hash(password, 12);

    try {
      const user = await prisma.user.create({
        data: {
          email,
          name,
          passwordHash,
          role: role ?? UserRole.STUDENT,
        },
        select: {
          id: true,
          email: true,
          name: true,
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

    const primaryClientId = process.env.GOOGLE_CLIENT_ID;
    const extraClientIds = process.env.GOOGLE_CLIENT_EXTRA_IDS
      ? process.env.GOOGLE_CLIENT_EXTRA_IDS.split(',')
          .map((id) => id.trim())
          .filter(Boolean)
      : [];

    if (!primaryClientId) {
      return reply.code(500).send({ error: 'Google OAuth not configured' });
    }

    const allowedClientIds = [primaryClientId, ...extraClientIds];

    const client = new OAuth2Client(allowedClientIds[0]);

    try {
      const ticket = await client.verifyIdToken({
        idToken,
        audience: allowedClientIds as string[],
      });

      const payload = ticket.getPayload();
      if (!payload) {
        app.log.error('Google token verification returned null payload');
        return reply.code(401).send({ error: 'invalid Google token' });
      }

      const googleId = payload.sub;
      const email = payload.email;
      const name = payload.name || payload.given_name || null;
      const emailVerified = payload.email_verified;

      if (!email) {
        app.log.error('Google Sign-In: No email in token payload');
        return reply.code(400).send({ error: 'No email found in Google account' });
      }

      const allowedDomain = process.env.ALLOWED_EMAIL_DOMAIN || '@iiitnr.edu.in';
      if (!email.toLowerCase().endsWith(allowedDomain.toLowerCase())) {
        app.log.warn(
          `Google Sign-In: Email domain not allowed - email: ${email}, allowedDomain: ${allowedDomain}`,
        );
        return reply.code(403).send({
          error: `Only ${allowedDomain} email addresses are allowed. Your email (${email}) does not match the required domain.`,
        });
      }

      const allowUnverifiedEmail = process.env.ALLOW_UNVERIFIED_EMAIL === 'true';

      if (!emailVerified) {
        if (!allowUnverifiedEmail) {
          return reply.code(400).send({
            error:
              'Google account email is not verified. Please verify your email address in your Google account settings, or set ALLOW_UNVERIFIED_EMAIL=true in .env for development.',
          });
        }
      }

      let user = await prisma.user.findFirst({
        where: {
          OR: [{ googleId }, { email }],
        },
        select: {
          id: true,
          email: true,
          name: true,
          role: true,
          googleId: true,
        },
      });

      if (user) {
        if (!user.googleId || (name && user.name !== name)) {
          user = await prisma.user.update({
            where: { id: user.id },
            data: {
              googleId: user.googleId || googleId,
              name: name || user.name,
            },
            select: {
              id: true,
              email: true,
              name: true,
              role: true,
              googleId: true,
            },
          });
        }
      } else {
        user = await prisma.user.create({
          data: {
            email,
            name,
            googleId,
            role: UserRole.STUDENT,
          },
          select: {
            id: true,
            email: true,
            name: true,
            role: true,
            googleId: true,
          },
        });
      }

      const token = app.jwt.sign({ sub: user.id, role: user.role }, { expiresIn: '1d' });

      return reply.send({
        user: {
          id: user.id,
          email: user.email,
          name: user.name,
          role: user.role,
        },
        token,
      });
    } catch (err: unknown) {
      app.log.error({ err }, 'Google Sign-In error');

      type GoogleAuthError = Error & { code?: string };

      if (err instanceof Error) {
        const { message } = err;
        const code = (err as GoogleAuthError).code;

        if (message.includes('audience') || code === 'auth/id-token-audience-mismatch') {
          return reply.code(400).send({
            error:
              'Token audience mismatch. The Web application Client ID in the backend must match the serverClientId used in the app.',
          });
        }

        const errorMessage = message || String(err) || 'unknown error';
        return reply.code(400).send({ error: `Invalid Google token: ${errorMessage}` });
      }

      const fallbackMessage = String(err) || 'unknown error';
      return reply.code(400).send({ error: `Invalid Google token: ${fallbackMessage}` });
    }
  });
};

export default authRoutes;
