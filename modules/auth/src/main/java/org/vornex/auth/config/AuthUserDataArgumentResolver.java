package org.vornex.auth.config;

import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.vornex.auth.CustomUserDetails;
import org.vornex.authapi.AuthUserData;

public class AuthUserDataArgumentResolver implements HandlerMethodArgumentResolver {

    private final HandlerMethodArgumentResolver delegate;

    public AuthUserDataArgumentResolver(HandlerMethodArgumentResolver delegate) {
        this.delegate = new AuthenticationPrincipalArgumentResolver();
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                && AuthUserData.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        Object principal = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser(); // возвращаем AuthUserData
        }

        return principal;
    }
}
