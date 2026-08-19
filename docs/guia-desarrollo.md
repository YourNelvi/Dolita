# Guía de desarrollo — ERP (Dolar)

Guía para desarrolladores que se incorporan al proyecto. Primero se explica el camino feliz de la app (cómo fluyen los datos de principio a fin), y después cada capa en detalle, con convenciones, testing y errores comunes.

---

## 1. Mapa general de la app

La app se llama **Dolar** (`app_name`), el proyecto y el módulo se llaman **ERP** (`settings.gradle.kts`, `rootProject.name = "ERP"`). Es una app de una sola pantalla: consulta cotizaciones de dólar/euro (BCV) y USDT P2P (Binance), permite convertir bolívares ↔ divisa y muestra un gráfico de evolución.

Puntos de partida para orientarse:

| Archivo | Rol |
|---|---|
| `app/src/main/java/com/example/erp/MainActivity.kt` | Actividad única, `setContent`, estado del tema |
| `app/src/main/java/com/example/erp/ui/DolarScreen.kt` | Toda la UI de la pantalla principal |
| `app/src/main/java/com/example/erp/ui/DolarViewModel.kt` | Estado de UI y lógica de carga |
| `app/src/main/java/com/example/erp/data/DolarData.kt` | Modelos (`DolarQuote`, `PricePoint`) y contrato `DolarRepository` |
| `app/src/main/java/com/example/erp/data/ApiDolarRepository.kt` | Implementación real: OkHttp + parsing JSON |
| `app/src/main/java/com/example/erp/ui/components/EvolutionChart.kt` | Gráfico dibujado con `Canvas` |
| `app/src/main/java/com/example/erp/ui/theme/Theme.kt` | `ERPTheme`, paletas `AppTheme`, dynamic color |

---

## 2. Arquitectura: MVVM + Repository

La app sigue el patrón **MVVM** (Model-View-ViewModel) con una capa **Repository** para el acceso a datos:

```
┌─────────────────┐
│   MainActivity  │  Actividad única: setContent { ERPTheme { DolarScreen() } }
└────────┬────────┘
         │ Compose recomponiendo según el estado
┌────────▼────────┐   observe (collectAsState)   ┌──────────────────────┐
│   DolarScreen   │ ◄─────────────────────────── │   DolarViewModel     │
│  (View, UI)     │                              │  (ViewModel +        │
└────────┬────────┘                              │   StateFlow<UiState>)│
         │ onSelectCasa / onRefresh              └──────────┬───────────┘
         │                                                  │ viewModelScope.launch
         │                                                  ▼
         │                              ┌───────────────────────────────┐
         │                              │   DolarRepository (interfaz)   │
         │                              │   getQuotes() / getHistorial() │
         │                              └───────────────┬───────────────┘
         │                                              │ implementa
         │                                              ▼
         │                        ┌──────────────────────────────────────┐
         └──────────────────────► │   ApiDolarRepository                 │
                                  │   OkHttp → rates.dolarvzla.com (BCV) │
                                  │           → p2p.binance.com (P2P)    │
                                  └──────────────────────────────────────┘
```

**Flujo del camino feliz** (lo que pasa al abrir la app):

1. `MainActivity.onCreate` llama a `enableEdgeToEdge()` y a `setContent { ... }`, envolviendo la pantalla en `ERPTheme(theme = currentTheme)`.
2. `DolarScreen` obtiene el `DolarViewModel` (por defecto con `viewModel()`) y se suscribe al estado con `collectAsState()`.
3. El `DolarViewModel` arranca `load()` en su `init`: lanza una corrutina en `viewModelScope`, pone `loading = true` y llama a `repository.getQuotes()`.
4. `ApiDolarRepository` hace dos llamadas HTTP con **OkHttp** (una a BCV, una a Binance P2P), parsea las respuestas con **`org.json`** (`JSONObject`) y devuelve `List<DolarQuote>`.
5. El ViewModel actualiza el `MutableStateFlow<DolarUiState>`; Compose detecta el cambio y recompone `DolarScreen`.
6. La UI muestra la tarjeta destacada, los chips de fuente, la calculadora, el gráfico y la lista de cotizaciones.

