# Plan de debloat — Xiaomi Mi TV Stick (MiTV-AESP0)

> **Para workers agénticos:** sub-skill REQUERIDA: `superpowers:executing-plans`.
> Los pasos usan casillas (`- [ ]`) para seguimiento.

**Objetivo:** Quitar telemetría, contenido preinstalado y apps que no se usan del
Xiaomi Mi TV Stick (Android 9 / API 28, Google TV), dejando el launcher de
Google TV intacto y todo reversible.

**Arquitectura:** Scripts bash en `tools/` que hablan con el stick por ADB
(`tools/env.sh` centraliza la conexión). Nada se desinstala: todo se desactiva
con `pm disable-user --user 0`, y un único fichero `tools/disabled.list` es la
fuente de verdad de lo desactivado, que consume `tools/restore.sh`.

**Stack:** bash + `adb` (platform-tools 37.0.1) + `pm`/`dumpsys` de Android 9.

**Spec:** `C:\Users\Hugop\Downloads\PROMPT.md` (adaptado: solo debloat, sin
launcher propio — decisión del usuario).

## Restricciones globales

- Solo `pm disable-user --user 0`. **Nunca** `pm uninstall` de paquetes de sistema.
- Una orden `pm` por llamada de shell (las encadenadas se paran en silencio).
- Lotes pequeños con captura de pantalla entre lotes.
- Nada de DNS privado (decisión del usuario).
- Comprobación de pantalla viva entre lotes: `dumpsys activity activities | grep topResumedActivity`.

## Decisiones del usuario (checklist 2026-10-20)

| Elemento | Decisión |
|---|---|
| Apps de streaming | Mantener Prime Video, SkyShowtime, HBO Max, Spotify, Twitch, Plex. Desactivar el resto. |
| Asistente de voz | Desactivar (katniss + pumpkin). `com.google.android.tts` se mantiene por accesibilidad (TalkBack). |
| Chromecast (mediashell) | Mantener. |
| Actualizaciones de firmware | Mantener (`com.xiaomi.mitv.updateservice`). |
| DNS anti-ads | No. |

## NO tocar (con motivo)

| Paquete | Motivo |
|---|---|
| `com.google.android.tvlauncher` | Es el launcher activo (HOME). El usuario pidió solo debloat. |
| `com.google.android.tungsten.setupwraith` | La red Wi‑Fi de la 1ª configuración es suya (`cname=com.google.android.tungsten.setupwraith`). Desactivarlo **borraría esa red** y el stick se quedaría sin Wi‑Fi ni ADB. |
| `com.android.tv.settings` | Ajustes del sistema (incluye entrada de HDMI y pairing del mando). |
| `com.google.android.gms`, `gsf`, `vending` | Play Services y Play Store. |
| `com.google.android.inputmethod.latin` | Teclado en pantalla. |
| `com.google.android.tts` | Accesibilidad (TalkBack). |
| `com.google.android.apps.mediashell` | Chromecast. |
| `com.xiaomi.mitv.updateservice` | Actualizaciones de firmware. |
| `com.mitv.dream` | Protector de pantalla (evita quemado de panel). |
| `com.mitv.service` | Servicio de TV (tuner/HDMI). |
| `com.droidlogic.*`, `com.android.bluetooth`, `InputDevices` | Hardware, pairing y mando. |
| `com.android.shell` | Se usa para el propio ADB. |

## Lotes

- [ ] **Lote 1 — Contenido y publicidad Xiaomi (6)**
  `com.mitv.tvhome.mitvplus`, `com.mitv.tvhome.atv`, `com.mitv.tvhome.michannel`,
  `com.xm.webcontent`, `com.xiaomo.tv.milegal`, `com.miui.tv.analytics`
- [ ] **Lote 2 — Servicios Xiaomi de fondo (5)**
  `com.mitv.download.service`, `com.mitv.videoplayer`, `com.mitv.milinkservice`,
  `android.autoinstalls.config.xioami.mibox3`, `tv.alphonso.alphonso_eula`
- [ ] **Lote 3 — Google: recomendaciones, telemetría y feedback (5)**
  `com.google.android.tvrecommendations`, `com.google.android.backdrop`,
  `com.google.android.feedback`, `com.google.android.tv.bugreportsender`,
  `com.google.android.partnersetup`
- [ ] **Lote 4 — Sincronización y asistente de configuración (3)**
  `com.google.android.syncadapters.contacts`, `com.google.android.syncadapters.calendar`,
  `com.xiaomi.android.tvsetup.partnercustomizer`
- [ ] **Lote 5 — Asistente de voz (2)**
  `com.google.android.katniss`, `com.google.android.speech.pumpkin`
- [ ] **Lote 6 — Streaming no usado (6)**
  `com.netflix.ninja`, `com.disney.disneyplus`, `tv.accedo.mitele_xdk`,
  `com.antena3.atresplayer.tv`, `dw.com.androidtv.live`, `com.rt.tv`
- [ ] **Verificación final** — reinicio, captura, `pm list packages -d`, cierre de ADB.

## Tareas transversales

- [ ] `tools/backup.sh` — snapshot de paquetes, getprop y settings.
- [ ] `tools/restore.sh` — reactiva lo que haya en `tools/disabled.list`.
- [ ] `tools/shot.sh` — captura de pantalla al stick.
- [ ] `README.md` — cómo revertir, qué se tocó y por qué.

## Revisión (qué mirar antes de dar por bueno)

| Riesgo | Comportamiento esperado |
|---|---|
| Desactivar `setupwraith` | El stick se queda sin Wi‑Fi y sin ADB → **no se toca** (verificado con `cname=`). |
| Desactivar el launcher por error | La pantalla se queda en negro sin HOME → verificar `topResumedActivity` tras cada lote. |
| `tvrecommendations` desactivado | La pantalla de inicio sigue arrancando; solo desaparecen filas de recomendaciones. |
| `partnersetup` desactivado | No afecta al Launcher ni a Play Store; solo al aprovisionamiento/atribución del fabricante. |
| Reinicio tras desactivar | El stick vuelve a levantar el launcher en <60 s; si no, hay que recuperar por USB o restablecer de fábrica. |
