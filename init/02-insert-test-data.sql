-- Insert test data for development
-- This script runs after database initialization

\c hypersend;

-- Insert test users (passwords are hashed with BCrypt)
-- Default password for all test users: "password123"
INSERT INTO app_user (user_name, email, password, enabled, account_non_expired, account_non_locked, credentials_non_expired)
VALUES
    ('admin', 'admin@hypersend.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', true, true, true, true),
    ('john_doe', 'john.doe@hypersend.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', true, true, true, true),
    ('jane_smith', 'jane.smith@hypersend.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', true, true, true, true),
    ('test_user', 'test@hypersend.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', true, true, true, true)
    ON CONFLICT (email) DO NOTHING; -- Avoid duplicates if script runs multiple times

-- Update sequence to continue from current max ID
SELECT setval('user_id_sequence', (SELECT COALESCE(MAX(id), 1) FROM app_user), true);