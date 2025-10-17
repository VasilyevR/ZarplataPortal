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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RemoteIpResolverFilter implements Filter {
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String X_REAL_IP_HEADER = "X-Real-IP";
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String newRemoteAddr = null;

        String xRealIp = httpRequest.getHeader(X_REAL_IP_HEADER);
        if (xRealIp != null && !xRealIp.isEmpty()) {
            newRemoteAddr = xRealIp;
        } else {
            String xForwardedFor = httpRequest.getHeader(X_FORWARDED_FOR_HEADER);
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String[] ips = xForwardedFor.split(",");
                newRemoteAddr = ips[0].trim();
            }
        }

        if (newRemoteAddr != null && !newRemoteAddr.equals(httpRequest.getRemoteAddr())) {
            HttpServletRequestWrapper wrappedRequest = new RemoteAddrRequestWrapper(httpRequest, newRemoteAddr);
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
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

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("x-real-ip".equalsIgnoreCase(name) || "x-forwarded-for".equalsIgnoreCase(name) || "x-forwarded-proto".equalsIgnoreCase(name) || "x-forwarded-port".equalsIgnoreCase(name)) {
                return Collections.emptyEnumeration();
            }
            return super.getHeaders(name);
        }

        @Override
        public String getHeader(String name) {
            if ("x-real-ip".equalsIgnoreCase(name) || "x-forwarded-for".equalsIgnoreCase(name) || "x-forwarded-proto".equalsIgnoreCase(name) || "x-forwarded-port".equalsIgnoreCase(name)) {
                return null;
            }
            return super.getHeader(name);
        }
    }
}
