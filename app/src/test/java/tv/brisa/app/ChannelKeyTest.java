package tv.brisa.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChannelKeyTest {
    @Test public void separatesSameNameAcrossPlatforms(){assertEquals("rubius",ChannelKey.parse("Rubius"));assertEquals("kick:rubius",ChannelKey.parse("https://kick.com/Rubius"));assertNotEquals(ChannelKey.parse("rubius"),ChannelKey.parse("kick:rubius"));}
    @Test public void parsesLinksWithoutChangingLegacyTwitchHistory(){assertEquals("rubius",ChannelKey.parse("https://www.twitch.tv/rubius/"));assertEquals("kick:my-channel",ChannelKey.parse("kick.com/my-channel?x=1"));}
    @Test public void rejectsOtherOriginsAndNestedPaths(){for(String s:new String[]{"https://kick.com.evil.test/test","https://evil.test/kick.com/test","https://kick.com/a/b","javascript://kick.com/test","kick:../test"}){try{ChannelKey.parse(s);fail(s);}catch(IllegalArgumentException expected){}}}
    @Test public void usesPlatformColors(){assertEquals(0xffa970ff,ChannelKey.color("rubius"));assertEquals(0xff53fc18,ChannelKey.color("kick:rubius"));}
}
