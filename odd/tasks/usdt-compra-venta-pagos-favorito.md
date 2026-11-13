# Feature: USDT compra/venta, countdown, perfiles de pago y moneda favorita

Branch: `stabilize-release-readiness`
Delivery: sin commits por ahora (el usuario no los pidió; el README sigue sin commitear)
Test runner: `./gradlew testDebugUnitTest`

## Objective

Agregar cuatro cosas a Dolita:

1. Una cuenta regresiva con barra de progreso para la muestra horaria de USDT y
   un botón para refrescar USDT en el momento.
2. Las tasas de compra y venta de USDT, con un selector para elegir cuál usa la
   calculadora, ubicadas debajo de la calculadora.
3. Perfiles de pago (Pago móvil y Cuenta de banco) administrables desde
   Ajustes y seleccionables al compartir un cobro. Zelle se retiró en S8.
4. Una moneda favorita en Ajustes que la app abre por defecto.

## Specs

### S1 — USDT compra y venta

> "que el usdt tenga esos cálculos tanto para la venta como para la compra un
> poco más abajo de la calculadora, por si el usuario quiere elegir cuál va a
> usar"

- `DolarQuote` gana `compra: Double?` y `venta: Double?`, opcionales y con
  default `null` para no romper la caché ni el histórico ya guardado.
- Para USDT, `compra` es la mediana de las ofertas `tradeType: "BUY"` de
  Binance P2P y `venta` la mediana de las ofertas `tradeType: "SELL"`. La
  mediana evita que una oferta atípica (una compra a 1.115,80) arrastre el lado
  y voltee el spread.
- `promedio` sigue siendo el número canónico del hero y del histórico:
  `(compra + venta) / 2` cuando ambos lados existen. Si un lado falla, el quote
  se publica con el lado disponible y `promedio` igual a ese lado; si ambos
  fallan, USDT no se publica y el resto de fuentes sigue funcionando.
- El parseo de ofertas se extrae a una función pura y se cubre con payloads de
  ambos lados, lista vacía y JSON inválido.
- La tarjeta de tasas USDT se muestra solo cuando la fuente seleccionada es
  USDT y la calculadora usa la tasa de hoy. Muestra Compra, Venta y Promedio; el
  valor seleccionado queda resaltado.
- Checks: los tres valores de la referencia (Compra 1.076,65 / Venta 1.073,66 /
  Promedio 1.075,16) se reproducen con un payload equivalente; una caché vieja
  sin `compra`/`venta` sigue decodificando.

### S2 — Selector de tasa USDT para la calculadora

> "por si el usuario quiere elegir cuál va a usar"

- Tocar Compra, Venta o Promedio cambia la tasa que usa la calculadora y el
  texto que se comparte del resultado.
- El valor por defecto es Promedio.
- El selector solo aplica a la tasa de hoy. Si la calculadora está en una tasa
  histórica o futura, la tarjeta se oculta: una muestra histórica no tiene
  compra/venta y fingir que el selector la cambia sería mentir.
- Si la tasa elegida deja de estar disponible tras un refresh (un lado falló),
  la calculadora cae al promedio sin romper el estado.
- Checks: elegir Compra cambia el resultado de la calculadora y el share;
  cambiar de fuente y volver conserva la elección mientras la app vive.

### S3 — Countdown y actualización manual de USDT

> "que salga como una cuenta regresiva con una barra de estado para el usdt que
> se actualiza cada hora, y al lado un botón para actualizarlo en ese mismo
> momento"

- Con USDT seleccionado, arriba de la calculadora aparece una tarjeta con:
  - la cuenta regresiva a la próxima muestra,
  - una barra que se drena desde el slot anterior hasta el próximo,
  - un botón compacto de refresh al lado.
- La próxima muestra respeta la ventana real del worker (08:00–21:59): dentro
  de la ventana es la próxima hora en punto; antes de las 08:00 es hoy a las
  08:00; a las 21:00 o después es mañana a las 08:00.
- La barra representa la fracción restante del ciclo programado (slot anterior →
  próximo slot): arranca llena justo después de una muestra y se drena hasta
  vaciarse justo antes de la próxima. No usa el tiempo transcurrido desde el
  último sample, así que un refresh manual no la reinicia: la próxima muestra
  automática sigue cayendo en la hora en punto.
- De noche el tramo real es 21:00 → 08:00 (11 h); la barra se drena sobre ese
  tramo completo, no finge una hora que no existe.
- La última muestra se lee de `fechaActualizacion` del quote USDT
  (`OffsetDateTime`). Si no se puede parsear, la tarjeta muestra la cadencia
  sin countdown y el botón sigue disponible.
- El botón refresca SOLO USDT (`refreshUsdt`), no vuelve a pedir BCV, actualiza
  el quote en pantalla y la caché, y muestra un spinner mientras corre.
- Si el refresh falla, el quote viejo se queda y la tarjeta muestra un error
  inline; no se blankea la pantalla.
