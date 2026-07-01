set windows-shell := ["cmd.exe", "/c"]

default: dev

install:
    cd backend && bun install

up-dep:
    cd backend && bun update --latest

image:
    cd backend && podman build -t test . && podman images test && podman run --env-file .env -p4000:4000 test

dev:
    cd backend && bun run dev

db-up:
    cd backend && podman compose -f compose.db.yaml --profile test up -d

db-down:
    cd backend && podman compose -f compose.db.yaml --profile test down

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

[unix]
desk:
    cd app && _JAVA_AWT_WM_NONREPARENTING=1 ./gradlew desktop:run

[windows]
desk:
    cd app && .\gradlew.bat desktop:run

[unix]
andro:
    cd app && ./gradlew assembleDebug

[windows]
andro:
    cd app && .\gradlew.bat assembleDebug

[unix]
lint:
    cd backend && bun run lint
    cd app && ./gradlew ktlintCheck

[windows]
lint:
    cd backend && bun run lint
    cd app && .\gradlew.bat ktlintCheck

[unix]
lint-fix:
    cd backend && bun run lint:fix
    cd app && ./gradlew ktlintFormat

[windows]
lint-fix:
    cd backend && bun run lint:fix
    cd app && .\gradlew.bat ktlintFormat

typecheck:
    cd backend && bun run typecheck

[unix]
fmt:
    cd backend && bun run format
    cd app && ./gradlew ktlintFormat

[windows]
fmt:
    cd backend && bun run format
    cd app && .\gradlew.bat ktlintFormat

[unix]
detekt:
    cd app && ./gradlew detekt

[windows]
detekt:
    cd app && .\gradlew.bat detekt


[unix]
web:
    cd app && ./gradlew :web:wasmJsBrowserDevelopmentRun

[windows]
web:
    cd app && ./gradlew :web:wasmJsBrowserDevelopmentRun