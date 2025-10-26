CREATE TABLE like_comment (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    comment_id BIGINT NOT NULL REFERENCES comment(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_like_comment_user_comment UNIQUE (user_id, comment_id)
);

CREATE INDEX idx_like_comment_user_id ON like_comment(user_id);
CREATE INDEX idx_like_comment_comment_id ON like_comment(comment_id);
CREATE INDEX idx_like_comment_created_at ON like_comment(created_at);
