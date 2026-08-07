package kr.adapterz.Artifact;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 단위 테스트가 놓칠 수 있는 Spring Bean 생성과 생성자 주입 오류를 확인합니다.
 */
// 중요: 기본 DB 프로필이 없으므로 컨텍스트 테스트가 사용할 DB를 명시합니다.
@ActiveProfiles("h2")
@SpringBootTest(properties = {
        // 실제 사용자 H2 파일에 영향을 주지 않도록 테스트 전용 인메모리 DB를 사용합니다.
        "spring.datasource.url=jdbc:h2:mem:context-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // 컨텍스트 테스트가 업로드 폴더를 사용하더라도 임시 경로만 바라보게 합니다.
        "app.upload.directory=${java.io.tmpdir}/artifact-context-test-uploads"
})
class ArtifactApplicationContextTest {

    @Test
    void contextLoads() {
        // 모든 Bean을 생성하고 의존성 주입을 완료하면 테스트가 성공합니다.
    }
}
