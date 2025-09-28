#!/bin/bash
# Fixed script for PostgreSQL Slave configuration
# File: postgres-slave/setup-slave.sh

set -e

echo "Configuring PostgreSQL Slave..."

# Wait for Master to be ready
until pg_isready -h postgres-master -p 5432 -U ${POSTGRES_USER}; do
    echo "Waiting for PostgreSQL Master..."
    sleep 2
done

echo "PostgreSQL Master detected"

# Check if this is the first initialization
if [ ! -f ${PGDATA}/PG_VERSION ]; then
    echo "Initializing Slave via pg_basebackup..."

    # Clean the data directory completely
    rm -rf ${PGDATA}/*

    # Create backup from Master using replicator user
    PGPASSWORD=replicator_password pg_basebackup \
        -h postgres-master \
        -D ${PGDATA} \
        -U replicator \
        -v \
        -P \
        -R \
        -X stream \
        -C \
        -S slave_slot

    echo "Master backup completed"

    # Slave-specific configuration
    cat >> ${PGDATA}/postgresql.conf <<EOF

# Slave Configuration
hot_standby = on
max_standby_streaming_delay = 30s
max_standby_archive_delay = 30s
wal_receiver_status_interval = 10s
hot_standby_feedback = on

# Logging for debugging
log_min_messages = info
log_line_prefix = '[SLAVE] %t %u@%d '

EOF

    # Configure connection to Master
    cat > ${PGDATA}/postgresql.auto.conf <<EOF
# Automatic configuration for replication
primary_conninfo = 'host=postgres-master port=5432 user=replicator password=replicator_password application_name=slave1'
primary_slot_name = 'slave_slot'
EOF

    # Ensure standby.signal file exists
    touch ${PGDATA}/standby.signal

else
    echo "Slave database already initialized"
fi

echo "Slave configuration completed"