- Checks: la cuenta baja cada segundo; la barra se drena hacia atrás (1.0 al
  inicio del ciclo, 0.5 a la mitad, 0.0 antes del próximo slot); un refresh
  manual no la reinicia; un fallo de red deja el quote anterior y muestra el
  error.

### S4 — Perfiles de pago

> "perfiles de pago (pago móvil, cuenta de banco, zelle) ... cuando lo vaya a
> compartir pueda seleccionar qué tipo de pago le quiere mandar al cliente
> cuando le vaya a cobrar"

- Modelo local `PaymentProfile` con `id`, `type`, y los campos de cada tipo:
  - Pago móvil: nombre, teléfono, documento V/E/J + número, banco.
  - Cuenta de banco: nombre, banco, número de cuenta, documento V/E/J + número.
- Zelle se retiró del modelo y de la UI en S8. El codec descarta las entradas
  Zelle viejas una por una, sin perder los demás perfiles guardados.
- El selector de documento ofrece V (Venezolano), E (Extranjero) y J
  (Jurídico), con la leyenda "V Venezolano · E Extranjero · J Jurídico" debajo
  de la fila.
- Se guardan como JSON en SharedPreferences, solo en el dispositivo. Nunca se
  loguean ni se envían a ningún lado; solo salen cuando el usuario comparte
  explícitamente.
- Ajustes tiene una fila "Perfiles de pago" con el total guardado que abre una
  pantalla completa con pestañas por tipo, lista, agregar, editar y eliminar.
- El formulario valida nombre y los campos obligatorios de cada tipo; muestra
  errores inline y no guarda un perfil incompleto.
- Se pueden tener varios perfiles por tipo.
- Checks: round-trip del codec; un JSON corrupto devuelve lista vacía; agregar,
  editar y borrar persiste tras cerrar y abrir la pantalla.

### S5 — Compartir con datos de pago

> "cuando lo vaya a compartir pueda seleccionar qué tipo de pago le quiere
> mandar al cliente cuando le vaya a cobrar"

- Al tocar compartir (tasa o resultado de la calculadora), si hay perfiles
  guardados aparece un selector con "No incluir" y todos los perfiles,
  agrupados por tipo.
- Sin perfiles guardados, el share actual sigue igual: no aparece un diálogo
  vacío.
- El perfil elegido se agrega al texto y, si "Compartir como imagen" está
  activo, a la tarjeta PNG, en un bloque de datos de pago separado del número y
  de la fecha. La fecha sigue siendo la última línea del texto. Con el switch
  apagado se comparte solo el texto.
- Si el usuario cancela el selector, no se comparte nada.
- Checks: el texto con perfil contiene los datos y la fecha al final; la
  tarjeta conserva título, número, bloque de pago y fecha; sin perfil el
  payload es idéntico al actual.

### S6 — Moneda favorita al abrir

> "una opción en ajustes para elegir la moneda favorita y que al abrir la app
> salga directamente en esa moneda"

- Ajustes tiene "Moneda favorita" con Dólar, Euro y USDT.
- Se persiste en `SourcePreferences` (`favorite_source`, default `usd`).
- Al arrancar, el ViewModel inicializa `selectedFuente` con la favorita; si esa
  fuente no está disponible en el snapshot, cae al primer quote como hoy.
- Cambiar la favorita también selecciona esa fuente de inmediato.
- Tocar un chip de fuente no cambia la favorita.
- Checks: default `usd`; valor inválido cae a `usd`; cambiar la favorita
  persiste y selecciona la fuente.

### S7 — Verificación y release

- `./gradlew testDebugUnitTest` verde.
- `./gradlew assembleRelease` verde.
- Smoke test en el dispositivo `c3228f2a` de las features.
- El bump de versión (`1.32.0`, `versionCode` +1) y el release se hacen recién
  cuando el usuario lo pida. `app/build.gradle.kts` sigue siendo el único lugar
  donde se escribe una versión.

### S8 — Tarjeta compacta, compartir como imagen y retiro de Zelle

Pedido del usuario (resumen del pedido, sin cita textual):

- La tarjeta de compartir salía muy grande; compactarla y mejorar su estética.
- Un switch en Ajustes para elegir si se comparte con imagen o solo texto.
- Borrar Zelle de los métodos y perfiles de pago.
- El dropdown de bancos debe quedar compacto y scrollable.

- La tarjeta usa ancho fijo 1080 y alto dinámico entre 760 y 1080. El bloque de
  datos de pago va en un panel redondeado propio, el número se ajusta al ancho
  disponible y la fecha queda al pie sobre una línea divisoria.
- Ajustes tiene el switch "Compartir como imagen" en PAGOS, después de "Moneda
  favorita", con default activado para conservar el comportamiento anterior.
- `ShareImage.share(..., asImage)` omite el render y el archivo cuando es
  `false`; el chooser queda como "Compartir texto" y el caption sigue siendo
  seleccionable.
