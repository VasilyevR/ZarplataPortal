package og.portal.zarplata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import waffle.spring.NegotiateSecurityFilter;
import waffle.spring.NegotiateSecurityFilterEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final NegotiateSecurityFilter filter;
    private final NegotiateSecurityFilterEntryPoint entryPoint;

    public SecurityConfig(NegotiateSecurityFilter filter, NegotiateSecurityFilterEntryPoint entryPoint) {
        this.filter = filter;
        this.entryPoint = entryPoint;
    }

    @Bean
    SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .addFilterBefore(filter, BasicAuthenticationFilter.class)
                .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint));
        return http.build();
    }
}
