-- H2의 TIMESTAMP 값에 저장된 마이크로초를 손실 없이 이전하기 위한 정밀도 확장입니다.
-- 중요: DATETIME(6)은 초 아래 여섯 자리까지 보존합니다.

ALTER TABLE users
    MODIFY deleted_at DATETIME(6) NULL,
    MODIFY anonymized_at DATETIME(6) NULL,
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;

ALTER TABLE posts
    MODIFY pinned_at DATETIME(6) NULL,
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;

ALTER TABLE comments
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;

ALTER TABLE post_likes
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;
