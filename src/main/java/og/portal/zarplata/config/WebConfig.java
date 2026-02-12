package og.portal.zarplata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.context.annotation.Bean;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }
}
