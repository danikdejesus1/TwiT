/**
 * TwiT — Creado por DanikDeJesus
 * Copyright (c) 2026 DanikDeJesus.
 * Firma de autor: no modifica el funcionamiento de la aplicación.
 */
package tv.brisa.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.TextUtils;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;
import okhttp3.*;

@androidx.media3.common.util.UnstableApi
public final class MainActivity extends Activity {
    static final int BG=0xff0c1418,PANEL=0xff19282b,MINT=0xff69ffb4,WHITE=0xfff3fff8,MUTED=0xff9fb5af;
    final Handler main=new Handler(Looper.getMainLooper());
    final ExecutorService net=Executors.newSingleThreadExecutor(),images=Executors.newFixedThreadPool(3);
    final android.util.LruCache<String,Bitmap> cache=new android.util.LruCache<String,Bitmap>(12*1024*1024){protected int sizeOf(String k,Bitmap v){return v.getByteCount();}};
    KickSession kickLoader;FrameLayout heroVisual;
    KickChat kickChat;EmoteChat chat;android.webkit.WebView chatText;int chatVersion;
    TwitchAccount account;SharedPreferences prefs;FrameLayout root,hero;LinearLayout rail,controls;TextView status,heroTitle,heroMeta,heroName;ImageView heroImage;Button watch,pause;
    final Map<String,TwitchAccount.Stream> directory=new LinkedHashMap<>();LinearLayout offlineRow;TextView offlineHeading;boolean loadingDirectory,browsingVods,vodMode;long directoryUpdated;String libraryChannel="",shelfSignature="";
    List<TwitchAccount.Stream> streams=new ArrayList<>();TwitchAccount.Stream selected;
    ExoPlayer preview;PlayerView previewView;int previewVersion;String previewLogin="";boolean foreground=true;Runnable previewJob;
    final List<Pane> panes=new ArrayList<>();LinearLayout videoGrid;ImageView channelIcon;TextView channelHeading;int activePane;
    final class Pane {String login;ExoPlayer engine;DefaultTrackSelector selector;FrameLayout box;PlayerView view;TextView label;}
    ExoPlayer player;DefaultTrackSelector tracks;String channel="";boolean playing,destroyed,refreshing,paused;int screenVersion,requestVersion;volatile int authVersion;AlertDialog authDialog;long authDeadline;int pollInterval=5;
    SeekBar vodSeek;TextView vodTime;boolean scrubbing;
    final Runnable vodTick=new Runnable(){public void run(){if(!playing||!vodMode||vodSeek==null)return;updateVodProgress();main.postDelayed(this,500);}};
    final Set<Integer> held=new HashSet<>();int revealKey=-1;boolean touching;
    final Runnable hide=new Runnable(){public void run(){if(!playing||controls==null)return;if(!hasWindowFocus()||!held.isEmpty()||touching){scheduleHide();return;}controls.setVisibility(View.GONE);}};
    final Runnable refreshTick=new Runnable(){public void run(){if(destroyed)return;if(!playing&&!browsingVods)refresh();else if(account.connected())net.execute(()->{try{account.validateSession();}catch(Exception ignored){}});main.postDelayed(this,60000);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().getDecorView().setSystemUiVisibility(5894);prefs=getSharedPreferences("brisa",0);account=new TwitchAccount(this);streams.addAll(KickSession.cached(this));loadCatalog();home();net.execute(this::cleanOldModels);}
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    GradientDrawable outline(int color,int radius,int stroke){GradientDrawable d=shape(color,radius);d.setStroke(dp(2),stroke);return d;}
    TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    Button button(String title,Runnable run){Button b=new Button(this);b.setId(View.generateViewId());b.setText(title);b.setTextSize(14);b.setTextColor(WHITE);b.setAllCaps(false);b.setPadding(dp(18),dp(5),dp(18),dp(5));b.setBackground(shape(PANEL,12));b.setMinHeight(dp(44));b.setOnClickListener(v->run.run());b.setOnFocusChangeListener((v,f)->{b.setBackground(f?outline(0xff234439,12,MINT):shape(PANEL,12));b.setTextColor(f?MINT:WHITE);});return b;}
    void marginAdd(LinearLayout row,View v,int w,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));p.setMargins(0,0,dp(10),0);row.addView(v,p);}
    Button subtle(String title,Runnable run){Button b=button(title,run);b.setTextSize(12);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),0,dp(12),0);b.setBackground(shape(0x221f3534,10));b.setOnFocusChangeListener((v,f)->{b.setBackground(f?outline(0xff234439,10,MINT):shape(0x221f3534,10));b.setTextColor(f?MINT:MUTED);});return b;}
    void accounts(){optionSheet("Tus cuentas",new String[]{"Kick  ·  "+(KickSession.connected(this)?"Conectada":"Ingresar"),"Twitch  ·  "+(account.connected()?"@"+account.name():"Ingresar")},-1,i->{if(i==0)kickAccount();else accountDialog();});}
    void home(){release();browsingVods=false;vodMode=false;HomeCatalog.sortLive(streams);if(selected==null&&!streams.isEmpty())selected=streams.get(0);playing=false;screenVersion++;requestVersion++;root=new FrameLayout(this);root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff0c1418,0xff102522,0xff0a1118}));setContentView(root);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);LinearLayout body=col();body.setPadding(dp(120),dp(18),dp(30),dp(28));scroll.addView(body);root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView brand=text("TwiT",30,MINT);brand.setTypeface(null,Typeface.BOLD);top.addView(brand,new LinearLayout.LayoutParams(0,dp(38),1));
        Button search=subtle("Buscar canal",this::search);search.setCompoundDrawablesWithIntrinsicBounds(tv.brisa.app.R.drawable.ic_search,0,0,0);search.setCompoundDrawablePadding(dp(8));search.setBackgroundColor(Color.TRANSPARENT);marginAdd(top,search,148,32);marginAdd(top,subtle("Cuentas  ·  K / T",this::accounts),136,32);body.addView(top);
        TextView sub=text("TUS CANALES. TU MOMENTO.",10,MUTED);sub.setLetterSpacing(.15f);sub.setPadding(0,dp(4),0,dp(12));body.addView(sub);
        hero=new FrameLayout(this);hero.setBackground(shape(0xff152b29,20));hero.setClipToOutline(true);body.addView(hero,new LinearLayout.LayoutParams(-1,dp(285)));
        LinearLayout heroRow=new LinearLayout(this);hero.addView(heroRow,new FrameLayout.LayoutParams(-1,-1));LinearLayout content=col();content.setPadding(dp(20),dp(15),dp(18),dp(14));heroRow.addView(content,new LinearLayout.LayoutParams(0,-1,1));
        heroName=text("BIENVENIDO A TWIT",21,MINT);heroName.setTypeface(null,Typeface.BOLD);heroName.setSingleLine();heroName.setEllipsize(TextUtils.TruncateAt.END);content.addView(heroName);
        heroTitle=text("Todo lo que sigues. Más cerca.",18,WHITE);heroTitle.setMaxLines(2);heroTitle.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams titleP=new LinearLayout.LayoutParams(-1,0,1);titleP.topMargin=dp(8);content.addView(heroTitle,titleP);
        heroMeta=text("Conecta Twitch o Kick",11,MUTED);heroMeta.setSingleLine();heroMeta.setEllipsize(TextUtils.TruncateAt.END);content.addView(heroMeta);
        watch=subtle("Ver directo  →",()->{if(selected!=null)openChannel(selected.login);else accounts();});watch.setTextColor(MINT);LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(dp(148),dp(36));wp.topMargin=dp(8);content.addView(watch,wp);
        heroVisual=new FrameLayout(this);heroVisual.setBackgroundColor(Color.BLACK);heroRow.addView(heroVisual,new LinearLayout.LayoutParams(0,-1,1.2f));heroImage=new ImageView(this);heroImage.setScaleType(ImageView.ScaleType.FIT_CENTER);heroVisual.addView(heroImage,new FrameLayout.LayoutParams(-1,-1));previewView=(PlayerView)getLayoutInflater().inflate(tv.brisa.app.R.layout.home_preview,heroVisual,false);previewView.setVisibility(View.INVISIBLE);heroVisual.addView(previewView);
        offlineHeading=text("OFFLINE",15,MUTED);offlineHeading.setTypeface(null,Typeface.BOLD);offlineHeading.setPadding(0,dp(16),0,dp(8));body.addView(offlineHeading);offlineRow=new LinearLayout(this);body.addView(shelf(offlineRow),new LinearLayout.LayoutParams(-1,dp(88)));
        status=text("",11,MUTED);status.setPadding(0,dp(10),0,0);body.addView(status);
        LinearLayout dock=col();dock.setGravity(Gravity.CENTER_HORIZONTAL);dock.setBackground(outline(0xf219272a,28,0xff294139));dock.setElevation(dp(12));dock.setClipToOutline(true);dock.setPadding(dp(6),dp(12),dp(6),dp(10));TextView live=text("● LIVE",11,MINT);live.setGravity(Gravity.CENTER);live.setTypeface(null,Typeface.BOLD);dock.addView(live,new LinearLayout.LayoutParams(-1,dp(28)));
        ScrollView railScroll=new ScrollView(this);railScroll.setVerticalScrollBarEnabled(false);railScroll.setClipToPadding(true);rail=col();rail.setGravity(Gravity.CENTER_HORIZONTAL);rail.setPadding(dp(4),dp(8),dp(4),dp(8));railScroll.addView(rail);dock.addView(railScroll,new LinearLayout.LayoutParams(-1,0,1));FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(88),-1,Gravity.LEFT);rp.setMargins(dp(18),dp(20),0,dp(20));root.addView(dock,rp);
        shelfSignature="";renderRail();renderShelves();renderSelection();watch.requestFocus();refresh();refreshDirectory();
    }
    HorizontalScrollView shelf(LinearLayout row){HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);row.setPadding(dp(3),dp(3),dp(3),dp(3));scroll.addView(row);scroll.setOnScrollChangeListener((View v,int x,int y,int ox,int oy)->loadVisibleAvatars(row));row.post(()->loadVisibleAvatars(row));return scroll;}
    ImageView platformBadge(String key){ImageView badge=new ImageView(this);badge.setImageResource(ChannelKey.kick(key)?tv.brisa.app.R.drawable.ic_kick:tv.brisa.app.R.drawable.ic_twitch);badge.setBackground(shape(0xff0c1418,5));badge.setPadding(dp(3),dp(3),dp(3),dp(3));badge.setContentDescription(ChannelKey.kick(key)?"Kick":"Twitch");return badge;}
    void loadVisibleAvatars(View v){if(v instanceof ImageView&&v.getTag() instanceof String&&((String)v.getTag()).startsWith("pending:")&&v.getGlobalVisibleRect(new Rect()))loadImage((ImageView)v,((String)v.getTag()).substring(8));if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)loadVisibleAvatars(g.getChildAt(i));}}
    void avatarInto(FrameLayout box,TwitchAccount.Stream info,int size){avatarInto(box,info,size,false);}
    void avatarInto(FrameLayout box,TwitchAccount.Stream info,int size,boolean lazy){ImageView image=new ImageView(this);image.setBackground(shape(0xff29443b,size/2));image.setClipToOutline(true);image.setScaleType(ImageView.ScaleType.CENTER_CROP);box.addView(image,new FrameLayout.LayoutParams(-1,-1));if(info!=null&&!info.avatar.isEmpty()){if(lazy)image.setTag("pending:"+info.avatar);else loadImage(image,info.avatar);}else{TextView letter=text(info==null||info.name.isEmpty()?"?":info.name.substring(0,1).toUpperCase(Locale.ROOT),20,WHITE);letter.setGravity(Gravity.CENTER);box.addView(letter,new FrameLayout.LayoutParams(-1,-1));}}
    View channelCard(String key,boolean offline){TwitchAccount.Stream info=streamInfo(key);LinearLayout card=col();card.setId(View.generateViewId());card.setGravity(Gravity.CENTER);card.setPadding(dp(6),dp(4),dp(6),dp(4));card.setFocusable(true);card.setClickable(true);String name=info==null?ChannelKey.slug(key):info.name;card.setContentDescription(name+", "+(ChannelKey.kick(key)?"Kick":"Twitch")+(offline?", offline, ver retransmisiones":""));
        FrameLayout avatar=new FrameLayout(this);avatarInto(avatar,info,46,true);card.addView(avatar,new LinearLayout.LayoutParams(dp(46),dp(46)));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(18),dp(18),Gravity.BOTTOM|Gravity.RIGHT);avatar.addView(platformBadge(key),bp);if(offline){avatar.setAlpha(.48f);ColorMatrix gray=new ColorMatrix();gray.setSaturation(0);((ImageView)avatar.getChildAt(0)).setColorFilter(new ColorMatrixColorFilter(gray));}
        TextView label=text(name,11,offline?MUTED:WHITE);label.setGravity(Gravity.CENTER);label.setSingleLine();label.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(22));lp.topMargin=dp(3);card.addView(label,lp);
        card.setOnFocusChangeListener((v,f)->card.setBackground(f?outline(0xff213b32,12,MINT):shape(Color.TRANSPARENT,12)));card.setOnClickListener(v->{if(offline)openLibrary(key);else openChannel(key);});return card;
    }
    void renderShelves(){if(playing||browsingVods||offlineRow==null)return;List<TwitchAccount.Stream> offline=HomeCatalog.offline(directory.values(),streams);StringBuilder sig=new StringBuilder(recentChannels().toString());for(TwitchAccount.Stream item:directory.values())sig.append(item.login).append(item.avatar);for(TwitchAccount.Stream item:streams)sig.append(item.login);if(sig.toString().equals(shelfSignature))return;shelfSignature=sig.toString();
        offlineRow.removeAllViews();offlineHeading.setText("OFFLINE");for(TwitchAccount.Stream item:offline){LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(76),dp(82));cp.rightMargin=dp(2);offlineRow.addView(channelCard(item.login,true),cp);}offlineRow.post(()->loadVisibleAvatars(offlineRow));if(offline.isEmpty())offlineRow.addView(text(loadingDirectory?"Cargando tus canales…":"Aquí aparecerán tus seguidos que no estén en directo.",12,MUTED));
    }
    void renderRail(){HomeCatalog.sortLive(streams);String focused=rail.findFocus()==null?"":String.valueOf(rail.findFocus().getTag());rail.removeAllViews();
        for(TwitchAccount.Stream item:streams){LinearLayout cell=col();cell.setId(View.generateViewId());cell.setTag(item.id);cell.setGravity(Gravity.CENTER);cell.setFocusable(true);cell.setClickable(true);cell.setNextFocusRightId(watch.getId());cell.setPadding(dp(5),dp(5),dp(5),dp(3));cell.setContentDescription(item.name+", "+(ChannelKey.kick(item.login)?"Kick":"Twitch"));FrameLayout avatar=new FrameLayout(this);avatar.setPadding(dp(2),dp(2),dp(2),dp(2));avatar.setBackground(outline(PANEL,28,ChannelKey.color(item.login)));avatarInto(avatar,item,50);cell.addView(avatar,new LinearLayout.LayoutParams(dp(50),dp(50)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(68),dp(68));lp.bottomMargin=dp(7);rail.addView(cell,lp);
            cell.setOnFocusChangeListener((v,f)->{avatar.setScaleX(f?1.18f:1);avatar.setScaleY(f?1.18f:1);if(f){selected=item;watch.setNextFocusLeftId(cell.getId());renderSelection();}});cell.setOnClickListener(v->openChannel(item.login));if(selected!=null&&selected.id.equals(item.id))watch.setNextFocusLeftId(cell.getId());if(item.id.equals(focused))cell.requestFocus();
        }
    }
    void loadCatalog(){try{org.json.JSONArray a=new org.json.JSONArray(prefs.getString("channel_catalog","[]"));for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);TwitchAccount.Stream item=new TwitchAccount.Stream();item.login=j.getString("key");if(ChannelKey.kick(item.login)?!KickSession.connected(this):!account.connected())continue;item.id=j.optString("id");item.name=j.optString("name",ChannelKey.slug(item.login));item.avatar=j.optString("avatar");directory.put(item.login,item);}}catch(Exception ignored){}for(TwitchAccount.Stream item:KickSession.allCached(this))directory.put(item.login,item);}
    void saveCatalog(){try{org.json.JSONArray a=new org.json.JSONArray();for(TwitchAccount.Stream item:directory.values())a.put(new JSONObject().put("key",item.login).put("id",item.id).put("name",item.name).put("avatar",item.avatar));prefs.edit().putString("channel_catalog",a.toString()).apply();}catch(Exception ignored){}}
    void refreshDirectory(){if(loadingDirectory||!account.connected()||SystemClock.elapsedRealtime()-directoryUpdated<300000&&directoryUpdated>0)return;loadingDirectory=true;String owner=account.name();net.execute(()->{try{List<TwitchAccount.Stream> all=account.allFollowed();main.post(()->{loadingDirectory=false;if(destroyed||!account.connected()||!owner.equals(account.name()))return;directoryUpdated=SystemClock.elapsedRealtime();directory.entrySet().removeIf(e->!ChannelKey.kick(e.getKey()));for(TwitchAccount.Stream item:all)directory.put(item.login,item);saveCatalog();renderShelves();});}catch(Exception e){main.post(()->{loadingDirectory=false;if(!destroyed&&!playing&&!browsingVods)status.setText("No se pudo actualizar la lista completa de Twitch. Se reintentará automáticamente.");});}});}
    void renderSelection(){if(selected==null){heroImage.setImageDrawable(null);heroImage.setTag("");stopPreview();if(account.connected()||KickSession.connected(this)){heroName.setText("TUS DIRECTOS, AQUÍ");heroTitle.setText("Tu próximo directo\nte espera.");heroMeta.setText("Los canales que sigues aparecerán aquí cuando estén en vivo.");watch.setText("Ver mi cuenta");}return;}heroName.setText(selected.name);heroName.setTextColor(ChannelKey.color(selected.login));heroTitle.setText(selected.title);heroMeta.setText((ChannelKey.kick(selected.login)?"Kick":"Twitch")+" · "+selected.game+"  ·  "+String.format(Locale.getDefault(),"%,d",selected.viewers)+" espectadores");watch.setText("Ver directo  →");if(!selected.thumbnail.equals(heroImage.getTag()))loadImage(heroImage,selected.thumbnail);startPreview();}
    List<String> recentChannels(){
        List<String> out=new ArrayList<>();String stored=prefs.getString("recent_channels",prefs.getString("last_channel",""));
        for(String c:stored.split(","))if(c.matches("(?:kick:[a-z0-9_][a-z0-9_-]{0,24}|[a-z0-9_]{1,25})")&&!out.contains(c)&&out.size()<12)out.add(c);return out;
    }
    void rememberChannel(String c){List<String> recent=recentChannels();recent.remove(c);recent.add(0,c);if(recent.size()>12)recent=recent.subList(0,12);prefs.edit().putString("last_channel",c).putString("recent_channels",android.text.TextUtils.join(",",recent)).apply();}
    void stopPreview(){previewVersion++;if(previewJob!=null)main.removeCallbacks(previewJob);previewLogin="";if(previewView!=null){previewView.setPlayer(null);previewView.setVisibility(View.INVISIBLE);}if(preview!=null){preview.release();preview=null;}}
    void startPreview(){
        if(playing||browsingVods||!foreground||selected==null||previewView==null)return;String login=selected.login;if(login.equals(previewLogin))return;
        stopPreview();previewLogin=login;int version=previewVersion;
        previewJob=()->images.execute(()->{try{String url=resolveStream(login);main.post(()->{
            if(destroyed||playing||!foreground||version!=previewVersion)return;
            DefaultTrackSelector selector=new DefaultTrackSelector(this);selector.setParameters(selector.buildUponParameters().setMaxVideoSize(854,480).setMaxVideoBitrate(1200000));
            preview=new ExoPlayer.Builder(this).setTrackSelector(selector).build();preview.setVolume(0);previewView.setPlayer(preview);previewView.setVisibility(View.VISIBLE);
            preview.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException e){if(version==previewVersion)stopPreview();}});
            preview.setMediaItem(MediaItem.fromUri(url));preview.prepare();preview.play();
        });}catch(Exception ignored){main.post(()->{if(version==previewVersion)stopPreview();});}});
        main.postDelayed(previewJob,650);
    }
    void loadImage(ImageView view,String url){view.setTag(url);view.setImageDrawable(null);if(!url.startsWith("https://"))return;Bitmap found=cache.get(url);if(found!=null){view.setImageBitmap(found);return;}images.execute(()->{try(Response r=TwitchSource.HTTP.newCall(new Request.Builder().url(url).build()).execute()){if(!r.isSuccessful()||r.body().contentLength()>4000000)return;byte[] b=r.peekBody(4000001).bytes();if(b.length>4000000)return;Bitmap bitmap=BitmapFactory.decodeByteArray(b,0,b.length);if(bitmap==null)return;cache.put(url,bitmap);main.post(()->{if(!destroyed&&url.equals(view.getTag()))view.setImageBitmap(bitmap);});}catch(Exception ignored){}});}
    void refresh(){if(playing||browsingVods)return;refreshKick();refreshDirectory();if(refreshing||!account.connected())return;refreshing=true;int v=screenVersion;status.setText("Actualizando tus directos…");net.execute(()->{try{List<TwitchAccount.Stream> result=account.followed();main.post(()->{refreshing=false;if(destroyed||playing||v!=screenVersion)return;for(TwitchAccount.Stream item:streams)if(ChannelKey.kick(item.login))result.add(item);streams=result;String old=selected==null?"":selected.id;selected=streams.stream().filter(s->s.id.equals(old)).findFirst().orElse(streams.isEmpty()?null:streams.get(0));for(TwitchAccount.Stream item:streams)directory.put(item.login,item);saveCatalog();renderRail();renderShelves();renderSelection();status.setText(streams.isEmpty()?"Ninguno de tus canales seguidos está en directo ahora.":streams.size()+" canales seguidos en directo · Violeta: Twitch · Verde: Kick");});}catch(Exception e){main.post(()->{refreshing=false;if(!destroyed&&!playing&&v==screenVersion)status.setText(account.connected()?"No se pudo actualizar. Comprueba tu conexión y vuelve a intentarlo.":"La sesión caducó. Conecta tu cuenta de nuevo.");});}});}
    void search(){EditText input=new EditText(this);input.setSingleLine();input.setHint("Canal de Twitch o enlace de Twitch / Kick");input.setTextColor(WHITE);new AlertDialog.Builder(this).setTitle("Buscar un canal").setView(input).setPositiveButton("Ver directo",(d,w)->openChannel(input.getText().toString())).setNegativeButton("Cancelar",null).show();}
    void kickAccount(){
        if(!KickSession.connected(this)){startActivityForResult(new Intent(this,KickActivity.class),27);return;}
        optionSheet("Tu cuenta de Kick",new String[]{"Sincronizar / iniciar sesión","Desconectar Kick de TwiT"},-1,i->{
            if(i==0)startActivityForResult(new Intent(this,KickActivity.class),27);
            else{getSharedPreferences("kick",0).edit().clear().apply();String cookies=android.webkit.CookieManager.getInstance().getCookie("https://kick.com");if(cookies!=null)for(String part:cookies.split(";")){String name=part.trim().split("=",2)[0];for(String domain:new String[]{"kick.com",".kick.com"})android.webkit.CookieManager.getInstance().setCookie("https://kick.com",name+"=; Max-Age=0; Path=/; Domain="+domain+"; Secure");}android.webkit.CookieManager.getInstance().flush();directory.entrySet().removeIf(e->ChannelKey.kick(e.getKey()));saveCatalog();streams.removeIf(item->ChannelKey.kick(item.login));if(selected!=null&&ChannelKey.kick(selected.login))selected=null;home();}
        });
    }
    void refreshKick(){
        if(kickLoader!=null||!KickSession.connected(this)||playing||browsingVods||!foreground)return;int version=screenVersion;
        KickSession loader=new KickSession(this);kickLoader=loader;
        loader.refresh((json,error)->{if(kickLoader!=loader)return;kickLoader=null;loader.close();if(destroyed||playing||version!=screenVersion)return;
            if(error!=null){status.setText("Kick no pudo actualizarse. Abre Cuenta Kick para volver a conectar.");return;}
            try{KickSession.save(this,json);directory.entrySet().removeIf(e->ChannelKey.kick(e.getKey()));for(TwitchAccount.Stream item:KickSession.parseAll(json))directory.put(item.login,item);List<TwitchAccount.Stream> kick=KickSession.parse(json);String old=selected==null?"":selected.id;streams.removeIf(item->ChannelKey.kick(item.login));streams.addAll(kick);selected=streams.stream().filter(item->item.id.equals(old)).findFirst().orElse(streams.isEmpty()?null:streams.get(0));for(TwitchAccount.Stream item:streams)directory.put(item.login,item);saveCatalog();renderRail();renderShelves();renderSelection();status.setText(streams.size()+" canales seguidos en directo · Violeta: Twitch · Verde: Kick");}catch(Exception e){status.setText("No se pudo actualizar Kick.");}
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==27){directory.entrySet().removeIf(e->ChannelKey.kick(e.getKey()));for(TwitchAccount.Stream item:KickSession.allCached(this))directory.put(item.login,item);saveCatalog();streams.removeIf(item->ChannelKey.kick(item.login));streams.addAll(KickSession.cached(this));home();}}
    void accountDialog(){if(!account.connected()){beginAuth();return;}optionSheet("Twitch · @"+account.name(),new String[]{"Actualizar mis canales","Desconectar Twitch"},-1,i->{if(i==0){directoryUpdated=0;refresh();}else{authVersion++;net.execute(()->{account.logout();main.post(()->{streams.removeIf(item->!ChannelKey.kick(item.login));directory.entrySet().removeIf(e->!ChannelKey.kick(e.getKey()));directoryUpdated=0;saveCatalog();selected=null;home();});});}});}
    void beginAuth(){
        int a=++authVersion;
        LinearLayout panel=new LinearLayout(this);panel.setGravity(Gravity.CENTER_VERTICAL);panel.setPadding(dp(24),dp(12),dp(24),dp(12));
        ImageView qr=new ImageView(this);qr.setContentDescription("Código QR para autorizar TwiT en Twitch");qr.setVisibility(View.GONE);panel.addView(qr,new LinearLayout.LayoutParams(dp(200),dp(200)));
        TextView code=text("Preparando conexión…",20,WHITE);code.setPadding(dp(24),0,0,0);panel.addView(code,new LinearLayout.LayoutParams(0,-2,1));
        authDialog=new AlertDialog.Builder(this).setTitle("Conecta tu cuenta de Twitch").setView(panel).setNegativeButton("Cancelar",null).create();
        authDialog.setOnDismissListener(d->{if(a==authVersion)authVersion++;});authDialog.show();authDialog.getWindow().setBackgroundDrawable(new ColorDrawable(BG));authDialog.getWindow().setLayout(dp(700),-2);
        net.execute(()->{try{
            JSONObject j=account.device();String userCode=j.getString("user_code"),device=j.getString("device_code");
            String url="https://www.twitch.tv/activate?public=true&device-code="+android.net.Uri.encode(userCode);
            com.google.zxing.common.BitMatrix matrix=new com.google.zxing.qrcode.QRCodeWriter().encode(url,com.google.zxing.BarcodeFormat.QR_CODE,600,600);
            int[] pixels=new int[600*600];for(int y=0;y<600;y++)for(int x=0;x<600;x++)pixels[y*600+x]=matrix.get(x,y)?Color.BLACK:Color.WHITE;
            Bitmap bitmap=Bitmap.createBitmap(pixels,600,600,Bitmap.Config.ARGB_8888);
            main.post(()->{if(destroyed||a!=authVersion)return;
                pollInterval=Math.max(5,j.optInt("interval",5));authDeadline=SystemClock.elapsedRealtime()+j.optLong("expires_in",1800)*1000;
                qr.setImageBitmap(bitmap);qr.setVisibility(View.VISIBLE);
                code.setText("Escanea el QR con tu móvil\no abre twitch.tv/activate\n\n"+userCode+"\n\nAutoriza «TwiT TV de Danik».\nTus seguidos aparecerán automáticamente.");poll(a,device,code);
            });
        }catch(Exception e){main.post(()->{if(a==authVersion)code.setText("No se pudo conectar con Twitch. Comprueba internet y vuelve a intentarlo.");});}});
    }
    void poll(int a,String device,TextView code){main.postDelayed(()->{if(destroyed||a!=authVersion)return;if(SystemClock.elapsedRealtime()>authDeadline){code.setText("El código ha caducado. Cierra y vuelve a conectar.");return;}net.execute(()->{try{JSONObject tokens=account.poll(device);if(a!=authVersion)return;account.accept(tokens);if(a!=authVersion){account.logout();return;}main.post(()->{if(a!=authVersion||destroyed)return;authVersion++;authDialog.dismiss();home();});}catch(TwitchAccount.ApiError e){main.post(()->{if(a!=authVersion)return;if(AuthPolicy.pending(e.code)){pollInterval=AuthPolicy.nextInterval(pollInterval,e.code);poll(a,device,code);}else code.setText("La autorización terminó o caducó. Cierra y vuelve a intentarlo.");});}catch(Exception e){main.post(()->{if(a==authVersion)poll(a,device,code);});}});},pollInterval*1000L);}
    static String resolveStream(String key)throws Exception{return ChannelKey.kick(key)?KickSource.resolve(key):TwitchSource.resolve(key);}
    void openChannel(String input){String c;try{c=ChannelKey.parse(input);}catch(Exception e){error(e.getMessage());return;}int r=++requestVersion;if(status!=null&&!playing)status.setText("Abriendo "+c+"…");((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);net.execute(()->{try{String url=resolveStream(c);main.post(()->{if(!destroyed&&r==requestVersion)play(url,c);});}catch(Exception e){main.post(()->{if(!destroyed&&r==requestVersion)error("No se pudo abrir "+c+". Puede estar desconectado o la plataforma rechazó este reproductor.");});}});}
    void openLibrary(String key){
        release();playing=false;browsingVods=true;vodMode=false;libraryChannel=key;int version=++screenVersion;requestVersion++;root=new FrameLayout(this);root.setBackgroundColor(BG);setContentView(root);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout body=col();body.setPadding(dp(34),dp(24),dp(34),dp(24));scroll.addView(body);root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);TwitchAccount.Stream info=streamInfo(key);FrameLayout avatar=new FrameLayout(this);avatarInto(avatar,info,48);header.addView(avatar,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView name=text((info==null?ChannelKey.slug(key):info.name)+"  ·  Retransmisiones",25,ChannelKey.color(key));name.setPadding(dp(14),0,0,0);header.addView(name,new LinearLayout.LayoutParams(0,dp(52),1));Button back=subtle("← Inicio",this::home);header.addView(back,new LinearLayout.LayoutParams(dp(100),dp(36)));body.addView(header);
        TextView note=text("Cargando vídeos disponibles…",12,MUTED);note.setPadding(0,dp(14),0,dp(18));body.addView(note);LinearLayout grid=col();body.addView(grid);Button more=subtle("Cargar más",()->{});more.setVisibility(View.GONE);body.addView(more,new LinearLayout.LayoutParams(dp(150),dp(38)));back.requestFocus();loadVideos(key,"",grid,note,more,version,true);
    }
    void loadVideos(String key,String cursor,LinearLayout grid,TextView note,Button more,int version,boolean first){
        more.setEnabled(false);net.execute(()->{try{VodSource.Page page=ChannelKey.kick(key)?VodSource.kick(key):account.videos(ChannelKey.slug(key),cursor);main.post(()->{if(destroyed||!browsingVods||version!=screenVersion)return;note.setText(page.videos.isEmpty()&&first?"Este canal no tiene retransmisiones públicas disponibles.":"Selecciona un vídeo · Durante la reproducción puedes avanzar o retroceder 30 segundos");
            LinearLayout row=null;View initial=null;for(int i=0;i<page.videos.size();i++){VodSource.Video video=page.videos.get(i);if(i%3==0){row=new LinearLayout(this);grid.addView(row,new LinearLayout.LayoutParams(-1,-2));}LinearLayout card=col();card.setPadding(dp(7),dp(7),dp(7),dp(10));card.setBackground(shape(PANEL,12));card.setFocusable(true);card.setClickable(true);ImageView thumb=new ImageView(this);thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);card.addView(thumb,new LinearLayout.LayoutParams(-1,dp(126)));loadImage(thumb,video.thumbnail);TextView title=text(video.title,13,WHITE);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);title.setPadding(dp(5),dp(8),dp(5),0);card.addView(title,new LinearLayout.LayoutParams(-1,dp(44)));String date=video.date.length()>10?video.date.substring(0,10):video.date;TextView meta=text(date+"  ·  "+video.duration,11,MUTED);meta.setPadding(dp(5),0,0,0);card.addView(meta);card.setContentDescription(video.title+", "+date);card.setOnFocusChangeListener((v,f)->card.setBackground(f?outline(0xff254239,12,MINT):shape(PANEL,12)));card.setOnClickListener(v->openVideo(key,video));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(210),1);cp.setMargins(0,0,dp(12),dp(12));row.addView(card,cp);if(initial==null)initial=card;}
            if(row!=null)for(int i=page.videos.size()%3;i>0&&i<3;i++)row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
            more.setEnabled(true);more.setVisibility(page.cursor.isEmpty()?View.GONE:View.VISIBLE);more.setOnClickListener(v->loadVideos(key,page.cursor,grid,note,more,version,false));if(first&&initial!=null)initial.requestFocus();
        });}catch(Exception e){main.post(()->{if(!destroyed&&browsingVods&&version==screenVersion){note.setText("No se pudieron cargar los vídeos. Puedes reintentar; algunos canales limitan el acceso a sus retransmisiones.");more.setText("Reintentar");more.setEnabled(true);more.setVisibility(View.VISIBLE);more.setOnClickListener(v->loadVideos(key,cursor,grid,note,more,version,first));}});}});
    }
    void openVideo(String key,VodSource.Video video){int request=++requestVersion;net.execute(()->{try{String url=ChannelKey.kick(key)?video.source:TwitchSource.resolveVod(video.id);main.post(()->{if(!destroyed&&foreground&&request==requestVersion){startMulti(Collections.singletonList(key),Collections.singletonList(url),true);channelHeading.setText((streamInfo(key)==null?ChannelKey.slug(key):streamInfo(key).name)+" · VOD");}});}catch(Exception e){main.post(()->{if(!destroyed&&request==requestVersion)error("No se pudo reproducir este vídeo. Puede haber caducado o requerir acceso que TwiT no tiene.");});}});}
    void seekVod(long amount){if(player!=null&&player.isCurrentMediaItemSeekable()){long duration=player.getDuration();long target=Math.max(0,player.getCurrentPosition()+amount);if(duration!=C.TIME_UNSET)target=Math.min(target,duration);player.seekTo(target);}scheduleHide();}
    void play(String url,String c){startMulti(Collections.singletonList(c),Collections.singletonList(url));}
    TwitchAccount.Stream streamInfo(String login){for(TwitchAccount.Stream item:streams)if(item.login.equals(login))return item;return directory.get(login);}
    void startMulti(List<String> logins,List<String> urls){startMulti(logins,urls,false);}
    void startMulti(List<String> logins,List<String> urls,boolean archived){
        release();browsingVods=false;vodMode=archived;playing=true;screenVersion++;paused=false;root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);getWindow().addFlags(128);
        videoGrid=col();root.addView(videoGrid,new FrameLayout.LayoutParams(-1,-1));
        int count=logins.size(),columns=count==1?1:2,rows=count>2?2:1;
        for(int rowIndex=0;rowIndex<rows;rowIndex++){
            LinearLayout row=new LinearLayout(this);videoGrid.addView(row,new LinearLayout.LayoutParams(-1,0,1));
            for(int column=0;column<columns;column++){
                int index=rowIndex*columns+column;if(index>=count){row.addView(new View(this),new LinearLayout.LayoutParams(0,-1,1));continue;}
                Pane pane=new Pane();pane.login=logins.get(index);pane.selector=new DefaultTrackSelector(this);
                pane.selector.setParameters(pane.selector.buildUponParameters().setMaxVideoSize(Integer.MAX_VALUE,Integer.MAX_VALUE).setViewportSize(Integer.MAX_VALUE,Integer.MAX_VALUE,false));
                pane.engine=new ExoPlayer.Builder(this).setTrackSelector(pane.selector).build();pane.engine.setVolume(index==0?1:0);
                pane.box=new FrameLayout(this);pane.box.setPadding(dp(2),dp(2),dp(2),dp(2));row.addView(pane.box,new LinearLayout.LayoutParams(0,-1,1));
                pane.view=(PlayerView)getLayoutInflater().inflate(tv.brisa.app.R.layout.home_preview,pane.box,false);pane.view.setPlayer(pane.engine);pane.box.addView(pane.view);
                pane.label=text(ChannelKey.label(pane.login),12,WHITE);pane.label.setPadding(dp(10),dp(5),dp(10),dp(5));pane.label.setBackgroundColor(0xb0000000);pane.box.addView(pane.label,new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT));
                panes.add(pane);pane.engine.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException e){pane.label.setText(pane.login+" · No se pudo reproducir. Prueba otra calidad o menos pantallas.");}});
                pane.engine.setMediaItem(MediaItem.fromUri(urls.get(index)));pane.engine.prepare();pane.engine.play();rememberChannel(pane.login);
            }
        }
        controls=col();controls.setGravity(Gravity.CENTER_HORIZONTAL);controls.setPadding(dp(18),dp(10),dp(18),dp(12));controls.setBackground(outline(0xee0c181b,18,0xff2b403d));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER);channelIcon=new ImageView(this);channelIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);channelIcon.setBackground(shape(PANEL,16));channelIcon.setClipToOutline(true);header.addView(channelIcon,new LinearLayout.LayoutParams(dp(28),dp(28)));channelHeading=text("",15,WHITE);channelHeading.setSingleLine();channelHeading.setEllipsize(TextUtils.TruncateAt.END);channelHeading.setPadding(dp(9),0,0,0);header.addView(channelHeading,new LinearLayout.LayoutParams(-2,dp(28)));controls.addView(header);
        if(archived)addVodTimeline();
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);LinearLayout.LayoutParams rowP=new LinearLayout.LayoutParams(-1,dp(42));rowP.topMargin=dp(6);controls.addView(row,rowP);pause=subtle("Pausar",this::togglePause);marginAdd(row,pause,78,34);
        if(archived){marginAdd(row,subtle("−30 s",()->seekVod(-30000)),72,34);marginAdd(row,subtle("+30 s",()->seekVod(30000)),72,34);marginAdd(row,subtle("Calidad",this::quality),82,34);marginAdd(row,subtle("Vídeos",()->openLibrary(channel)),78,34);}
        else{marginAdd(row,subtle("Multivista",this::channelPicker),96,34);marginAdd(row,subtle("Audio",this::audioPicker),72,34);marginAdd(row,subtle("Calidad",this::quality),80,34);marginAdd(row,subtle("Favorito",this::favorite),86,34);marginAdd(row,subtle("Chat",this::toggleChat),66,34);}
        marginAdd(row,subtle("Inicio",this::home),72,34);
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(Math.min(dp(704),getResources().getDisplayMetrics().widthPixels-dp(48)),-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);cp.setMargins(0,0,0,dp(20));root.addView(controls,cp);selectAudio(0);if(archived)main.post(vodTick);pause.requestFocus();scheduleHide();
    }
    static String videoTime(long ms){long sec=Math.max(0,ms/1000);return sec>=3600?String.format(Locale.US,"%d:%02d:%02d",sec/3600,(sec/60)%60,sec%60):String.format(Locale.US,"%d:%02d",sec/60,sec%60);}
    void addVodTimeline(){
        vodTime=text("0:00 / —",12,MUTED);vodTime.setGravity(Gravity.CENTER);controls.addView(vodTime,new LinearLayout.LayoutParams(-1,dp(22)));
        vodSeek=new SeekBar(this);vodSeek.setId(View.generateViewId());vodSeek.setContentDescription("Posición del vídeo. Izquierda o derecha para retroceder o avanzar diez segundos");vodSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(MINT));vodSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(MINT));vodSeek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff40504f));controls.addView(vodSeek,new LinearLayout.LayoutParams(-1,dp(32)));
        vodSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar bar){scrubbing=true;}public void onStopTrackingTouch(SeekBar bar){if(player!=null&&player.isCurrentMediaItemSeekable())player.seekTo(bar.getProgress()*1000L);scrubbing=false;scheduleHide();}public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){if(!fromUser||player==null)return;vodTime.setText(videoTime(progress*1000L)+" / "+videoTime(player.getDuration()));if(!scrubbing&&player.isCurrentMediaItemSeekable())player.seekTo(progress*1000L);scheduleHide();}});
        vodSeek.setOnFocusChangeListener((v,f)->{vodTime.setTextColor(f?MINT:MUTED);scheduleHide();});
    }
    void updateVodProgress(){if(player==null||vodSeek==null)return;long duration=player.getDuration();boolean available=duration>0&&duration!=C.TIME_UNSET&&player.isCurrentMediaItemSeekable();vodSeek.setEnabled(available);if(!available){vodTime.setText(videoTime(player.getCurrentPosition())+" / —");return;}vodSeek.setMax((int)Math.min(Integer.MAX_VALUE,duration/1000));vodSeek.setKeyProgressIncrement(10);if(!scrubbing){vodSeek.setProgress((int)(player.getCurrentPosition()/1000));vodSeek.setSecondaryProgress((int)(player.getBufferedPosition()/1000));vodTime.setText(videoTime(player.getCurrentPosition())+" / "+videoTime(duration));}}
    void selectAudio(int index){if(index<0||index>=panes.size())return;closeChat();activePane=index;Pane active=panes.get(index);channel=active.login;player=active.engine;tracks=active.selector;paused=!player.getPlayWhenReady();pause.setText(paused?"Continuar":"Pausar");
        for(int i=0;i<panes.size();i++){Pane item=panes.get(i);item.engine.setVolume(i==index?1:0);item.box.setBackgroundColor(i==index&&panes.size()>1?MINT:Color.BLACK);item.label.setText((i==index?"● AUDIO · ":"")+ChannelKey.label(item.login));item.label.setVisibility(panes.size()>1?View.VISIBLE:View.GONE);}
        TwitchAccount.Stream info=streamInfo(channel);channelHeading.setText((info==null?ChannelKey.slug(channel):info.name)+" · "+(ChannelKey.kick(channel)?"Kick":"Twitch"));channelHeading.setTextColor(ChannelKey.color(channel));loadImage(channelIcon,info==null?"":info.avatar);
    }
    void audioPicker(){String[] names=new String[panes.size()];for(int i=0;i<names.length;i++)names[i]="Pantalla "+(i+1)+" · "+ChannelKey.label(panes.get(i).login);optionSheet("Escuchar un directo",names,activePane,this::selectAudio);}
    void togglePause(){if(player==null)return;paused=!paused;for(Pane pane:panes)pane.engine.setPlayWhenReady(!paused);pause.setText(paused?"Continuar":"Pausar");scheduleHide();}
    void quality(){if(panes.size()==1){qualityFor(panes.get(0));return;}String[] names=new String[panes.size()];for(int i=0;i<names.length;i++)names[i]="Pantalla "+(i+1)+" · "+ChannelKey.label(panes.get(i).login);optionSheet("Calidad de cada pantalla",names,-1,w->qualityFor(panes.get(w)));}
    void qualityFor(Pane pane){
        List<TrackSelectionOverride> choices=new ArrayList<>();List<Format> formats=new ArrayList<>();
        for(Tracks.Group group:pane.engine.getCurrentTracks().getGroups())if(group.getType()==C.TRACK_TYPE_VIDEO)for(int i=0;i<group.length;i++){Format f=group.getTrackFormat(i);if(group.isTrackSupported(i)&&f.height>=480){choices.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));formats.add(f);}}
        List<Integer> order=new ArrayList<>();for(int i=0;i<formats.size();i++)order.add(i);order.sort((x,y)->{int h=Integer.compare(formats.get(y).height,formats.get(x).height);if(h!=0)return h;return Float.compare(formats.get(y).frameRate,formats.get(x).frameRate);});
        List<String> labels=new ArrayList<>();labels.add("Auto");if(!order.isEmpty())labels.add("Máxima disponible · "+formats.get(order.get(0)).height+"p");
        for(int i:order){Format f=formats.get(i);labels.add(f.height+"p"+(f.frameRate>0?" · "+Math.round(f.frameRate)+" fps":""));}
        optionSheet("Calidad · "+ChannelKey.label(pane.login),labels.toArray(new String[0]),-1,w->{if(!panes.contains(pane))return;DefaultTrackSelector.Parameters.Builder params=pane.selector.buildUponParameters().clearOverridesOfType(C.TRACK_TYPE_VIDEO).setMaxVideoSize(Integer.MAX_VALUE,Integer.MAX_VALUE).setMaxVideoBitrate(Integer.MAX_VALUE);if(w>0)params.setOverrideForType(choices.get(order.get(w==1?0:w-2)));pane.selector.setParameters(params);if(pane.engine.getPlaybackState()==Player.STATE_IDLE){pane.engine.prepare();pane.engine.play();}});
    }
    void closeChat(){chatVersion++;if(chatText!=null&&root!=null)root.removeView(chatText);if(chat!=null){chat.close();chat=null;}if(kickChat!=null){kickChat.close();kickChat=null;}chatText=null;}
    void toggleChat(){if(chatText!=null){closeChat();return;}if(ChannelKey.kick(channel)){kickChat=new KickChat(this,ChannelKey.slug(channel));chatText=kickChat.view;}else{TwitchAccount.Stream info=streamInfo(channel);chat=new EmoteChat(this,channel,info==null?"":info.id);chatText=chat.view;}chatText.setBackground(shape(0xed0c1519,14));chatText.setClipToOutline(true);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(310),dp(350),Gravity.TOP|Gravity.RIGHT);cp.setMargins(0,dp(20),dp(20),0);root.addView(chatText,cp);}

    void favorite(){Set<String> saved=new TreeSet<>(prefs.getStringSet("favorites",Collections.emptySet()));boolean removed=!saved.add(channel);if(removed)saved.remove(channel);prefs.edit().putStringSet("favorites",saved).apply();Toast.makeText(this,removed?"Favorito eliminado":"Guardado en favoritos",Toast.LENGTH_SHORT).show();}
    void error(String s){if(!destroyed)new AlertDialog.Builder(this).setTitle("TwiT").setMessage(s).setPositiveButton("Aceptar",null).show();}
    void optionSheet(String title,String[] choices,int selected,java.util.function.IntConsumer action){
        Dialog dialog=new Dialog(this);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout panel=col();panel.setPadding(dp(24),dp(22),dp(24),dp(22));panel.setBackground(outline(BG,22,0xff294139));
        TextView heading=text(title,25,MINT);heading.setTypeface(null,Typeface.BOLD);heading.setPadding(0,0,0,dp(18));panel.addView(heading);
        LinearLayout body=new LinearLayout(this);panel.addView(body,new LinearLayout.LayoutParams(-1,0,1));ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);LinearLayout list=col();scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
        Button close=button("Cerrar",dialog::dismiss);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(140),dp(50));cp.leftMargin=dp(18);body.addView(close,cp);Button first=null;
        for(int i=0;i<choices.length;i++){final int index=i;Button row=button((i==selected?"✓  ":"")+choices[i],()->{dialog.dismiss();action.accept(index);});row.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);row.setNextFocusRightId(close.getId());View.OnFocusChangeListener style=row.getOnFocusChangeListener();row.setOnFocusChangeListener((v,f)->{style.onFocusChange(v,f);if(f)close.setNextFocusLeftId(row.getId());});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(50));rp.bottomMargin=dp(8);list.addView(row,rp);if(first==null)first=row;}
        dialog.setContentView(panel);dialog.setOnDismissListener(d->scheduleHide());dialog.show();dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));dialog.getWindow().setLayout(dp(740),dp(420));if(first!=null)first.requestFocus();
    }
    void channelPicker(){
        List<String> available=new ArrayList<>();for(Pane pane:panes)available.add(pane.login);for(TwitchAccount.Stream item:streams)if(!available.contains(item.login))available.add(item.login);
        boolean[] checked=new boolean[available.size()];for(int i=0;i<available.size();i++)for(Pane pane:panes)if(pane.login.equals(available.get(i)))checked[i]=true;
        Dialog dialog=new Dialog(this);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=col();panel.setPadding(dp(26),dp(22),dp(26),dp(22));panel.setBackground(outline(BG,22,0xff294139));
        TextView title=text("Multivista",28,MINT);title.setTypeface(null,Typeface.BOLD);panel.addView(title);
        TextView hint=text("Elige hasta 4 directos · Pulsa → para continuar",14,MUTED);hint.setPadding(0,dp(6),0,dp(18));panel.addView(hint);
        LinearLayout body=new LinearLayout(this);panel.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setClipToPadding(true);scroll.setPadding(dp(3),dp(3),dp(12),dp(3));LinearLayout list=col();scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout actions=col();actions.setPadding(dp(18),dp(8),0,0);body.addView(actions,new LinearLayout.LayoutParams(dp(210),-1));
        TextView count=text("",16,MINT);count.setPadding(0,0,0,dp(18));actions.addView(count);
        Button apply=button("Ver selección  →",()->{}),cancel=button("Cancelar",dialog::dismiss);actions.addView(apply,new LinearLayout.LayoutParams(-1,dp(50)));LinearLayout.LayoutParams cancelP=new LinearLayout.LayoutParams(-1,dp(50));cancelP.topMargin=dp(12);actions.addView(cancel,cancelP);
        TextView help=text("← Volver a los canales",12,MUTED);help.setPadding(0,dp(18),0,0);actions.addView(help);
        Runnable updateCount=()->{int total=0;for(boolean v:checked)if(v)total++;count.setText(total+" de 4 seleccionados");};updateCount.run();
        Button first=null;
        for(int i=0;i<available.size();i++){
            final int index=i;String login=available.get(i);TwitchAccount.Stream info=streamInfo(login);String name=(info==null?ChannelKey.slug(login):info.name)+" · "+(ChannelKey.kick(login)?"Kick":"Twitch");
            Button row=button((checked[i]?"✓  ":"+  ")+name,()->{});row.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);row.setNextFocusRightId(apply.getId());
            row.setOnClickListener(v->{int total=0;for(boolean value:checked)if(value)total++;if(!checked[index]&&total>=4){count.setText("Máximo 4 · desmarca uno");return;}checked[index]=!checked[index];row.setText((checked[index]?"✓  ":"+  ")+name);updateCount.run();});
            View.OnFocusChangeListener style=row.getOnFocusChangeListener();row.setOnFocusChangeListener((v,f)->{style.onFocusChange(v,f);if(f){apply.setNextFocusLeftId(row.getId());cancel.setNextFocusLeftId(row.getId());}});
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(50));rp.bottomMargin=dp(8);list.addView(row,rp);if(first==null)first=row;
        }
        apply.setNextFocusDownId(cancel.getId());cancel.setNextFocusUpId(apply.getId());
        apply.setOnClickListener(v->{List<String> picked=new ArrayList<>();for(int i=0;i<checked.length;i++)if(checked[i])picked.add(available.get(i));if(picked.isEmpty()){count.setText("Elige al menos un directo");return;}dialog.dismiss();int request=++requestVersion;net.execute(()->{try{List<String> urls=new ArrayList<>();for(String login:picked)urls.add(resolveStream(login));main.post(()->{if(!destroyed&&foreground&&request==requestVersion)startMulti(picked,urls);});}catch(Exception e){main.post(()->{if(!destroyed&&request==requestVersion)error("No se pudo abrir la selección. Algún canal puede haberse desconectado.");});}});});
        dialog.setContentView(panel);dialog.setOnDismissListener(d->scheduleHide());dialog.show();dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));dialog.getWindow().setLayout(dp(820),dp(440));if(first!=null)first.requestFocus();
    }
    void scheduleHide(){main.removeCallbacks(hide);if(playing&&controls!=null&&controls.getVisibility()==View.VISIBLE)main.postDelayed(hide,5000);}
    void release(){main.removeCallbacks(vodTick);vodSeek=null;vodTime=null;scrubbing=false;if(kickLoader!=null){kickLoader.close();kickLoader=null;}stopPreview();closeChat();main.removeCallbacks(hide);held.clear();touching=false;for(Pane pane:panes){pane.view.setPlayer(null);pane.engine.release();}panes.clear();player=null;controls=null;}
    @Override public boolean dispatchKeyEvent(KeyEvent e){if(playing&&controls!=null){int k=e.getKeyCode();if(e.getAction()==KeyEvent.ACTION_UP){held.remove(k);scheduleHide();if(revealKey==k){revealKey=-1;return true;}}else{held.add(k);if((k==KeyEvent.KEYCODE_MENU||k>=19&&k<=23)&&controls.getVisibility()!=View.VISIBLE){controls.setVisibility(View.VISIBLE);pause.requestFocus();revealKey=k;scheduleHide();return true;}scheduleHide();}}return super.dispatchKeyEvent(e);}
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){touching=e.getActionMasked()!=MotionEvent.ACTION_UP&&e.getActionMasked()!=MotionEvent.ACTION_CANCEL;scheduleHide();return super.dispatchTouchEvent(e);}
    @Override public void onWindowFocusChanged(boolean f){super.onWindowFocusChanged(f);if(f)scheduleHide();else{held.clear();touching=false;main.removeCallbacks(hide);}}
    @Override public void onBackPressed(){if(playing){if(controls!=null&&controls.getVisibility()==View.VISIBLE){controls.setVisibility(View.GONE);main.removeCallbacks(hide);}else if(vodMode)openLibrary(channel);else home();}else if(browsingVods)home();else super.onBackPressed();}
    @Override protected void onStart(){super.onStart();getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);foreground=true;refreshKick();if(!playing&&!browsingVods&&selected!=null&&previewView!=null)startPreview();main.removeCallbacks(refreshTick);main.postDelayed(refreshTick,60000);if(playing&&vodMode){main.removeCallbacks(vodTick);main.post(vodTick);}}
    @Override protected void onStop(){super.onStop();getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);foreground=false;main.removeCallbacks(vodTick);if(kickLoader!=null){kickLoader.close();kickLoader=null;}stopPreview();main.removeCallbacks(refreshTick);main.removeCallbacks(hide);authVersion++;requestVersion++;if(authDialog!=null)authDialog.dismiss();closeChat();if(player!=null){for(Pane pane:panes)pane.engine.pause();paused=true;if(pause!=null)pause.setText("Continuar");}}
    @Override protected void onDestroy(){destroyed=true;authVersion++;requestVersion++;main.removeCallbacksAndMessages(null);release();net.shutdownNow();images.shutdownNow();cache.evictAll();super.onDestroy();}
    void cleanOldModels(){String[] names={"vosk-en-015","yamnet.tflite","jfk.wav","source.spm","target.spm","vocab.json","encoder_model_quantized_v2.ort","decoder_model_merged_quantized_v2.ort","encoder_model_quantized.onnx","decoder_model_merged_quantized.onnx","encoder_model_quantized.ort","decoder_model_merged_quantized.ort","ggml-tiny.en-q5_1.bin"};for(String n:names)delete(new java.io.File(getFilesDir(),n));java.io.File[] files=getCacheDir().listFiles((d,n)->n.startsWith("delay-"));if(files!=null)for(java.io.File f:files)delete(f);}
    void delete(java.io.File f){if(f.isDirectory()){java.io.File[] a=f.listFiles();if(a!=null)for(java.io.File x:a)delete(x);}f.delete();}
}
