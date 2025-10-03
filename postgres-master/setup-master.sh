#!/bin/bash
set -e

echo "Configuration du PostgreSQL Master..."

cat >> ${PGDATA}/postgresql.conf <<EOF

# Configuration pour Master-Slave Replication
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
synchronous_commit = on
archive_mode = on
archive_command = 'test ! -f /var/lib/postgresql/archive/%f && cp %p /var/lib/postgresql/archive/%f'

# Connection settings
listen_addresses = '*'
max_connections = 100
EOF

cat >> ${PGDATA}/pg_hba.conf <<EOF

# Configuration pour la réplication
host    replication     replicator      all                     md5
host    all             all             172.0.0.0/8             md5
EOF

mkdir -p /var/lib/postgresql/archive
chown postgres:postgres /var/lib/postgresql/archive

echo "Configuration Master finished.."