La dirección de las dependencias es siempre hacia adentro: la UI conoce al ViewModel, el ViewModel conoce a la interfaz `DolarRepository`, y la implementación concreta (`ApiDolarRepository`) solo se instancia por defecto en el constructor del ViewModel. No hay inyección de dependencias (sin Hilt/Koin).

---

## 3. Las capas en detalle

### 3.1. Capa de datos — `data/DolarData.kt`

Define los modelos y el contrato:

- `DolarQuote(fuente, nombre, promedio, anterior, variacion, fechaActualizacion)` — una cotización. `fuente` es la clave (`"usd"`, `"eur"`, `"usdt"`) y se usa para seleccionar la fuente activa.
- `PricePoint(precio, hora)` — un punto del gráfico de evolución.
- `interface DolarRepository` — contrato con dos operaciones `suspend`:
  - `getQuotes(): List<DolarQuote>`
  - `getHistorial(quote: DolarQuote): List<PricePoint>`
- `object DolarSimulation` — genera una serie simulada de 12 puntos (drift + onda senoidal) cuando una fuente no tiene dato anterior real. **Es simulación, no un endpoint histórico.**

### 3.2. Capa de datos — `data/ApiDolarRepository.kt`

Implementación real de `DolarRepository`. Detalles importantes:

- Cliente `OkHttpClient` compartido a nivel de archivo con timeouts de 15 s (conexión y lectura).
- `getQuotes()` ejecuta la llamada al BCV dentro de `withContext(Dispatchers.IO)`; la llamada a Binance está envuelta en `try/catch`: si falla, se registra con `Log.w(TAG, ...)` y se omite (la app funciona solo con BCV).
- **BCV**: GET a `https://rates.dolarvzla.com/bcv/current.json`. Del JSON se leen los objetos `current`, `previous` y `changePercentage`, y se construyen dos `DolarQuote`: `usd` ("Dólar (BCV)") y `eur` ("Euro (BCV)").
- **Binance P2P**: POST a `https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search` con un cuerpo JSON fijo (activo `USDT`, fiat `VES`, `tradeType: "BUY"`). El precio promedio se calcula promediando los `adv.price` de las ofertas. También envía un `User-Agent` de navegador.
- Errores: si el código HTTP no es 200 se lanza `IOException`. El ViewModel lo captura con `runCatching` y lo expone en `uiState.error`.
- `getHistorial()`: si la cotización tiene `anterior`, devuelve dos puntos reales ("Ayer"/"Hoy"); si no (caso USDT), devuelve `DolarSimulation.historial(promedio)`.

**Nota sobre serialización**: el proyecto NO usa Retrofit ni kotlinx.serialization. Las respuestas se parsean a mano con `org.json` (incluido en Android). Mantener esa convención al agregar endpoints nuevos.

### 3.3. Capa de UI — `ui/DolarViewModel.kt`

- `DolarUiState` (data class) agrupa todo el estado de la pantalla: `quotes`, `selectedFuente`, `historial`, `loading`, `error`.
- El estado se expone como `StateFlow<DolarUiState>` inmutable (`asStateFlow()`); las mutaciones pasan por `MutableStateFlow.update`.
- `load()`: pone `loading = true`, llama a `repository.getQuotes()` con `runCatching`, y en éxito elige la fuente seleccionada (o la primera disponible) y carga su `historial`.
- `select(fuente)`: cambia la fuente activa y recarga el historial. Si la fuente es la misma, no hace nada.
- El constructor acepta un `DolarRepository` con `ApiDolarRepository()` por defecto: eso permite testear con un repository falso.

### 3.4. Capa de UI — `ui/DolarScreen.kt`

La pantalla completa. Estructura de composables:

