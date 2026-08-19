# ERP — Dolar

Aplicación Android en Kotlin + Jetpack Compose que muestra la cotización del dólar y el euro publicadas por el BCV (a través de rates.dolarvzla.com) y del USDT P2P (a través de Binance), con calculadora de conversión a bolívares y gráfico de evolución. El nombre visible de la app es **Dolar** (`app_name`), aunque el proyecto y el módulo se llaman `ERP`.

<!-- Ruta de screenshots -->
<!-- Agregar aquí capturas de pantalla, por ejemplo:
![Pantalla principal](docs/screenshots/home.png)
![Gráfico de evolución](docs/screenshots/evolution.png)
-->

## Características

- [x] Cotización de tres fuentes: Dólar (BCV), Euro (BCV) y USDT (P2P Binance).
- [x] Tarjeta destacada con precio promedio, variación porcentual y fecha de actualización.
- [x] Selector de fuente mediante chips (la tarjeta, la calculadora y el gráfico siguen la fuente elegida).
- [x] Calculadora de conversión bolívares ↔ divisa (dos direcciones, con `BigDecimal`).
- [x] Gráfico de evolución: compara con la cotización del día anterior cuando la fuente la provee; para fuentes sin dato anterior (USDT) genera una serie simulada dentro de la app.
- [x] Cuatro temas de color seleccionables desde la barra superior (Dólar Verde, Azul Bancario, Violeta Elegante, Alto Contraste) + dynamic color del sistema en Android 12+.
- [x] Estados de carga y de error con botón de reintento.
- [x] Botón de actualización manual de las cotizaciones.

## Stack tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Kotlin | 2.2.10 |
| UI | Jetpack Compose (Material 3) | Compose BOM 2026.02.01 |
| Arquitectura | MVVM (ViewModel + StateFlow) + Repository | — |
| Red | OkHttp | 4.12.0 |
| Serialización | `org.json` (JSONObject, integrado en Android) | — |
| Lifecycle | `lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose` | 2.6.1 |
| Íconos | `material-icons-extended` | vía BOM |
| Tests unitarios | JUnit 4 | 4.13.2 |
| Tests instrumentados | androidx.test.ext:junit, Espresso, Compose UI test | 1.1.5 / 3.5.1 / vía BOM |
| Build | Android Gradle Plugin | 9.3.1 |
| Gradle | Gradle Wrapper | 9.5.0 |

## Estructura del proyecto

```
ERP/
├── app/
│   ├── build.gradle.kts              # Configuración del módulo (SDKs, dependencias)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Permiso INTERNET, actividad única (MainActivity)
│       │   ├── java/com/example/erp/
│       │   │   ├── MainActivity.kt   # Punto de entrada, setContent, estado del tema
│       │   │   ├── data/
│       │   │   │   ├── DolarData.kt           # Modelos y contrato DolarRepository
│       │   │   │   └── ApiDolarRepository.kt  # OkHttp + parsing (BCV, Binance P2P)
│       │   │   └── ui/
│       │   │       ├── DolarViewModel.kt      # Estado de UI (StateFlow) y lógica
│       │   │       ├── DolarScreen.kt         # Pantalla principal + preview
│       │   │       ├── components/
│       │   │       │   └── EvolutionChart.kt  # Gráfico con Canvas + etiquetas
│       │   │       └── theme/
│       │   │           ├── Theme.kt  # ERPTheme, AppTheme (4 paletas), dynamic color
│       │   │           ├── Color.kt  # Paletas y colores de semáforo (sube/baja)
│       │   │           └── Type.kt   # Tipografías de Material3
│       │   ├── keepRules/rules.keep  # Reglas R8 (vacío, solo comentarios)
│       │   └── res/                  # Recursos (strings, themes.xml, íconos, etc.)
│       ├── test/java/com/example/erp/        # Tests unitarios locales (JVM)
│       └── androidTest/java/com/example/erp/ # Tests instrumentados (dispositivo)
├── gradle/
│   ├── libs.versions.toml            # Catálogo de versiones y dependencias
│   ├── wrapper/                      # Gradle Wrapper 9.5.0
│   └── gradle-daemon-jvm.properties  # Toolchain JDK del daemon (25)
├── build.gradle.kts                  # Plugins raíz (apply false)
├── settings.gradle.kts               # Repositorios y módulo :app
├── gradle.properties                 # JVM args, configuration cache, código estilo "official"
├── gradlew / gradlew.bat             # Scripts del wrapper
└── AGENTS.md                         # Convenciones para agentes/colaboradores
```

