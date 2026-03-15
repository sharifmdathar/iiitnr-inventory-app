import { buildApp } from './app.js';
import { prisma } from './lib/prisma.js';

const app = await buildApp();
const port = Number(process.env.PORT ?? 4000);

try {
  await prisma.$queryRaw`SELECT 1`;
  app.log.info('Database connection OK');
} catch {
  app.log.error('DB connection failed, check your connection string');
  process.exit(1);
}

try {
  const rows = await prisma.$queryRaw<
    { n: number }[]
  >`SELECT 1 AS n FROM _prisma_migrations WHERE finished_at IS NOT NULL LIMIT 1`;
  if (rows.length === 0) {
    app.log.error('No migrations applied. Run: bunx prisma migrate deploy');
    process.exit(1);
  }
  app.log.info('Migrations applied');
} catch {
  app.log.error(
    'Migrations not applied or migration table missing. Run: bunx prisma migrate deploy',
  );
  process.exit(1);
}

try {
  await app.listen({ port, host: '0.0.0.0' });
} catch (err) {
  app.log.error(err);
  process.exit(1);
}

const signals = ['SIGINT', 'SIGTERM'];
for (const signal of signals) {
  process.on(signal, async () => {
    app.log.info(`${signal} received, shutting down gracefully...`);
    try {
      await app.close();
      app.log.info('Server closed successfully.');
      process.exit(0);
    } catch (err) {
      app.log.error(err, 'Error during graceful shutdown');
      process.exit(1);
    }
  });
}
