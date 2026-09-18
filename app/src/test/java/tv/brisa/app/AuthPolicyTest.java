package tv.brisa.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class AuthPolicyTest {
    @Test public void slowDownRespectsTwitchBackoff(){assertEquals(10,AuthPolicy.nextInterval(5,"slow_down"));assertEquals(15,AuthPolicy.nextInterval(10,"slow_down"));assertEquals(5,AuthPolicy.nextInterval(5,"authorization_pending"));}
    @Test public void denialAndExpiryAreTerminal(){assertTrue(AuthPolicy.pending("authorization_pending"));assertTrue(AuthPolicy.pending("slow_down"));assertFalse(AuthPolicy.pending("access_denied"));assertFalse(AuthPolicy.pending("expired_token"));assertFalse(AuthPolicy.pending("invalid device code"));}
    @Test public void rejectSecretsUrlsAndEmptyConfiguration(){assertFalse(AuthPolicy.validClient(""));assertFalse(AuthPolicy.validClient("https://twitch.tv"));assertFalse(AuthPolicy.validClient(" abcdefghijklmnopqrstuvwxyz "));assertTrue(AuthPolicy.validClient("abcdefghijklmnopqrstuvwxyz1234"));}
}
