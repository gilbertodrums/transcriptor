# TASKS — Backlog detallado

> El agente toma la **primera tarea sin marcar**, la completa según el flujo de `AGENTS.md`, la marca,
> reporta y espera. Cuando se agotan las tareas de una fase y se cumplen sus criterios en
> `ROADMAP.md`, se piden las tareas de la siguiente fase (o el agente las propone para tu aprobación).

---

## Fase 0 — Esqueleto

- [x] **0.1** Crear el proyecto Android (Kotlin + Jetpack Compose, Gradle Kotlin DSL, minSdk 26).
      Confirmar versiones estables actuales antes de fijarlas.
- [x] **0.2** Crear la estructura de paquetes `ui/`, `domain/`, `data/` (con subcarpetas vacías de
      `data/` según `ARCHITECTURE.md`).
- [x] **0.3** Pantalla inicial Compose con un texto simple. Confirmar que compila y abre.
      (Compilación verificada. Prueba en dispositivo pendiente — ver reporte tarea 0.3.)
- [x] **0.4** Configurar `strings.xml` y mover ahí cualquier texto visible.

## Fase 1 — Grabar y guardar audio

- [ ] **1.1** Declarar `RECORD_AUDIO` en el manifiesto y crear el flujo de solicitud de permiso en
      tiempo de ejecución (con manejo de "denegado").
- [ ] **1.2** En `data/audio/`, crear un `AudioRecorder` que grabe PCM (16 kHz mono) a un archivo en
      almacenamiento privado. Exponerlo detrás de una interfaz simple.
- [ ] **1.3** Modelo `Recording` en `domain/` (id, título, fecha, ruta del archivo).
- [ ] **1.4** ViewModel + pantalla con botón Grabar/Detener que use el `AudioRecorder`.
- [ ] **1.5** Lista de grabaciones (en memoria por ahora) que muestre las hechas en la sesión.
- [ ] **1.6** Reproductor mínimo para confirmar que el audio guardado es válido.
- [ ] **1.7** Pasos de prueba manual escritos y ejecutados en dispositivo físico.

> Las tareas de la Fase 2 en adelante se detallarán cuando lleguemos a ellas, para no fijar
> decisiones técnicas (qué motor de ASR, etc.) antes de tiempo. Mantener este archivo vivo.

---

## Bitácora de decisiones (la llena el agente conforme avanza)

| Fecha | Decisión tomada | Por qué |
|---|---|---|
| 2026-06-04 | AGP 9.2.0 + Gradle 9.4.1 + Kotlin 2.3.20 + Compose BOM 2026.05.00, compileSdk/targetSdk 36, minSdk 26 | Versiones estables verificadas en docs oficiales a la fecha |
| 2026-06-04 | Removido plugin `org.jetbrains.kotlin.android`; AGP 9.0+ tiene soporte Kotlin incorporado | Error explícito de AGP 9.x al intentar aplicar ambos |
| 2026-06-04 | `org.gradle.java.home` apunta al JBR de Android Studio (JDK 21); sin `jvmToolchain` | JBR es Java 21; `jvmToolchain(17)` causaba fallo de toolchain lookup |
