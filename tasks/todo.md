# Lista de Tareas: Mejoras Android ERP

## Fase 1: Fundamentos de Testing

### Tarea 1: Crear infraestructura de tests con Fakes y Mocks
**Descripción:** Establecer la base para tests unitarios creando implementaciones falsas (Fakes) de las interfaces principales y configurando el entorno de testing.

**Criterios de aceptación:**
- [ ] Crear `FakeDolarRepository` que implemente `DolarRepository`
- [ ] Crear `FakeRateHistoryStore` que implemente `RateHistoryStore`
- [ ] Crear `FakeThemePreferences` que implemente `ThemePreferences`
- [ ] Configurar Mockito-Kotlin para mocking cuando sea necesario
- [ ] Crear test de ejemplo que use los Fakes

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Los Fakes se pueden instanciar y usar

**Dependencias:** Ninguna
**Archivos tocados:**
- `app/src/test/java/com/example/erp/FakeDolarRepository.kt`
- `app/src/test/java/com/example/erp/FakeRateHistoryStore.kt`
- `app/src/test/java/com/example/erp/FakeThemePreferences.kt`
- `app/build.gradle.kts` (agregar dependencias de testing)

**Alcance:** Medium (3-5 archivos)

### Tarea 2: Tests para ApiDolarRepository (parsing JSON)
**Descripción:** Crear tests unitarios para verificar el parsing correcto de las respuestas JSON de las APIs de BCV y Binance.

**Criterios de aceptación:**
- [ ] Test para parsing de respuesta BCV exitosa
- [ ] Test para parsing de respuesta Binance exitosa
- [ ] Test para manejo de errores HTTP
- [ ] Test para parsing con datos faltantes
- [ ] Test para parsing con datos malformados

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Tests cubren casos exitosos y de error

**Dependencias:** Tarea 1
**Archivos tocados:**
- `app/src/test/java/com/example/erp/ApiDolarRepositoryTest.kt`
- `app/src/test/resources/bcv_response.json` (datos de prueba)
- `app/src/test/resources/binance_response.json` (datos de prueba)

**Alcance:** Small (1-2 archivos)

### Tarea 3: Tests para DolarViewModel
**Descripción:** Crear tests para la lógica del ViewModel, incluyendo carga de datos, selección de fuente, y manejo de estados.

**Criterios de aceptación:**
- [ ] Test para carga exitosa de datos
- [ ] Test para error en carga de datos
- [ ] Test para selección de fuente
- [ ] Test para cambio de tema
- [ ] Test para estados de loading/error/success

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: ViewModel funciona correctamente

**Dependencias:** Tarea 1
**Archivos tocados:**
- `app/src/test/java/com/example/erp/DolarViewModelTest.kt`

**Alcance:** Small (1-2 archivos)

### Tarea 4: Tests para RateHistoryStore
**Descripción:** Crear tests para la persistencia de datos históricos, incluyendo lectura, escritura y atomicidad.

**Criterios de aceptación:**
- [ ] Test para escritura exitosa de samples
- [ ] Test para lectura de datos existentes
- [ ] Test para manejo de archivos corruptos
- [ ] Test para atomicidad de escritura
- [ ] Test para RateSamplingPolicy

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Persistencia funciona correctamente

**Dependencias:** Tarea 1
**Archivos tocados:**
- `app/src/test/java/com/example/erp/RateHistoryStoreTest.kt`
- `app/src/test/java/com/example/erp/RateSamplingPolicyTest.kt`

**Alcance:** Medium (3-5 archivos)

### Tarea 5: Tests para ThemePreferences
**Descripción:** Crear tests para la persistencia de preferencias de tema, incluyendo guardado y carga de configuraciones.

**Criterios de aceptación:**
- [ ] Test para guardado de tema
- [ ] Test para carga de tema
- [ ] Test para cambio de modo (claro/oscuro/sistema)
- [ ] Test para dynamic color
- [ ] Test para valores por defecto

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Preferencias se guardan y cargan

**Dependencias:** Tarea 1
**Archivos tocados:**
- `app/src/test/java/com/example/erp/ThemePreferencesTest.kt`