- `DolarScreen(uiState)` — puente entre ViewModel y contenido; obtiene el estado con `collectAsState()` y delega.
- `DolarScreenContent(...)` — `Scaffold` con `CenterAlignedTopAppBar` (título "Dolar", botón de actualizar, menú de temas) y un `when` de tres estados: cargando (`CircularProgressIndicator`), error (`ErrorState` con reintento), o contenido.
- `DolarContent(...)` — `LazyColumn` con: `FeaturedCard` (tarjeta destacada con gradiente), `CasaChips` (chips de fuente), `CalculatorCard` (conversión bidireccional con `BigDecimal`), `EvolutionCard` (gráfico + etiquetas), lista `QuoteRow` de cotizaciones, y el pie "Precios: rates.dolarvzla.com (BCV)".
- Formateadores: precios con 2 decimales (`$773,31`), cálculo con hasta 4, fechas con `LocalDate`/`OffsetDateTime` y `DateTimeFormatter`.
- Al final del archivo hay un `@Preview` (`DolarScreenPreview`) con datos de ejemplo (`previewQuotes()`).

### 3.5. Capa de UI — `ui/components/EvolutionChart.kt`

- `EvolutionChart(values, lineColor, modifier)` — dibuja con `Canvas`: línea con `Stroke` redondeado, relleno con gradiente vertical y un círculo en el último punto.
- `ChartLabels(values)` — fila con los valores mínimo y máximo ("Mín ..." / "Máx ...").
- Componentes reutilizables: si un componente se usa en más de una pantalla, vive en `ui/components/`.

### 3.6. Tema — `ui/theme/`

**`Theme.kt`** — el corazón del theming:

- `enum class AppTheme(lightPrimary, lightSecondary, lightTertiary, darkPrimary, darkSecondary, darkTertiary, displayName)` define **cuatro paletas**: `DOLAR_VERDE`, `AZUL_BANCARIO`, `VIOLETA_ELEGANTE`, `ALTO_CONTRASTE`. `displayName` es lo que se muestra en el menú de temas.
- `ERPTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = true, theme = AppTheme.DOLAR_VERDE, content)` elige el esquema así:
  1. Si `dynamicColor && SDK >= S` (Android 12+): usa `dynamicDarkColorScheme` / `dynamicLightColorScheme` del sistema.
  2. Si no, y `darkTheme`: `darkScheme(theme)` (colores oscuros de la paleta).
  3. Si no: `lightScheme(theme)` (colores claros de la paleta).
- **Gotcha importante**: `MainActivity` llama `ERPTheme(theme = currentTheme)` sin tocar `dynamicColor`, que por defecto es `true`. En dispositivos Android 12+ el dynamic color del sistema **gana** sobre la paleta elegida: el menú de temas cambia `currentTheme`, pero el color visible no cambia en esos dispositivos. Para ver las paletas propias hay que probar en Android 11 o pasar `dynamicColor = false`.

**`Color.kt`** — paletas como `Color(...)` (verde, azul, violeta, alto contraste) y los colores de semáforo usados en la UI: `UpGreenLight`/`UpGreenDark`, `DownRedLight`/`DownRedDark` (sube/baja de la variación).

**`Type.kt`** — tipografía. Define cinco estilos: `bodyLarge`, `displayLarge`, `headlineMedium`, `titleMedium`, `labelMedium`. Si se necesita un estilo nuevo, se define aquí para que quede disponible vía `MaterialTheme.typography` (no hay que agregar estilos sueltos en cada composable).

**Cadena del theming — no romperla**: los composables deben leer los colores de `MaterialTheme.colorScheme` (y `isSystemInDarkTheme()` para los semáforos). No hardcodear colores de las paletas dentro de las pantallas: así el dark mode, el dynamic color y las cuatro paletas siguen funcionando.

### 3.7. Punto de entrada — `MainActivity.kt`

- `enableEdgeToEdge()` y `setContent { ... }`.
- Guarda el tema elegido en estado de Compose: `var currentTheme by remember { mutableStateOf(AppTheme.DOLAR_VERDE) }`.
- Pasa `onThemeChange = { currentTheme = it }` a `DolarScreen`; el menú de la barra superior lo invoca.

---

## 4. Convenciones del proyecto

