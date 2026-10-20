# Debloat del Xiaomi Mi TV Stick (MiTV-AESP0)

Debloat reversible de un **Xiaomi Mi TV Stick — MiTV-AESP0** (codename `aquaman`),
Android 9 / API 28, **Google TV**, firmware `PI.2055` (build 22.9.6.2055), 1 GB de RAM.

Todo lo hecho es **reversible**: no se ha desinstalado nada del sistema, no se ha
tocado la partición de firmware y no se ha modificado el bootloader.

## Resultado

| Métrica | Antes | Después |
|---|---|---|
| Apps de usuario instaladas | 11 | 5 (las que usas) + `com.mitv.tvhome.mitvplus` deshabilitada |
| Disco libre en `/data` | 1,8 GB (60% ocupado) | **2,2 GB (52% ocupado)** |
| Paquetes de terceros activos | 99 | 72 |
| RAM disponible | ~242 MB | ~321 MB |

Apps de usuario que quedan (las que sí usas): **Prime Video, SkyShowtime,
HBO Max, Spotify, Twitch y Plex**.

## Launcher propio

Además del debloat, se instaló un **launcher propio estilo Apple TV**
(`launcher/` en este repo) que sustituye al launcher de Google TV: sin fila
"Patrocinado", sin "Mi Channel", sin recomendaciones. Ver
[`launcher/README.md`](launcher/README.md).

Estado actual del stick: `com.suop.tvlauncher` es el HOME por defecto y
`com.google.android.tvlauncher` está deshabilitado (si no, el sistema lo
relanza y se pone delante). `launcher/tools/uninstall-launcher.sh` deshace
todo y devuelve el launcher de fábrica.

## Qué se desactivó (21 paquetes, `pm disable-user --user 0`)

Se usan **`pm disable-user --user 0`**, nunca `pm uninstall` de paquetes del
sistema: el APK sigue en el dispositivo y la operación es reversible con
`pm enable`.

### Contenido, publicidad y telemetría de Xiaomi
| Paquete | Qué es |
|---|---|
| `com.mitv.tvhome.mitvplus` | Mi TV Plus / PatchWall (contenido preinstalado) |
| `com.mitv.tvhome.atv` | Mi TV Home |
| `com.mitv.tvhome.michannel` | Mi Channel |
| `com.xm.webcontent` | Contenido web de Xiaomi |
| `com.xiaomo.tv.milegal` | Avisos legales de Xiaomi |
| `com.miui.tv.analytics` | Telemetría de Xiaomi |
| `com.mitv.download.service` | Descargas de contenido (además crasheaba solo) |
| `com.mitv.videoplayer` | Reproductor de vídeo de Xiaomi |
| `com.mitv.milinkservice` | Mi Link (mando desde el móvil) |
| `android.autoinstalls.config.xioami.mibox3` | **Auto Install Config**: permite que Play Store autoinstale apps preinstaladas |
| `tv.alphonso.alphonso_eula` | EULA de Alphonso (telemetría de TV) |
| `com.xiaomi.android.tvsetup.partnercustomizer` | Asistente de configuración de Xiaomi |

### Google: recomendaciones, telemetría y asistente
| Paquete | Qué es |
|---|---|
| `com.google.android.tvrecommendations` | Filas de recomendaciones y "Patrocinado" |
| `com.google.android.backdrop` | Fondo ambiental |
| `com.google.android.feedback` | Envío de feedback |
| `com.google.android.tv.bugreportsender` | Envío de informes de error |
| `com.google.android.partnersetup` | Atribución a partners |
| `com.google.android.syncadapters.contacts` | Sincronización de contactos |
| `com.google.android.syncadapters.calendar` | Sincronización de calendario |
| `com.google.android.katniss` | Asistente de Google (voz) |
| `com.google.android.speech.pumpkin` | Reconocimiento de voz |

### Streaming que no usas (`pm uninstall --user 0`)
`com.netflix.ninja` (42 MB), `com.disney.disneyplus` (23 MB),
`com.antena3.atresplayer.tv` (23 MB), `com.rt.tv` (11 MB),
`tv.accedo.mitele_xdk` (8 MB), `dw.com.androidtv.live` (2 MB).
Además se desinstaló `com.overdevs.at4k`, que **se autoinstaló sola** a las
17:19 durante la sesión a través del Auto Install Config de Xiaomi.

