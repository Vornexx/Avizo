package org.vornex.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {
        "org.vornex.app",
        "org.vornex.user",
        "org.vornex.auth",
        "org.vornex.userapi",
        "org.vornex.authapi",
        "org.vornex.jwtapi",
        "org.vornex.listing",
        "org.vornex.events",
        "org.vornex.exception"
})
@EnableJpaRepositories(basePackages =
        {
                "org.vornex.user.repository",
                "org.vornex.auth.repository",
                "org.vornex.listing.repository",
                "org.vornex.events.repository"
        })
@EntityScan(basePackages =
        {
                "org.vornex.user.entity",
                "org.vornex.auth.entity",
                "org.vornex.listing.entity",
                "org.vornex.events.entity"
        })
@EnableScheduling
public class AppApplication {
    public static void main(String[] args) throws ClassNotFoundException {
        SpringApplication.run(AppApplication.class, args);
    }

}
