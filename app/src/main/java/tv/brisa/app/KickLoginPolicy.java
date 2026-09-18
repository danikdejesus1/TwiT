package tv.brisa.app;

import java.net.URI;

/** Exact HTTPS authorization hosts only; no provider-wide wildcard or user-agent spoofing. */
final class KickLoginPolicy {
    private static String host(String url){
        try{URI u=new URI(url);if(!"https".equalsIgnoreCase(u.getScheme())||u.getRawUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return "";return u.getHost()==null?"":u.getHost().toLowerCase(java.util.Locale.ROOT);}catch(Exception e){return "";}
    }
    static boolean apple(String url){String h=host(url);return h.equals("appleid.apple.com")||h.equals("account.apple.com");}
    static boolean google(String url){return host(url).equals("accounts.google.com");}
}
