# Feature: ajustes, spread, historico horario y alertas

## Objective
Cerrar el circulo de la app: que el usuario pueda ajustar el comportamiento desde
un solo lugar, entender la brecha con el oficial, ver el historico del paralelo con
granularidad horaria y recibir un aviso cuando el precio cruza un valor que el
mismo eligio.

## Problem
- El icono de la barra superior abre "temas" cuando en realidad es configuracion.
- El paralelo vive en una pestaña aislada: no se ve la brecha con el oficial, que
  es el numero que en verdad mueve una decision.
- El historico del paralelo se ve en 15 dias, que esconde el movimiento de una
  jornada. No hay selector de rango.
- No existe forma de enterarse de un precio sin abrir la app.
- El widget muestra numeros pelados, sin la forma de la tendencia.

## Scope

### T1 — Boton de configuracion en vez de tema
- [x] Top bar: el icono de paleta pasa a un icono de ajustes.
- [x] La hoja se titula "Ajustes" y agrupa: Apariencia + Preferencias.
- [x] Copy en el idioma de la app, sin jerga.

### T2 — Brecha del paralelo sobre el oficial
- [x] En la vista USDT, un renglón con el porcentaje de brecha contra el BCV.
- [x] Solo cuando hay ambas tasas.
- [x] La brecha NO usa verde/rojo: una brecha mayor no es una "ganancia", es una
      magnitud. El signo lleva la informacion, el acento lleva el enfasis.

### T3 — Descartado por decision del usuario
La grafica por horas del paralelo quedo fuera: el muestreo sigue 08:00-22:00, tal
como se pidio, y el historico de la app no cambia. Sin selector de rango y sin
muestreo nocturno. Motivo: la ventana horaria fue una decision conscious de
consumo, y el grafico de 24h habriarequired traicionarla.

### T4 — Descartado junto con T3
Sin cambios al worker de paralelo: se mantiene la ventana diurna y el muestreo
de una vez por hora dentro de ella.

### T5 — Alerta de precio elegida por el usuario
- [x] En Preferencias: activar, elegir fuente (BCV / Euro / Paralelo), valor y
      sentido (sube de / baja de).
- [x] La evalua el worker que ya descarga ese precio; no hay trabajo programado nuevo.
- [x] Dispara una vez por cruce; se rearma si el precio vuelve.
- [x] Apagada, no se evalua ni se guarda nada.
- [x] 13 tests: cruce, no-repetido, rearme, primera observacion, reloj atrasado.

### T6 — Sparkline en el widget
- [x] Renderizada a bitmap y asignada con `setImageViewBitmap` — `RemoteViews`
      no dibuja, y el metodo correcto no es `setViewBitmap` (no existe en el
      framework; verificado con javap contra android.jar).
- [x] La tendencia viene del historico guardado, no de otra consulta.
- [x] El widget ya es push, asi que la imagen se repinta con los datos.

## Bugs encontrados y corregidos durante el trabajo
- [x] **El tab de USDT desaparecia.** Regresion mia: el worker de BCV escribe
      solo usd+eur, y el camino de cache fresco se lo tomaba como snapshot
      completo y no iba a la red, asi que la app renderizaba 2 de 3 fuentes. Un
      cache reciente que no puede llenar todas las pestañas no es un cache
      utilizable: `RateSchedulePolicy.isSnapshotComplete` con 4 tests.
- [x] **La grafica no seguia al tema.** El trazo usaba verde/rojo semantico.
      Ahora usa el acento; la direccion sigue ahi, en el texto del tooltip.

## Constraints
- R8 sigue apagado. Sin dependencias nuevas.
- La key de firma sigue siendo la legacy: la rotacion esta cerrada por decision.
- El worker es el dueno de la red. Ni la app, ni el widget, ni el overlay consultan.
- Las notificaciones avisan cambios, no cadencias.

## Acceptance
- [ ] Abrir la app sigue en 0 requests.
- [ ] 69 tests existentes en verde + los nuevos de cada politica.
- [ ] La brecha no aparece si falta una de las dos tasas.
- [ ] La alerta no dispara si el usuario la apago, y no vuelve a disparar sin
      que el precio vuelva a cruzar.

## Route
Delegated direct, single writer thread. Work-unit commit per task.

## Progress
- 2026-09-25: documento creado, implementacionstarting.
