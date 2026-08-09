-- posts 테이블의 title과 content 컬럼에 n-gram 기반의 Full-Text Index를 추가합니다.
-- 인덱스 이름은 ft_post_title_content로 지정합니다.
ALTER TABLE posts
ADD FULLTEXT INDEX ft_post_title_content (title, content) WITH PARSER ngram;