> **Sobre la reversibilidad de la desinstalación (verificado en la práctica):**
> `pm uninstall --user 0` **borra el APK de `/data/app` de forma asíncrona**.
> Si se restaura el paquete con `pm install-existing` *antes* de que ese borrado
> se ejecute, funciona y no hay que volver a descargarlo. Si se deja pasar el
> borrado, el paquete desaparece del sistema y `install-existing` responde
> `NameNotFoundException: Package ... doesn't exist`: hay que reinstalarlo
> desde Play Store.
>
> En esta sesión se comprobó con Netflix (restaurado con `install-existing` y
> luego desinstalado otra vez) y con las otras cinco, cuyo APK ya había
> desaparecido. **Todas son gratuitas y están en Play Store**, así que
> reinstalarlas es un trámite; pero conviene saberlo: la desinstalación de
> apps de usuario libera espacio de verdad y eso no es gratis de deshacer.
>
> `pm disable-user --user 0` (lo que se ha hecho con los 21 paquetes del
> sistema) **no borra nada**: el APK sigue ahí y se revierte en un segundo con
> `pm enable`.

## Qué NO se tocó, y por qué

| Paquete | Motivo |
|---|---|
| `com.google.android.tvlauncher` | Es el launcher activo (HOME). El objetivo era debloat, no cambiar el launcher. |
| `com.google.android.tungsten.setupwraith` | La red Wi‑Fi guardada en la primera configuración es suya (`dumpsys wifi` → `cname=com.google.android.tungsten.setupwraith`). Desactivarla **borraría esa red** y el stick se quedaría sin Wi‑Fi ni ADB. |
| `com.android.tv.settings` | Ajustes del sistema: entrada HDMI, pairing del mando. |
| `com.google.android.gms`, `gsf`, `vending` | Play Services, Google Services Framework y Play Store. |
| `com.google.android.inputmethod.latin` | Teclado en pantalla. |
| `com.google.android.tts` | Se mantiene por accesibilidad (TalkBack). |
| `com.google.android.apps.mediashell` | Chromecast (decisión del usuario). |
| `com.xiaomi.mitv.updateservice` | Actualizaciones de firmware (decisión del usuario). |
| `com.mitv.service` | Servicio de TV de Xiaomi: permiso **HDMI_CEC** y apagado del TV. Se probó desactivarlo buscando el origen de la fila "Mi Channel" y **resultó que no era el responsable**, así que se reactivó. |
| `com.mitv.dream` | Protector de pantalla (evita quemado de panel). |
| `com.droidlogic.*`, `com.android.bluetooth`, `InputDevices` | Hardware, pairing y mando. |
| `com.android.shell` | Se usa para el propio ADB. |
| `com.google.android.tv` | App Google TV (EPG de Live TV). Se probó desactivarla y **no eliminaba la fila "Mi Channel"**, así que se reactivó. |

## Lo que NO se puede arreglar sin root

- **La fila "Mi Channel"** del inicio: es la integración de Live TV que el
  launcher de Google TV trae embebida. Se comprobó que **no** viene de
  `mitv.service` ni de `com.google.android.tv` (desactivando ambos sigue
  apareciendo), y no hay ningún paquete que la sirva de forma desactivable.
- **Los procesos PERSISTENT** (`xiaomi.tvservice`, `mitv.service` y
  `com.xiaomi.android.tvsetup.partnercustomizer`): Android ignora el `disable`
  de los paquetes persistentes y `pm disable` a nivel de componente está
  bloqueado por seguridad (`SecurityException: Shell cannot change component
  state ... to 2`). Sin root, son inmortales. Ocupan ~50 MB de RAM entre los dos.
- **La fila "Patrocinado"**: laSponsored row la pinta el launcher; sin
  launcher propio no se puede quitar. Alternativa: un DNS bloqueador de
  anuncios (no aplicado, decisión del usuario).

## Restaurar

```bash
cd /mnt/c/Users/Hugop/Dev/firestick-debloat

# Todo (reactiva los 21 deshabilitados y reinstala las desinstaladas)
./tools/restore.sh

# Un paquete concreto
./tools/restore.sh com.google.android.katniss
```

