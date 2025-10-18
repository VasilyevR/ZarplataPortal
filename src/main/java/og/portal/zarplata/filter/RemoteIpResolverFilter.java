package og.portal.zarplata.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RemoteIpResolverFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RemoteIpResolverFilter.class);
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String X_REAL_IP_HEADER = "X-Real-IP";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("RemoteIpResolverFilter initialized.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String originalRemoteAddr = httpRequest.getRemoteAddr();
        String newRemoteAddr = null;

        log.debug("RemoteIpResolverFilter: Original remoteAddr: {}", originalRemoteAddr);

        String xRealIp = httpRequest.getHeader(X_REAL_IP_HEADER);
        if (xRealIp != null && !xRealIp.isEmpty()) {
            newRemoteAddr = xRealIp.split(",")[0].trim();
            log.debug("RemoteIpResolverFilter: Found {} header: {}", X_REAL_IP_HEADER, newRemoteAddr);
        } else {
            String xForwardedFor = httpRequest.getHeader(X_FORWARDED_FOR_HEADER);
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String[] ips = xForwardedFor.split(",");
                newRemoteAddr = ips[0].trim();
                log.debug("RemoteIpResolverFilter: Found {} header: {}", X_FORWARDED_FOR_HEADER, newRemoteAddr);
            }
        }

        if (newRemoteAddr != null && !newRemoteAddr.equals(httpRequest.getRemoteAddr())) {
            log.info("RemoteIpResolverFilter: Rewriting remote address from {} to {}", originalRemoteAddr, newRemoteAddr);
            HttpServletRequestWrapper wrappedRequest = new RemoteAddrRequestWrapper(httpRequest, newRemoteAddr);
            chain.doFilter(wrappedRequest, response);
        } else {
            log.debug("RemoteIpResolverFilter: No change to remote address ({}).", originalRemoteAddr);
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        log.info("RemoteIpResolverFilter destroyed.");
    }

    private static class RemoteAddrRequestWrapper extends HttpServletRequestWrapper {
        private final String remoteAddr;

        public RemoteAddrRequestWrapper(HttpServletRequest request, String remoteAddr) {
            super(request);
            this.remoteAddr = remoteAddr;
        }

        @Override
        public String getRemoteAddr() {
            return remoteAddr;
        }

        @Override
        public String getRemoteHost() {
            return remoteAddr;
        }
    }
}
