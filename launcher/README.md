# Launcher propio para el Mi TV Stick

Launcher de Android TV en Java, estilo Apple TV, para el **Xiaomi Mi TV Stick
(MiTV-AESP0, Android 9 / API 28, Google TV)**.

Sustituye al launcher de Google TV: sin fila "Patrocinado", sin "Mi Channel",
sin recomendaciones. Solo apps, un dock y la hora.

## Compilar

El build usa el toolchain de Windows (SDK de Android + JDK 21), porque es donde
están instalados. Desde WSL:

```bash
./build.sh assembleDebug          # compila
./build.sh testDebugUnitTest      # tests unitarios
```

Desde Windows, directamente:

```bat
build.bat assembleDebug
```

El APK sale en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar

```bash
./tools/install.sh      # instala y lo pone como inicio
```

O a mano:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity com.suop.tvlauncher/.LauncherActivity
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb reboot
```

**El paso 3 es imprescindible**: mientras el launcher de Google TV esté
habilitado, el sistema (a través de `mitv.service`, que corre como
`uid 1000`) lo relanza y se pone delante, aunque el HOME preferido sea el
nuestro. Ver "Por que hace falta deshabilitarlo" más abajo.

## Desinstalar y volver atrás

```bash
./tools/uninstall-launcher.sh
```

Devuelve el HOME al launcher de Google TV, lo reactiva, desinstala el nuestro
y reinicia.

## Configuración

`app/src/main/res/values/config.xml`:

```xml
<string-array name="dock_packages">
    <item>com.google.android.youtube.tv</item>
    <item>com.amazon.amazonvideo.livingroom</item>
    <item>com.skyshowtime.skyshowtime.google</item>
    <item>com.wbd.stream</item>
</string-array>
<integer name="grid_columns">4</integer>
```

El orden es el del dock, de izquierda a derecha. Si una app no está
instalada, se ignora y el dock tiene menos iconos (no se inventan apps).

## Qué hace

- Fondo negro, reloj grande y fino arriba a la izquierda con la fecha y el
  saludo, todo en **Outfit** (OFL, `app/src/main/res/font/`).
- Pastillas translucidas arriba a la derecha: **Entradas**, **Wi-Fi** y
  **Ajustes**.
- Rejilla de 4 columnas con las apps de TV instaladas, en orden alfabético.
- Un único "cristal" translúcido que salta de tesela en tesela, en vez de un
  borde por tesela. La tesela enfocada crece y muestra su nombre debajo.
- Dock con 4 iconos; sobre él, el nombre de la app enfocada en tipo grande
  con resplandor del color de su icono.
- Navegación determinista: Up/Down mueven exactamente una fila, Up desde el
  dock sube a la barra superior, y los extremos de la barra no se caen a las
  teselas. Se recuerda la última tesela enfocada.

## Estructura

| Ruta | Qué es |
|---|---|
| `grid/Grid.java` | Rejilla de 4 columnas y selección del dock (testeado) |
| `nav/Navigator.java` | Navegación determinista con D-pad (testeado) |
| `ui/Clock.java`, `ui/Greeting.java`, `ui/Tint.java` | Reloj, saludo y color (testeados) |
| `ui/GlassHighlight.java` | El cristal que se mueve entre teselas |
| `ui/RoundedIcon.java` | Icono con esquinas redondeadas (recorta los banners) |
| `ui/AppAdapter.java` | Adaptador de la rejilla |
| `AppCatalog.java` | Lista de apps de TV instaladas |
| `LauncherActivity.java` | Orquestación, foco y acciones |

Tests: `app/src/test/java/` (29 tests). Se ejecutan con
`./build.sh testDebugUnitTest`.

## Detalles del dispositivo

- **API 28, no 31**: `LauncherActivityInfo.getActivityInfo()` es API 31, así
  que el catálogo usa `getComponentName()` y `PackageManager`.
- **`PackageManager`, no `LauncherApps`**: en Android 9 `LauncherApps` solo
  devuelve parte de las apps (dejaba fuera Amazon Video, HBO Max, Play Games
  y Videos). Con `PackageManager` se obtienen las 13.
- **Dos consultas, no una**: `LEANBACK_LAUNCHER` y `LAUNCHER` se consultan por
  separado. En un solo intent, Android exige que la app declare *todas* las
  categorías, y juntos solo saldrían las que declaran las dos.
- **Sin blur ni vídeo**: el stick tiene 1 GB de RAM. El cristal es un
  degradado, no un desenfoque.

## Por qué hay que deshabilitar el launcher de Google TV

Poner `set-home-activity` no basta: el launcher de Google TV se relanza a sí
mismo con `launchedFromUid=0` (system_server) a través de `mitv.service`, que
corre como `uid 1000` y es PERSISTENT, así que ignora el `disable`. Con
`pm disable-user --user 0 com.google.android.tvlauncher` ya no lo relanza y
nuestro launcher queda como inicio, también tras reiniciar.

`mitv.service` **no** se deshabilita: tiene el permiso `HDMI_CEC` y gestiona
el apagado del televisor. Además, la red Wi-Fi guardada en la primera
configuración pertenece a `com.google.android.tungsten.setupwraith`; si esa app
se deshabilita, Android borra la red y el stick se queda sin Wi-Fi (y sin
ADB). Para comprobarlo:

```bash
adb shell "dumpsys wifi | grep -o 'cname=[^ ]*'"
```

Si pone `cname=com.google.android.tungsten.setupwraith`, esa app no se toca.
