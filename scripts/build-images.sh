#!/usr/bin/env bash
# Build Docker images for backend and frontend (does not start containers).
# Usage (from repo root):
#   ./scripts/build-images.sh
#   TAG=v1.0.0 ./scripts/build-images.sh
#   ./scripts/build-images.sh --tag v1.0.0 --platform linux/amd64
#   ./scripts/build-images.sh --registry ghcr.io/myorg --tag v1.0.0 --push
#   ./scripts/build-images.sh --no-cache
#
# To run after local build:
#   docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

log() { printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

need() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

usage() {
  cat <<'EOF'
Usage: ./scripts/build-images.sh [options]

Build e-wallet backend and frontend Docker images (no containers started).

Options:
  --tag <value>        Image tag (default: latest; override with TAG=)
  --platform <value>   Target platform(s), e.g. linux/amd64 or
                       linux/amd64,linux/arm64 (override with PLATFORM=)
  --registry <value>   Registry prefix, e.g. ghcr.io/myorg (override with REGISTRY=)
                       Images become <registry>/e-wallet-backend:<tag>
  --push               Push images to the registry (requires --registry)
  --no-cache           Pass --no-cache to the build
  -h, --help           Show this help

Environment:
  TAG, PLATFORM, REGISTRY, PUSH=1   Same as the flags above

Examples:
  ./scripts/build-images.sh --platform linux/amd64
  ./scripts/build-images.sh --registry ghcr.io/myorg --tag v1.0.0 --push
  PLATFORM=linux/amd64,linux/arm64 REGISTRY=ghcr.io/myorg ./scripts/build-images.sh --push
EOF
}

NO_CACHE=0
TAG="${TAG:-latest}"
PLATFORM="${PLATFORM:-}"
REGISTRY="${REGISTRY:-}"

# Allow PUSH=1 / PUSH=true from env before flags may set it
case "${PUSH:-0}" in
  1|true|TRUE|yes|YES) PUSH=1 ;;
  *) PUSH=0 ;;
