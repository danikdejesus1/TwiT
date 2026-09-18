package tv.brisa.app;

import okhttp3.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** Anonymous read-only IRC connection. Never sends chat messages. */
final class ChatReader implements AutoCloseable {
    private WebSocket socket;
    ChatReader(String channel, Consumer<String> sink) {
        socket = TwitchSource.HTTP.newWebSocket(new Request.Builder().url("wss://irc-ws.chat.twitch.tv:443").build(), new WebSocketListener() {
            @Override public void onOpen(WebSocket s, Response response) {
                s.send("CAP REQ :twitch.tv/tags twitch.tv/commands\r\n");
                s.send("NICK justinfan" + ThreadLocalRandom.current().nextInt(10000, 99999) + "\r\n");
                s.send("JOIN #" + channel + "\r\n");
            }
            @Override public void onMessage(WebSocket s, String message) {
                for (String line : message.split("\r\n")) {
                    if (line.startsWith("PING ")) s.send("PONG " + line.substring(5) + "\r\n");
                    ChatLine parsed=ChatLine.parse(line);
                    if(parsed.command.equals("PRIVMSG")||parsed.command.equals("ROOMSTATE")||parsed.command.equals("CLEARMSG")||parsed.command.equals("CLEARCHAT"))sink.accept(line);
                }
            }
            @Override public void onFailure(WebSocket s, Throwable t, Response response) {  }
        });
    }
    @Override public void close() { if (socket != null) { socket.cancel(); socket = null; } }
}
