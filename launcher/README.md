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

El stick ("HA TV dormitorio") cambia de IP con el DHCP, así que se localiza
por su MAC con `tools/tv.sh`. Se puede fijar a mano:

```bash
TV_SERIAL=192.168.3.69:5555 ./tools/install.sh
```

O a mano:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell cmd package set-home-activity es.hugoperezvigo.tvlauncher/.LauncherActivity
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
<string-array name="apps_first">
    <item>com.google.android.youtube.tv</item>
    <item>com.amazon.amazonvideo.livingroom</item>
    <item>com.skyshowtime.skyshowtime.google</item>
    <item>com.wbd.stream</item>
    <item>com.spotify.tv.android</item>
</string-array>

<string-array name="apps_last">
    <item>com.google.android.videos</item>
</string-array>

<integer name="grid_columns">4</integer>
```

`apps_first` va arriba en ese orden, `apps_last` al final, y el resto
alfabéticamente en medio. Los paquetes que no estén instalados se ignoran.

## Qué hace

- Fondo negro con un halo muy suave arriba a la izquierda (degradado radial),
  reloj grande y fino con la fecha y el saludo, todo en **Outfit** (OFL,
  `app/src/main/res/font/`).
- Menús redondeados arriba a la derecha: **Entradas**, **Wi-Fi** y **Ajustes**,
  con icono. Al enfocarse, la pastilla se ilumina y se agranda con un rebote:
  se ve de un vistazo dónde está el foco. Los tres hacen algo:
  - **Entradas** abre un desplegable navegable con las entradas reales del
    dispositivo (el selector del sistema si lo hay, y la TV en directo). Los
    servicios de previsualización de las apps de streaming se descartan: el
    sistema los registra como si fueran tuner, pero no se puede cambiar a
    ellos.
  - **Wi-Fi** abre los ajustes de red del sistema.
  - **Ajustes** abre los ajustes del sistema.
- Rejilla de 4 columnas a todo el ancho con todas las apps de TV instaladas.
- Un único "cristal" que salta de tesela en tesela, en vez de un borde por
  tesela. La tesela enfocada crece y muestra su nombre debajo.
- Navegación determinista: Up/Down mueven exactamente una fila, Up desde la
  primera fila sube a la barra superior, y los extremos de la barra no se
  caen a las teselas. Se recuerda la última tesela enfocada.
- Al desplazarse, la fila enfocada queda **entera arriba**. El scroll es de
  píxeles, no un reposicionamiento: así la tesela enfocada no se recicla y no
  se pierde el foco (que es lo que pasaba con `scrollToPositionWithOffset`).
- La última fila puede estar incompleta (13 apps en 4 columnas): el foco no
  cae en un hueco y la izquierda desde su primera tesela salta al final de la
  fila de arriba.
- Al volver de una app se recupera la última tesela enfocada, no la primera.

### Detalles de acabado

- **El icono crece dentro de sus límites**, no escalando la vista. Si se
  escalase la vista, al enfocarse se saldría de su celda en la rejilla y
  quedaría cortado. `RoundedIcon` dibuja el icono al 84% y, enfocado, al
  100%, ajustando el radio de las esquinas para que nada quede recortado.
- **Los menús no llevan reborde**: el estado enfocado se distingue solo por
  el relleno más claro.

## Estructura

| Ruta | Qué es |
|---|---|
| `grid/Grid.java` | Rejilla de 4 columnas (testeado) |
| `nav/Navigator.java` | Navegación determinista con D-pad (testeado) |
| `inputs/TvInputs.java` | Qué entradas son reales y cuáles no (testeado) |
| `ui/Clock.java`, `ui/Greeting.java`, `ui/Tint.java` | Reloj, saludo y color (testeados) |
| `ui/AppOrder.java` | Orden de las apps: prioritarias, resto y últimas (testeado) |
| `ui/GlassHighlight.java` | El cristal que se mueve entre teselas |
| `ui/RoundedIcon.java` | Icono con esquinas redondeadas (recorta los banners) |
| `ui/AppAdapter.java` | Adaptador de la rejilla |
| `AppCatalog.java` | Lista de apps de TV instaladas |
| `LauncherActivity.java` | Orquestación, foco y acciones |

Tests: `app/src/test/java/` (42 tests). Se ejecutan con
`./build.sh testDebugUnitTest`.

## Despertar con el mando

El mando del stick es **Bluetooth**, no infrarrojo. Con el protector de
pantalla desactivado (`com.google.android.backdrop`, desactivado en el
debloat), el stick entraba en suspensión profunda, se apagaba el Bluetooth y
el mando ya no lo despertaba: había que apagar y encender la TV.

Arreglo aplicado en el stick:

```bash
adb shell settings put secure screensaver_components com.mitv.dream/.MiDream
adb shell settings put global stay_on_while_plugged_in 7
adb shell settings put system screen_off_timeout 1800000
```

- `com.mitv.dream/.MiDream` es el protector de pantalla de Xiaomi, un
  `DreamService` válido. Con él, el stick entra en modo protector en vez de
  suspensión, así que el Bluetooth sigue vivo y el mando lo despierta.
- `stay_on_while_plugged_in 7` lo mantiene despierto mientras está
  alimentado (el stick se enchufa al TV por USB, `mPlugType=2`).
- `screen_off_timeout` a 30 minutos.

**Si se cambia de protector de pantalla, hay que volver a apuntar a
`com.mitv.dream/.MiDream`**, o el mando volverá a no despertar el stick.

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
