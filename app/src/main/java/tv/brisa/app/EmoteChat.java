package tv.brisa.app;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.*;
import android.webkit.*;
import org.json.*;
import okhttp3.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only chat renderer. No account tokens or remote scripts enter the WebView. */
final class EmoteChat implements AutoCloseable {
    final WebView view;private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final String login;private String room="";private volatile boolean closed;private boolean ready,loading;private ChatReader reader;
    private final ArrayDeque<JSONObject> queue=new ArrayDeque<>();private JSONObject catalog=new JSONObject();
    private final Runnable flush=new Runnable(){public void run(){if(closed)return;if(ready&&!queue.isEmpty()){JSONArray batch=new JSONArray();while(!queue.isEmpty())batch.put(queue.removeFirst());view.evaluateJavascript("receive("+batch+")",null);}main.postDelayed(this,500);}};
    private final Runnable refresh=new Runnable(){public void run(){if(closed)return;loadCatalog();main.postDelayed(this,300000);}};
    @SuppressLint("SetJavaScriptEnabled") EmoteChat(Context context,String channel,String id){
        login=channel;room=id==null?"":id;view=new WebView(context);view.setBackgroundColor(Color.TRANSPARENT);view.setFocusable(false);view.setFocusableInTouchMode(false);
        WebSettings settings=view.getSettings();settings.setJavaScriptEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setDomStorageEnabled(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        view.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return true;}@Override public void onPageFinished(WebView w,String u){if(closed)return;ready=true;view.evaluateJavascript("setTitle("+JSONObject.quote(login)+");setCatalog("+catalog+")",null);}});
        try(java.io.InputStream in=context.getResources().openRawResource(tv.brisa.app.R.raw.emote_chat)){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);view.loadDataWithBaseURL("https://twit.invalid/",bytes.toString("UTF-8"),"text/html","UTF-8",null);}catch(Exception ignored){}
        reader=new ChatReader(login,raw->main.post(()->receive(raw)));main.post(flush);main.post(refresh);
    }
    private void receive(String raw){if(closed)return;ChatLine m=ChatLine.parse(raw);if(room.isEmpty()&&!m.tags.getOrDefault("room-id","").isEmpty()){room=m.tags.get("room-id");if(!loading)loadCatalog();}
        if(m.command.equals("ROOMSTATE"))return;
        try{JSONObject j=new JSONObject().put("command",m.command).put("name",m.name).put("body",m.body).put("emotes",m.tags.getOrDefault("emotes","")).put("id",m.tags.getOrDefault("id","")).put("color",m.tags.getOrDefault("color","")).put("target",m.tags.getOrDefault("target-msg-id","")).put("user",m.command.equals("CLEARCHAT")?m.tags.getOrDefault("target-user-id",""):m.tags.getOrDefault("user-id",""));queue.addLast(j);while(queue.size()>60)queue.removeFirst();}catch(Exception ignored){}
    }
    private String get(String url)throws Exception{if(closed||Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException();try(Response r=TwitchSource.HTTP.newCall(new Request.Builder().url(url).build()).execute()){if(!r.isSuccessful())throw new java.io.IOException();byte[] bytes=r.peekBody(6000001).bytes();if(bytes.length>6000000)throw new java.io.IOException();return new String(bytes,java.nio.charset.StandardCharsets.UTF_8);}}
    private void add(JSONObject out,String code,String url,boolean overlay)throws Exception{if(url.startsWith("//"))url="https:"+url;HttpUrl parsed=HttpUrl.parse(url);if(parsed==null||!parsed.isHttps()||!Arrays.asList("cdn.7tv.app","cdn.betterttv.net","cdn.frankerfacez.com").contains(parsed.host())||code.isEmpty())return;out.put(code,new JSONObject().put("url",url).put("overlay",overlay));}
    private void bttv(JSONObject out,JSONArray emotes)throws Exception{if(emotes==null)return;for(int i=0;i<emotes.length();i++){JSONObject e=emotes.getJSONObject(i);add(out,e.getString("code"),"https://cdn.betterttv.net/emote/"+e.getString("id")+"/2x",e.optBoolean("modifier"));}}
    private void seven(JSONObject out,JSONObject set)throws Exception{if(set==null)return;JSONArray emotes=set.optJSONArray("emotes");if(emotes==null)return;for(int i=0;i<emotes.length();i++){JSONObject e=emotes.getJSONObject(i),data=e.optJSONObject("data");if(data==null)continue;JSONObject host=data.optJSONObject("host");if(host==null)continue;JSONArray files=host.optJSONArray("files");String file="";if(files!=null)for(int k=0;k<files.length();k++){String name=files.getJSONObject(k).optString("name");if(name.equals("2x.webp")){file=name;break;}if(file.isEmpty()&&name.endsWith(".webp"))file=name;}if(!file.isEmpty())add(out,e.getString("name"),host.getString("url")+"/"+file,(data.optInt("flags")&256)!=0);}}
    private void ffz(JSONObject out,JSONObject response)throws Exception{JSONObject sets=response.optJSONObject("sets");if(sets==null)return;JSONArray defaults=response.optJSONArray("default_sets");Set<String> allowed=new HashSet<>();if(defaults!=null)for(int i=0;i<defaults.length();i++)allowed.add(defaults.get(i).toString());Iterator<String> keys=sets.keys();while(keys.hasNext()){String key=keys.next();if(defaults!=null&&!allowed.contains(key))continue;JSONArray emotes=sets.getJSONObject(key).getJSONArray("emoticons");for(int i=0;i<emotes.length();i++){JSONObject e=emotes.getJSONObject(i),urls=e.optJSONObject("animated");if(urls==null)urls=e.getJSONObject("urls");add(out,e.getString("name"),urls.optString("2",urls.optString("1")),e.optBoolean("modifier"));}}}
    private void loadCatalog(){if(closed||loading)return;loading=true;final String id=room;worker.execute(()->{
        JSONObject out=new JSONObject();
        try{ffz(out,new JSONObject(get("https://api.frankerfacez.com/v1/set/global")));}catch(Exception ignored){}
        try{bttv(out,new JSONArray(get("https://api.betterttv.net/3/cached/emotes/global")));}catch(Exception ignored){}
        try{seven(out,new JSONObject(get("https://7tv.io/v3/emote-sets/global")));}catch(Exception ignored){}
        try{ffz(out,new JSONObject(get("https://api.frankerfacez.com/v1/room/"+login)));}catch(Exception ignored){}
        if(id.matches("[0-9]+")){
            try{JSONObject j=new JSONObject(get("https://api.betterttv.net/3/cached/users/twitch/"+id));bttv(out,j.optJSONArray("channelEmotes"));bttv(out,j.optJSONArray("sharedEmotes"));}catch(Exception ignored){}
            try{seven(out,new JSONObject(get("https://7tv.io/v3/users/twitch/"+id)).optJSONObject("emote_set"));}catch(Exception ignored){}
        }
        main.post(()->{if(closed)return;loading=false;catalog=out;if(ready)view.evaluateJavascript("setCatalog("+out+")",null);if(!id.equals(room))loadCatalog();});
    });}
    @Override public void close(){closed=true;main.removeCallbacksAndMessages(null);if(reader!=null)reader.close();worker.shutdownNow();queue.clear();view.stopLoading();view.destroy();}
}
