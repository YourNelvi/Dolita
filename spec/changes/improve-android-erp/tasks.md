# Tareas de Implementación: Mejoras Android ERP

## Fase 1: Fundamentos de Testing

### Tarea 1: Crear infraestructura de tests con Fakes y Mocks
- [ ] Crear `FakeDolarRepository` que implemente `DolarRepository`
- [ ] Crear `FakeRateHistoryStore` que implemente `RateHistoryStore`
- [ ] Crear `FakeThemePreferences` que implemente `ThemePreferences`
- [ ] Configurar Mockito-Kotlin para mocking cuando sea necesario
- [ ] Crear test de ejemplo que use los Fakes
- [ ] Verificar: `./gradlew testDebugUnitTest` pasa

### Tarea 2: Tests para ApiDolarRepository (parsing JSON)
- [ ] Crear test para parsing de respuesta BCV exitosa
- [ ] Crear test para parsing de respuesta Binance exitosa
- [ ] Crear test para manejo de errores HTTP
- [ ] Crear test para parsing con datos faltantes
- [ ] Crear test para parsing con datos malformados
- [ ] Verificar: Tests cubren casos exitosos y de error

### Tarea 3: Tests para DolarViewModel
- [ ] Crear test para carga exitosa de datos
- [ ] Crear test para error en carga de datos
- [ ] Crear test para selección de fuente
- [ ] Crear test para cambio de tema
- [ ] Crear test para estados de loading/error/success
- [ ] Verificar: ViewModel funciona correctamente

### Tarea 4: Tests para RateHistoryStore
- [ ] Crear test para escritura exitosa de samples
- [ ] Crear test para lectura de datos existentes
- [ ] Crear test para manejo de archivos corruptos
- [ ] Crear test para atomicidad de escritura
- [ ] Crear test para RateSamplingPolicy
- [ ] Verificar: Persistencia funciona correctamente

### Tarea 5: Tests para ThemePreferences
- [ ] Crear test para guardado de tema
- [ ] Crear test para carga de tema
- [ ] Crear test para cambio de modo (claro/oscuro/sistema)
- [ ] Crear test para dynamic color
- [ ] Crear test para valores por defecto
- [ ] Verificar: Preferencias se guardan y cargan

### Checkpoint: Fundamentos de Testing
- [ ] Todos los tests pasan
- [ ] Cobertura mínima del 60% en capa de datos
- [ ] Build sin errores
- [ ] Revisión con humano antes de continuar

---

## Fase 2: Inyección de Dependencias

### Tarea 6: Configurar Hilt en el proyecto
- [ ] Agregar dependencias de Hilt en `build.gradle.kts`
- [ ] Crear `ERPApplication.kt` con `@HiltAndroidApp`
- [ ] Configurar Hilt en `AndroidManifest.xml`
- [ ] Configurar Hilt en `MainActivity.kt`
- [ ] Verificar: App compila con Hilt

### Tarea 7: Crear módulos de DI para Repository
- [ ] Crear `RepositoryModule.kt` para `DolarRepository`
- [ ] Crear `NetworkModule.kt` para `OkHttpClient`
- [ ] Crear `StorageModule.kt` para `RateHistoryStore`
- [ ] Crear `ThemeModule.kt` para `ThemeRepository`
- [ ] Verificar: Dependencias se inyectan correctamente

### Tarea 8: Refactorizar ViewModel para usar DI
- [ ] Modificar `DolarViewModel` para usar `@Inject constructor`
- [ ] Eliminar instanciación directa de dependencias
- [ ] Verificar: ViewModel funciona con DI

### Tarea 9: Tests actualizados con DI
- [ ] Actualizar tests para usar `@HiltAndroidTest`
- [ ] Crear módulos de test para Fakes
- [ ] Verificar: Todos los tests siguen pasando

