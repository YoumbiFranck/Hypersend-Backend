-- Script d'initialisation pour la réplication PostgreSQL
-- Fichier: init/04-setup-replication.sql

\c hypersend;

-- Créer l'utilisateur de réplication
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'replicator') THEN

CREATE ROLE replicator WITH REPLICATION PASSWORD 'replicator_password' LOGIN;
END IF;
END
$do$;

-- Accorder les privilèges nécessaires
GRANT CONNECT ON DATABASE hypersend TO replicator;
GRANT USAGE ON SCHEMA public TO replicator;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO replicator;

-- Créer un slot de réplication pour le Slave (seulement si il n'existe pas)
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT 1 FROM pg_replication_slots WHERE slot_name = 'slave_slot'
   ) THEN
      PERFORM pg_create_physical_replication_slot('slave_slot');
END IF;
END
$do$;

-- Vérification de la configuration
SHOW wal_level;
SHOW max_wal_senders;
SHOW max_replication_slots;

-- Logs pour vérification
INSERT INTO app_user (user_name, email, password, enabled)
VALUES ('replication_test', 'replication@test.com', 'test', true)
    ON CONFLICT (email) DO NOTHING;

SELECT 'Replication setup completed' AS status;