- Zelle sale de `PaymentProfileType`, del modelo, del formulario y de las
  etiquetas. `PaymentProfileCodec.decode` decodifica entrada por entrada y
  descarta solo las entradas Zelle viejas; los demás perfiles sobreviven.
- Checks: preferencia default/persistencia; codec con lista mixta; tarjeta de
  1080×760–1080; smoke de imagen y de solo texto en el dispositivo.

### S9 — Animación de apertura

Pedido del usuario (cita textual):

> "puedes hacer algun tipo de animacion cuando abres la aplicacion para ocultar
> los pocos segundos que tarda cargando"

- Al abrir la app en frío se muestra una cortina de marca Dolita que cubre la
  espera: moneda con órbita, wordmark "Dolita", "Actualizando la tasa" y una
  barra con brillo viajero que se mueve mientras la tasa llega.
- La cortina usa los tokens del tema (acento y superficie), así que sigue a
  cada paleta, al color dinámico y a AMOLED, y respeta la opción del sistema
  de quitar animaciones.
- Mínimo 900 ms para que no parpadee un solo frame; máximo 3 s, tras el cual
  el skeleton de carga toma el relevo y la espera larga se muestra con honestidad.
- Solo en arranque en frío: rotar o volver desde segundo plano no repite la
  animación.
- El splash del sistema debe usar la misma superficie que la app, para que no
  haya un destello blanco o gris entre el splash y la cortina.
- Checks: `openingVisible` con warm start, mínimo, máximo y error; suite
  unitaria; `assembleRelease`; captura de la cortina en el dispositivo.

### S10 — Ventana del creador, pie de página y ajuste de la animación

Pedido del usuario (citas textuales):

> "quiero que al final de la pagina de las distintas tasas salga "Hecho por
> @yournelvi" para hacer publicidad a mi perfil de instagram"

> "quiero que salga un boton que muestre una informacion en una ventana
> emergente, con una vista parecida a esta ... sale un circulo como para poner
> una imagen quiero que coloques esta metida en un circulo asi como si fuera de
> instagram"

> "y tambien quiero que este debajo de lo que te dije de "hecho por
> @yournelvi" con un boton que diga conocenos con el mismo estilo de la pantalla
> principal"

> "y quiero que salga una ventana asi despues de que el usuario tenga mas de un
> dia usando la app y que le aparezca de forma aleatoria una sola vez al dia"

> "mejor ponlo que se vea una sola vez al dia la segunda vez que abre la
> aplicacion al dia"

> "recuerda que diga que los siga en el perfil de instagram y en el perfil de
> github y que le den una estrella al proyecto"

> "en vez de proyecto que sea al repositorio"

> "pon en la ventana en vez de "sobre el creador" ponle como algo "apoya al
> creador del Dolita""

> "sobre la animacion cuando abres la aplicacion quiero que la edites y le
> quites la barra de carga y el aro que esta dando vueltas al rededor"

Reglas:

- Al final del board de tasas va "Hecho por @yournelvi" y debajo el botón
  "Conócenos", con la misma voz hairline que el resto de las acciones de la app.
- La ventana se titula "Apoya al creador de Dolita" y lleva: avatar circular,
  "¡Hola! 👋", dos párrafos, una tarjeta con "Mi perfil de Instagram" y "Mi
  perfil de GitHub" (cada uno con su acción "Seguir"), el botón primario
  "Seguirme en Instagram", el botón secundario "Dame una estrella al
  repositorio ⭐" y el cierre "Gracias por utilizar la aplicación ❤️".
- Se abre a mano desde "Conócenos", o sola en la **segunda apertura del día**,
  una sola vez por día, y solo a partir del segundo día de uso (se toma la
  fecha de instalación del paquete, no la de la primera apertura tras la
  actualización). Abrirla a mano también quema el día.