### Checkpoint: Inyección de Dependencias
- [ ] Hilt funciona correctamente
- [ ] Tests siguen pasando
- [ ] App compila y ejecuta
- [ ] Revisión con humano antes de continuar

---

## Fase 3: Manejo de Errores Mejorado

### Tarea 10: Definir tipos de error más granulares
- [ ] Expandir `Error.kt` con tipos específicos
- [ ] Agregar errores de caché y timeout
- [ ] Mantener compatibilidad con errores existentes
- [ ] Verificar: Errores son más específicos

### Tarea 11: Implementar RetryPolicy
- [ ] Crear `RetryPolicy.kt` con configuración de intentos
- [ ] Implementar retry exponencial para errores de red
- [ ] Configurar número máximo de reintentos
- [ ] Verificar: Retry funciona en errores de red

### Tarea 12: Crear ErrorHandler global
- [ ] Crear `ErrorHandler.kt` como singleton
- [ ] Implementar mapeo de excepciones a errores tipados
- [ ] Agregar logging centralizado de errores
- [ ] Verificar: Errores se manejan centralizadamente

### Tarea 13: UI de errores mejorada
- [ ] Mejorar `DolarScreen.kt` con errores más descriptivos
- [ ] Agregar botones de acción específicos por error
- [ ] Implementar retry desde la UI
- [ ] Verificar: UI de errores es clara y útil

### Checkpoint: Manejo de Errores
- [ ] Errores se manejan consistentemente
- [ ] Retry funciona para errores de red
- [ ] UI muestra errores de forma clara
- [ ] Revisión con humano antes de continuar

---

## Fase 4: Caché de Datos

### Tarea 14: Implementar caché en memoria (LruCache)
- [ ] Crear `MemoryCache.kt` con LruCache
- [ ] Configurar tamaño máximo del caché
- [ ] Implementar TTL (Time To Live) para datos
- [ ] Verificar: Caché funciona en memoria

### Tarea 15: Implementar caché persistente (Room/DataStore)
- [ ] Evaluar Room vs DataStore para caché persistente
- [ ] Implementar esquema de datos para caché
- [ ] Crear DAO o DataStore para acceso a datos
- [ ] Verificar: Datos persisten entre sesiones

### Tarea 16: Integrar caché en Repository
- [ ] Modificar `ApiDolarRepository.kt` para usar caché
- [ ] Implementar lógica de fallback a caché
- [ ] Verificar: Caché reduce llamadas a API

### Tarea 17: Estrategia de invalidación de caché
- [ ] Implementar invalidación por tiempo
- [ ] Implementar invalidación por actualización manual
- [ ] Implementar invalidación por cambios en API
- [ ] Verificar: Invalidación funciona correctamente

### Checkpoint: Caché de Datos
- [ ] Caché funciona correctamente
- [ ] Datos se actualizan cuando es necesario
- [ ] Offline support básico funciona
- [ ] Revisión con humano antes de continuar

---

## Fase 5: Polish y Documentación

### Tarea 18: Actualizar documentación
- [ ] Actualizar `README.md` con nuevas dependencias
- [ ] Actualizar `AGENTS.md` con nuevas convenciones
- [ ] Documentar sistema de DI
- [ ] Documentar sistema de caché
- [ ] Verificar: Documentación es precisa

### Tarea 19: Code review y refactorización
- [ ] Revisar naming conventions
- [ ] Eliminar código duplicado
- [ ] Mejorar comentarios y documentación inline
- [ ] Verificar: Código es limpio y consistente

### Tarea 20: Preparar para release
- [ ] Verificar que no hay TODOs pendientes
- [ ] Ejecutar todos los tests
- [ ] Verificar que el build de release funciona
- [ ] Preparar changelog
- [ ] Verificar: App está lista para release

### Checkpoint: Completo
- [ ] Todos los criterios de aceptación cumplidos
- [ ] Documentación actualizada
- [ ] Listo para revisión
- [ ] Aprobación del humano
