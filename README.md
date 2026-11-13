# Dolita

Aplicación Android en Kotlin + Jetpack Compose para consultar las cotizaciones del dólar y el euro del BCV y del USDT P2P de Binance, con calculadora, histórico, alertas y widget. El paquete y el proyecto Gradle conservan el nombre `ERP`; el nombre visible de la app es **Dolita**.

<!-- Capturas: pendiente de agregar la galería. -->

## Características

- [x] Cotizaciones de tres fuentes: Dólar (BCV), Euro (BCV) y USDT (P2P Binance).
- [x] Tarjeta destacada con precio, variación porcentual y fecha de actualización.
- [x] Selector de fuente: la tarjeta, la calculadora y el gráfico siguen la fuente elegida.
- [x] Diferencias entre monedas: cada tasa se compara contra la seleccionada, en bolívares.
- [x] Calculadora bidireccional bolívares ↔ divisa, con `BigDecimal`, y consulta de la tasa de una fecha (últimos 3 meses).
- [x] Histórico con las muestras que guarda la app; para BCV se apoya en la cotización del día anterior.
- [x] Aviso de la próxima tasa BCV y alertas por umbral con notificaciones: un aviso por cruce, no cada hora.
- [x] Widget de la tasa con minigráfico.
- [x] Tarjeta para compartir la cotización como imagen o texto.
- [x] Cuatro paletas (Dólar Verde, Azul Bancario, Violeta Elegante, Alto Contraste), modo sistema/claro/oscuro, color dinámico en Android 12+ y negro puro opcional para AMOLED.
- [x] Actualización desde la app contra GitHub Releases: descarga el APK y verifica el SHA-256 antes de instalar. Frecuencia diaria, semanal o manual.
- [x] Estados de carga y error con reintento, y actualización manual de cotizaciones.

## Instalación

1. Descargá el APK desde [Releases](https://github.com/YourNelvi/Dolita/releases/latest).
2. Habilitá la instalación de apps desconocidas para el navegador o el gestor de archivos.
3. Instalá el APK. Las versiones publicadas comparten la misma firma, así que se actualizan encima sin desinstalar.

Con la app abierta, **Ajustes → Actualizaciones** permite buscar una versión nueva en el momento.

> No requiere cuenta ni registro.

### El aviso de Play Protect

Dolita no está en la Play Store: se distribuye desde los releases de GitHub. Por eso
**Play Protect escanea el APK y puede avisar que "la app puede ser dañina" o que no la
reconoce**. Ese aviso no dice que el APK esté mal: dice que la app no viene de una fuente
que Google conoce. El APK se puede verificar: el release publica su SHA-256 y la app
comprueba ese valor antes de instalar.

Para instalarla, elegí **"Instalar de todos modos"** (o "Detalles → Instalar de todos
modos"). Y recordá que la firma es la misma desde la 1.22.1, así que una actualización
nunca pide desinstalar: no se borran ni los perfiles de pago ni las preferencias.

## Stack tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Kotlin | 2.2.10 |
| UI | Jetpack Compose (Material 3) | Compose BOM 2026.02.01 |
| Arquitectura | MVVM (ViewModel + StateFlow) + Repository | — |
| Red | OkHttp | 4.12.0 |
| Serialización | `org.json` (JSONObject, integrado en Android) | — |
| Segundo plano | WorkManager | 2.9.0 |
| Lifecycle | `lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose` | 2.6.1 |
| Íconos | `material-icons-extended` | vía BOM |
| Tests unitarios | JUnit 4 | 4.13.2 |
| Tests instrumentados | androidx.test.ext:junit, Espresso, Compose UI test | 1.1.5 / 3.5.1 / vía BOM |
| Build | Android Gradle Plugin | 9.3.1 |
| Gradle | Gradle Wrapper | 9.5.0 |

## Fuentes de datos

- **BCV (dólar y euro):** `https://rates.dolarvzla.com/bcv/current.json`, un servicio de terceros que publica las tasas oficiales.
- **USDT (P2P):** endpoint público de Binance P2P; se promedian las primeras ofertas de compra.
- Si Binance falla, la app sigue mostrando BCV. Si falla BCV, muestra el error con botón de reintento.

## Estructura del proyecto

```
ERP/
├── app/
│   ├── build.gradle.kts              # Configuración del módulo (SDKs, dependencias)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/erp/
│       │   │   ├── MainActivity.kt   # Punto de entrada y estado del tema
│       │   │   ├── data/             # Repositorios, caché, histórico, alertas y actualización
│       │   │   ├── notification/     # Canales y notificaciones
│       │   │   ├── widget/           # Widget de la tasa
│       │   │   └── ui/               # Pantallas, componentes y tema
│       │   ├── keepRules/rules.keep  # Reglas R8 (vacío, solo comentarios)
│       │   └── res/                  # Recursos
│       ├── test/                     # Tests unitarios (JVM)
│       └── androidTest/              # Tests instrumentados
├── docs/guia-desarrollo.md           # Guía de desarrollo
├── gradle/                           # Catálogo de versiones y wrapper
├── odd/tasks/                        # Documentos de features
├── build.gradle.kts
├── settings.gradle.kts
└── AGENTS.md                         # Convenciones para agentes/colaboradores
```

## Requisitos

| Requisito | Detalle |
|---|---|
| Android Studio | Versión reciente con soporte para AGP 9.x |
| JDK | 17 o superior para ejecutar Gradle 9.5. El proyecto configura el daemon con toolchain JDK 25 (`gradle-daemon-jvm.properties`) y lo auto-provisiona con foojay si no está instalado |
| SDK de Android | `minSdk 30` (Android 11), `compileSdk 37`, `targetSdk 37` |
| Dispositivo/emulador | Android 11+ (API 30+) |

## Puesta en marcha

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/YourNelvi/Dolita.git
   ```
2. Abrir la carpeta en Android Studio y esperar a que Gradle sincronice.
3. Verificar que exista `local.properties` con la ruta del SDK. Es local y no se versiona.
4. Compilar el APK de depuración:
   ```bash
   ./gradlew assembleDebug
   ```

## Comandos de build y test

| Comando | Qué hace |
|---|---|
| `./gradlew assembleDebug` | Compila el APK de depuración |
| `./gradlew testDebugUnitTest` | Ejecuta los tests unitarios locales (JVM) |
| `./gradlew connectedDebugAndroidTest` | Ejecuta los tests instrumentados en un dispositivo/emulador |
| `./gradlew assembleRelease` | Compila el APK de release (requiere las credenciales de firma) |

**Por qué no usar `./gradlew test`:** la tarea genérica ejecuta los unit tests de todas las variantes (más lento y con fallos ajenos al cambio). La convención del proyecto es usar la tarea específica por source set.

## Desarrollo

La guía ampliada está en [`docs/guia-desarrollo.md`](docs/guia-desarrollo.md). Para reportar un problema, abrí un [issue](https://github.com/YourNelvi/Dolita/issues).

## Aviso

Las cotizaciones son referenciales y pueden diferir de las tasas oficiales. La app no constituye asesoría financiera.

## Licencia

Software propietario. Todos los derechos reservados. Ver [`LICENSE.md`](LICENSE.md).

© 2026 YourNelvi