- Los logos son los que ya tenía la app: el de Instagram conserva su color y
  el de GitHub se tiñe con el tema. El de GitHub apunta al perfil
  (https://github.com/YourNelvi); la estrella apunta al repositorio.
- La animación de apertura pierde el aro y la barra de progreso: quedan la
  moneda, el halo, el wordmark y "Actualizando la tasa".
- Checks: `CreatorPromptTest` con primer día, segunda apertura, mismo día y
  día nuevo; suite unitaria; `assembleRelease`; captura en el dispositivo.

## Tasks

- [x] T1 — Modelo y repositorio USDT compra/venta (S1): quote con
  `compra`/`venta`, dos requests BUY/SELL, parser puro y tests.
  - Evidencia: `UsdtPayloadParsingTest` verde con
    `./gradlew testDebugUnitTest --tests "com.example.erp.data.UsdtPayloadParsingTest"`.
- [x] T2 — Tarjeta de tasas USDT y selector para la calculadora (S1, S2):
  `UsdtRateMode`, derivación del quote de cálculo y UI debajo de la calculadora.
  - Evidencia: `UsdtRateModeTest` + suite completa verde (374 tests).
- [x] T3 — Countdown, progreso y refresh manual (S3): política de próxima
  muestra, `refreshUsdt` en repositorio/ViewModel y tarjeta.
  - Evidencia: `UsdtSchedulePolicyTest` (21 casos) + suite completa verde.
- [x] T4 — Modelo, codec y store de perfiles de pago (S4).
  - Evidencia: `PaymentProfileCodecTest` + `PaymentProfileTest` (14 tests).
- [x] T5 — Pantalla de perfiles: pestañas, lista, formulario, editar y borrar;
  entrada desde Ajustes (S4).
  - Evidencia: suite completa verde (388 tests). Smoke de UI pendiente en T8.
- [x] T6 — Share con datos de pago (S5): `ShareContent`, bloque de pago en
  texto/PNG, selector de perfil y tests.
  - Evidencia: `ShareContentTest` + suite completa verde (396 tests).
  - Nota: el payload sin perfil queda igual salvo un salto de línea final que
    `RateShareText` arrastraba y ningún test fijaba; se normalizó.
- [x] T7 — Moneda favorita (S6): preferencia, init del ViewModel, fila y
  diálogo en Ajustes, tests.
  - Evidencia: `SourcePreferencesTest` + suite completa verde (400 tests).
- [x] T8 — Verificación completa (S7): tests, `assembleRelease`, smoke test en
  dispositivo.
  - Hecho: suite completa 400 tests verde; `assembleRelease` verde; APK
    `app/build/outputs/apk/release/app-release.apk` (49.013.886 bytes,
    SHA-256 `a15d361bfb4f54c8250b9a640ea515aff079e2f1aaf55454b1628ddd53b90c17`).
  - Smoke en el Redmi `c3228f2a`: countdown/tasas/perfiles/moneda favorita
    verificados en sesiones anteriores; en esta sesión se suman Ajustes,
    perfiles y el share.
- [x] T9 — Retirar Zelle (S8): enum/modelo/UI/etiquetas y codec por entrada.
  - Evidencia: `PaymentProfileCodecTest` 7 tests, incluido el caso de lista
    mixta con Zelle omitido; `PaymentProfileTest` sin Zelle; suite 416 tests
    verde.
- [x] T10 — Compartir como imagen opcional (S8): preferencia, switch, ViewModel,
  `ShareImage` condicional.
  - Evidencia: `ShareImagePreferenceTest` 3 tests; share de solo texto
    verificado en el chooser ("Compartir texto", sin miniatura).
- [x] T11 — Tarjeta compacta (S8): renderer 1080 × 760–1080, panel de pago y
  tipografías ajustadas.
  - Evidencia: tarjeta enviada a WhatsApp "Yo (Tú)" a 1080×~980, sin el vacío
    vertical anterior.
- [x] T12 — Verificación en dispositivo y limpieza (S8): release instalado,
  smoke de imagen/texto, perfil de prueba y archivos temporales eliminados.
  - Evidencia: `assembleRelease` verde; APK 49.046.654 bytes, SHA-256
    `954efe510a4d1c9293908bb368d87013638a6ee7c7956ef7fbc86a64342982aa`;
    perfil "Prueba Dolita" borrado; `/sdcard/erp_*` y `/sdcard/wa_*`
    eliminados.
- [x] T13 — Fix del teclado y del handle al abrir Ajustes desde la calculadora:
  limpiar foco y ocultar el IME antes de mostrar el overlay.
  - Evidencia: `DolarScreen` usa `LocalFocusManager` y
    `LocalSoftwareKeyboardController` en `openSettings()`/`openPaymentProfiles()`.
    En el Redmi, con el campo USD enfocado, abrir Ajustes deja
    `mInputShown=false` y sin el handle azul; al volver, tocar el campo vuelve a
    abrir el teclado normalmente. APK nuevo 49.046.654 bytes, SHA-256
    `6c0ebaffe40aea8b6e628e2ef84f82c46aec68d8b57aa0ecad7752aeef727ac3`.
- [x] T14 — Animación de apertura (S9): cortina de marca en el arranque en
  frío, con mínimo, máximo y splash del sistema alineados al tema.
  - Evidencia: `OpeningScreen` (moneda con órbita, wordmark "Dolita" y barra
    de progreso con brillo Viajero), `openingVisible` con 6 casos en
    `OpeningStateTest` (RED observado antes de implementar), suite 422 tests
    con 0 fallos, `assembleRelease` verde. APK 49.063.266 bytes, SHA-256
    `5ff5cd299bdc935fb629cbe703ef01bbacec8bb1ca7f1165d2c3933c1d4d60bc`.
    En el Redmi: el splash del sistema sale oscuro (ya no gris), la cortina se
    ve a opacidad completa con la moneda, el wordmark y la barra de progreso,
    y al volver desde segundo plano NO se repite (el log de Android confirma
    "Activity not started, its current task has been brought to the front",
    o sea que la actividad ni se recrea).
- [x] T15 — Ventana del creador, pie de página y ajuste de la animación (S10):
  "Hecho por @yournelvi" + botón "Conócenos" al final del board, ventana
  "Apoya al creador de Dolita", aviso en la segunda apertura del día y
  cortina sin aro ni barra.
  - Evidencia: `CreatorPrompt.kt` + `CreatorPromptTest` (6 casos, RED
    observado antes de implementar), `CreatorDialog.kt` con los logos que ya
    tenía la app, `MainActivity` cuenta las aperturas en `onStart` (una
    rotación no cuenta), suite 428 tests con 0 fallos, `assembleRelease` verde.
    APK 49.100.130 bytes, SHA-256
    `61e89b553e05aad90e34e64ceca17aa4bd957b520c03cfeadbc63070f3641ab6`.
    En el Redmi: pie de página con "Hecho por @yournelvi" y "Conócenos"; la
    ventana abre con el avatar, las dos filas de perfil y los dos botones; el
    aviso automático aparece solo en una apertura (verificado con un build
    temporal que simula un usuario con más de un día de uso).
  - Avatar real: `app/src/main/res/drawable/creator_avatar.png` (640×640,
    copia de `Escritorio/Nueva carpeta/imagen del perfil.png`). El círculo lo
    recorta solo (`ContentScale.Crop`), sin recorte manual. APK final
    49.563.286 bytes, SHA-256
    `e0c60715a446d436c63777fe5c2a0c07645b98992a9287c14795ae99f2823143`.
- [x] T16 — Release 1.31.1 publicado (S9, S10): versión 1019016 / "1.31.1",
  tag `v1.31.1` sobre el commit de bump, APK adjunto y `digest` publicado.
  - Evidencia: la firma del APK es idéntica a la de la 1.31.0 (certificado
    SHA-256 `2c460f11…efe7c`), así que se instala encima sin desinstalar. El
    release publicado expone `tag_name`, el asset `Dolita-1.31.1.apk` y
    `digest: sha256:ea977e91…22bb945b47`, que es lo que la app necesita para
    habilitar la descarga desde el diálogo. Verificado además en un teléfono
    ajeno: la actualización se descargó y se instaló desde la app.
  - Commits: `7ab691c` (feat) + `5ff564c` (chore: bump version to 1.31.1),
    tag `v1.31.1` sobre `5ff564c`, rama `stabilize-release-readiness` pusheada.
- [x] T17 — El banco de los perfiles de pago ya no se escribe (S10): el campo
  es un selector y el valor sale del catálogo.
  - Evidencia: `isKnownVenezuelanBank` en `VenezuelanBanks.kt` con 5 casos en
    `VenezuelanBanksTest` (label del catálogo, código suelto, texto libre,
    código inexistente y vacío). `BankField` queda deshabilitado con los
    colores normales: sin teclado, sin cursor y sin edición; la flechita es
    la única entrada al catálogo. La validación del formulario ahora exige
    un banco del catálogo, no solo texto no vacío.
  - Verificado en el Redmi: al tocar el texto del campo `mInputShown=false`
    y el teclado no aparece; `input text` no escribe nada en el campo; la
    flechita abre la lista; al elegir "0102 · Banco de Venezuela (BDV)" el
    campo se completa y "Guardar" se habilita.
  - Commit `820976c`, pusheado. Queda para el próximo release.

## Log

### L1 — Pedido original

> "que el usdt tenga una cuenta regresiva con una barra de estado que se
> actualiza cada hora y al lado un botón para actualizarlo en ese mismo
> momento"
>
> "que el usdt tenga esos cálculos tanto para la venta como para la compra un
> poco más abajo de la calculadora, por si el usuario quiere elegir cuál va a
> usar"
>
> "perfiles de pago (pago móvil, cuenta de banco, zelle) ... cuando lo vaya a
> compartir pueda seleccionar qué tipo de pago le quiere mandar al cliente
> cuando le vaya a cobrar"
>
> "una opción en ajustes para elegir la moneda favorita y que al abrir la app
> salga directamente en esa moneda"

Referencias visuales: capturas de la app "al cambio" para el countdown, las
tasas USDT y los formularios de Pago móvil / Cuenta de banco / Zelle.

### L2 — Decisiones de diseño

- Compra = `tradeType BUY` (lo que cuesta comprar USDT); Venta = `tradeType
  SELL` (lo que se recibe al vender). Promedio = media de ambos, que reproduce
  la referencia 1.075,16.
- El selector USDT solo aplica a la tasa de hoy. El histórico no guarda
  compra/venta y no se inventa.
- El countdown respeta la ventana 08:00–21:59 del worker; a la noche muestra la
  próxima muestra real (08:00), no un 00:00 que nunca se cumple.
- El refresh manual es solo USDT para no gastar una request de BCV ni resetear
  la fuente seleccionada.
- Los perfiles de pago son locales, sin cuenta ni backend. El share es la única
  salida y siempre requiere una elección explícita.
- La tarjeta PNG conserva su layout: el bloque de pago va en una banda propia
  arriba de la fecha, no dentro del número grande.

### L3 — Implementación y verificación

- T1–T7 implementados en la working tree, sin commits (el usuario no los pidió).
- Suite completa: 400 tests, 0 fallos.
- `assembleRelease`: verde. APK en
  `app/build/outputs/apk/release/app-release.apk` (49.013.886 bytes, SHA-256
  `a15d361bfb4f54c8250b9a640ea515aff079e2f1aaf55454b1628ddd53b90c17`).
- El APK queda en `1.31.0` (`versionCode 1019015`): el bump a `1.32.0` se hace
  recién cuando el usuario pida publicar.
- T8 en curso: smoke visual en el Redmi `c3228f2a`; pendiente cerrar la
  verificación de la barra y limpiar los residuos de prueba.
- Pendientes de decisión del usuario: commitear la feature + README, y
  fast-forward de `master` a `stabilize-release-readiness` + push.

### L4 — Feedback de dispositivo (2026-11-12)

Pedido del usuario, textual:

- "quiero que lo del tiempo restante este un poco mas finito para que no ocupe
  tanto espacio, y que las tasas de venta y compra sean mas reales, auque veo
  que no se aleja demasiado la verdad siento que esta bien mas aparte que se
  actualiza cada hora o en tiempo real si lo desea el usuario"
- "otra cosa en los perfiles de pago tambien esta la opcion de poner la "J" que
  es de juridico en dado caso por favor ya que vi que colocaste la v y la e para
  los extrajeros"
- "oye sabes que parece que la barra de progreso para la actualizacion del usdt
  esta yendo hacia adelante tal parece quiero que vaya para atras haciendo
  referencia a cuento tiempo le queda, y qeu la barra de progreso sea real. ya
  que me parece que habia dicho que se actualice cada hora no?"

Decisiones:

- Tarjeta de countdown compacta: refresh como `FilledTonalIconButton` de 40 dp,
  countdown en `headlineSmall`, padding 16/12 y textos de cadencia en
  `labelSmall`.
- Tasas: la media de la primera página se contaminaba con una oferta atípica de
  1.115,80 y hacía `venta > compra`. `parseUsdtPrice` pasa a mediana; el
  subtítulo de la tarjeta dice "Mediana de ofertas P2P · elige la tasa".
- Documento: se agrega J (Jurídico) a V/E y la leyenda de los tres tipos.
- Barra: en vez de la fracción transcurrida desde el último sample, usa la
  fracción restante del ciclo programado (slot anterior → próximo slot). Se
  agrega `previousUsdtFetchMillis`; un refresh manual no reinicia la barra.
  `UsdtCountdown.progress` se renombra a `remainingFraction`.

Evidencia:

- `UsdtSchedulePolicyTest` nuevo: 1.0 al inicio del ciclo, 0.75/0.5/0.25 en
  :15/:30/:45, 0.5 tras un refresh manual, 9.5/11 y 0.5/11 en el tramo nocturno.
- `UsdtPayloadParsingTest`: mediana y caso de oferta atípica.
- `PaymentProfileTest`: share con prefijo J.
- Suite completa y `assembleRelease` verdes.
- En el Redmi: a las 10:59:28 la barra mostraba 00:32 restantes (casi vacía);
  pendiente confirmar el reinicio lleno a las 11:00 y el pill J.

### L5 — Tarjeta compacta, switch de imagen y retiro de Zelle (2026-11-12)

Pedido del usuario (resumen del pedido, sin cita textual):

- La tarjeta de compartir tasa + monto + datos de pago sale muy grande;
  mejorarla estéticamente y compactarla.
- Agregar un switch en Ajustes para elegir si se comparte con imagen o solo
  texto; el usuario decide.
- Borrar Zelle de los métodos y perfiles de pago.
- El dropdown de bancos ya debe quedar compacto y scrollable.
- No commitear, pushear ni bumpear versión sin aprobación explícita.
- Borrar el perfil de prueba y los archivos temporales del teléfono al terminar.

Decisiones:

- La preferencia se llama `shareAsImageEnabled`, default `true` para conservar
  el comportamiento actual. El switch vive en PAGOS, después de "Moneda
  favorita".
- La tarjeta nueva tiene ancho fijo 1080 y alto dinámico 760–1080, con panel
  propio para los datos de pago. El número se ajusta al ancho disponible.
- Zelle se elimina del enum/modelo/UI. El codec decodifica entrada por entrada
  para descartar solo los perfiles Zelle viejos sin perder los demás.

Evidencia:

- Suite unitaria: 416 tests, 0 fallos. Nuevos: `ShareImagePreferenceTest` (3) y
  el caso de lista mixta con Zelle en `PaymentProfileCodecTest` (7).
- `assembleRelease` verde; APK 49.046.654 bytes, SHA-256
  `954efe510a4d1c9293908bb368d87013638a6ee7c7956ef7fbc86a64342982aa`.
- En el Redmi `c3228f2a`: Ajustes muestra "Pago móvil y cuenta de banco" y el
  switch "Compartir como imagen"; el selector de perfiles solo ofrece Pago
  móvil y Cuenta de banco.
- Tarjeta nueva compartida a WhatsApp "Yo (Tú)" con USD 20,00 / Bs 18.088,97:
  se ve compacta, con panel de datos de pago y sin el vacío vertical anterior.
- Con el switch apagado, el chooser muestra "Compartir texto" y solo el
  caption, sin miniatura.
- Switch devuelto a activado; perfil "Prueba Dolita" eliminado; `/sdcard/erp_*`
  y `/sdcard/wa_*` borrados.
- Sin commits, sin push y sin bump de versión.
- Mirror de Engram pendiente: el servidor rechaza el proyecto detectado como
  ruta (`invalid_project`); el documento local queda como fuente de verdad.

### L6 — Teclado y handle al abrir Ajustes desde la calculadora (2026-11-12)

Reporte del usuario:

> "sabes que el teclado se bugea, cuando estas en la calculadora y le das al
> boton de ajustes el teclado sigue permaneciendo ahi, siquieres mira como esta
> para veas que es cierto"

> "tambien permanece la bolita que muestra donde estas parado, eso lo soluciona
> de la misma manera?"

Diagnóstico:

- La calculadora sigue compuesta debajo del overlay de Ajustes, así que el
  `TextField` de USD conserva el foco y la conexión con el IME. El teclado y el
  handle del cursor (la "bolita" azul) se dibujan sobre Ajustes.

Fix:

- `DolarScreen` obtiene `LocalFocusManager` y
  `LocalSoftwareKeyboardController`. `openSettings()` y
  `openPaymentProfiles()` hacen `clearFocus(force = true)` y `hide()` antes de
  mostrar el overlay.

Evidencia:

- Reproducido antes del fix: teclado abierto y handle azul visibles sobre
  Ajustes.
- Después del fix, en el Redmi `c3228f2a`: `mInputShown=false` y sin handle al
  abrir Ajustes.
- Al volver a la calculadora, tocar el campo USD vuelve a abrir el teclado
  (`mInputShown=true`).
- Suite unitaria y `assembleRelease` verdes. Sin commit, sin push y sin bump.

### L7 — Animación de apertura (2026-11-12)

Pedido del usuario:

> "puedes hacer algun tipo de animacion cuando abres la aplicacion para ocultar
> los pocos segundos que tarda cargando"

Decisiones:

- La cortina cubre el arranque en frío, no "la carga": la tasa cacheada se
  pinta antes del primer frame, así que preguntar por el estado "todavía
  cargando" salteaba la animación justo cuando el usuario está mirando el
  splash del sistema.
- Mínimo 900 ms (para que la marca no parpadee un solo frame) y máximo 3 s
  (pasado ese punto, el skeleton honesto toma el relevo).
- La detección de "app abierta" la da el proceso, no `savedInstanceState`:
  al relanzar una app killada Android restaura el estado de la tarea y un
  arranque en frío llega con bundle no nulo, indistinguible de una rotación.

Implementación:

- `OpeningScreen`: moneda con órbita, wordmark "Dolita", "Actualizando la
  tasa" y barra de progreso con brillo viajero. Todo con los tokens del tema
  (acento, superficie), así que sigue a cada paleta, color dinámico y AMOLED.
  Respeta "quitar animaciones" del sistema.
- `openingVisible` + `OpeningStateTest`: la regla de visibilidad queda fijada
  en 6 casos (RED observado antes de implementar).
- `MainActivity.firstActivityOfProcess`: marca el primer arranque del proceso.
- `Theme.ERP` con `android:windowBackground` = `@color/splash_background`
  (blanco de día, grafito de noche): el splash del sistema deja de salir gris.

Evidencia:

- Suite unitaria 422 tests, 0 fallos.
- `assembleRelease` verde; APK 49.063.266 bytes, SHA-256
  `5ff5cd299bdc935fb629cbe703ef01bbacec8bb1ca7f1165d2c3933c1d4d60bc`.
- En el Redmi: splash del sistema oscuro y cortina visible a opacidad completa
  (moneda con la órbita, "Dolita", "Actualizando la tasa" y la barra con el
  brillo viajero).
- Volver desde segundo plano no repite la animación: la actividad no se
  recrea y las ocho capturas seguidas muestran el contenido directo.
- Temporales de la verificación borrados del teléfono (`/sdcard/*.png` de
  prueba y `start.mp4`); quedó intacto el archivo del usuario
  "pedido movil.mp4".
- Sin commit, sin push y sin bump de versión.

### L8 — Ventana del creador y ajuste de la animación (2026-11-12)

Pedido del usuario:

> "puedes hacer algun tipo de animacion cuando abres la aplicacion para ocultar
> los pocos segundos que tarda cargando" (animación, ya entregada en L7)

> "sobre la animacion cuando abres la aplicacion quiero que la edites y le
> quites la barra de carga y el aro que esta dando vueltas al rededor"

> "quiero que al final de la pagina de las distintas tasas salga "Hecho por
> @yournelvi""

> "quiero que salga un boton que muestre una informacion en una ventana
> emergente ... sale un circulo como para poner una imagen"

> "y tambien quiero que este debajo de lo que te dije de "hecho por
> @yournelvi" con un boton que diga conocenos"

> "y quiero que salga una ventana asi despues de que el usuario tenga mas de un
> dia usando la app y que le aparezca de forma aleatoria una sola vez al dia"

> "mejor ponlo que se vea una sola vez al dia la segunda vez que abre la
> aplicacion al dia"

> "pon en la ventana en vez de "sobre el creador" ponle como algo "apoya al
> creador del Dolita""

> "recuerda que diga que los siga en el perfil de instagram y en el perfil de
> github y que le den una estrella al proyecto" / "en vez de proyecto que sea
> al repositorio"

Decisiones:

- El aviso automático NO es aleatorio: el usuario pidió que salga en la
  segunda apertura del día, así que la regla es determinista. La primera
  apertura del día es el momento en que el lector quiere la tasa; la segunda
  es cuando ya se instaló.
- "Más de un día usando la app" se mide con la **fecha de instalación del
  paquete**, no con la primera apertura tras la actualización: si no, la
  ventana tardaría un día extra en llegarle justamente a quien la merece.
- Una apertura manual ("Conócenos") también quema el día: si el usuario ya
  la vio, que no le vuelva sola un rato después.
- Las aperturas se cuentan en `MainActivity.onStart`, no en la composición:
  volver desde segundo plano es abrir la app; rotar el teléfono no lo es.
- Los logos del diálogo son los que ya tenía la app (`ic_instagram` con su
  color, `ic_github` teñido con el tema). GitHub apunta al perfil y la estrella
  al repositorio.

Evidencia:

- `CreatorPromptTest` con 6 casos (primer día de uso, primera apertura,
  segunda apertura, una vez por día, día nuevo), RED observado antes de
  implementar.
- Suite unitaria 428 tests, 0 fallos. `assembleRelease` verde; APK
  49.100.130 bytes, SHA-256
  `61e89b553e05aad90e34e64ceca17aa4bd957b520c03cfeadbc63070f3641ab6`.
- En el Redmi: pie de página con "Hecho por @yournelvi" y el botón
  "Conócenos"; la ventana muestra el título "Apoya al creador de Dolita",
  "¡Hola! 👋", las filas "Mi perfil de Instagram" y "Mi perfil de GitHub",
  "Seguirme en Instagram", "Dame una estrella al repositorio ⭐" y el cierre.
- El aviso automático se verificó con un build temporal que simulaba un
  usuario con más de un día de uso: la ventana aparece sola en la apertura
  que corresponde, sin que nadie toque nada. El build temporal se revirtió
  antes de publicar.
- Pendiente: el avatar es un marcador de posición. La imagen adjunta en el
  chat no queda guardada ni en disco ni en el almacenamiento de la sesión, así
  que hace falta el archivo original para ponerla.
- Sin commit, sin push y sin bump de versión.

### L9 — Release 1.31.1 publicado (2026-11-12)

Pedido del usuario:

> "ok ahora cambia la version de la aplicacion por la 1.31.1 y en los ajustes
> tambien y sube el release si todo esta bien para que los demas ya actualicen"

> "pero cambia en el codigo la version de la aplicacion tambien"

Publicación:

- `versionCode` 1019015 → 1019016 y `versionName` "1.31.0" → "1.31.1" en
  `app/build.gradle.kts`, el único lugar donde se escribe la versión. Ajustes
  la lee de `BuildConfig.VERSION_NAME`, así que "en los ajustes también"
  sale solo.
- Los previews del diálogo de actualización se dejaron en 1.31.1 para que
  siguan mostrando una actualización coherente y no la versión instalada.
- Commits: `7ab691c` con todo el trabajo de la feature y `5ff564c` con el
  bump, siguiendo la convención del repo (el tag va sobre el commit de bump).
- Tag `v1.31.1` sobre `5ff564c`, rama `stabilize-release-readiness` pusheada.
- Release "Dolita 1.31.1" con el APK adjunto como `Dolita-1.31.1.apk`.

Verificación:

- La firma del APK es la misma que la de la 1.31.0 (certificado SHA-256
  `2c460f11…efe7c`): se instala encima sin desinstalar ni perder datos.
- El release publicado expone `tag_name`, asset y `digest`
  (`sha256:ea977e91…22bb945b47`), que es exactamente lo que el actualizador
  necesita para habilitar la descarga dentro de la app.
- Probado en un teléfono ajeno: la actualización se descargó y se instaló
  desde el propio diálogo.
- Suite unitaria 427 tests, 0 fallos.
- APK final 49.563.286 bytes, SHA-256
  `ea977e91e997afbac6a233de6f963e00ee74f9fa4c23ed94e332af22bb945b47`.
- El avatar real quedó en `app/src/main/res/drawable/creator_avatar.png`
  (copia de `Escritorio/Nueva carpeta/imagen del perfil.png`).

