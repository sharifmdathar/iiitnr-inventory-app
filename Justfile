default: dev

install:
    cd backend && bun install

image:
    cd backend && podman build -t test . && podman images test && podman run --env-file .env -p4000:4000 test

dev:
    cd backend && bun run dev

up:
    cd backend && podman compose up -d

down:
    cd backend && podman compose down

restart:
    cd backend && podman compose down
    cd backend && podman compose up -d

logs:
    cd backend && podman compose logs -f

test:
    cd backend && podman compose -f compose.db.yaml --profile test up -d
    cd backend && bun test
    cd backend && podman compose -f compose.db.yaml --profile test down

desk:
    cd app && ./gradlew desktop:run

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