esac

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-cache)
      NO_CACHE=1
      shift
      ;;
    --push)
      PUSH=1
      shift
      ;;
    --tag)
      [[ $# -ge 2 ]] || die "--tag requires a value"
      TAG="$2"
      shift 2
      ;;
    --platform)
      [[ $# -ge 2 ]] || die "--platform requires a value"
      PLATFORM="$2"
      shift 2
      ;;
    --registry)
      [[ $# -ge 2 ]] || die "--registry requires a value"
      REGISTRY="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1 (try --help)"
      ;;
  esac
done

[[ -n "$TAG" ]] || die "TAG must not be empty"

# Normalize registry: strip trailing slashes
REGISTRY="${REGISTRY%/}"

if [[ "$PUSH" -eq 1 && -z "$REGISTRY" ]]; then
  die "--push requires --registry (e.g. --registry ghcr.io/myorg)"
fi

# Multi-platform images cannot be loaded into a local Docker engine
if [[ "$PLATFORM" == *","* && "$PUSH" -eq 0 ]]; then
  die "Multi-platform builds (--platform with commas) require --push and --registry"
fi

need docker
docker compose version >/dev/null 2>&1 || die "docker compose plugin is required"

USE_BUILDX=0
if [[ -n "$PLATFORM" || -n "$REGISTRY" || "$PUSH" -eq 1 ]]; then
  USE_BUILDX=1
  docker buildx version >/dev/null 2>&1 || die "docker buildx is required for --platform / --registry / --push"
fi

if [[ ! -f .env.properties ]]; then
  [[ -f .env.example ]] || die "Missing .env.properties and .env.example"
  cp .env.example .env.properties
  log "Created .env.properties from .env.example — edit secrets before production use"
fi

# Compose interpolates ${db_*} from `.env` (not from env_file). Keep them in sync.
ln -sfn .env.properties .env

BACKEND_NAME="e-wallet-backend"
FRONTEND_NAME="e-wallet-frontend"

image_ref() {
  local name="$1"
  if [[ -n "$REGISTRY" ]]; then
    printf '%s/%s:%s' "$REGISTRY" "$name" "$TAG"
  else
    printf '%s:%s' "$name" "$TAG"
  fi
}

ensure_buildx() {
  if ! docker buildx inspect e-wallet-builder >/dev/null 2>&1; then
    log "Creating buildx builder 'e-wallet-builder'..."
    docker buildx create --name e-wallet-builder --driver docker-container --use >/dev/null
  else
    docker buildx use e-wallet-builder >/dev/null
  fi
  docker buildx inspect --bootstrap >/dev/null
}

build_one() {
  local context="$1"
  local name="$2"
  local ref
  ref="$(image_ref "$name")"

  local -a args=(buildx build)
  args+=(-f "${context}/Dockerfile")
  args+=(-t "$ref")

  # Also tag :latest when TAG is not latest (local or registry)
  if [[ "$TAG" != "latest" ]]; then
    if [[ -n "$REGISTRY" ]]; then
      args+=(-t "${REGISTRY}/${name}:latest")
    else
      args+=(-t "${name}:latest")
    fi
  fi

  if [[ -n "$PLATFORM" ]]; then
    args+=(--platform "$PLATFORM")
  fi
  if [[ "$NO_CACHE" -eq 1 ]]; then
    args+=(--no-cache)
  fi
  if [[ "$PUSH" -eq 1 ]]; then
    args+=(--push)
  else
    args+=(--load)
  fi
  args+=("$context")

  log "Building ${ref}${PLATFORM:+ (platform=${PLATFORM})}..."
  docker "${args[@]}"
}

if [[ "$USE_BUILDX" -eq 1 ]]; then
  ensure_buildx
  build_one backend "$BACKEND_NAME"
  build_one frontend "$FRONTEND_NAME"
else
  # Simple local path: docker compose build (same as before)
  COMPOSE=(
    docker compose
    --env-file .env.properties
    -f docker-compose.yml
    -f docker-compose.prod.yml
  )

  BUILD_ARGS=(build backend frontend)
  if [[ "$NO_CACHE" -eq 1 ]]; then
    BUILD_ARGS=(build --no-cache backend frontend)
  fi

  log "Building images (backend, frontend)..."
  "${COMPOSE[@]}" "${BUILD_ARGS[@]}"

  if [[ "$TAG" != "latest" ]]; then
    log "Tagging images as :${TAG} (and keeping :latest)"
    docker tag "${BACKEND_NAME}:latest" "${BACKEND_NAME}:${TAG}"
    docker tag "${FRONTEND_NAME}:latest" "${FRONTEND_NAME}:${TAG}"
  else
    if ! docker image inspect "${BACKEND_NAME}:latest" >/dev/null 2>&1; then
      BACKEND_ID="$(docker images -q "${BACKEND_NAME}" | head -n1)"
      FRONTEND_ID="$(docker images -q "${FRONTEND_NAME}" | head -n1)"
      [[ -n "$BACKEND_ID" ]] || die "Built image not found: ${BACKEND_NAME}"
      [[ -n "$FRONTEND_ID" ]] || die "Built image not found: ${FRONTEND_NAME}"
      docker tag "$BACKEND_ID" "${BACKEND_NAME}:latest"
      docker tag "$FRONTEND_ID" "${FRONTEND_NAME}:latest"
    fi
  fi
fi

log "Images:"
if [[ -n "$REGISTRY" ]]; then
  docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}\t{{.CreatedSince}}' \
    | awk -v reg="$REGISTRY" 'NR==1 || index($1, reg "/") == 1'
  if [[ "$PUSH" -eq 1 ]]; then
    log "Pushed:"
    printf '  %s\n' "$(image_ref "$BACKEND_NAME")"
    printf '  %s\n' "$(image_ref "$FRONTEND_NAME")"
  fi
else
  docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}\t{{.CreatedSince}}' \
    | awk 'NR==1 || $1 ~ /^e-wallet-(backend|frontend)$/'
  log "Done. Start with:"
  printf '  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d\n'
fi
