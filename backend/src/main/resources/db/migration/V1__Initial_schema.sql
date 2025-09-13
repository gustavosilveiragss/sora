DROP TABLE IF EXISTS notification CASCADE;
DROP TABLE IF EXISTS comment CASCADE;
DROP TABLE IF EXISTS like_post CASCADE;
DROP TABLE IF EXISTS follow CASCADE;
DROP TABLE IF EXISTS post_media CASCADE;
DROP TABLE IF EXISTS post CASCADE;
DROP TABLE IF EXISTS travel_permission CASCADE;
DROP TABLE IF EXISTS country_visited CASCADE;
DROP TABLE IF EXISTS collection CASCADE;
DROP TABLE IF EXISTS country CASCADE;
DROP TABLE IF EXISTS user_account CASCADE;

CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    bio TEXT,
    profile_picture VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE country (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(5) NOT NULL UNIQUE,
    name_key VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    timezone VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE collection (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_key VARCHAR(100) NOT NULL,
    icon_name VARCHAR(100),
    sort_order INTEGER NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE travel_permission (
    id BIGSERIAL PRIMARY KEY,
    grantor_id BIGINT NOT NULL,
    grantee_id BIGINT NOT NULL,
    country_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    invitation_message TEXT,
    responded_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (grantor_id) REFERENCES user_account(id),
    FOREIGN KEY (grantee_id) REFERENCES user_account(id),
    FOREIGN KEY (country_id) REFERENCES country(id),
    CONSTRAINT uk_travel_permission_grantor_grantee_country UNIQUE(grantor_id, grantee_id, country_id),
    CHECK (status IN ('PENDING', 'ACTIVE', 'DECLINED', 'REVOKED'))
);

CREATE TABLE post (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    profile_owner_id BIGINT NOT NULL,
    country_id BIGINT NOT NULL,
    collection_id BIGINT NOT NULL,
    city_name VARCHAR(255) NOT NULL,
    city_latitude DOUBLE PRECISION,
    city_longitude DOUBLE PRECISION,
    caption TEXT,
    visibility_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    shared_post_group_id VARCHAR(36),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES user_account(id),
    FOREIGN KEY (profile_owner_id) REFERENCES user_account(id),
    FOREIGN KEY (country_id) REFERENCES country(id),
    FOREIGN KEY (collection_id) REFERENCES collection(id),
    CONSTRAINT post_visibility_type_check CHECK (visibility_type IN ('PERSONAL', 'SHARED', 'COLLABORATIVE'))
);

CREATE TABLE post_media (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    cloudinary_public_id VARCHAR(255) NOT NULL,
    cloudinary_url VARCHAR(500) NOT NULL,
    media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    file_size BIGINT,
    width INTEGER,
    height INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CHECK (media_type IN ('IMAGE', 'VIDEO'))
);

CREATE TABLE follow (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (follower_id) REFERENCES user_account(id),
    FOREIGN KEY (following_id) REFERENCES user_account(id),
    CONSTRAINT uk_follow_follower_following UNIQUE(follower_id, following_id),
    CHECK (follower_id != following_id)
);

CREATE TABLE like_post (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CONSTRAINT uk_like_post_user_post UNIQUE(user_id, post_id)
);

CREATE TABLE comment (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    content TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES user_account(id),
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES comment(id) ON DELETE CASCADE
);

CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT,
    reference_id VARCHAR(100),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES user_account(id),
    CHECK (type IN ('NEW_FOLLOWER', 'TRAVEL_PERMISSION_INVITATION', 'TRAVEL_PERMISSION_ACCEPTED', 'TRAVEL_PERMISSION_DECLINED', 'POST_LIKED', 'POST_COMMENTED', 'COMMENT_REPLIED'))
);

CREATE INDEX idx_user_account_username ON user_account(username);
CREATE INDEX idx_user_account_email ON user_account(email);

CREATE INDEX idx_travel_permission_grantor ON travel_permission(grantor_id);
CREATE INDEX idx_travel_permission_grantee ON travel_permission(grantee_id);
CREATE INDEX idx_travel_permission_country ON travel_permission(country_id);
CREATE INDEX idx_travel_permission_status ON travel_permission(status);

CREATE INDEX idx_post_author ON post(author_id);
CREATE INDEX idx_post_profile_owner ON post(profile_owner_id);
CREATE INDEX idx_post_country ON post(country_id);
CREATE INDEX idx_post_created_at ON post(created_at DESC);
CREATE INDEX idx_post_visibility_type ON post(visibility_type);
CREATE INDEX idx_post_shared_group ON post(shared_post_group_id);
CREATE INDEX idx_post_author_profile ON post(author_id, profile_owner_id);
CREATE INDEX idx_post_city_location ON post(city_latitude, city_longitude);

CREATE INDEX idx_post_media_post ON post_media(post_id);

CREATE INDEX idx_follow_follower ON follow(follower_id);
CREATE INDEX idx_follow_following ON follow(following_id);

CREATE INDEX idx_like_post_post ON like_post(post_id);
CREATE INDEX idx_like_post_user ON like_post(user_id);

CREATE INDEX idx_comment_post ON comment(post_id);
CREATE INDEX idx_comment_parent ON comment(parent_comment_id);

CREATE INDEX idx_notification_recipient ON notification(recipient_id);
CREATE INDEX idx_notification_read ON notification(is_read);