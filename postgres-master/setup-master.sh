#!/bin/bash
# Script de configuration pour PostgreSQL Master
# Fichier: postgres-master/setup-master.sh

set -e

echo "Configuration du PostgreSQL Master..."

# Configuration PostgreSQL pour la réplication
cat >> ${PGDATA}/postgresql.conf <<EOF

# Configuration pour Master-Slave Replication
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
synchronous_commit = on
archive_mode = on
archive_command = 'test ! -f /var/lib/postgresql/archive/%f && cp %p /var/lib/postgresql/archive/%f'

# Performance tuning
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 64MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100

# Logging
log_destination = 'stderr'
logging_collector = on
log_directory = 'pg_log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_statement = 'mod'
log_min_duration_statement = 1000

# Connection settings
listen_addresses = '*'
max_connections = 100

EOF

# Configuration de l'authentification pour la réplication
cat >> ${PGDATA}/pg_hba.conf <<EOF

# Configuration pour la réplication
host    replication     replicator      all                     md5
host    all             all             172.0.0.0/8             md5

EOF

# Créer le répertoire d'archive
mkdir -p /var/lib/postgresql/archive
chown postgres:postgres /var/lib/postgresql/archive

echo "Configuration Master terminée"