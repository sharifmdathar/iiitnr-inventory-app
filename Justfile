default: dev

install:
    cd backend && bun install

dev:
    cd backend && bun run dev

test:
    cd backend && podman compose --profile test up -d
    cd backend && bun test
    cd backend && podman compose --profile test down

lint:
    cd backend && bun run lint
    cd app && ./gradlew ktlintCheck

lint-fix:
    cd backend && bun run lint:fix
    cd app && ./gradlew ktlintFormat

typecheck:
    cd backend && bun run typecheck

fmt:
    cd backend && bun run format
    cd app && ./gradlew ktlintFormat

detekt:
    cd app && ./gradlew detekt