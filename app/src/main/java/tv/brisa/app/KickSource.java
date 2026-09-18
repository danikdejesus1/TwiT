package tv.brisa.app;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

/** Experimental public HLS playback. No account cookies, proxies or challenge bypass. */
final class KickSource {
    static String resolve(String key)throws Exception{
        String normalized=ChannelKey.parse(key);
        if(!ChannelKey.kick(normalized))throw new IllegalArgumentException("Se esperaba un canal de Kick");
        HttpUrl endpoint=new HttpUrl.Builder().scheme("https").host("kick.com")
            .addPathSegments("api/v2/channels").addPathSegment(ChannelKey.slug(normalized)).build();
        try(Response r=TwitchSource.HTTP.newCall(new Request.Builder().url(endpoint).header("Accept","application/json").build()).execute()){
            if(!r.isSuccessful())throw new IOException("Kick no permitió abrir el directo ("+r.code()+")");
            if(r.body()==null||r.body().contentLength()>2000000)throw new IOException("Respuesta de Kick no válida");
            byte[] data=r.peekBody(2000001).bytes();if(data.length>2000000)throw new IOException("Respuesta de Kick demasiado grande");
            JSONObject j=new JSONObject(new String(data,java.nio.charset.StandardCharsets.UTF_8));
            if(j.isNull("livestream")||!j.has("livestream"))throw new IOException("Este canal de Kick está desconectado");
            String url=j.optString("playback_url","");
            if(!validPlayback(url))throw new IOException("Kick no proporcionó una fuente de vídeo compatible");
            return url;
        }
    }
    static boolean validPlayback(String value){
        HttpUrl u=HttpUrl.parse(value);if(u==null||!u.isHttps()||u.port()!=443||!u.username().isEmpty()||!u.password().isEmpty())return false;
        String h=u.host();return (h.endsWith(".playback.live-video.net")||h.equals("stream.kick.com"))&&u.encodedPath().endsWith(".m3u8");
    }
}
