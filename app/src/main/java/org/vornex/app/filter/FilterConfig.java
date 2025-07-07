package org.vornex.app.filter;


import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<MDCFilter> mdcFilter(MDCFilter mdcFilter) {
        FilterRegistrationBean<MDCFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(mdcFilter);
        // Гарантированно после Security (но не требует прямой зависимости от auth)
//        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 10);

        return registration;
    }
}
