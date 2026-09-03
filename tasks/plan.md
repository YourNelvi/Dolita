# Plan de Implementación: Mejoras al Proyecto Android ERP

## Resumen
Este plan detalla las mejoras recomendadas para el proyecto Android ERP (app "Dolar"), una aplicación de cotizaciones de dólar/euro/USDT con calculadora y gráfico de evolución. Las mejoras se enfocan en robustez, mantenibilidad, testing y funcionalidad.

## Decisiones de Arquitectura

### 1. Testing exhaustivo
- **Decisión**: Implementar tests unitarios reales para toda la lógica de negocio
- **Razón**: Actualmente solo hay tests de ejemplo que no cubren funcionalidad real
- **Impacto**: Alta confianza en cambios futuros, detección temprana de regresiones

### 2. Inyección de Dependencias (DI)
- **Decisión**: Implementar Hilt para inyección de dependencias
- **Razón**: Actualmente el ViewModel instancia directamente `ApiDolarRepository()`
- **Beneficios**: Testabilidad, desacoplamiento, gestión de ciclos de vida

### 3. Manejo de errores mejorado
- **Decisión**: Crear un sistema de errores más granular y consistente
- **Razón**: Los errores actuales son básicos (NetworkError, ApiError, ParseError)
- **Mejora**: Errores más específicos, RetryPolicy, Circuit Breaker pattern

### 4. Caché de datos
- **Decisión**: Implementar caché en memoria y persistente
- **Razón**: La app siempre consulta la API, no hay caché
- **Beneficios**: Mejor experiencia de usuario, menor uso de datos, offline support

### 5. Navegación (futuro)
- **Decisión**: Evaluar Navigation Compose para múltiples pantallas
- **Razón**: Actualmente es app de una sola pantalla
- **Nota**: Esta mejora es opcional y futura

## Lista de Tareas

### Fase 1: Fundamentos de Testing
- [ ] Tarea 1: Crear infraestructura de tests con Fakes y Mocks
- [ ] Tarea 2: Tests para ApiDolarRepository (parsing JSON)
- [ ] Tarea 3: Tests para DolarViewModel
- [ ] Tarea 4: Tests para RateHistoryStore
- [ ] Tarea 5: Tests para ThemePreferences

### Checkpoint: Fundamentos de Testing
- [ ] Todos los tests pasan
- [ ] Cobertura mínima del 60% en capa de datos
- [ ] Build sin errores

### Fase 2: Inyección de Dependencias
- [ ] Tarea 6: Configurar Hilt en el proyecto
- [ ] Tarea 7: Crear módulos de DI para Repository
- [ ] Tarea 8: Refactorizar ViewModel para usar DI
- [ ] Tarea 9: Tests actualizados con DI

### Checkpoint: Inyección de Dependencias
- [ ] Hilt funciona correctamente
- [ ] Tests siguen pasando
- [ ] App compila y ejecuta

### Fase 3: Manejo de Errores Mejorado
- [ ] Tarea 10: Definir tipos de error más granulares
- [ ] Tarea 11: Implementar RetryPolicy
- [ ] Tarea 12: Crear ErrorHandler global
- [ ] Tarea 13: UI de errores mejorada

### Checkpoint: Manejo de Errores
- [ ] Errores se manejan consistentemente
- [ ] Retry funciona para errores de red
- [ ] UI muestra errores de forma clara

### Fase 4: Caché de Datos
- [ ] Tarea 14: Implementar caché en memoria (LruCache)
- [ ] Tarea 15: Implementar caché persistente (Room/DataStore)
- [ ] Tarea 16: Integrar caché en Repository
- [ ] Tarea 17: Estrategia de invalidación de caché

### Checkpoint: Caché de Datos
- [ ] Caché funciona correctamente
- [ ] Datos se actualizan cuando es necesario
- [ ] Offline support básico funciona

### Fase 5: Polish y Documentación
- [ ] Tarea 18: Actualizar documentación
- [ ] Tarea 19: Code review y refactorización
- [ ] Tarea 20: Preparar para release

### Checkpoint: Completo
- [ ] Todos los criterios de aceptación cumplidos
- [ ] Documentación actualizada
- [ ] Listo para revisión

## Riesgos y Mitigaciones

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Complejidad de Hilt | Alto | Empezar con módulos simples, documentar bien |
| Tests frágiles | Medio | Usar Fakes, no Mocks when possible |
| Caché puede causar bugs | Medio | Invalidación agresiva, tests de caché |
| Cambios en API externa | Bajo | Circuit Breaker, fallback a caché |

## Preguntas Abiertas
- ¿Se quiere implementar Navigation Compose para múltiples pantallas?
- ¿Priorizar offline support o solo caché temporal?
- ¿Agregar analytics o monitoreo de errores?
