package tv.brisa.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class KickSourceTest {
 @Test public void acceptsHttpsHlsFromPlaybackCdn(){assertTrue(KickSource.validPlayback("https://test.us-west-2.playback.live-video.net/api/video/v1/test.m3u8?token=example"));assertTrue(KickSource.validPlayback("https://stream.kick.com/live/playlist.m3u8"));}
 @Test public void rejectsUnsafeAndUnrelatedPlayback(){for(String u:new String[]{"http://stream.kick.com/a.m3u8","https://stream.kick.com.evil.test/a.m3u8","https://evil.test/a.m3u8","https://user:pass@stream.kick.com/a.m3u8","https://stream.kick.com:444/a.m3u8","file:///a.m3u8","https://stream.kick.com/a.html"})assertFalse(u,KickSource.validPlayback(u));}
}
