#!/bin/bash
# Script: scripts/init-slave.sh
# Ne s'exécute que si c'est une nouvelle initialisation

set -e

echo "Initializing PostgreSQL Slave..."

# Ce script s'exécute AVANT l'initialisation standard de PostgreSQL
# Il doit empêcher l'initialisation normale et faire le pg_basebackup

# Vérifier si nous devons créer le slave
if [ "$POSTGRES_INITDB_SKIP" != "true" ]; then
    echo "Stopping standard PostgreSQL initialization..."
    echo "Setting up slave from master..."

    # Attendre que le Master soit prêt
    until pg_isready -h postgres-master -p 5432 -U "$POSTGRES_USER"; do
        echo "Waiting for master PostgreSQL..."
        sleep 2
    done

    echo "Master is ready. Starting pg_basebackup..."

    # Vider le répertoire de données
    rm -rf "$PGDATA"/*

    # Faire le backup depuis le Master
    PGPASSWORD=replicator_password pg_basebackup \
        -h postgres-master \
        -D "$PGDATA" \
        -U replicator \
        -v \
        -P \
        -W \
        -R

    echo "pg_basebackup completed successfully!"

    # Configuration du Slave
    cat >> "$PGDATA/postgresql.conf" <<EOF

# Slave configuration
hot_standby = on
log_line_prefix = '[SLAVE] %t %u@%d '
EOF

    # Marquer comme initialisé
    export POSTGRES_INITDB_SKIP=true
fi

echo "Slave initialization completed!"