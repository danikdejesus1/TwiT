package tv.brisa.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class KickLoginPolicyTest {
 @Test public void allowsProviderHttpsHosts(){assertTrue(KickLoginPolicy.apple("https://appleid.apple.com/auth/authorize?state=test"));assertTrue(KickLoginPolicy.apple("https://account.apple.com/"));assertTrue(KickLoginPolicy.google("https://accounts.google.com/o/oauth2/auth"));}
 @Test public void rejectsSpoofedHostsAndUnsafeSchemes(){for(String u:new String[]{"http://appleid.apple.com/","https://appleid.apple.com.evil.test/","https://evil.test/appleid.apple.com","https://evil.test@appleid.apple.com/","https://appleid.apple.com:444/","javascript:alert(1)","intent://accounts.google.com","https://accounts.google.com.evil.test/"}){assertFalse(u,KickLoginPolicy.apple(u));assertFalse(u,KickLoginPolicy.google(u));}}
 @Test public void providersStaySeparate(){assertFalse(KickLoginPolicy.apple("https://accounts.google.com/"));assertFalse(KickLoginPolicy.google("https://appleid.apple.com/"));assertFalse(KickLoginPolicy.apple("https://www.apple.com/"));}
}
