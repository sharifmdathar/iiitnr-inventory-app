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

export const requireAdminOrLA = async (request: FastifyRequest, reply: FastifyReply) => {
  await request.jwtVerify();
  const userRole = (request.user as { role?: UserRoleValue })?.role;
  if (userRole !== UserRole.ADMIN && userRole !== UserRole.LA) {
    return reply.code(403).send({ error: 'forbidden: admin or LA role required' });
  }
};

export const isAdminOrLA = (role?: UserRoleValue): boolean =>
  role === UserRole.ADMIN || role === UserRole.LA;

export const requireAdmin = async (request: FastifyRequest, reply: FastifyReply) => {
  await request.jwtVerify();
  const userRole = (request.user as { role?: UserRoleValue })?.role;
  if (userRole !== UserRole.ADMIN) {
    return reply.code(403).send({ error: 'forbidden: admin role required' });
  }
};
