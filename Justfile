set windows-shell := ["cmd.exe", "/c"]

default: dev

install:
    cd backend && bun install

up-dep:
    cd backend && bun update --latest && bun install && just test

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
    cd backend && bun test --parallel=1
    cd backend && podman compose -f compose.db.yaml --profile test down

[unix]
desk:
    cd app && _JAVA_AWT_WM_NONREPARENTING=1 ./gradlew desktop:run -PappDebug=true

[windows]
desk:
    cd app && .\gradlew.bat desktop:run -PappDebug=true

[unix]
andro:
    cd app && ./gradlew assembleDebug -PappDebug=true

[windows]
andro:
    cd app && .\gradlew.bat assembleDebug -PappDebug=true

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
    cd app && ./gradlew :web:wasmJsBrowserDevelopmentRun -PappDebug=true

[windows]
web:
    cd app && ./gradlew :web:wasmJsBrowserDevelopmentRun -PappDebug=true

[unix]
deploy-web:
    export GOOGLE_WEB_CLIENT_ID="$(grep GOOGLE_WEB_CLIENT_ID backend/.env | cut -d= -f2)"
    cd app && ./gradlew :web:wasmJsBrowserDistribution -Pkotlin.native.ignoreDisabledTargets=true
    mkdir -p /srv/web
    cp -r app/web/build/dist/wasmJs/productionExecutable/. /srv/web/
    echo "Web app deployed to /srv/web"

deploy:
    git pull
    just deploy-web
    podman compose -f backend/compose.prod.yaml up --build -d