package tv.brisa.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class TwitchSourceTest {
    @Test public void normalizesChannelAndWebLink() {
        assertEquals("example_1",TwitchSource.channel(" Example_1 "));
        assertEquals("example",TwitchSource.channel("https://www.twitch.tv/Example?ref=abc"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsLookalikeHost(){TwitchSource.channel("https://twitch.tv.evil.example/channel");}
    @Test(expected=IllegalArgumentException.class) public void rejectsPathInjection(){TwitchSource.channel("name/other");}
}