El script lee `tools/disabled.list` y `tools/uninstalled.list`, que son la
fuente de verdad de lo tocado. Los 21 deshabilitados se recuperan al instante.
Las desinstaladas se recuperan con `pm install-existing` **si su APK sigue en
`/data/app`**; si ya no, hay que instalarlas desde Play Store (el script avisa
del caso).

## Conectar

```bash
source tools/env.sh          # define adb_tv y TV_SERIAL
adb_tv shell getprop ro.product.model
```

El `adb` nativo está en `/tmp/opencode/pt/platform-tools` y reutiliza tu clave
ya autorizada (`~/.android/adbkey`, copiada de Windows) para no tener que
volver a aceptar el diálogo en la tele.

## Herramientas

| Script | Para qué |
|---|---|
| `tools/backup.sh` | Instantánea de paquetes, `getprop` y todos los `settings` |
| `tools/disable.sh <pkg>…` | Desactiva paquetes y registra la operación |
| `tools/restore.sh` | Revierte todo |
| `tools/check.sh` | Estado del stick: launcher en pie, crashes, paquetes off |
| `tools/shot.sh <nombre>` | Captura de pantalla al stick |

## Más allá del debloat

- **Desinstalar apps de usuario** es lo que de verdad libera espacio
  (`disable` no borra nada del disco). Se ha hecho con las de streaming que no
  usas.
- **El launcher propio estilo Apple TV** quitaría el ruido visual del inicio
  (fila "Patrocinado", "Mi Channel", recomendaciones), pero **no** libera RAM
  ni disco, y en un Google TV el asistente de configuración (`setupwraith`)
  puede recuperar el control tras un reinicio — lo que te dejaría sin ADB.
- **Un DNS bloqueador de anuncios** (Private DNS → `dns.adguard-dns.com`)
  atajaría la fila "Patrocinado" sin tocar nada del sistema. No se aplicó
  (decisión del usuario). Se revierte con
  `settings put global private_dns_mode opportunistic`.
- **El servicio de actualizaciones de Xiaomi** (`com.xiaomi.mitv.updateservice`,
  86 MB de RAM) no solo actualiza el firmware: también notifica cada app
  instalada a Xiaomi con tu `anonymous_device_id` y tiene un componente
  (`pie.InstallEventReceiver`) de autoinstalación. Sus componentes no se
  pueden desactivar sin root, así que la única forma de quitarlo es
  desactivar el paquete entero (y perder las actualizaciones automáticas).

## Notas de la sesión

- Una captura salió en negro justo después del lote 3: era el launcher
  redibujándose tras el evento de paquete, no un fallo. Se comprobó con
  `logcat -b crash` (sin `FATAL EXCEPTION` del launcher) y con un reinicio.
- El `com.mitv.download.service` ya crasheaba solo antes de tocar nada
  (`NullPointerException` en `DownloadThread.run`).
- `pm install-existing` funciona en este dispositivo: se comprobó restaurando
  Netflix y volviéndolo a desinstalar.
- **Bug encontrado y corregido en `restore.sh`**: `adb shell` hereda el `stdin`
  del bucle `while read` y se come el resto de la lista, así que solo se
  reactivaba el primer paquete. Se arregló con `< /dev/null` en cada llamada
  a `adb` (también en `disable.sh` y `backup.sh`). El restaurador se probó de
  verdad: deshabilita todo y vuelve a habilitar los 21 correctamente.

## ADB cerrado (estado final)

Al terminar, el ADB de red se dejó **cerrado**, como pide el prompt:

```bash
settings put global adb_enabled 0     # equivalente a apagar USB debugging
adb shell reboot
```

Verificado tras el reinicio:

```
$ timeout 5 bash -c '</dev/tcp/192.168.3.124/5555'
bash: connect: Connection refused        # puerto 5555 CERRADO
$ adb connect 192.168.3.124:5555
failed to connect: Connection refused
```

El stick sigue vivo y con Wi‑Fi (ping 3 ms). El puerto 5555 va en texto
plano: con `ro.adb.secure=0` cualquier equipo de la red podría abrir una
shell; por eso se cierra.

> **Para volver a conectar ADB** (si lo necesitas en el futuro): en el stick,
> Ajustes → Developer options → *ADB debugging* (o *Network debugging*), y
> conectar con `adb connect 192.168.3.124:5555`. Vuelve a aparecer el diálogo
> "Permitir depuración" que hay que aceptar con el mando.
