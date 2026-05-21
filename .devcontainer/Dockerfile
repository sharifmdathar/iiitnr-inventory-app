FROM docker.io/library/alpine:3.23.4

ENV BUN_INSTALL=/root/.bun
ENV PATH="$BUN_INSTALL/bin:$PATH"

RUN apk add --no-cache \
        git just curl bash gnupg libstdc++ libgcc docker-cli docker-compose\
    && curl -fsSL https://bun.sh/install | bash \
    && curl -o /etc/apk/keys/adoptium.rsa.pub \
        https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk \
    && echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories \
    && apk add --no-cache temurin-25-jdk