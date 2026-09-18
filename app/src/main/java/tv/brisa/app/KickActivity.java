package tv.brisa.app;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.webkit.*;
import android.content.Intent;
import android.net.Uri;

/** Displays the official Kick login and provider authorization pages. */
public final class KickActivity extends Activity {
    KickSession session;KickSession popup;FrameLayout root;LinearLayout social;LinearLayout actions;TextView status;boolean player,closed,pageControls=true;final Handler main=new Handler(Looper.getMainLooper());
    final Runnable hide=()->{if(player&&hasWindowFocus())actions.setVisibility(View.GONE);};
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    Button button(String label,Runnable action){Button b=new Button(this);b.setFocusableInTouchMode(true);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setOnClickListener(v->action.run());
        View.OnFocusChangeListener focus=(v,f)->{if(f)pageControls=false;GradientDrawable bg=new GradientDrawable();bg.setColor(f?0xff203e21:0xff19282b);bg.setCornerRadius(dp(12));bg.setStroke(dp(2),f?0xff53fc18:0xff294139);b.setBackground(bg);};b.setOnFocusChangeListener(focus);focus.onFocusChange(b,false);return b;}
    void add(LinearLayout row,Button b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(44));p.leftMargin=dp(8);row.addView(b,p);}
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);getWindow().addFlags(128);
        String key=getIntent().getStringExtra("channel");player=key!=null;
        if(player)try{key=ChannelKey.parse(key);if(!ChannelKey.kick(key))throw new IllegalArgumentException();}catch(Exception e){finish();return;}
        session=new KickSession(this);root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        FrameLayout.LayoutParams webParams=new FrameLayout.LayoutParams(-1,-1);if(!player){webParams.topMargin=dp(88);webParams.bottomMargin=dp(64);}root.addView(session.web,webParams);
        actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);actions.setPadding(dp(20),dp(8),dp(20),dp(8));actions.setBackgroundColor(0xf00c1418);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView name=new TextView(this);name.setText(player?"KICK · "+ChannelKey.slug(key):"Conecta tu cuenta de Kick");name.setTextColor(0xff53fc18);name.setTextSize(22);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        add(row,button("Volver a TwiT",this::finish));
        if(player){add(row,button("Recargar",()->session.web.reload()));add(row,button("Controles Kick",()->{actions.setVisibility(View.GONE);session.web.requestFocus();}));}
        else{add(row,button("Sincronizar seguidos",this::sync));add(row,button("Iniciar sesión",this::openLogin));}
        actions.addView(row);status=new TextView(this);status.setTextColor(0xffb5c8c0);status.setTextSize(12);status.setText(player?"Menú: opciones de TwiT · Atrás: volver":"Flechas: cambiar de campo · OK: escribir o aceptar · Menú: sincronizar seguidos");actions.addView(status);
        root.addView(actions,new FrameLayout.LayoutParams(-1,-2,player?Gravity.BOTTOM:Gravity.TOP));
        if(!player){
            configureLogin(session.web);
            social=new LinearLayout(this);social.setGravity(Gravity.CENTER);social.setPadding(dp(16),dp(8),dp(16),dp(8));social.setBackgroundColor(0xff0c1418);
            add(social,button("Continuar con Google",()->openSocial("google")));
            add(social,button("Continuar con Apple",()->openSocial("apple")));
            root.addView(social,new FrameLayout.LayoutParams(-1,dp(64),Gravity.BOTTOM));
        }
        session.web.loadUrl(player?"https://player.kick.com/"+ChannelKey.slug(key)+"?autoplay=true&muted=false":"https://kick.com");
        session.web.requestFocus();pageControls=true;if(player)main.postDelayed(hide,5000);
    }
    WebView activeWeb(){return popup==null?session.web:popup.web;}
    void configureLogin(WebView web){
        web.getSettings().setSupportMultipleWindows(true);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        web.setOnFocusChangeListener((v,f)->{if(f)pageControls=true;});
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return route(r.getUrl(),r.isForMainFrame());}
            @Override public boolean shouldOverrideUrlLoading(WebView v,String url){return route(Uri.parse(url),true);}
            @Override public void onPageFinished(WebView v,String url){
                if(closed)return;
                String host=Uri.parse(url).getHost();
                if(KickLoginPolicy.apple(url))status.setText("Apple · "+host+" · Atrás: volver a Kick");
                v.evaluateJavascript("(function(){var s=document.createElement('style');s.textContent='input:focus,button:focus,a:focus,[tabindex]:focus{outline:3px solid #53fc18!important;outline-offset:3px!important}';document.head.appendChild(s);})()",null);
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onCreateWindow(WebView source,boolean dialog,boolean gesture,Message result){
                if(closed||popup!=null)return false;
                popup=new KickSession(KickActivity.this);configureLogin(popup.web);
                FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-1);p.topMargin=dp(88);p.bottomMargin=dp(64);root.addView(popup.web,1,p);
                ((WebView.WebViewTransport)result.obj).setWebView(popup.web);result.sendToTarget();popup.web.requestFocus();pageControls=true;return true;
            }
            @Override public void onCloseWindow(WebView window){if(popup!=null&&popup.web==window)closePopup();}
        });
    }
    boolean route(Uri uri,boolean mainFrame){
        String url=uri.toString();
        if(KickSession.allowed(uri)||KickLoginPolicy.apple(url))return false;
        if(mainFrame&&KickLoginPolicy.google(url)){
            try{startActivity(new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE));status.setText("Google se abre en el navegador. Su sesión no se transfiere automáticamente a TwiT.");}
            catch(android.content.ActivityNotFoundException e){status.setText("Google necesita un navegador compatible instalado en el Fire TV.");}
            if(popup!=null)main.post(this::closePopup);
        }else if(mainFrame)status.setText("No se abrió una dirección ajena al acceso de Kick, Apple o Google.");
        return true;
    }
    void closePopup(){if(popup==null)return;root.removeView(popup.web);popup.close();popup=null;session.web.requestFocus();pageControls=true;}
    void openSocial(String provider){
        closePopup();
        if(!KickSession.allowed(Uri.parse(session.web.getUrl()==null?"":session.web.getUrl()))){session.web.loadUrl("https://kick.com");status.setText("Espera a que cargue Kick y vuelve a elegir tu cuenta.");return;}
        session.web.requestFocus();pageControls=true;
        session.web.evaluateJavascript("(function(){var e=document.querySelector('[data-testid=oauth-"+provider+"]');if(e){e.scrollIntoView({block:'center'});e.click();return true;}var b=document.querySelector('button[data-testid=login]');if(b)b.click();return false;})()",result->{
            if(closed||"true".equals(result))return;
            main.postDelayed(()->{if(closed)return;session.web.evaluateJavascript("(function(){var e=document.querySelector('[data-testid=oauth-"+provider+"]');if(!e)return false;e.scrollIntoView({block:'center'});e.click();return true;})()",ok->{if(!closed&&!"true".equals(ok))status.setText("Kick todavía está cargando. Vuelve a pulsar el botón.");});},700);
        });
    }
    void sync(){if(popup!=null){status.setText("Termina la autorización y vuelve a Kick antes de sincronizar.");return;}status.setText("Consultando tus canales seguidos…");session.read((json,error)->{if(closed)return;if(error!=null){status.setText(error);return;}try{KickSession.save(this,json);setResult(RESULT_OK);finish();}catch(Exception e){status.setText("Kick devolvió una lista que todavía no podemos leer.");}});}
    void openLogin(){closePopup();if(!KickSession.allowed(Uri.parse(session.web.getUrl()==null?"":session.web.getUrl()))){session.web.loadUrl("https://kick.com");return;}session.web.requestFocus();pageControls=true;session.web.evaluateJavascript("(function(){var b=document.querySelector('button[data-testid=login]');if(b)b.click();setTimeout(function(){var e=Array.from(document.querySelectorAll('input')).find(function(x){return x.type!=='hidden'&&!x.disabled&&x.getBoundingClientRect().height>0});if(e){window.__twitFocus=e;e.focus();e.scrollIntoView({block:'center'});}},400);})()",null);}
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        int key=e.getKeyCode();
        if(key==KeyEvent.KEYCODE_MENU){if(e.getAction()==KeyEvent.ACTION_DOWN){pageControls=false;actions.setVisibility(View.VISIBLE);((LinearLayout)actions.getChildAt(0)).getChildAt(player?1:2).requestFocus();if(player){main.removeCallbacks(hide);main.postDelayed(hide,5000);}}return true;}
        if(!player&&session!=null&&pageControls){
            if(key>=KeyEvent.KEYCODE_DPAD_UP&&key<=KeyEvent.KEYCODE_DPAD_RIGHT){
                if(e.getAction()==KeyEvent.ACTION_DOWN){int direction=(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_LEFT)?-1:1;
                    activeWeb().evaluateJavascript("(function(){var pw=document.querySelector('input[type=password]');var scope=document.querySelector('[role=dialog]')||document;var items=Array.from(scope.querySelectorAll('input,textarea,button,a[href],[tabindex]')).filter(function(x){var r=x.getBoundingClientRect();return !x.disabled&&x.type!=='hidden'&&x.tabIndex>=0&&r.width>0&&r.height>0&&getComputedStyle(x).visibility!=='hidden';});if(!items.length)return;var i=items.indexOf(window.__twitFocus||document.activeElement);i=(i+"+direction+"+items.length)%items.length;var e=items[i];window.__twitFocus=e;e.focus();e.scrollIntoView({block:'center'});})()",null);}
                return true;
            }
            if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){
                if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0)activeWeb().evaluateJavascript("(function(){var e=window.__twitFocus||document.activeElement;if(!e||!e.isConnected)return null;var r=e.getBoundingClientRect();return {x:r.left+r.width/2,y:r.top+r.height/2,width:window.innerWidth};})()",value->{if(closed)return;try{org.json.JSONObject rect=new org.json.JSONObject(value);float scale=activeWeb().getWidth()/(float)rect.getDouble("width");float x=(float)rect.getDouble("x")*scale,y=(float)rect.getDouble("y")*scale;long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(now,now+40,MotionEvent.ACTION_UP,x,y,0);activeWeb().dispatchTouchEvent(down);activeWeb().dispatchTouchEvent(up);down.recycle();up.recycle();}catch(Exception ignored){}});
                return true;
            }
        }
        if(player&&actions.getVisibility()==View.VISIBLE){main.removeCallbacks(hide);main.postDelayed(hide,5000);}return super.dispatchKeyEvent(e);
    }
    @Override protected void onResume(){super.onResume();if(session!=null)session.web.onResume();}
    @Override protected void onPause(){if(session!=null){if(player)session.web.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){v.pause();})",null);session.web.onPause();}super.onPause();}
    @Override public void onBackPressed(){if(popup!=null){closePopup();return;}if(!player&&!KickSession.allowed(Uri.parse(session.web.getUrl()==null?"":session.web.getUrl()))){session.web.loadUrl("https://kick.com");return;}finish();}
    @Override protected void onDestroy(){closed=true;main.removeCallbacksAndMessages(null);if(popup!=null)popup.close();if(session!=null)session.close();super.onDestroy();}
}
