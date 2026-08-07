package kr.adapterz.Artifact.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

/**
 * 실행 중 업로드된 파일을 HTTP URL로 조회할 수 있도록 연결하는 MVC 설정입니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDirectory;

    public WebConfig(
            @Value("${app.upload.directory:uploads}") String uploadDirectory
    ) {
        this.uploadDirectory = uploadDirectory;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = Path.of(uploadDirectory)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        // /uploads/profiles/파일명 요청을 로컬 uploads/profiles 파일과 연결합니다.
        // file URI의 마지막 슬래시를 유지해야 하위 경로를 올바르게 결합할 수 있습니다.
        if (!uploadLocation.endsWith("/")) {
            uploadLocation += "/";
        }

        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation)
                // UUID 파일명은 이미지 변경 때마다 달라지므로 같은 URL을 장기간 캐시해도 안전합니다.
                .setCacheControl(
                        // UUID URL은 이미지가 바뀔 때 새로 생성되므로 기존 URL을 1년 보관해도 내용이 달라지지 않습니다.
                        CacheControl.maxAge(Duration.ofDays(365))
                                .cachePublic()
                                .immutable()
                );
    }
}