## Requisitos

| Requisito | Detalle |
|---|---|
| Android Studio | Versión reciente con soporte para AGP 9.x. Android Studio muestra un aviso al abrir el proyecto si la versión instalada no soporta el AGP 9.3.1 configurado. |
| JDK | 17 o superior para ejecutar Gradle 9.5. El proyecto configura el daemon con toolchain JDK 25 (`gradle-daemon-jvm.properties`) y lo auto-provisiona mediante el resolver de foojay si no está instalado. |
| SDK de Android | `minSdk 30` (Android 11), `compileSdk 37`, `targetSdk 37`. |
| Dispositivo/emulador | Android 11+ (API 30+) para ejecutar los tests instrumentados y probar la app. |

## Puesta en marcha

1. Clonar el repositorio:
   ```bash
   git clone <url-del-repositorio>
   ```
2. Abrir la carpeta del proyecto en Android Studio y esperar a que Gradle sincronice (el wrapper descarga Gradle 9.5.0 automáticamente).
3. Verificar que exista `local.properties` con la ruta del SDK de Android. **Este archivo es local y NO se versiona** (está en `.gitignore`); Android Studio lo genera al abrir el proyecto.
4. Compilar el APK de depuración:
   ```bash
   ./gradlew assembleDebug
   ```
   En Windows se puede usar `gradlew.bat` en lugar de `./gradlew`.
5. Instalar el APK (`app/build/outputs/apk/debug/app-debug.apk`) o ejecutar la configuración de `app` desde Android Studio.

## Comandos de build y test

| Comando | Qué hace | Cuándo usarlo |
|---|---|---|
| `./gradlew assembleDebug` | Compila el APK de depuración. | Verificar que el proyecto compila. |
| `./gradlew testDebugUnitTest` | Ejecuta los tests unitarios locales (JVM del host), fuente `app/src/test/`. | Antes de cada commit. |
| `./gradlew connectedDebugAndroidTest` | Ejecuta los tests instrumentados en un dispositivo/emulador conectado, fuente `app/src/androidTest/`. | Para validar comportamiento en dispositivo. |

**Por qué no usar `./gradlew test`:** la tarea genérica `test` ejecuta los unit tests de *todas* las variantes de build (debug, release, etc.), lo que es más lento y puede fallar por motivos ajenos a tu cambio (por ejemplo, la variante release con ofuscación). Los tests instrumentados no se ejecutan con `test`: requieren un dispositivo conectado y su propia tarea `connectedDebugAndroidTest`. La convención del proyecto es usar siempre la tarea específica por source set.

## Cómo contribuir

1. Crear una rama con nombre descriptivo desde `master` (por ejemplo, `feature/calculadora` o `fix/error-red`).
2. Hacer cambios mínimos y enfocados, con mensajes de commit convencionales (`feat:`, `fix:`, `docs:`, `refactor:`, ...).
3. Ejecutar `./gradlew testDebugUnitTest` y `./gradlew assembleDebug` antes de abrir el pull request.
4. Abrir un pull request contra `master`. El repositorio no tiene CI configurado ni plantilla de PR: la revisión se hace manualmente.

## Licencia

Sin licencia definida. El repositorio no incluye un archivo `LICENSE`; por defecto se aplican las restricciones del derecho de autor y no se concede permiso explícito de uso, copia o distribución.