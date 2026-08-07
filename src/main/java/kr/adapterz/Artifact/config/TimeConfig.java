package kr.adapterz.Artifact.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    @Bean
    public Clock koreaClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
