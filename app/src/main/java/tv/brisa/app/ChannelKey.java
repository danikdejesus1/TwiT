package tv.brisa.app;

import java.net.URI;
import java.util.Locale;

/** Twitch keeps its legacy key; Kick is namespaced to prevent account collisions. */
final class ChannelKey {
    static boolean kick(String key){return key.startsWith("kick:");}
    static String slug(String key){return kick(key)?key.substring(5):key;}
    static String label(String key){return slug(key)+" · "+(kick(key)?"Kick":"Twitch");}
    static int color(String key){return kick(key)?0xff53fc18:0xffa970ff;}
    static String parse(String input){
        String s=input.trim().toLowerCase(Locale.ROOT);boolean kick=false;
        if(s.startsWith("kick:")){kick=true;s=s.substring(5);}
        else if(s.contains("://")||s.startsWith("kick.com/")||s.startsWith("www.kick.com/")||s.startsWith("twitch.tv/")||s.startsWith("www.twitch.tv/")){
            URI u=URI.create(s.contains("://")?s:"https://"+s);
            if(!"https".equals(u.getScheme())&&!"http".equals(u.getScheme()))throw new IllegalArgumentException("Enlace no válido");
            String host=u.getHost();kick="kick.com".equals(host)||"www.kick.com".equals(host);
            if(!kick&&!"twitch.tv".equals(host)&&!"www.twitch.tv".equals(host))throw new IllegalArgumentException("Usa un enlace de Twitch o Kick");
            s=u.getPath().replaceFirst("^/","").replaceFirst("/$","");
        }
        if(!s.matches(kick?"[a-z0-9_][a-z0-9_-]{0,24}":"[a-z0-9_]{1,25}"))throw new IllegalArgumentException("Introduce un canal o enlace de Twitch o Kick");
        return kick?"kick:"+s:s;
    }
}
