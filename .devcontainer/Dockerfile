FROM docker.io/library/alpine:latest

ENV BUN_INSTALL=/root/.bun
ENV PATH="$BUN_INSTALL/bin:$PATH"

RUN apk add --no-cache git just curl bash gnupg libstdc++ libgcc \
	&& curl -fsSL https://bun.com/install | bash \
	&& apk add --no-cache docker-cli docker-cli-buildx docker-cli-compose \
	&& wget -O /etc/apk/keys/adoptium.rsa.pub https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk \
	&& echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories \
	&& apk add temurin-21-jdk