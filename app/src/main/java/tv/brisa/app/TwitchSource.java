package tv.brisa.app;

import okhttp3.*;
import org.json.*;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Experimental public-stream resolver. No account credentials, proxies or integrity bypass. */
final class TwitchSource {
    static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS).build();
    static String channel(String input) {
        String value = input.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("https://")) {
            HttpUrl url = HttpUrl.parse(value);
            if (url == null || !(url.host().equals("twitch.tv") || url.host().equals("www.twitch.tv")))
                throw new IllegalArgumentException("Escribe un canal o un enlace de twitch.tv");
            value = url.pathSegments().get(0);
        }
        if (!value.matches("[a-z0-9_]{1,25}")) throw new IllegalArgumentException("Nombre de canal inválido");
        return value;
    }
    static String resolve(String channel) throws Exception {return resolveToken(channel,"");}
    static String resolveVod(String id)throws Exception{if(!id.matches("[0-9]+"))throw new IllegalArgumentException("VOD inválido");return resolveToken("",id);}
    private static String resolveToken(String channel,String vod) throws Exception {
        boolean live=vod.isEmpty();
        JSONObject vars = new JSONObject().put("isLive", live).put("login", channel)
                .put("isVod", !live).put("vodID", vod).put("playerType", "embed").put("platform", "site");
        JSONObject query = new JSONObject().put("operationName", "PlaybackAccessToken")
                .put("variables", vars).put("extensions", new JSONObject().put("persistedQuery",
                        new JSONObject().put("version", 1).put("sha256Hash", "ed230aa1e33e07eebb8928504583da78a5173989fadfb1ac94be06a04f3cdbe9")));
        Request request = new Request.Builder().url("https://gql.twitch.tv/gql")
                .header("Client-ID", "kimne78kx3ncx6brgo4mv6wki5h1ko")
                .header("User-Agent", "Mozilla/5.0")
                .post(RequestBody.create(query.toString(), MediaType.get("application/json"))).build();
        try (Response response = HTTP.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Twitch rechazó la conexión (" + response.code() + ")");
            JSONObject json = new JSONObject(response.body().string());
            if (json.has("errors")) throw new IOException("Twitch no autorizó este reproductor. La integración experimental puede dejar de funcionar.");
            JSONObject data = json.optJSONObject("data");
            JSONObject token = data == null ? null : data.optJSONObject(live?"streamPlaybackAccessToken":"videoPlaybackAccessToken");
            if (token == null) throw new IOException("No se encontró un directo disponible para este canal");
            return new HttpUrl.Builder().scheme("https").host("usher.ttvnw.net")
                    .addPathSegments(live?"api/v2/channel/hls":"vod").addPathSegment((live?channel:vod) + ".m3u8")
                    .addQueryParameter("sig", token.getString("signature"))
                    .addQueryParameter("token", token.getString("value"))
                    .addQueryParameter("allow_source", "true").addQueryParameter("allow_audio_only", "true")
                    .addQueryParameter("supported_codecs", "h264").addQueryParameter("platform", "web")
                    .build().toString();
        }
    }
}
