CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(255) UNIQUE,
    user_id UUID,
    email VARCHAR(512),
    type VARCHAR(50),
    status VARCHAR(50),
    sent_at TIMESTAMP,
    error_message TEXT,
    message VARCHAR(1000),
    saga_id VARCHAR(255),
    step VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_notification_correlation ON notification_logs(correlation_id);
