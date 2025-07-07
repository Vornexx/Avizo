package org.vornex.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {
        "org.vornex.app",
        "org.vornex.user",
        "org.vornex.auth",
        "org.vornex.userapi",
        "org.vornex.authapi"
})
@EnableJpaRepositories(basePackages = "org.vornex.user.repository") // укажи здесь пакет с репозиториями
@EntityScan(basePackages = "org.vornex.user.entity") // укажи где лежат entity
public class AppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

}
