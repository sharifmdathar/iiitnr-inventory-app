import type { FastifyReply, FastifyRequest } from 'fastify';
import { UserRole } from '../utils/enums.js';
import type { UserRoleValue } from '../utils/enums.js';

export const requireAuth = async (request: FastifyRequest, reply: FastifyReply) => {
  await request.jwtVerify();
  const userRole = (request.user as { role?: UserRoleValue })?.role;
  if (userRole === UserRole.PENDING) {
    return reply.code(403).send({ error: 'forbidden: pending user must be verified' });
  }
};

export const requireAdminOrTA = async (request: FastifyRequest, reply: FastifyReply) => {
  await request.jwtVerify();
  const userRole = (request.user as { role?: UserRoleValue })?.role;
  if (userRole !== UserRole.ADMIN && userRole !== UserRole.TA) {
    return reply.code(403).send({ error: 'forbidden: admin or TA role required' });
  }
};

export const isAdminOrTA = (role?: UserRoleValue): boolean =>
  role === UserRole.ADMIN || role === UserRole.TA;