| Convención | Detalle |
|---|---|
| Paquete | Todo el código vive bajo `com.example.erp` (los tres source sets: `main`, `test`, `androidTest`). |
| Capas | `data/` para modelos y repositorios; `ui/` para ViewModels y pantallas; `ui/components/` para composables reutilizables; `ui/theme/` para tema. |
| Dependencias | Todas las versiones se declaran en `gradle/libs.versions.toml` y se referencian con `libs.*`. No hardcodear versiones en `app/build.gradle.kts`. |
| Estilo de código | `kotlin.code.style=official` (en `gradle.properties`). |
| `local.properties` | Nunca se versiona (está en `.gitignore`); es local de cada máquina (ruta del SDK). |
| keepRules | Las reglas R8 van en `app/src/main/keepRules/rules.keep` (hoy solo comentarios). Si se agrega interop nativo o interfaces JS de WebView, las reglas van ahí. |
| Strings | Los textos de UI se definen en `app/src/main/res/values/strings.xml` (`app_name` = "Dolar"). |
| Commits | Conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`, etc. Sin atribución de IA ni "Co-Authored-By". |

---

## 5. Cómo agregar una pantalla nueva

Pasos concretos, siguiendo el patrón existente:

1. **Modelo de estado**: crear una data class `MiPantallaUiState` (en `ui/`) con los campos que la pantalla necesita y valores por defecto.
2. **ViewModel**: crear `MiPantallaViewModel` en `ui/` extendiendo `ViewModel`, con `MutableStateFlow` privado expuesto como `StateFlow` inmutable, y toda la lógica en funciones que se llaman desde la UI (nunca lógica en el composable).
3. **Pantalla**: crear `MiPantalla.kt` en `ui/` con un composable público que reciba el estado y callbacks (`onXxx`), más un `@Preview` al final del mismo archivo.
4. **Colores**: usar `MaterialTheme.colorScheme.*` y `isSystemInDarkTheme()`; no colores hardcodeados.
5. **Conexión**: si la pantalla es la principal, exponerla desde `MainActivity` con `setContent`. El proyecto **no tiene librería de navegación** todavía (es una app de una sola pantalla): si se agrega navegación, evaluar Navigation Compose o un estado de ruta en el ViewModel, y documentar la decisión.
6. **Verificar**: `./gradlew testDebugUnitTest` y `./gradlew assembleDebug`.

No mover un composable sin su `@Preview`: las previews son privadas y referencian composables privados del mismo archivo; separarlos rompe la compilación.

---

## 6. Cómo agregar un endpoint o modelo nuevo en la capa de datos

1. **Modelo**: agregar la `data class` en `data/DolarData.kt` (o un archivo nuevo en `data/`).
2. **Contrato**: agregar la operación `suspend` a la interfaz `DolarRepository`.
3. **Implementación**: en `ApiDolarRepository.kt` usar `OkHttpClient` (métodos auxiliares `buildGet`/POST), parsear con `org.json` y lanzar `IOException` si el código HTTP no es 200. Loguear con el `TAG` existente.
4. **Resiliencia**: seguir el patrón actual — si la fuente nueva puede fallar, envolverla en `try/catch` dentro de `getQuotes()` y devolver `null` para no romper el resto (como hace `fetchUsdt()`).
5. **ViewModel**: consumir la fuente nueva y exponerla en el `UiState`; la UI la muestra automáticamente en chips y lista si aparece en `quotes`.
6. **Historial**: si el endpoint nuevo no provee `anterior`, `getHistorial()` caerá en `DolarSimulation`. Si la fuente tiene dato anterior real, se muestran los puntos reales. No inventar series históricas fuera de estos dos caminos.

---

## 7. Testing

| Tarea | Qué corre | Dónde viven los tests | Requisito |
|---|---|---|---|
| `./gradlew testDebugUnitTest` | Tests unitarios locales (JVM del host) | `app/src/test/java/com/example/erp/` | Ninguno (solo JVM) |
| `./gradlew connectedDebugAndroidTest` | Tests instrumentados (en dispositivo/emulador) | `app/src/androidTest/java/com/example/erp/` | Emulador/dispositivo Android 11+ conectado |

**Cobertura actual**: los tests existentes son plantillas de ejemplo, no cubren lógica de negocio:

- `ExampleUnitTest` verifica una suma trivial (`2 + 2 == 4`).
- `ExampleInstrumentedTest` verifica que el package name de la app sea `com.example.erp`.

No hay infraestructura de testing más allá de estas plantillas (sin MockWebServer, sin FakeRepository, sin TDD estricto). El siguiente paso natural es testear el parsing de `ApiDolarRepository` (con respuestas JSON fijas) y el `DolarViewModel` (inyectando un repository falso por el constructor).

**Por qué no `./gradlew test`**: esa tarea ejecuta los unit tests de todas las variantes (debug, release, ...), es más lenta y puede fallar por motivos ajenos al cambio. La convención del proyecto es la tarea por source set (`testDebugUnitTest`). Los instrumentados requieren la tarea `connectedDebugAndroidTest` y un dispositivo conectado; `test` no los ejecuta.

---

## 8. Errores comunes que evitar

| Error | Por qué evitarlo | Correcto |
|---|---|---|
| `./gradlew test` para correr los unit tests | Corre unit tests de todas las variantes; más lento y con fallos ajenos al cambio | `./gradlew testDebugUnitTest` |
| `./gradlew connectedAndroidTest` | La tarea no existe con ese nombre | `./gradlew connectedDebugAndroidTest` |
| Editar `local.properties` o commitearlo | Es local de cada máquina y está en `.gitignore` | Dejarlo como lo genera Android Studio |
| Hardcodear colores de paleta en un composable | Rompe dark mode, dynamic color y el selector de temas | `MaterialTheme.colorScheme.*` |
| Probar los 4 temas en Android 12+ y "no ver nada" | No es un bug: con `dynamicColor = true` (default) el sistema manda en Android 12+ | Probar en Android 11 o usar `dynamicColor = false` |
| Mover un composable sin su `@Preview` | Las previews privadas referencian composables privados del mismo archivo; rompe la compilación | Mantener preview y composable juntos |
| Agregar un estilo tipográfico fuera de `Type.kt` | No queda expuesto en `MaterialTheme.typography` | Definirlo en `Type.kt` |
| Usar Retrofit/kotlinx.serialization en un endpoint nuevo | El proyecto parsea con `org.json` y usa solo OkHttp | Seguir la convención existente |
| Tratar el historial de USDT como dato real | Para fuentes sin `anterior`, `getHistorial()` devuelve `DolarSimulation` (simulado) | Conocer la diferencia: BCV real (Ayer/Hoy), USDT simulado |
| Hardcodear versiones en `app/build.gradle.kts` | El proyecto centraliza versiones en el catálogo | Declarar en `gradle/libs.versions.toml` y usar `libs.*` |

---

## 9. Checklist antes de hacer commit

- [ ] `./gradlew testDebugUnitTest` pasa.
- [ ] `./gradlew assembleDebug` compila sin errores.
- [ ] No hay `local.properties` ni credenciales en los archivos staged.
- [ ] Cambios mínimos y enfocados en un solo propósito.
- [ ] Mensaje de commit convencional (`feat:`, `fix:`, `docs:`, `refactor:`, ...).
- [ ] Sin atribución de IA ni "Co-Authored-By" en el mensaje.
- [ ] Si se agregaron strings, están en `app/src/main/res/values/strings.xml`.
- [ ] Si se agregaron reglas R8, están en `app/src/main/keepRules/rules.keep`.

---

## 10. Fuentes de verdad

- Los archivos de build (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`) mandan sobre cualquier prosa: si un documento contradice el build, confiar en el build.
- `AGENTS.md` en la raíz resume convenciones para agentes, pero puede quedar desactualizado (por ejemplo, su descripción del theming no refleja las cuatro paletas de `AppTheme` ni los cinco estilos de `Type.kt`). Verificar siempre contra el código.