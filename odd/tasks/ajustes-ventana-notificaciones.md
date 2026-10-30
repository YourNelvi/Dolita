# Feature: ajustes como ventana y notificaciones por apartado

## Objective
Reemplazar la hoja inferior de Ajustes por una pantalla completa, hacer que toda
la fila de un ajuste lo active (no solo el switch) y que cada notificación abra
el apartado que le corresponde, incluida una notificación de la tasa del euro.

## Specs

### S1 — La fila entera activa el ajuste
> "La pestañas está bullada ya que pulsas encima de un ajuste y no se activa
> simplemente se queda parpadeando en gris y si se activo cuando únicamente
> pulsas el switch que está ahí"

- Tocar cualquier punto de la fila de un ajuste lo activa o desactiva.
- No queda ripple/parpadeo gris sin acción.
- Cubre: Color dinámico, Negro puro (AMOLED), Alta precisión y "Avisarme cuando
  cruce un precio".
- Checks: tocar el texto de cada fila cambia el switch; tocar el switch sigue
  cambiándolo una sola vez.

### S2 — Ajustes se abre como ventana
> "sigue bugueado el scroll para salirse yo creo que va a tener que colocarse
> como si fuese una ventana"

- Ajustes se muestra como pantalla completa opaca sobre la app, sin hoja
  arrastrable ni scrim.
- Se cierra con la flecha de la barra superior y con el botón físico Atrás.
- El contenido no compite con ningún gesto de arrastre: la única forma de salir
  es volver.
- Checks: abrir desde el icono Tune, scroll interno hasta el final, volver.

### S3 — Cada notificación abre su apartado
> "Que dependiendo de la notificación en la que me meta se meta al apartado
> correspondiente"

- Tocar una notificación abre la app en la pestaña de la tasa que ese aviso
  comunica: dólar → BCV, euro → Euro, USDT → Paralelo, alerta → la fuente
  elegida.
- Implementado con un extra en el intent (`NotificationHelper.EXTRA_RATE_SOURCE`)
  y un `PendingIntent` con request code propio por tasa; compartir el código
  hacía que todas abrieran la misma pestaña, porque la identidad del
  `PendingIntent` ignora los extras.
- Checks: `am start` con `NEW_TASK|CLEAR_TASK` y extra `eur` abre en Euro;
  sin extra abre en Dólar.

### S4 — Notificación de la tasa del euro
> "Agrega notificación para la tasa de euro"

- Existe un aviso propio del euro ("Euro BCV hoy"), con su propio registro en
  `NotificationState` y su propio id.
- El aviso diario del dólar ya agregaba "EUR" cuando la tasa llegaba, pero su
  compuerta depende del dólar: un euro que se movía solo nunca avisaba.
- Checks: compila y la ruta es la misma del aviso diario; no se pudo forzar el
  worker en el dispositivo durante la verificación.

## Tasks
- T1 — Settings como pantalla completa (S2). Inline. Hecho.
- T2 — Fila entera activa el ajuste (S1). Inline. Hecho.
- T3 — Deep link de notificación a la pestaña de la tasa (S3). Inline. Hecho.
- T4 — Notificación del euro (S4). Inline. Hecho.

## Log
L1 — Pedido del usuario, verbatim:
> "La pestañas está bullada ya que pulsas encima de un ajuste y no se activa
> simplemente se queda parpadeando en gris y si se activo cuando únicamente
> pulsas el switch que está ahí y sigue bugueado el scroll para salirse yo creo
> que va a tener que colocarse como si fuese una ventana, Que dependiendo de la
> notificación en la que me meta se meta al apartado correspondiente, Agrega
> notificación para la tasa de euro"

L2 — Diagnóstico: las filas de los tres switches no tienen `clickable`; el
"parpadeo gris" lo pintaba el `.clickable { /* consume clicks */ }` del panel en
`DolarScreen.kt`, que se tragaba el toque con un ripple sin acción.

L3 — La hoja se reemplaza por una pantalla completa; se elimina todo el estado de
arrastre (`sheetDragOffset`, `draggable`, `SHEET_*`). La pantalla principal queda
compuesta debajo, así que al volver se conserva el scroll.

L4 — Verificado en el dispositivo (release instalada): Ajustes abre como ventana
con flecha Atrás; tocar el texto de una fila cambia el switch; tocar el switch lo
cambia una sola vez; Atrás y la flecha cierran; APARIENCIA arranca en y=278, sin
contenido comido.

L5 — Las notificaciones abren la pestaña de su tasa: `am start` con
`NEW_TASK|CLEAR_TASK` y extra `eur` abre en Euro; `usdt` abre en USDT; sin extra
queda en Dólar. El camino "app ya abierta sin CLEAR_TASK" no cambia la pestaña;
las notificaciones siempre usan CLEAR_TASK, así que el camino real queda cubierto.

L6 — 298 tests unitarios en verde y `assembleRelease` en verde.
