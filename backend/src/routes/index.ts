import type { FastifyPluginAsync } from 'fastify';
import authRoutes from './auth.js';
import componentsRoutes from './components.js';
import requestsRoutes from './requests.js';

const routes: FastifyPluginAsync = async (app) => {
  await app.register(authRoutes, { prefix: '/auth' });
  await app.register(componentsRoutes, { prefix: '/components' });
  await app.register(requestsRoutes);
};

export default routes;
