package tv.brisa.app;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class HomeCatalogTest {
 private TwitchAccount.Stream s(String key,int viewers){TwitchAccount.Stream s=new TwitchAccount.Stream();s.login=key;s.name=key;s.viewers=viewers;return s;}
 @Test public void sortsAcrossPlatformsAndBreaksTies(){List<TwitchAccount.Stream> list=new ArrayList<>(Arrays.asList(s("twitch",20),s("kick:b",100),s("alpha",100)));HomeCatalog.sortLive(list);assertEquals("alpha",list.get(0).login);assertEquals("kick:b",list.get(1).login);assertEquals("twitch",list.get(2).login);}
 @Test public void offlineExcludesLiveWithoutMixingIdenticalSlugs(){TwitchAccount.Stream twitch=s("same",100),kick=s("kick:same",0);List<TwitchAccount.Stream> result=HomeCatalog.offline(Arrays.asList(twitch,kick),Arrays.asList(twitch));assertEquals(1,result.size());assertEquals("kick:same",result.get(0).login);}
}
