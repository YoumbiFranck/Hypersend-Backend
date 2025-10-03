-- Script to create messages table for message service
-- This script should be placed in the init/ directory and will run after user table creation

\c hypersend;

-- Create messages table
CREATE TABLE IF NOT EXISTS messages (
                                        id BIGSERIAL PRIMARY KEY,
                                        sender_id INTEGER NOT NULL,
                                        receiver_id INTEGER NOT NULL,
                                        content TEXT NOT NULL,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints to ensure referential integrity
                                        CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES app_user(id) ON DELETE CASCADE,

    -- Check constraints for data validation
    CONSTRAINT chk_messages_content_not_empty CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_messages_content_length CHECK (LENGTH(content) <= 1000),
    CONSTRAINT chk_messages_different_users CHECK (sender_id != receiver_id)
    );

-- Create indexes for better query performance
-- Index for finding messages between two specific users (most common query)
CREATE INDEX IF NOT EXISTS idx_messages_conversation
    ON messages(sender_id, receiver_id, created_at);

-- Index for finding all messages involving a specific user (sender or receiver)
CREATE INDEX IF NOT EXISTS idx_messages_user_participation
    ON messages(sender_id, created_at)
    INCLUDE (receiver_id, content);

CREATE INDEX IF NOT EXISTS idx_messages_receiver_time
    ON messages(receiver_id, created_at)
    INCLUDE (sender_id, content);

-- Index for finding latest messages (used for conversation summaries)
CREATE INDEX IF NOT EXISTS idx_messages_created_at_desc
    ON messages(created_at DESC);

-- Composite index for efficient conversation queries
CREATE INDEX IF NOT EXISTS idx_messages_sender_receiver_time
    ON messages(sender_id, receiver_id, created_at DESC);

-- Index for updated_at (useful for sync operations)
CREATE INDEX IF NOT EXISTS idx_messages_updated_at
    ON messages(updated_at DESC);

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_messages_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at on record modification
DROP TRIGGER IF EXISTS trigger_update_messages_updated_at ON messages;
CREATE TRIGGER trigger_update_messages_updated_at
    BEFORE UPDATE ON messages
    FOR EACH ROW
    EXECUTE FUNCTION update_messages_updated_at();

-- Grant permissions to the application user
GRANT ALL PRIVILEGES ON TABLE messages TO hypersend_user;
GRANT ALL PRIVILEGES ON SEQUENCE messages_id_seq TO hypersend_user;

-- Create view for message statistics (optional, useful for admin/monitoring)
CREATE OR REPLACE VIEW message_stats AS
SELECT
    COUNT(*) as total_messages,
    COUNT(DISTINCT sender_id) as unique_senders,
    COUNT(DISTINCT receiver_id) as unique_receivers,
    COUNT(DISTINCT CASE WHEN sender_id < receiver_id THEN CONCAT(sender_id, '-', receiver_id)
                        ELSE CONCAT(receiver_id, '-', sender_id) END) as unique_conversations,
    AVG(LENGTH(content)) as avg_message_length,
    MIN(created_at) as earliest_message,
    MAX(created_at) as latest_message
FROM messages;

-- Grant access to the view
GRANT SELECT ON message_stats TO hypersend_user;

-- Add comments for documentation
COMMENT ON TABLE messages IS 'Table storing all messages between users in the system';
COMMENT ON COLUMN messages.id IS 'Unique identifier for each message';
COMMENT ON COLUMN messages.sender_id IS 'ID of the user who sent the message';
COMMENT ON COLUMN messages.receiver_id IS 'ID of the user who received the message';
COMMENT ON COLUMN messages.content IS 'The actual message content (max 1000 characters)';
COMMENT ON COLUMN messages.created_at IS 'Timestamp when the message was first created';
COMMENT ON COLUMN messages.updated_at IS 'Timestamp when the message was last modified';

-- Create additional helper function for finding conversation partners
CREATE OR REPLACE FUNCTION get_conversation_partners(user_id INTEGER)
RETURNS TABLE(partner_id INTEGER, last_message_time TIMESTAMP) AS $$
BEGIN
RETURN QUERY
SELECT
    CASE
        WHEN m.sender_id = user_id THEN m.receiver_id
        ELSE m.sender_id
        END as partner_id,
    MAX(m.created_at) as last_message_time
FROM messages m
WHERE m.sender_id = user_id OR m.receiver_id = user_id
GROUP BY partner_id
ORDER BY last_message_time DESC;
END;
$$ LANGUAGE plpgsql;

-- Grant execute permission on the function
GRANT EXECUTE ON FUNCTION get_conversation_partners(INTEGER) TO hypersend_user;