# TwiT 1.0

![TwiT](branding/TwiT-banner.png)

**Creado por DanikDeJesus.** Cliente independiente para ver Twitch y Kick en Fire TV y Android TV, con navegación mediante mando.

## Descargar e instalar

El APK se publica en [Releases](https://github.com/danikdejesus1/TwiT/releases). En Downloader puedes introducir el enlace directo:

`https://github.com/danikdejesus1/TwiT/releases/latest/download/TwiT.apk`

Activa el permiso de instalación de aplicaciones desconocidas para Downloader, descarga el APK e instálalo. No necesitas un código numérico; este requiere crear un enlace corto aparte.

Requiere Android 9/API 28 o posterior; se ha probado en Fire OS 7. Las actualizaciones publicadas deben conservar el identificador y certificado de firma de TwiT.

## Funciones

- Inicio con preview de directo, seguidos de Twitch/Kick y orden por espectadores.
- Panel LIVE con aro violeta para Twitch y verde para Kick.
- Canales OFFLINE con acceso a retransmisiones públicas.
- Reproductor nativo, calidad automática/manual y multivista de hasta cuatro canales.
- Selección de audio de una pantalla, pausa y favoritos locales.
- VOD con barra de tiempo, duración y desplazamiento con el mando.
- Chat de Twitch con emotes; chat de Kick de lectura mediante su popout oficial.
- Conexión de cuentas, sin pedir contraseñas de Twitch dentro de la app.
- Sin servicio de traducción ni suscripción propia.

## Probar y reportar problemas

TwiT 1.0 es una versión para pruebas comunitarias. Cada persona inicia sesión con sus propias cuentas; el APK no incluye cuentas ni sesiones del creador.

Para reportar un fallo, abre un [Issue](https://github.com/danikdejesus1/TwiT/issues) e indica el modelo del dispositivo, versión de Fire OS/Android, plataforma (Twitch o Kick), pasos para reproducirlo y lo que esperabas que ocurriera. Puedes adjuntar una captura sin datos personales. No publiques contraseñas, cookies, códigos de acceso ni tokens.

## Uso con mando

Selecciona un canal con las flechas y pulsa el botón central. Dentro del vídeo, el botón de menú o las flechas muestran los controles; estos se ocultan después de cinco segundos sin uso. En un VOD, sube hasta la barra y usa izquierda/derecha para moverte diez segundos. En multivista puedes elegir el audio y la calidad de cada pantalla.

## Compilar

Necesitas JDK 17, Android SDK 35 y Build Tools 35.0.0. Configura `ANDROID_HOME` o un `local.properties` local con `sdk.dir`.

```sh
sh gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Para generar un APK sin depuración:

```sh
sh gradlew :app:assembleRelease
```

La salida release está sin firmar: el mantenedor debe firmarla con su almacén privado usando `apksigner`. Nunca subas el almacén ni sus contraseñas. El APK debug generado localmente puede tener otro certificado y no sustituir la versión distribuida.

## Límites conocidos

Esta app no es oficial ni está afiliada a Twitch, Kick o Amazon. La reproducción y parte de la integración de Kick dependen de interfaces del sitio que pueden cambiar. Los servicios conservan sus condiciones y restricciones; TwiT no garantiza acceso a vídeos privados ni eliminación de anuncios de las plataformas. Los términos de Kick para desarrolladores indican el uso de su reproductor insertado: https://dev.kick.com/terms-of-service . La integración nativa actual es experimental.

Cuatro directos a máxima calidad pueden exceder la memoria o capacidad de decodificación del Fire TV HD. El chat de Kick es de lectura; no implementa envío con mando. No hay subtítulos traducidos por IA en esta versión. La compatibilidad no está certificada para todos los dispositivos.

## Privacidad y autoría

Consulta [PRIVACY.md](PRIVACY.md), [AUTHORS.md](AUTHORS.md) y [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). El identificador público del cliente OAuth de Twitch no es una contraseña; los tokens personales se generan al conectar cada cuenta y no se incluyen en este repositorio.

Copyright © 2026 DanikDeJesus. La publicación del código no concede por sí sola una licencia de reutilización; todavía no se ha elegido una licencia para el código original. Las dependencias conservan sus propias licencias.
