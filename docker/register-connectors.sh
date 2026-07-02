#!/usr/bin/env bash
# Debezium Connect ayakta geldikten sonra connectors/ ve debezium/ altındaki
# connector config'lerini Kafka Connect REST API'sine kaydeder.
# Kullanım: docker compose up -d && ./register-connectors.sh
set -euo pipefail

CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

register() {
    local config_file="$1"
    local name
    name=$(basename "$config_file" .json)

    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" "$CONNECT_URL/connectors/$name")
    if [ "$status" = "200" ]; then
        echo "Connector '$name' zaten kayıtlı, atlanıyor."
        return
    fi

    echo "Connector '$name' kaydediliyor..."
    curl -sf -X POST -H "Content-Type: application/json" \
        --data @"$config_file" "$CONNECT_URL/connectors" > /dev/null
    echo "Connector '$name' kaydedildi."
}

for config_file in "$SCRIPT_DIR"/connectors/*.json "$SCRIPT_DIR"/debezium/*.json; do
    [ -f "$config_file" ] || continue
    register "$config_file"
done
