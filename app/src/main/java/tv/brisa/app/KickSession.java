package tv.brisa.app;

import android.content.*;
import android.os.*;
import android.webkit.*;
import java.util.*;
import org.json.*;

/** Experimental website-session integration. No password or cookie is exported or logged. */
final class KickSession implements AutoCloseable {
    interface Result {void done(String json,String error);}
    final WebView web;final Handler main=new Handler(Looper.getMainLooper());boolean closed,busy;
    KickSession(Context context){
        web=new WebView(context);web.setFocusable(true);web.setFocusableInTouchMode(true);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(false);web.setBackgroundColor(android.graphics.Color.BLACK);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !allowed(r.getUrl());}});
    }
    static boolean allowed(android.net.Uri u){String h=u.getHost();return "https".equals(u.getScheme())&&h!=null&&(h.equals("kick.com")||h.endsWith(".kick.com"));}
    static boolean connected(Context c){return c.getSharedPreferences("kick",0).getBoolean("connected",false);}
    static List<TwitchAccount.Stream> cached(Context c){try{return parse(c.getSharedPreferences("kick",0).getString("channels","[]"));}catch(Exception e){return new ArrayList<>();}}
    static List<TwitchAccount.Stream> allCached(Context c){try{return parseAll(c.getSharedPreferences("kick",0).getString("channels","[]"));}catch(Exception e){return new ArrayList<>();}}
    static List<TwitchAccount.Stream> parse(String json)throws Exception{List<TwitchAccount.Stream> all=parseAll(json);all.removeIf(s->!s.live);return all;}
    static List<TwitchAccount.Stream> parseAll(String json)throws Exception{
        List<TwitchAccount.Stream> out=new ArrayList<>();Set<String> seen=new HashSet<>();JSONArray data=new JSONArray(json);
        for(int i=0;i<data.length();i++){JSONObject v=data.getJSONObject(i);
            String slug=v.optString("channel_slug","");String key;try{key=ChannelKey.parse("kick:"+slug);}catch(Exception e){continue;}if(!seen.add(key))continue;
            TwitchAccount.Stream s=new TwitchAccount.Stream();s.live=v.optBoolean("is_live");s.id=key;s.login=key;s.name=v.optString("user_username",slug);if(s.name.isEmpty())s.name=slug;
            s.title=v.optString("session_title","");s.game=v.optString("category_name","");s.avatar=v.optString("profile_picture","");s.thumbnail="";s.viewers=v.optInt("viewer_count");out.add(s);
        }return out;
    }
    static void save(Context c,String json)throws Exception{parse(json);c.getSharedPreferences("kick",0).edit().putBoolean("connected",true).putString("channels",json).putLong("updated",System.currentTimeMillis()).apply();CookieManager.getInstance().flush();}
    void read(Result result){
        if(closed||busy)return;String url=web.getUrl();android.net.Uri u=url==null?android.net.Uri.EMPTY:android.net.Uri.parse(url);
        if(!"kick.com".equals(u.getHost())||!"https".equals(u.getScheme())){result.done(null,"Abre Kick e inicia sesión primero.");return;}
        busy=true;String token="";String cookies=CookieManager.getInstance().getCookie("https://kick.com");
        if(cookies!=null)for(String part:cookies.split(";")){String p=part.trim();if(p.startsWith("session_token="))token=android.net.Uri.decode(p.substring(14));}
        if(token.isEmpty()){busy=false;result.done(null,"Inicia sesión en Kick antes de sincronizar tus seguidos.");return;}
        String js="(function(){window.__twitFollowResult=null;var headers={Accept:'application/json'};var token="+JSONObject.quote(token)+";if(token)headers.Authorization='Bearer '+token;"+
            "(async function(){try{var all=[],cursor=null,seen={};for(var page=0;page<100;page++){var url='/api/v2/channels/followed'+(cursor===null?'':'?cursor='+encodeURIComponent(cursor));var r=await fetch(url,{credentials:'include',headers:headers});if(!r.ok)throw new Error('HTTP '+r.status);var j=await r.json();if(!Array.isArray(j.channels))throw new Error('Formato no compatible');all=all.concat(j.channels);if(all.length>10000)throw new Error('Lista demasiado grande');cursor=j.nextCursor;if(cursor===null||cursor===undefined){window.__twitFollowResult={channels:all};return;}if(seen[cursor])throw new Error('Página repetida');seen[cursor]=true;}throw new Error('Demasiadas páginas');}catch(e){window.__twitFollowResult={error:String(e.message)};}})();})()";
        web.evaluateJavascript(js,null);poll(result,SystemClock.elapsedRealtime()+30000);
    }
    void poll(Result result,long deadline){main.postDelayed(()->{if(closed)return;if(SystemClock.elapsedRealtime()>deadline){busy=false;result.done(null,"Kick no respondió. Vuelve a intentarlo.");return;}
        web.evaluateJavascript("JSON.stringify(window.__twitFollowResult||null)",value->{if(closed)return;try{
            Object decoded=new JSONTokener(value).nextValue();if(!(decoded instanceof String)||"null".equals(decoded)){poll(result,deadline);return;}
            JSONObject j=new JSONObject((String)decoded);busy=false;
            if(j.has("error")){result.done(null,"Kick no permitió consultar tus seguidos. Inicia sesión y vuelve a sincronizar.");return;}
            String data=j.getJSONArray("channels").toString();if(data.length()>2000000)throw new IllegalArgumentException();result.done(data,null);
        }catch(Exception e){busy=false;result.done(null,"No se pudo leer la lista de Kick.");}});
    },500);}
    void refresh(Result result){
        final boolean[] started={false};Runnable timeout=()->{if(!closed&&!started[0]){started[0]=true;result.done(null,"Kick no respondió.");}};main.postDelayed(timeout,15000);
        web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !allowed(r.getUrl());}
            @Override public void onPageFinished(WebView v,String url){if(closed||started[0])return;started[0]=true;main.removeCallbacks(timeout);read(result);}});
        web.loadUrl("https://kick.com/robots.txt");
    }
    @Override public void close(){if(closed)return;closed=true;main.removeCallbacksAndMessages(null);web.stopLoading();web.destroy();}
}
