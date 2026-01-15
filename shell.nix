{ pkgs ? import <nixpkgs> {} }:

let
  unstable = import <nixpkgs-unstable> {};
in

pkgs.mkShell {
  packages = [
    pkgs.pnpm
    pkgs.nodejs-slim
    pkgs.openssl
    unstable.prisma-engines_7
    pkgs.podman
    pkgs.podman-compose
  ];
  shellHook = ''
    export PRISMA_QUERY_ENGINE_LIBRARY="${unstable.prisma-engines_7}/lib/libquery_engine.node"
    export PRISMA_SCHEMA_ENGINE_BINARY="${unstable.prisma-engines_7}/bin/schema-engine"
    export PRISMA_INTROSPECTION_ENGINE_BINARY="${unstable.prisma-engines_7}/bin/introspection-engine"
    export PRISMA_FMT_BINARY="${unstable.prisma-engines_7}/bin/prisma-fmt"
  '';
}