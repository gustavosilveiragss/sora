CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    bio TEXT,
    profile_picture VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE country (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(5) NOT NULL UNIQUE,
    name_key VARCHAR(100) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    timezone VARCHAR(50)
);

CREATE TABLE city (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_key VARCHAR(100) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    country_id BIGINT NOT NULL,
    FOREIGN KEY (country_id) REFERENCES country(id)
);

CREATE TABLE collection (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_key VARCHAR(100) NOT NULL,
    icon_name VARCHAR(100),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE trip (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    creator_id BIGINT NOT NULL,
    country_id BIGINT NOT NULL,
    is_collaborative BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES user_account(id),
    FOREIGN KEY (country_id) REFERENCES country(id)
);

CREATE TABLE trip_invitation (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    inviter_id BIGINT NOT NULL,
    invitee_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message TEXT,
    invited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE,
    FOREIGN KEY (inviter_id) REFERENCES user_account(id),
    FOREIGN KEY (invitee_id) REFERENCES user_account(id),
    CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED'))
);

CREATE TABLE trip_participant (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    UNIQUE(trip_id, user_id),
    CHECK (role IN ('CREATOR', 'PARTICIPANT'))
);

CREATE TABLE post (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    collection_id BIGINT NOT NULL,
    city_id BIGINT NOT NULL,
    caption TEXT,
    posted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES user_account(id),
    FOREIGN KEY (trip_id) REFERENCES trip(id),
    FOREIGN KEY (collection_id) REFERENCES collection(id),
    FOREIGN KEY (city_id) REFERENCES city(id)
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
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CHECK (media_type IN ('IMAGE', 'VIDEO'))
);

CREATE TABLE follow (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    followed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (follower_id) REFERENCES user_account(id),
    FOREIGN KEY (following_id) REFERENCES user_account(id),
    UNIQUE(follower_id, following_id),
    CHECK (follower_id != following_id)
);

CREATE TABLE like_post (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    liked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    UNIQUE(user_id, post_id)
);

CREATE TABLE comment (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES user_account(id),
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES comment(id) ON DELETE CASCADE
);

CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    reference_id VARCHAR(100),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES user_account(id),
    CHECK (type IN ('NEW_FOLLOWER', 'TRIP_INVITATION', 'TRIP_INVITATION_ACCEPTED', 'TRIP_INVITATION_DECLINED', 'POST_LIKED', 'POST_COMMENTED', 'COMMENT_REPLIED'))
);

CREATE INDEX idx_user_account_username ON user_account(username);
CREATE INDEX idx_user_account_email ON user_account(email);
CREATE INDEX idx_city_country ON city(country_id);
CREATE INDEX idx_trip_creator ON trip(creator_id);
CREATE INDEX idx_trip_country ON trip(country_id);
CREATE INDEX idx_trip_collaborative ON trip(is_collaborative);
CREATE UNIQUE INDEX idx_trip_creator_country_unique ON trip(creator_id, country_id) WHERE is_collaborative = FALSE;
CREATE INDEX idx_trip_invitation_trip ON trip_invitation(trip_id);
CREATE INDEX idx_trip_invitation_invitee ON trip_invitation(invitee_id);
CREATE INDEX idx_trip_invitation_status ON trip_invitation(status);
CREATE INDEX idx_trip_participant_trip ON trip_participant(trip_id);
CREATE INDEX idx_trip_participant_user ON trip_participant(user_id);
CREATE INDEX idx_post_author ON post(author_id);
CREATE INDEX idx_post_trip ON post(trip_id);
CREATE INDEX idx_post_city ON post(city_id);
CREATE INDEX idx_post_posted_at ON post(posted_at DESC);
CREATE INDEX idx_post_media_post ON post_media(post_id);
CREATE INDEX idx_follow_follower ON follow(follower_id);
CREATE INDEX idx_follow_following ON follow(following_id);
CREATE INDEX idx_like_post_post ON like_post(post_id);
CREATE INDEX idx_like_post_user ON like_post(user_id);
CREATE INDEX idx_comment_post ON comment(post_id);
CREATE INDEX idx_comment_parent ON comment(parent_comment_id);
CREATE INDEX idx_notification_recipient ON notification(recipient_id);
CREATE INDEX idx_notification_read ON notification(is_read);