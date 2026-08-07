-- Never Enough의 현재 JPA Entity를 기준으로 만든 MySQL 최초 스키마입니다.
-- 중요: 검색 정확성과 기준 성능을 먼저 확보하기 위해 FULLTEXT 인덱스는 V1에 포함하지 않습니다.

CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(10) NOT NULL,
    profile_image VARCHAR(500) NULL,
    recovery_profile_image VARCHAR(500) NULL,
    token_version INTEGER NOT NULL DEFAULT 0,
    role VARCHAR(20) NOT NULL,
    deleted_at DATETIME NULL,
    anonymized_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    INDEX idx_users_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE posts (
    post_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(26) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'DAILY_SHARE',
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    pinned_at DATETIME NULL,
    image_url VARCHAR(500) NULL,
    likes_count BIGINT NOT NULL DEFAULT 0,
    views_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_posts_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_posts_created_at_post_id (created_at, post_id),
    INDEX idx_posts_user_id (user_id),
    INDEX idx_posts_category_created_at_post_id (category, created_at, post_id),
    INDEX idx_posts_pinned_pinned_at (pinned, pinned_at)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comments (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (comment_id),
    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id) REFERENCES posts (post_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_comments_post_id (post_id),
    INDEX idx_comments_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE post_likes (
    like_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (like_id),
    CONSTRAINT uk_post_likes_user_post UNIQUE (user_id, post_id),
    CONSTRAINT fk_post_likes_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_post_likes_post
        FOREIGN KEY (post_id) REFERENCES posts (post_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_post_likes_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
