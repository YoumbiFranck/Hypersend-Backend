-- Database initialization script
-- This script runs automatically when PostgreSQL container starts

-- Create database if it doesn't exist
SELECT 'CREATE DATABASE hypersend'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hypersend')\gexec

-- Connect to the hypersend database
    \c hypersend;

-- Create sequence for user IDs with Hibernate default increment
CREATE SEQUENCE IF NOT EXISTS user_id_sequence
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create app_user table
CREATE TABLE IF NOT EXISTS app_user (
                                        id INTEGER PRIMARY KEY DEFAULT nextval('user_id_sequence'),
    user_name VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    account_non_expired BOOLEAN DEFAULT true,
    account_non_locked BOOLEAN DEFAULT true,
    credentials_non_expired BOOLEAN DEFAULT true,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user(email);
CREATE INDEX IF NOT EXISTS idx_app_user_username ON app_user(user_name);

-- Grant permissions to the application user
GRANT ALL PRIVILEGES ON TABLE app_user TO hypersend_user;
GRANT ALL PRIVILEGES ON SEQUENCE user_id_sequence TO hypersend_user;

CREATE TABLE IF NOT EXISTS messages (
                                        id BIGSERIAL PRIMARY KEY,
                                        sender_id INTEGER NOT NULL,
                                        receiver_id INTEGER NOT NULL,
                                        content TEXT NOT NULL,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (sender_id) REFERENCES app_user(id),
    FOREIGN KEY (receiver_id) REFERENCES app_user(id)
    );

CREATE INDEX IF NOT EXISTS idx_messages_sender_receiver
    ON messages(sender_id, receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at
    ON messages(created_at DESC);