FROM docker.io/oven/bun:1.3.12-alpine AS base
WORKDIR /app

FROM base AS builder
# Install deps (all — needed for typecheck)
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

# Copy source and typecheck
COPY . .
RUN bun run typecheck

# Prune to production deps only (no reinstall — just filter in place)
RUN bun install --frozen-lockfile --production

FROM base AS runner
RUN apk add --no-cache openssl ca-certificates libgcc libstdc++

WORKDIR /app
RUN mkdir -p /app/uploads/images

COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/src ./src
COPY --from=builder /app/scripts ./scripts
COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/docker-entrypoint.sh ./docker-entrypoint.sh

EXPOSE 4000
ENTRYPOINT ["./docker-entrypoint.sh"]