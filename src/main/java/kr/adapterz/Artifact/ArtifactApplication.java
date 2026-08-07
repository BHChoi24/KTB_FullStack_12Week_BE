package kr.adapterz.Artifact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

//내부에는 @ComponentScan포함 되어있어 시작시 자동으로 실행하고 @Component스캔 -> @Service, @Repository, @Controller...
@SpringBootApplication
@EnableScheduling
public class ArtifactApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArtifactApplication.class, args);
	}

}
