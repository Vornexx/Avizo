package org.vornex.auth.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration

public class WebConfig implements WebMvcConfigurer {
    private final AuthenticationPrincipalArgumentResolver defaultResolver;


    public WebConfig(ApplicationContext context) {
        this.defaultResolver = new AuthenticationPrincipalArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthUserDataArgumentResolver(defaultResolver));
    }
}
