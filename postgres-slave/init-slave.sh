#!/bin/bash
# Simple slave initialization script
# File: postgres-slave/init-slave.sh

set -e

echo "Initializing PostgreSQL Slave..."

# Wait for master
until pg_isready -h postgres-master -p 5432 -U ${POSTGRES_USER}; do
    echo "Waiting for master..."
    sleep 2
done

# Stop any running postgres
pg_ctl -D ${PGDATA} stop -m fast || true

# Clean and setup replication
rm -rf ${PGDATA}/*

# Create base backup
PGPASSWORD=replicator_password pg_basebackup \
    -h postgres-master \
    -D ${PGDATA} \
    -U replicator \
    -v \
    -P \
    -R \
    -X stream

# Configure slave
cat >> ${PGDATA}/postgresql.conf <<EOF
hot_standby = on
max_standby_streaming_delay = 30s
wal_receiver_status_interval = 10s
hot_standby_feedback = on
EOF

echo "Slave initialization completed"