ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;

ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN (
        'NEW_FOLLOWER',
        'TRAVEL_PERMISSION_INVITATION',
        'TRAVEL_PERMISSION_ACCEPTED',
        'TRAVEL_PERMISSION_DECLINED',
        'TRAVEL_PERMISSION_REVOKED',
        'POST_LIKED',
        'POST_COMMENTED',
        'COMMENT_REPLIED',
        'COMMENT_LIKED'
    ));
