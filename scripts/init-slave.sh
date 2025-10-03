#!/bin/bash

set -e

echo "Initializing PostgreSQL Slave..."

if [ "$POSTGRES_INITDB_SKIP" != "true" ]; then
    echo "Stopping standard PostgreSQL initialization..."
    echo "Setting up slave from master..."

    until pg_isready -h postgres-master -p 5432 -U "$POSTGRES_USER"; do
        echo "Waiting for master PostgreSQL..."
        sleep 2
    done

    echo "Master is ready. Starting pg_basebackup..."

    rm -rf "$PGDATA"/*

    PGPASSWORD=replicator_password pg_basebackup \
        -h postgres-master \
        -D "$PGDATA" \
        -U replicator \
        -v \
        -P \
        -W \
        -R

    echo "pg_basebackup completed successfully!"

    cat >> "$PGDATA/postgresql.conf" <<EOF

# Slave configuration
hot_standby = on
log_line_prefix = '[SLAVE] %t %u@%d '
EOF

    export POSTGRES_INITDB_SKIP=true
fi

echo "Slave initialization completed!"