package og.portal.zarplata.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import waffle.spring.NegotiateSecurityFilter;
import waffle.spring.NegotiateSecurityFilterEntryPoint;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${waffle.exclude.urls:}")
    private List<String> excludeUrls;

    @Value("${security.ignoring.urls:}")
    private String[] ignoringUrls;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> {
            if (ignoringUrls != null && ignoringUrls.length > 0) {
                web.ignoring().requestMatchers(ignoringUrls);
            }
        };
    }

    @Bean
    SecurityFilterChain filterChain(final HttpSecurity http, NegotiateSecurityFilter filter, NegotiateSecurityFilterEntryPoint entryPoint) throws Exception {
        http
            .securityContext(securityContext -> securityContext
                .requireExplicitSave(false)
            )
            .authorizeHttpRequests(requests -> requests
                .anyRequest().authenticated()
            )
            .addFilterBefore(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                    if (excludeUrls != null && excludeUrls.contains(request.getRequestURI())) {
                        filterChain.doFilter(request, response);
                    } else {
                        filter.doFilter(request, response, filterChain);
                    }
                }
            }, BasicAuthenticationFilter.class)
            .exceptionHandling(handling -> handling.authenticationEntryPoint(new AuthenticationEntryPoint() {
                @Override
                public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                    if (excludeUrls != null && excludeUrls.contains(request.getRequestURI())) {
                        response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
                    } else {
                        entryPoint.commence(request, response, authException);
                    }
                }
            }));
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<NegotiateSecurityFilter> waffleNegotiateSecurityFilterRegistration(NegotiateSecurityFilter filter) {
        FilterRegistrationBean<NegotiateSecurityFilter> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }
}
