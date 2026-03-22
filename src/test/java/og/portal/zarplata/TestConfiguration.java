package og.portal.zarplata;

import com.sun.jna.platform.win32.WinNT;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import waffle.servlet.spi.SecurityFilterProviderCollection;
import waffle.spring.NegotiateSecurityFilter;
import waffle.windows.auth.*;
import waffle.windows.auth.impl.*;

@Configuration
public class TestConfiguration {
    @Bean
    @Primary
    public NegotiateSecurityFilter filter() {
        NegotiateSecurityFilter filter = new NegotiateSecurityFilter();
        filter.setProvider(new SecurityFilterProviderCollection(new TestWindowsAuthProvider()));
        return filter;
    }

    public static class TestWindowsAuthProvider implements IWindowsAuthProvider {

        @Override
        public IWindowsIdentity logonUser(String s, String s1) {
            return new WindowsIdentityImpl(new WinNT.HANDLE());
        }

        @Override
        public IWindowsIdentity logonDomainUser(String s, String s1, String s2) {
            return new WindowsIdentityImpl(new WinNT.HANDLE());
        }

        @Override
        public IWindowsIdentity logonDomainUserEx(String s, String s1, String s2, int i, int i1) {
            return new WindowsIdentityImpl(new WinNT.HANDLE());
        }

        @Override
        public IWindowsAccount lookupAccount(String s) {
            return new WindowsAccountImpl(s);
        }

        @Override
        public IWindowsComputer getCurrentComputer() {
            return new WindowsComputerImpl("My Computer");
        }

        @Override
        public IWindowsDomain[] getDomains() {
            return new IWindowsDomain []{new WindowsDomainImpl("My Domain")};
        }

        @Override
        public IWindowsSecurityContext acceptSecurityToken(String s, byte[] bytes, String s1) {
            return new TestWindowsSecurityContext();
        }

        @Override
        public void resetSecurityToken(String s) {
        }
    }

    public static class TestWindowsSecurityContext extends WindowsSecurityContextImpl {
        @Override
        public IWindowsIdentity getIdentity() {
            return new IWindowsIdentity() {
                @Override
                public String getSidString() {
                    return "Login";
                }

                @Override
                public byte[] getSid() {
                    return new String("Login").getBytes();
                }

                @Override
                public String getFqn() {
                    return "Domain/Login";
                }

                @Override
                public IWindowsAccount[] getGroups() {
                    return new IWindowsAccount[]{new TestWindowsAccount("My Account")};
                }

                @Override
                public IWindowsImpersonationContext impersonate() {
                    return null;
                }

                @Override
                public void dispose() {

                }

                @Override
                public boolean isGuest() {
                    return false;
                }
            };
        }
    }

    public static class TestWindowsAccount extends WindowsAccountImpl {
        public TestWindowsAccount(String userName) {
            super(userName);
        }

        public TestWindowsAccount(String accountName, String systemName) {
            super(accountName);
        }

        public TestWindowsAccount() {
            this((String)null, (String)null);
        }
    }

    public static class TestNegotiateSecurityFilter extends SecurityFilterProviderCollection {
        public TestNegotiateSecurityFilter(IWindowsAuthProvider auth) {
            super(auth);
        }
    }
}
