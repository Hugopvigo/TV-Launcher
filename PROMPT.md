# El prompt

Pega esto en Claude Code (u otro asistente con acceso a shell) en un
ordenador conectado a la misma red que el stick. Funciona en televisores con
**Android TV o Google TV**; no en Samsung Tizen, LG webOS ni Fire TV.

Si has clonado este repositorio, dilo: el asistente puede reutilizar
`tools/` y `launcher/` en vez de escribirlos de cero, y solo tiene que
ajustar `launcher/app/src/main/res/values/config.xml`.

Antes de empezar, en el stick: Ajustes → Configuración del dispositivo →
Acerca de → pulsa **OK** 7 veces sobre *Compilación* → atrás → Opciones de
desarrollador → activa **Depuración ADB** (y *Network/ADB debugging* si
aparece). Deja el stick encendido.

---

Quiero dejar limpio mi Android TV y sustituir su launcher por una pantalla de
inicio limpia estilo Apple TV. Mi modelo es: <MODELO DEL TELEVISOR>. Trabaja
sobre el dispositivo real, paso a paso, y verifica cada cambio con capturas
sacadas de la tele. Pregúntame antes de cualquier cosa que dependa de mis
gustos.

Si este repositorio ya está clonado, **no rehagas el launcher ni los scripts**:
están en `launcher/` y `tools/`. Solo hay que revisar la lista de debloat de
`tools/disabled.list` (está comentada por categorías, con los motivos) y
ajustarla a lo que use esta tele, y decidir qué apps van arriba en
`launcher/app/src/main/res/values/config.xml`.

Conectar
- Localiza la tele en la red y conéctate por ADB en el puerto 5555. Si está en
  otra casa, te daré su dirección de Tailscale: úsala, no busques en mi red local.
- Dime cuándo va a aparecer el diálogo «¿Permitir depuración USB?» en la tele
  para que lo acepte.
- Si el `adb` de siempre falla con «No route to host» en macOS, es la
  privacidad de red local: no lo depures, usa `tools/tv.py`.
- Si el puerto 5555 no abre, la tele solo ofrece depuración wireless:
  búscala con `adb mdns services` y haz `adb pair` + `adb connect`.
- Si `adb install` se queda colgado en Android 14, está esperando una
  confirmación en pantalla: pasa el APK y ejecuta `pm install -r` por shell.

Copia de seguridad primero
- Guarda la lista completa de paquetes, los desactivados, el `getprop` y todos
  los `settings` en una carpeta de backup, y escribe un script que lo revierta.
- Pregúntame qué hay conectado en los puertos USB de la tele.
- Al terminar, cierra el ADB de red: desactiva la depuración, revoca las
  autorizaciones, reinicia y comprueba que `nc -vz <ip> 5555` falla. El puerto
  5555 va en texto plano.

Debloat
- Lista los paquetes instalados y clasifícalos: telemetría del fabricante,
  contenido y promociones, servicios en segundo plano, apps preinstaladas y
  apps de streaming.
- Desactiva los primeros con `pm disable-user --user 0`. Nunca desinstales
  paquetes del sistema.
- No toques lo que la tele necesita: la app de TV y los servicios de entrada
  (HDMI y tuner), el menú de entradas, el control remoto y el Bluetooth, el
  teclado en pantalla, Play Store y Play Services.
- Los nombres de paquete no dicen lo que hace la app: comprueba sus
  actividades y servicios con `dumpsys package` antes de decidir.
- Antes de tocar streaming, asistente de voz, Chromecast, Miracast o el
  servicio de actualizaciones, pregúntame cuáles uso con una lista y desactiva
  solo los que no marque.
- Una orden `pm` por llamada: encadenarlas puede parar en la primera sin avisar.
- Desactiva en lotes pequeños y haz una captura entre lotes.
- Ofréceme un DNS bloqueante de anuncios como extra: en Android 9 o superior,
  `settings put global private_dns_specifier dns.adguard-dns.com` y luego
  `settings put global private_dns_mode hostname`. No bloquea los anuncios de
  YouTube. Se revierte con `private_dns_mode opportunistic`.

Launcher
- Comprueba que la tele acepta APKs antes de construir nada: algunas cajas de
  mercado chino rechazan cualquier sideload con `INSTALL_FAILED_VERIFICATION_FAILURE`
  y otras exigen la firma del fabricante para una app que declare
  `android.intent.category.HOME`. Si pasa cualquiera de los dos casos, haz solo
  la mitad del debloat, dilo claro y deja de reintentar la instalación.
- No instales un launcher de terceros que cobre por lo básico. Construye uno
  nativo y pequeño para Android TV (Java, AndroidX RecyclerView, `minSdk`
  acorde a la tele) e instálalo por ADB, ponlo como inicio y desactiva el de
  fábrica.
- Antes de desactivar el launcher de fábrica, comprueba qué otras apps
  declaran HOME: en Google TV el asistente de configuración
  (`com.google.android.tungsten.setupwraith`) puede quedarse con Home. Antes
  de desactivarlo, ejecuta `dumpsys wifi | grep -o 'cname=[^ ]*'`: si la red
  Wi-Fi guardada en la primera configuración es suya, Android borra esa red al
  desactivarlo y la tele se queda sin Wi-Fi ni ADB.
- En Android 11 o superior el launcher debe declarar `<queries>` para
  `LEANBACK_LAUNCHER` o la rejilla sale vacía.
- Diseño estilo Apple TV 4K: fondo negro, rejilla de 4 columnas a todo el
  ancho, un único cristal translúcido que salta de tesela en tesela, la tesela
  enfocada crece y muestra su nombre, menús redondeados arriba a la derecha
  para Entradas, Wi-Fi y Ajustes, y un saludo con un reloj grande y fino y la
  fecha arriba a la izquierda en una tipografía de licencia abierta como
  Outfit.
- Navegación determinista: Up/Down mueven exactamente una fila, los extremos
  de la barra superior no se caen a las teselas, y se recuerda la última
  tesela enfocada.
- Ligero: la tele tiene unos 1 GB de RAM. Sin desenfoques, sin vídeo, sin
  consultas de red.

Verificar
- Después de cada cambio, inyecta pulsaciones del mando por ADB, haz una
  captura de la tele y mírala. Corrige lo que veas antes de seguir.
- Las capturas de Live TV y HDMI salen vacías (0 bytes): es la tele, no un
  fallo. Comprueba con `dumpsys activity activities | grep topResumedActivity`.
- Al terminar, dame un script de restauración, uno de compilación e instalación,
  y un README que explique cómo reactivar cualquier app y cómo cambiar el
  orden del dock o el fondo.

---

Espera tres o cuatro preguntas de lista (qué apps usas, cuáles cuatro van en el
dock, si conservas el asistente de voz, Chromecast y las actualizaciones de
firmware). Todo es reversible.
