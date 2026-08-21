#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/srv/backups/inventory}"
REMOTE="${REMOTE:-gdrive:inventory-backups}"
LOCAL_RETENTION_DAYS="${LOCAL_RETENTION_DAYS:-14}"
REMOTE_RETENTION_DAYS="${REMOTE_RETENTION_DAYS:-90d}"

PG_USER="${PG_USER:-iiitnr}"
PG_DATABASE="${PG_DATABASE:-iiitnr_inventory}"

CONTAINER="${CONTAINER:-$(podman ps --format '{{.Names}} {{.Image}}' | awk '$2 ~ /postgres/ {print $1; exit}')}"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

if [[ -z "${CONTAINER}" ]]; then
    log "ERROR: no running postgres container found. Set CONTAINER=<name> explicitly."
    exit 1
fi

mkdir -p "$BACKUP_DIR"

STAMP="$(date +%Y%m%d_%H%M%S)"
FILE="$BACKUP_DIR/inventory_${STAMP}.sql.gz"

# Remove the dump on any failure so a truncated file is never uploaded or kept.
cleanup() {
    local rc=$?
    if [[ $rc -ne 0 && -f "$FILE" ]]; then
        log "ERROR: backup failed (exit $rc), removing partial file $FILE"
        rm -f "$FILE"
    fi
}
trap cleanup EXIT

log "Starting backup of db '$PG_DATABASE' from container '$CONTAINER'"

podman exec "$CONTAINER" pg_dump -U "$PG_USER" -d "$PG_DATABASE" | gzip > "$FILE"

if [[ ! -s "$FILE" ]]; then
    log "ERROR: dump file $FILE is empty or missing — aborting before upload."
    exit 1
fi

SIZE="$(du -h "$FILE" | cut -f1)"
log "Dump created: $FILE ($SIZE)"

log "Uploading to $REMOTE"
rclone copy "$FILE" "$REMOTE" --transfers 1

log "Pruning local backups older than ${LOCAL_RETENTION_DAYS} days"
find "$BACKUP_DIR" -name 'inventory_*.sql.gz' -mtime +"$LOCAL_RETENTION_DAYS" -delete

log "Pruning remote backups older than ${REMOTE_RETENTION_DAYS}"
rclone delete "$REMOTE" --min-age "$REMOTE_RETENTION_DAYS"

log "Backup complete."
