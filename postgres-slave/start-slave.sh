#!/bin/bash
# External script for slave initialization
# File: postgres-slave/start-slave.sh

set -e

echo "Starting PostgreSQL Slave..."

# Wait for master
until pg_isready -h postgres-master -p 5432 -U ${POSTGRES_USER}; do
    echo "Waiting for master..."
    sleep 2
done

# Initialize slave if needed
if [ ! -f /data/postgres-slave/PG_VERSION ]; then
    echo "Initializing slave via pg_basebackup..."

    # Clean directory
    rm -rf /data/postgres-slave/*

    # Create backup from master
    PGPASSWORD=replicator_password pg_basebackup \
        -h postgres-master \
        -D /data/postgres-slave \
        -U replicator \
        -R \
        -P \
        -X stream

    echo "Slave initialization completed"
fi

# Start PostgreSQL
exec docker-entrypoint.sh postgres