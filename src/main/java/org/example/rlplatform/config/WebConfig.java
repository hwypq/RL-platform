package org.example.rlplatform.config;

import org.springframework.context.annotation.Configuration;

/**
 * 预留的 Web MVC 配置类，目前不再注册登录拦截器，
 * 登录校验由 Spring Security + JwtAuthenticationFilter 完成。
 */
@Configuration
public class WebConfig {
}
