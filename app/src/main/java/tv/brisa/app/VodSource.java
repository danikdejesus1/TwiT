package tv.brisa.app;
import java.util.*;
import java.io.IOException;
import okhttp3.*;
import org.json.*;
final class VodSource {
 static final class Video {String id="",title="",date="",duration="",thumbnail="",source="";}
 static final class Page {final List<Video> videos=new ArrayList<>();String cursor="";}
 static Page kick(String key)throws Exception{
  String channel=ChannelKey.parse(key);if(!ChannelKey.kick(channel))throw new IllegalArgumentException();
  HttpUrl url=new HttpUrl.Builder().scheme("https").host("kick.com").addPathSegments("api/v2/channels").addPathSegment(ChannelKey.slug(channel)).addPathSegment("videos").build();
  try(Response r=TwitchSource.HTTP.newCall(new Request.Builder().url(url).header("Accept","application/json").build()).execute()){
   if(!r.isSuccessful())throw new IOException("Kick no permitió consultar los vídeos");byte[] bytes=r.peekBody(4000001).bytes();if(bytes.length>4000000)throw new IOException("Lista demasiado grande");
   JSONArray a=new JSONArray(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));Page page=new Page();
   for(int i=0;i<a.length();i++){JSONObject v=a.getJSONObject(i),meta=v.optJSONObject("video");if(v.optBoolean("is_live")||meta==null||meta.optBoolean("is_private")||meta.optBoolean("is_pruned")||!meta.isNull("deleted_at"))continue;
    Video video=new Video();video.id=meta.optString("uuid");video.source=v.optString("source");if(!KickSource.validPlayback(video.source))continue;video.title=v.optString("session_title");video.date=v.optString("created_at");long minutes=v.optLong("duration")/60000;video.duration=(minutes/60)+"h "+(minutes%60)+"m";if(v.optJSONObject("thumbnail")!=null)video.thumbnail=v.getJSONObject("thumbnail").optString("src");page.videos.add(video);
   }return page;
  }
 }
}
