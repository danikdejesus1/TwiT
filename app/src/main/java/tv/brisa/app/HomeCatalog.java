package tv.brisa.app;
import java.util.*;
final class HomeCatalog {
 static void sortLive(List<TwitchAccount.Stream> live){live.sort(Comparator.comparingInt((TwitchAccount.Stream s)->s.viewers).reversed().thenComparing(s->s.name,String.CASE_INSENSITIVE_ORDER).thenComparing(s->s.login));}
 static List<TwitchAccount.Stream> offline(Collection<TwitchAccount.Stream> all,List<TwitchAccount.Stream> live){Set<String> keys=new HashSet<>();for(TwitchAccount.Stream s:live)keys.add(s.login);List<TwitchAccount.Stream> out=new ArrayList<>();for(TwitchAccount.Stream s:all)if(!keys.contains(s.login))out.add(s);out.sort(Comparator.comparing(s->s.name,String.CASE_INSENSITIVE_ORDER));return out;}
}
