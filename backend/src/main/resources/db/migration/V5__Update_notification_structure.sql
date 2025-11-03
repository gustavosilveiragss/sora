DELETE FROM notification;

ALTER TABLE notification ADD COLUMN trigger_user_id BIGINT;
ALTER TABLE notification ADD COLUMN post_id BIGINT;
ALTER TABLE notification ADD COLUMN comment_id BIGINT;

ALTER TABLE notification ADD FOREIGN KEY (trigger_user_id) REFERENCES user_account(id) ON DELETE CASCADE;
ALTER TABLE notification ADD FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE;
ALTER TABLE notification ADD FOREIGN KEY (comment_id) REFERENCES comment(id) ON DELETE CASCADE;

ALTER TABLE notification DROP COLUMN IF EXISTS message;
ALTER TABLE notification DROP COLUMN IF EXISTS reference_id;

ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;
ALTER TABLE notification ADD CONSTRAINT notification_type_check CHECK (type IN ('LIKE', 'COMMENT', 'COMMENT_REPLY', 'FOLLOW'));

CREATE INDEX IF NOT EXISTS idx_notification_recipient ON notification(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notification(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notification(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_trigger_user ON notification(trigger_user_id);
CREATE INDEX IF NOT EXISTS idx_notification_post ON notification(post_id);
