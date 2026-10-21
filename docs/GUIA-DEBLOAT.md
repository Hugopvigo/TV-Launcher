# Guía de debloat

Cómo dejar limpio un **Xiaomi Mi TV Stick** (`MiTV-AESP0`, Android 9, Google TV)
sin root, sin cables y sin romper nada. Todo lo de aquí es reversible.

Si prefieres que lo haga un asistente, el prompt está en
[`PROMPT.md`](../PROMPT.md). Si prefieres hacerlo tú, sigue esta guía.

---

## Antes de empezar

**En el stick**:

1. Ajustes → Configuración del dispositivo → Acerca de → pulsa **OK** 7 veces
   sobre *Compilación* para abrir las opciones de desarrollador.
2. Vuelve atrás → Opciones de desarrollador → **Depuración ADB** (o *ADB
   debugging*, la casilla de red si la hay) activada.
3. **Deja el stick encendido.** Algunas cajas cortan la alimentación de sus
   puertos USB mientras la depuración está activa.

**En el ordenador**: `adb` en el PATH y el stick en la misma red.

```bash
adb connect 192.168.3.26:5555      # la IP del stick (cambia con el DHCP)
adb devices                          # debe salir "device"
```

Te saldrá un diálogo en la tele: **«¿Permitir depuración USB?»**. Acéptalo
con el mando, una sola vez. Si el puerto 5555 no abre, el stick probablemente
solo ofrezca *depuración wireless*: búscalo con `adb mdns services`, o pídeme
el código de emparejamiento en *Opciones de desarrollador → Emparejamiento
inalámbrico*.

> Si el stick está en otra casa, usa su dirección de Tailscale en vez de
> buscarlo en la red local. Nunca abras el puerto 5555 en el router.

---

## 1. Copia de seguridad

Antes de tocar nada:

```bash
./tools/backup.sh
```

Guarda la lista completa de paquetes, los ya desactivados, `getprop` y **todos**
los `settings` (global, system y secure) en `backup/<fecha>/`. Con eso, cualquier
cambio se puede deshacer o comparar.

---

## 2. Elegir qué desactivar

Este stick trae 99 paquetes de terceros. La lista de
[`tools/disabled.list`](../tools/disabled.list) ya está hecha y comentada por
categorías, pero **léela y ajusta lo que uses**: lo que no uses, desactívalo;
lo que uses, déjalo.

Las categorías y **por qué** están en la lista:

| Categoría | Qué es | Riesgo |
|---|---|---|
| Telemetría de Xiaomi | `com.miui.tv.analytics`, `com.xiaomo.tv.milegal`, `tv.alphonso.alphonso_eula` | Ninguno: solo reportan datos |
| Contenido y promociones | `com.mitv.tvhome.*`, `com.xm.webcontent`, `com.google.android.tvrecommendations` | Ninguno: es lo queenseñaba anuncios |
| Servicios en segundo plano | `com.mitv.download.service`, `com.mitv.videoplayer`, `com.mitv.milinkservice`, `com.xiaomi.android.tvsetup.partnercustomizer` | Bajo: se reactivan si los necesitas |
| Auto Install Config | `android.autoinstalls.config.xioami.mibox3` | **Alto si lo dejas**: es lo que permite que Play Store autoinstale apps de fábrica |
| Asistente y sincronización | `com.google.android.katniss`, `com.google.android.speech.pumpkin`, `syncadapters.*` | Medio: si hablas con la tele, necesitas el asistente |
| Apps de usuario que no usabas | Netflix, Disney+, Atresplayer, RT.es, MiTele, DW | Se **desinstalan**: liberan ~109 MB |

**Lo que nunca se toca**, y por qué:

| Paquete | Motivo |
|---|---|
| `com.mitv.service` | Permiso **HDMI_CEC**: gestiona el apagado del televisor |
| `com.android.tv.settings` | Entrada HDMI y emparejamiento del mando |
| `com.google.android.gms`, `gsf`, `vending` | Play Services y Play Store |
| `com.google.android.inputmethod.latin` | Teclado en pantalla |
| `com.google.android.tts` | Accesibilidad (TalkBack) |
| `com.mitv.dream` | Protector de pantalla (ver la nota 2 más abajo) |
| `com.android.shell` | El propio ADB |

---

## 3. Aplicar

```bash
./tools/disable.sh tools/disabled.list
```

Desactiva en lotes de cinco y comprueba entre lotes que el launcher sigue
viviendo. Las apps de usuario con prefijo `!` en la lista se desinstalan.

O a mano, con la lista de tu gusto:

```bash
# Desactivar (no borra nada, se revierte con pm enable)
adb shell pm disable-user --user 0 com.miui.tv.analytics

# Desinstalar una app de usuario (libera disco)
adb shell pm uninstall --user 0 com.netflix.ninja
```

Una orden `pm` por llamada: encadenarlas con `&&` puede quedarse en la
primera sin avisar.

---

## 4. Poner el launcher propio

Este repositorio trae un launcher de TV en Java, estilo Apple TV, que sustituye
al de Google TV.

```bash
cd launcher
./build.sh assembleRelease                 # o: ./tools/install.sh
./tools/install.sh
```

`install.sh` hace los tres pasos que hacen falta: instalar, ponerlo como
inicio y **desactivar el launcher de Google TV**:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell cmd package set-home-activity es.hugoperezvigo.tvlauncher/.LauncherActivity
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb reboot
```

> **El paso 3 no es opcional.** Con el launcher de Google TV habilitado, el
> sistema lo relanza desde `mitv.service` (que corre como `uid 1000` y es
> PERSISTENT, así que ignora el `disable`) y se pone delante del tuyo. Está
> explicado en el [informe](INFORME.md#6-por-qu%C3%A9-hay-que-desactivar-el-launcher-de-google-tv).

Para volver atrás: `launcher/tools/uninstall-launcher.sh`.

---

## 5. Verificar

Después de cada cambio, con el stick delante:

```bash
./tools/check.sh        # estado, RAM, crashes
./tools/shot.sh mi-captura   # captura al stick
```

Y comprueba de verdad con el mando: que las flechas mueven una fila, que el
icono enfocado crece y muestra su nombre, que **Entradas** abre el desplegable
y que **Ajustes** abre los ajustes del sistema.

Una captura de la tele en HDMI o Live TV sale vacía (0 bytes): es la tele, no
un fallo. Comprueba entonces con:

```bash
adb shell "dumpsys activity activities | grep topResumedActivity"
```

---

## 6. Revertir

```bash
./tools/restore.sh
```

Reactiva todo lo desactivado, recupera las apps de usuario (con
`pm install-existing` si su APK sigue ahí, si no, desde Play Store) y devuelve
el launcher de Google TV como inicio.

---

## 7. Cerrar el ADB

El ADB de red va en texto plano por el puerto 5555: cualquier equipo de tu red
podría abrir una shell. Ciérralo cuando termines.

```bash
adb shell settings put global adb_enabled 0
adb shell reboot
```

Y comprueba que el puerto ya no abre:

```bash
nc -vz 192.168.3.26 5555     # debe fallar
```

En el stick, además: *Opciones de desarrollador* → desactiva **Depuración ADB**
y revoca las autorizaciones. El launcher sigue funcionando sin depuración.

---

## Si algo va mal

**El mando no despierta el stick.** El mando es Bluetooth, no infrarrojo. Si
el protector de pantalla del sistema está desactivado, el stick entra en
suspensión en vez de activar el protector, se apaga el Bluetooth y el mando ya
no llega. Solución:

```bash
adb shell settings put secure screensaver_components com.mitv.dream/.MiDream
adb shell settings put global stay_on_while_plugged_in 7
```

Ojo: `com.google.android.backdrop` está en la lista de desactivados **por
esto**. Si lo desactivas en otro Xiaomi, vuelve a apuntar el protector al de
Xiaomi.

**Home cae en un menú que no es el launcher.** En Google TV, el asistente de
configuración (`com.google.android.tungsten.setupwraith`) puede quedarse con
Home. No lo desactives sin comprobar antes:

```bash
adb shell "dumpsys wifi | grep -o 'cname=[^ ]*'"
```

Si la red guardada en la primera configuración es suya, al desactivarlo
**Android borra esa red** y el stick se queda sin Wi-Fi (y sin ADB). Primero
olvida la red en Ajustes y vuelve a unirte.

**Un paquete no se desactiva.** `pm disable` a estado 2 está bloqueado por
seguridad en los paquetes persistentes; usa `pm disable-user --user 0` (estado
3), que sí funciona. Para los persistentes (`mitv.service`, `mitv.dream`)
hace falta root.

**El launcher no aparece en la rejilla.** En Android 11 y superiores la app
tiene que declarar `<queries>` para `LEANBACK_LAUNCHER`, o la visibilidad de
paquetes oculta todas las apps. Ours lo declara.

**`adb shell` se come el bucle.** Si recorres una lista en un `while read`,
`adb shell` hereda el `stdin` y se come el resto de las líneas. Pon `< /dev/null`
en cada llamada.

El resto de trampas que encontramos, con su explicación, en el
[informe](INFORME.md#8-resumen-de-lo-aprendido-del-dispositivo).
