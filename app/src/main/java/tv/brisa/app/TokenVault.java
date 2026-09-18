package tv.brisa.app;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class TokenVault {
    private final SharedPreferences prefs;
    TokenVault(Context c) { prefs=c.getSharedPreferences("twit-account",0); }
    private javax.crypto.SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias("twit.oauth")) {
            KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");
            g.init(new KeyGenParameterSpec.Builder("twit.oauth",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();
        }
        return (javax.crypto.SecretKey)ks.getKey("twit.oauth",null);
    }
    String read() throws Exception {
        String raw=prefs.getString("session","");if(raw.isEmpty())return "";
        String[] p=raw.split(":",2);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],0)));
        return new String(c.doFinal(Base64.decode(p[1],0)),java.nio.charset.StandardCharsets.UTF_8);
    }
    void write(String json) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
        String value=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP);
        if(!prefs.edit().putString("session",value).commit())throw new java.io.IOException("No se pudo guardar la sesión");
    }
    void clear(){prefs.edit().remove("session").commit();}
}
