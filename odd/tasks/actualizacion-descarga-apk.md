# Feature: descarga e instalación del APK desde la app

Branch: `stabilize-release-readiness`
Delivery: sin commits (el usuario pidió dejar los cambios sin commitear)
Test runner: `./gradlew testDebugUnitTest`

## Objective

Pasar la actualización de Level 1 (avisar y abrir el release en el navegador) a
Level 2: bajar el APK publicado desde el diálogo, verificar su SHA-256 contra el
`digest` que informa GitHub e instalarlo con el instalador del sistema.

## Specs

### S1 — Descargar el APK desde el diálogo
> "ok ya que esta este no se puede hacer que lo descargue directamente de la app?"

- El diálogo de actualización ofrece "Descargar" cuando el release publica un
  asset APK.
- La descarga va a `filesDir/updates/` con el nombre `Dolita-<version>.apk`.
- Mientras baja, el diálogo muestra una barra de progreso y el porcentaje; el
  botón cambia a "Cancelar".
- Cancelar o descartar el diálogo aborta la descarga y borra el archivo parcial.
- Checks: descarga completa de un release publicado, progreso visible, cancelar
  deja `filesDir/updates/` vacío.

### S2 — Verificación SHA-256 obligatoria
> "Sin verificación de hash no lo firmo." (orquestador, aprobado por el usuario)

- El SHA-256 se calcula sobre los mismos bytes que se escriben a disco, en una
  sola pasada.
- Se compara con el `digest` del asset (`sha256:<hex>`) que devuelve la API de
  GitHub.
- Si no coincide, el archivo se borra y se informa el error; nunca se ofrece
  instalar bytes sin verificar.
- Si el release no publica `digest`, la app no descarga: cae a Level 1 y solo
  abre la página del release.
- Checks: digest válido instala; digest alterado borra el archivo y muestra el
  error; release sin digest no muestra "Descargar".

### S3 — Instalación con permiso de apps desconocidas
> "El usuario tiene que habilitar 'Instalar apps desconocidas' para Dolita."

- Se declara `REQUEST_INSTALL_PACKAGES` en el manifest.
- Antes de descargar se comprueba `canRequestPackageInstalls()`.
- Si falta el permiso, aparece un diálogo que explica y ofrece "Abrir ajustes"
  (`ACTION_MANAGE_UNKNOWN_APP_SOURCES`) y "Ya lo habilité" para reintentar.
- La instalación usa `ACTION_VIEW` con `FileProvider` y MIME
  `application/vnd.android.package-archive`.
- Checks: sin permiso no se descarga; con permiso el instalador del sistema se
  abre y actualiza sobre la app instalada.

### S4 — El diálogo informa, no adivina
- Estados: inactivo, descargando, error. Un error se muestra en el cuerpo del
  diálogo y permite reintentar.
- El botón "Ver release" sigue disponible como camino alternativo.
- Checks: error de red se lee en el diálogo; reintentar vuelve a descargar.

### S5 — Level 1 se conserva
- Sin asset APK o sin digest, el diálogo queda como hoy: versión, notas, tamaño
  y "Ver release".
- Checks: release con assets vacíos muestra solo "Ver release".

## Tasks

| ID | Specs | Ruta | Estado |
| --- | --- | --- | --- |
| T1 | S1, S2, S5 | inline | pendiente |
| T2 | S1, S2 | inline | pendiente |
| T3 | S3 | inline | pendiente |
| T4 | S3 | inline | pendiente |
| T5 | S1, S4 | inline | pendiente |
| T6 | S1, S3, S4 | inline | pendiente |
| T7 | S1-S5 | inline | pendiente |

- T1: extender `UpdateChecker` con `apkUrl`, `apkDigest`, `apkName` y
  `downloadable`.
- T2: `ApkDownloader` con stream, SHA-256 en una pasada, progreso y limpieza.
- T3: `ApkInstaller` con comprobación de permiso, intent de instalación y
  apertura de ajustes.
- T4: manifest (`REQUEST_INSTALL_PACKAGES`) y ruta `updates/` del `FileProvider`.
- T5: `UpdateDialog` con estado de descarga, progreso y error, más
  `InstallPermissionDialog`.
- T6: orquestación en `DolarScreen` (descarga, cancelación, reintento tras
  permiso).
- T7: tests JVM de las piezas puras y suite completa.

## Log

### L1 — Pedido original
> "ok ya que esta este no se puede hacer que lo descargue directamente de la app?"

### L2 — Aprobación de alcance
El usuario aceptó "Sí, implementalo": descarga + SHA-256 + instalación, dejando
Level 1 como fallback.

### L3 — Decisiones
- Sin verificación de hash no hay instalación: si GitHub no publica `digest`, el
  diálogo no ofrece descargar.
- El APK vive en `filesDir/updates/`, no en `cacheDir`: una instalación que el
  usuario confirma no debe quedar a merced de que el sistema reclame caché.
- No hay instalación silenciosa posible: Android exige la confirmación del
  instalador y el permiso por app.
- Sin commits: el usuario pidió expresamente dejar los cambios sin commitear.

### L4 — Limpieza del APK descargado
> "y sera que se puede hacer que despues que se instale borre el otro apk como hacemos?"

- No existe un "después de instalar" dentro de la app: cuando Dolita se
  actualiza a sí misma, Android mata el proceso al reemplazar el APK. No queda
  código propio para borrar nada en ese momento.
- La limpieza ocurre en el primer arranque de la versión nueva: un
  `LaunchedEffect` llama a `ApkDownloader.clearPending()` y vacía
  `filesDir/updates/`. También se borra al descartar el diálogo.
- No se borra al abrir el instalador: el instalador todavía tiene que leer el
  archivo; borrarlo antes rompería la instalación.
- Revisión independiente: PASS con advertencias. Se corrigieron dos:
  cancelación inmediata del socket al cancelar el job (antes podía tardar hasta
  el timeout de lectura de 15 s) y nombre único para el archivo `.part`, para
  que un intento cancelado no borre el parcial de uno nuevo.
- Test agregado: `pending files are cleared`.

### L5 — Versión de la próxima release
> "actualiza la version de la app para que sea una proxima actualizacion asi como te habia dicho"

- `versionName` pasa de `1.30.0` a `1.31.0` y `versionCode` de `1019014` a
  `1019015`: el contador estricto sube exactamente 1 por release, como pide
  `AGENTS.md`.
- El texto de la versión en Ajustes sigue saliendo de `BuildConfig.VERSION_NAME`,
  así que no hay ningún otro lugar que editar.
- Sin commit ni tag: el usuario no los pidió.