**Alcance:** Small (1-2 archivos)

## Checkpoint: Fundamentos de Testing
- [ ] Todos los tests pasan
- [ ] Cobertura mínima del 60% en capa de datos
- [ ] Build sin errores
- [ ] Revisión con humano antes de continuar

---

## Fase 2: Inyección de Dependencias

### Tarea 6: Configurar Hilt en el proyecto
**Descripción:** Configurar Hilt para inyección de dependencias en el proyecto Android.

**Criterios de aceptación:**
- [ ] Agregar dependencias de Hilt en build.gradle.kts
- [ ] Configurar HiltAndroidApp en Application class
- [ ] Configurar Hilt en MainActivity
- [ ] Verificar que la app compila con Hilt

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: App se ejecuta sin errores

**Dependencias:** Ninguna
**Archivos tocados:**
- `app/build.gradle.kts`
- `app/src/main/java/com/example/erp/ERPApplication.kt` (nuevo)
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/erp/MainActivity.kt`

**Alcance:** Medium (3-5 archivos)

### Tarea 7: Crear módulos de DI para Repository
**Descripción:** Crear módulos de Hilt para proporcionar instancias de Repository y dependencias relacionadas.

**Criterios de aceptación:**
- [ ] Crear `RepositoryModule` para proporcionar `DolarRepository`
- [ ] Crear `NetworkModule` para proporcionar `OkHttpClient`
- [ ] Crear `StorageModule` para proporcionar `RateHistoryStore`
- [ ] Crear `ThemeModule` para proporcionar `ThemeRepository`

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Dependencias se inyectan correctamente

**Dependencias:** Tarea 6
**Archivos tocados:**
- `app/src/main/java/com/example/erp/di/RepositoryModule.kt` (nuevo)
- `app/src/main/java/com/example/erp/di/NetworkModule.kt` (nuevo)
- `app/src/main/java/com/example/erp/di/StorageModule.kt` (nuevo)
- `app/src/main/java/com/example/erp/di/ThemeModule.kt` (nuevo)

**Alcance:** Medium (3-5 archivos)

### Tarea 8: Refactorizar ViewModel para usar DI
**Descripción:** Modificar el DolarViewModel para recibir dependencias a través de Hilt en lugar de instanciarlas directamente.

**Criterios de aceptación:**
- [ ] ViewModel usa `@Inject constructor`
- [ ] ViewModel recibe `DolarRepository` por DI
- [ ] ViewModel recibe `RateHistoryStore` por DI
- [ ] ViewModel recibe `ThemeRepository` por DI
- [ ] Se elimina la instanciación directa de dependencias

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: ViewModel funciona con DI

**Dependencias:** Tarea 7
**Archivos tocados:**
- `app/src/main/java/com/example/erp/ui/DolarViewModel.kt`

**Alcance:** Small (1-2 archivos)

### Tarea 9: Tests actualizados con DI
**Descripción:** Actualizar los tests existentes para funcionar con el nuevo sistema de DI.

**Criterios de aceptación:**
- [ ] Tests usan `@HiltAndroidTest` cuando sea necesario
- [ ] Tests proporcionan Fakes a través de módulos de test
- [ ] Todos los tests existentes siguen pasando
- [ ] Nuevos tests verifican la inyección correcta

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Tests funcionan con DI

**Dependencias:** Tarea 8
**Archivos tocados:**
- `app/src/test/java/com/example/erp/DolarViewModelTest.kt`
- `app/src/test/java/com/example/erp/di/TestModule.kt` (nuevo)

**Alcance:** Small (1-2 archivos)

## Checkpoint: Inyección de Dependencias
- [ ] Hilt funciona correctamente
- [ ] Tests siguen pasando
- [ ] App compila y ejecuta
- [ ] Revisión con humano antes de continuar

---

## Fase 3: Manejo de Errores Mejorado

### Tarea 10: Definir tipos de error más granulares
**Descripción:** Expandir el sistema de errores para cubrir más casos y ser más específico.

**Criterios de aceptación:**
- [ ] Definir errores específicos por tipo de problema
- [ ] Agregar errores de caché
- [ ] Agregar errores de timeout
- [ ] Agregar errores de autenticación (si aplica)
- [ ] Mantener compatibilidad con errores existentes

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Errores son más específicos

**Dependencias:** Ninguna
**Archivos tocados:**
- `app/src/main/java/com/example/erp/data/Error.kt`

**Alcance:** Small (1-2 archivos)

### Tarea 11: Implementar RetryPolicy
**Descripción:** Crear un sistema de reintento automático para errores transitorios.

**Criterios de aceptación:**
- [ ] Crear `RetryPolicy` con configuración de intentos
- [ ] Implementar retry exponencial para errores de red
- [ ] Configurar número máximo de reintentos
- [ ] Agregar delay entre reintentos

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Retry funciona en errores de red

**Dependencias:** Tarea 10
**Archivos tocados:**
- `app/src/main/java/com/example/erp/data/RetryPolicy.kt` (nuevo)
- `app/src/main/java/com/example/erp/data/ApiDolarRepository.kt`

**Alcance:** Small (1-2 archivos)

### Tarea 12: Crear ErrorHandler global
**Descripción:** Implementar un manejador centralizado de errores que procese y clasifique todos los errores de la app.

**Criterios de aceptación:**
- [ ] Crear `ErrorHandler` como singleton
- [ ] Implementar mapeo de excepciones a errores tipados
- [ ] Agregar logging centralizado de errores
- [ ] Integrar con el sistema de UI existente

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Errores se manejan centralizadamente

**Dependencias:** Tarea 10
**Archivos tocados:**
- `app/src/main/java/com/example/erp/data/ErrorHandler.kt` (nuevo)
- `app/src/main/java/com/example/erp/ui/DolarViewModel.kt`

**Alcance:** Small (1-2 archivos)

### Tarea 13: UI de errores mejorada
**Descripción:** Mejorar la experiencia de usuario cuando ocurren errores, con mensajes más claros y acciones específicas.

**Criterios de aceptación:**
- [ ] Mensajes de error más descriptivos
- [ ] Botones de acción específicos por tipo de error
- [ ] Soporte para retry desde la UI
- [ ] Mejor manejo de estados de error vacío

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: UI de errores es clara y útil

**Dependencias:** Tarea 12
**Archivos tocados:**
- `app/src/main/java/com/example/erp/ui/DolarScreen.kt`

**Alcance:** Small (1-2 archivos)

## Checkpoint: Manejo de Errores
- [ ] Errores se manejan consistentemente
- [ ] Retry funciona para errores de red
- [ ] UI muestra errores de forma clara
- [ ] Revisión con humano antes de continuar

---

## Fase 4: Caché de Datos

### Tarea 14: Implementar caché en memoria (LruCache)
**Descripción:** Crear un caché en memoria para datos frecuentemente accedidos.

**Criterios de aceptación:**
- [ ] Implementar `MemoryCache` con LruCache
- [ ] Configurar tamaño máximo del caché
- [ ] Implementar TTL (Time To Live) para datos
- [ ] Integrar con el Repository existente

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Caché funciona en memoria

**Dependencias:** Ninguna
**Archivos tocados:**
- `app/src/main/java/com/example/erp/data/MemoryCache.kt` (nuevo)
- `app/src/main/java/com/example/erp/data/ApiDolarRepository.kt`

**Alcance:** Small (1-2 archivos)

### Tarea 15: Implementar caché persistente (Room/DataStore)
**Descripción:** Crear un caché persistente para datos que sobrevivan cierre de la app.

**Criterios de aceptación:**
- [ ] Evaluar Room vs DataStore para caché persistente
- [ ] Implementar esquema de datos para caché
- [ ] Crear DAO o DataStore para acceso a datos
- [ ] Implementar estrategia de invalidación

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Datos persisten entre sesiones

**Dependencias:** Tarea 14
**Archivos tocados:**
- `app/src/main/java/com/example/erp/data/PersistentCache.kt` (nuevo)
- `app/build.gradle.kts` (agregar dependencias de Room/DataStore)
- `app/src/main/java/com/example/erp/data/CacheDao.kt` (nuevo)

**Alcance:** Medium (3-5 archivos)

### Tarea 16: Integrar caché en Repository
**Descripción:** Modificar el Repository para usar caché antes de consultar la API.

**Criterios de aceptación:**
- [ ] Repository verifica caché primero
- [ ] Si caché es válido, retorna datos del caché
- [ ] Si caché expiró, consulta API y actualiza caché
- [ ] Si API falla, usa caché como fallback

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Caché reduce llamadas a API

**Dependencias:** Tarea 15
**Archivos tocados:**
- `app/src/main/java/com/example/erp/data/ApiDolarRepository.kt`

**Alcance:** Small (1-2 archivos)

### Tarea 17: Estrategia de invalidación de caché
**Descripción:** Implementar lógica para invalidar caché cuando sea necesario.

**Criterios de aceptación:**
- [ ] Invalidar caché después de cierto tiempo
- [ ] Invalidar caché cuando el usuario fuerza actualización
- [ ] Invalidar caché cuando hay cambios en la API
- [ ] Manejar invalidación parcial por fuente

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Invalidación funciona correctamente

**Dependencias:** Tarea 16
**Archivos tocados:**
- `app/src/main/java/com/example/erp/data/CacheInvalidationStrategy.kt` (nuevo)
- `app/src/main/java/com/example/erp/data/ApiDolarRepository.kt`

**Alcance:** Small (1-2 archivos)

## Checkpoint: Caché de Datos
- [ ] Caché funciona correctamente
- [ ] Datos se actualizan cuando es necesario
- [ ] Offline support básico funciona
- [ ] Revisión con humano antes de continuar

---

## Fase 5: Polish y Documentación

### Tarea 18: Actualizar documentación
**Descripción:** Actualizar la documentación del proyecto para reflejar los cambios realizados.

**Criterios de aceptación:**
- [ ] Actualizar README.md con nuevas dependencias
- [ ] Actualizar AGENTS.md con nuevas convenciones
- [ ] Documentar sistema de DI
- [ ] Documentar sistema de caché
- [ ] Actualizar guía de desarrollo

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Documentación es precisa

**Dependencias:** Tarea 17
**Archivos tocados:**
- `README.md`
- `AGENTS.md`
- `docs/guia-desarrollo.md`

**Alcance:** Medium (3-5 archivos)

### Tarea 19: Code review y refactorización
**Descripción:** Revisar todo el código modificado para mejorar calidad y consistencia.

**Criterios de aceptación:**
- [ ] Revisar naming conventions
- [ ] Eliminar código duplicado
- [ ] Mejorar comentarios y documentación inline
- [ ] Verificar que no hay warnings importantes
- [ ] Asegurar que el código sigue convenciones del proyecto

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleDebug`
- [ ] Verificación manual: Código es limpio y consistente

**Dependencias:** Tarea 18
**Archivos tocados:** Múltiples archivos en `app/src/main/java/com/example/erp/`

**Alcance:** Large (5+ archivos)

### Tarea 20: Preparar para release
**Descripción:** Preparar el proyecto para un release de producción.

**Criterios de aceptación:**
- [ ] Verificar que no hay TODOs pendientes
- [ ] Ejecutar todos los tests
- [ ] Verificar que el build de release funciona
- [ ] Preparar changelog
- [ ] Verificar que no hay credenciales expuestas

**Verificación:**
- [ ] Tests pasan: `./gradlew testDebugUnitTest`
- [ ] Build exitoso: `./gradlew assembleRelease`
- [ ] Verificación manual: App está lista para release

**Dependencias:** Tarea 19
**Archivos tocados:** Múltiples archivos

**Alcance:** Medium (3-5 archivos)

## Checkpoint: Completo
- [ ] Todos los criterios de aceptación cumplidos
- [ ] Documentación actualizada
- [ ] Listo para revisión
- [ ] Aprobación del humano
