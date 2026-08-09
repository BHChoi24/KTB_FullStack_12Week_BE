# Artifact 검색 기능 고도화

상태: 코드 구현 완료, 로컬 MySQL 적용 및 기능 검증 대기

## 1. 목표

기존 제목 접두사 `LIKE` 검색을 MySQL FULLTEXT ngram 검색으로 교체한다.

- API: 기존 `GET /posts?keyword=` 유지
- 검색 대상: `posts.title`, `posts.content`
- 검색 방식: MySQL `FULLTEXT ... WITH PARSER ngram`
- ngram 크기: 2글자
- 기존 카테고리·기간·정렬·페이지네이션 유지
- 검색 관련도 정렬은 사용하지 않음

## 2. 확인한 MySQL 상태

Workbench에서 아래 사실을 확인했다.

| 항목 | 확인값 |
|---|---|
| 데이터베이스 | `never_enough` |
| MySQL 버전 | `8.4.10` |
| `ngram_token_size` | `2` |
| 게시글 테이블 | `posts` |
| Flyway 적용 이력 | V1, V2 성공 |
| V3 적용 이력 | 없음 |

기존 V1/V2는 이미 적용된 이력이므로 수정·삭제·병합하지 않는다. 새 인덱스는 V3로 추가한다.

## 3. Flyway V3 마이그레이션

파일: `src/main/resources/db/migration/mysql/V3__add_fulltext_index_to_post.sql`

```sql
-- posts 테이블의 title과 content 컬럼에 n-gram 기반의 Full-Text Index를 추가합니다.
-- 인덱스 이름은 ft_post_title_content로 지정합니다.
ALTER TABLE posts
ADD FULLTEXT INDEX ft_post_title_content (title, content) WITH PARSER ngram;
```

애플리케이션을 MySQL 프로필로 실행하면 Flyway가 V3를 한 번 적용하고, `flyway_schema_history`에 성공 이력을 기록한다. V3를 Workbench에서 직접 실행하지 않는다.

## 4. 검색어 규칙

### 4.1 일반 검색

공백으로 나눈 모든 유효 단어를 반드시 포함한다.

| 사용자 입력 | 내부 FULLTEXT 검색식 |
|---|---|
| `검색 기능` | `+검색 +기능` |
| `검색+기능` | `+검색 +기능` |
| `a 검색` | `+검색` |

`+`는 MySQL Boolean FULLTEXT에서 해당 단어가 반드시 포함돼야 함을 뜻한다.

### 4.2 특수문자와 최소 길이

- 구문 검색은 지원하지 않는다.
- `+ - < > ( ) ~ * " @`는 공백으로 바꾼다.
- 공백을 제외한 정규화 검색어가 2글자 미만이면 HTTP 400을 반환한다.
- 1글자 단어는 ngram 검색식에서 제외한다.
- 2글자 이상 유효 단어가 하나도 없으면 HTTP 400을 반환한다.
- `keyword`가 없거나 공백뿐이면 기존처럼 검색 없이 일반 목록을 반환한다.

오류 필드는 `keyword`, 오류 메시지는 `검색 가능한 단어는 2글자 이상 입력해야 합니다.`이다.

## 5. 구현 파일

| 파일 | 역할 |
|---|---|
| `service/PostSearchCondition.java` | 검색어 정규화, 길이 검증, Boolean 검색식 생성 |
| `repository/PostFullTextRepository.java` | FULLTEXT 검색 전용 인터페이스 |
| `repository/PostFullTextRepositoryImpl.java` | native query, 필터, 정렬, 페이지네이션, count query |
| `service/PostService.java` | 검색어 유무에 따른 FULLTEXT/일반 목록 조회 분기 |
| `repository/PostSpecifications.java` | 검색어 없는 목록의 카테고리·기간 조건만 유지 |
| `response/code/ValidationField.java` | `keyword` 오류 필드 상수 |
| `response/code/ValidationMessage.java` | 검색어 길이 오류 메시지 상수 |

기존 `LIKE` 검색 메서드와 LIKE escape 코드는 제거했다.

## 6. FULLTEXT 조회 구조

검색어가 있을 때 native query는 아래 구조를 사용한다.

```sql
SELECT p.*
FROM posts p
WHERE MATCH(p.title, p.content)
      AGAINST (:fullTextQuery IN BOOLEAN MODE)
  AND category 조건
  AND created_at 기간 조건
ORDER BY 기존 정렬 기준
LIMIT 페이지 크기 OFFSET 페이지 위치;
```

목록 조회와 별도로 같은 `WHERE` 조건의 `COUNT(*)` 쿼리를 실행해 페이지 전체 개수를 계산한다.

정렬은 사용자 문자열을 SQL에 직접 넣지 않고 `PostSort` enum으로만 선택한다.

## 7. 테스트 상태

- `PostSearchConditionTest`: 검색어 변환, 특수문자 처리, 1글자 단어 제외, HTTP 400 검증
- `PostServiceSearchTest`: 검색어가 있을 때 FULLTEXT Repository로 분기하는지 검증
- 기존 H2 Repository 테스트: LIKE 전용 테스트 제거
- `./gradlew test`: 성공

H2는 MySQL FULLTEXT ngram 문법을 지원하지 않으므로, 실제 FULLTEXT 기능은 V3 적용 후 Docker MySQL에서 검증한다.

## 8. 다음 검증 절차

1. 사용자가 로컬 애플리케이션을 MySQL 프로필로 실행한다.
2. Flyway 로그에서 V3 성공 여부를 확인한다.
3. Workbench에서 아래 SQL을 실행한다.

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SHOW INDEX FROM posts;
```

4. V3 성공과 `ft_post_title_content` FULLTEXT 인덱스를 확인한다.
5. 제목 검색, 본문 검색, AND 검색, 특수문자 처리, 2글자 미만 오류, 기간·정렬 결합을 검증한다.
6. 기능 검증 완료 후 `EXPLAIN ANALYZE`로 LIKE 기준선과 FULLTEXT 성능을 비교한다.
