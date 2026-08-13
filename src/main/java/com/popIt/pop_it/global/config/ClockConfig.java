package com.popIt.pop_it.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 인프라(MySQL/JVM/JDBC) 타임존이 전부 KST여도, now()가 어느 한 곳이라도
// 어긋나면 조용히 틀린 값을 만든다 - 코드가 인프라 설정에 기대지 않고
// 스스로 KST를 명시하도록 이 Clock을 모든 now() 호출 지점에서 사용한다.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
