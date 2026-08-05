FROM docker.io/oven/bun:1.3.12-alpine AS base
WORKDIR /app

FROM base AS builder
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile
COPY . .
RUN bun run typecheck

RUN rm -rf node_modules && bun install --frozen-lockfile --production

FROM base AS runner
RUN apk add --no-cache openssl ca-certificates libgcc libstdc++

WORKDIR /app
RUN mkdir -p /app/tmp /app/uploads/images

COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/src ./src
COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/docker-entrypoint.sh ./docker-entrypoint.sh
COPY --from=builder /app/src/drizzle ./drizzle

RUN chown -R bun:bun /app
USER bun
EXPOSE 4000
ENTRYPOINT ["./docker-entrypoint.sh"]