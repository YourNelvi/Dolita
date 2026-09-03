# Propuesta de Mejoras: Android ERP (Dolar)

## Change ID: `improve-android-erp`

## Resumen
Esta propuesta establece mejoras fundamentales para el proyecto Android ERP (app "Dolar"), enfocándose en robustez, mantenibilidad, testing y funcionalidad. Las mejoras se dividen en 4 áreas principales: Testing, Inyección de Dependencias, Manejo de Errores y Caché de Datos.

---

## Por qué (Why)

### Problemas Actuales
1. **Testing insuficiente**: Solo existen tests de ejemplo que no cubren funcionalidad real
2. **Acoplamiento fuerte**: El ViewModel instancia directamente `ApiDolarRepository()`
3. **Errores básicos**: Sistema de errores limitado (NetworkError, ApiError, ParseError)
4. **Sin caché**: La app siempre consulta la API, sin soporte offline
5. **Mantenibilidad**: Dificultad para agregar nuevas funcionalidades

### Oportunidades
- **Mayor confianza**: Tests reales permiten cambios seguros
- **Mejor arquitectura**: DI mejora testabilidad y desacoplamiento
- **Experiencia de usuario**: Errores claros y caché mejoran UX
- **Escalabilidad**: Base sólida para futuras funcionalidades

---

## Qué Cambia (What Changes)

### 1. Sistema de Testing Exhaustivo
- Infraestructura de tests con Fakes y Mocks
- Tests para ApiDolarRepository (parsing JSON)
- Tests para DolarViewModel (estados y lógica)
- Tests para RateHistoryStore (persistencia)
- Tests para ThemePreferences (configuración)
- **Cobertura objetivo**: 60% mínimo en capa de datos

### 2. Inyección de Dependencias con Hilt
- Configuración de Hilt en el proyecto
- Módulos de DI para Repository, Network, Storage, Theme
- Refactorización del ViewModel para usar DI
- Tests actualizados con soporte DI

### 3. Manejo de Errores Mejorado
- Tipos de error más granulares y específicos
- RetryPolicy para errores transitorios
- ErrorHandler global centralizado
- UI de errores mejorada con acciones específicas

### 4. Caché de Datos
- Caché en memoria (LruCache) para datos frecuentes
- Caché persistente (Room/DataStore) para offline support
- Integración en Repository con estrategia de invalidación
- Fallback a caché cuando la API falla

---

## Impacto (Impact)

### Especificaciones Afectadas
- **spec/specs/data-layer/spec.md**: Se modifican requisitos de Repository y persistencia
- **spec/specs/ui-layer/spec.md**: Se modifican requisitos de manejo de errores
- **spec/specs/architecture/spec.md**: Se agregan requisitos de DI y testing

### Código Afectado
- **Archivos modificados**:
  - `app/build.gradle.kts` (dependencias Hilt, Room/DataStore)
  - `app/src/main/java/com/example/erp/ui/DolarViewModel.kt` (DI)
  - `app/src/main/java/com/example/erp/data/ApiDolarRepository.kt` (caché, retry)
  - `app/src/main/java/com/example/erp/ui/DolarScreen.kt` (UI errores)
  - `app/src/main/java/com/example/erp/data/Error.kt` (tipos granulares)

- **Archivos nuevos**:
  - `app/src/main/java/com/example/erp/di/` (módulos Hilt)
  - `app/src/main/java/com/example/erp/data/MemoryCache.kt`
  - `app/src/main/java/com/example/erp/data/PersistentCache.kt`
  - `app/src/main/java/com/example/erp/data/RetryPolicy.kt`
  - `app/src/main/java/com/example/erp/data/ErrorHandler.kt`
  - `app/src/test/java/com/example/erp/` (tests completos)

### APIs Afectadas
- **Interna**: Se modifica interfaz `DolarRepository` (agrega métodos de caché)
- **Externa**: Sin cambios en APIs externas (BCV, Binance)

### Usuarios Afectados
- **Desarrolladores**: Mejor experiencia de desarrollo y testing
- **Usuarios finales**: Mejor UX con errores claros y caché
- **Mantenimiento**: Código más mantenible y escalable

---

## Riesgos y Mitigaciones

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Complejidad de Hilt | Alto | Empezar con módulos simples, documentar bien |
| Tests frágiles | Medio | Usar Fakes, no Mocks when possible |
| Caché puede causar bugs | Medio | Invalidación agresiva, tests de caché |
| Cambios en API externa | Bajo | Circuit Breaker, fallback a caché |
| Tiempo de implementación | Medio | Fases incrementales, checkpoints |

---

## Estimación de Tiempo

- **Fase 1 (Testing)**: 2-3 horas
- **Fase 2 (DI)**: 2-3 horas  
- **Fase 3 (Errores)**: 2-3 horas
- **Fase 4 (Caché)**: 3-4 horas
- **Fase 5 (Polish)**: 1-2 horas
- **Total**: 10-15 horas de trabajo de agente

---

## Próximos Pasos
1. Revisar y aprobar esta propuesta
2. Crear tareas de implementación en `tasks.md`
3. Definir especificaciones formales en `spec-delta.md`
4. Comenzar implementación por Fase 1 (Testing)
