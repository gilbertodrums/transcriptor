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

- [x] **1.1** Declarar `RECORD_AUDIO` en el manifiesto y crear el flujo de solicitud de permiso en
      tiempo de ejecución (con manejo de "denegado").
- [x] **1.2** En `data/audio/`, crear un `AudioRecorder` que grabe PCM (16 kHz mono) a un archivo en
      almacenamiento privado. Exponerlo detrás de una interfaz simple.
- [x] **1.3** Modelo `Recording` en `domain/` (id, título, fecha, ruta del archivo).
- [x] **1.4** ViewModel + pantalla con botón Grabar/Detener que use el `AudioRecorder`.
- [x] **1.5** Lista de grabaciones (en memoria por ahora) que muestre las hechas en la sesión.
- [x] **1.6** Reproductor mínimo para confirmar que el audio guardado es válido.
- [x] **1.7** Pasos de prueba manual — ejecutar en dispositivo físico:
      1. Instalar APK (`./gradlew installDebug` o Android Studio → Run).
      2. Abrir la app. Verificar que aparece el botón "Grabar" o el banner de permiso.
      3. Si aparece "Conceder permiso": pulsarlo y otorgar el permiso de micrófono.
      4. Pulsar "Grabar". Verificar que el botón cambia a "Detener grabación" (rojo)
         y aparece el texto "Grabando…".
      5. Hablar 5–10 segundos cerca del micrófono.
      6. Pulsar "Detener grabación". Verificar que aparece una entrada en la lista con la
         fecha/hora.
      7. Pulsar "Reproducir" en esa entrada. Verificar que se escucha el audio grabado.
      8. Pulsar "Detener" para cortar la reproducción.
      9. (Opcional) Repetir pasos 4–8 con una segunda grabación; confirmar que ambas
         aparecen en la lista y se pueden reproducir independientemente.
      ⚠️ Requiere dispositivo físico (el emulador no siempre tiene micrófono real).

## Fase 2 — Transcripción local

- [x] **2.1** Definir interfaz `SpeechRecognizer` en `data/asr/`.
- [x] **2.2** Agregar Vosk 0.3.47 + repositorio alphacephei. Añadir `INTERNET` (excepción documentada:
      descarga única del modelo desde alphacephei.com, sin datos de usuario). Modelo: vosk-model-small-es-0.42, 39 MB, Apache 2.0.
- [x] **2.3** `VoskModelManager`: descarga + descompresión del modelo con progreso.
- [x] **2.4** `VoskSpeechRecognizer`: implementación de la interfaz; procesa WAV → texto.
- [x] **2.5** Actualizar `Recording` con campo `transcript: String?`; `RecordingViewModel` con
      estado del modelo y lógica de transcripción.
- [x] **2.6** UI: estado de descarga del modelo, botón "Transcribir" por grabación, texto resultante.
- [x] **2.7** Test manual en dispositivo físico en modo avión (prueba clave de localidad):
      1. Activar modo avión.
      2. Abrir app. Verificar que aparece "Descargar modelo" (si modelo no descargado aún).
         O si ya está descargado, continuar desde el paso 4.
      3. (Primera vez con WiFi) Descargar el modelo → esperar barra de progreso → "Listo".
      4. Activar modo avión. Hacer una grabación de 10–20 s.
      5. Pulsar "Transcribir" → esperar (puede tardar 10–30 s según la duración).
      6. Verificar que aparece texto transcrito debajo de la grabación, **sin conexión**.
      ⚠️ Si el modelo ya estaba descargado, el modo avión completo funciona desde el paso 4.

## Fase 3 — Resumen local

- [x] **3.1** Definir interfaz `Summarizer` + clase `Summary` en `data/llm/`.
- [x] **3.2** `ExtractiveSummarizer` (Plan B): extracción heurística de frases clave,
      funciona en cualquier idioma y dispositivo sin modelos pesados. Marcado como "Resumen básico".
- [x] **3.3** ML Kit Summarization API descartada: la API real de `genai-summarization:1.0.0-beta1`
      no coincide con la documentación pública disponible (no se pudo verificar — regla AGENTS.md).
      Además, solo soporta inglés; para español el Plan B es siempre el correcto.
      La interfaz `Summarizer` está diseñada para enchufar un LLM real en Fase 5 cuando
      esté verificada la API (MediaPipe LLM / ML Kit estable).
- [x] **3.4** `Recording` actualizado con campo `summary: Summary?`.
      `RecordingViewModel`: método `summarize()`, estado `_summarizingId`.
- [x] **3.5** UI: botón "Resumir" (aparece tras transcripción), spinner mientras resume,
      lista de puntos + badge "Resumen IA" / "Resumen básico".
- [ ] **3.6** Test manual en dispositivo:
      1. Grabar y transcribir una clase de 2–5 minutos.
      2. Pulsar "Resumir". Esperar 2–10 s.
      3. Verificar que aparecen 3–5 puntos clave en español (Resumen básico).
      4. (Opcional) Verificar que la app no falla en modo avión.

> Las tareas de la Fase 4 en adelante se detallarán cuando lleguemos a ellas.

---

## Bitácora de decisiones (la llena el agente conforme avanza)

| Fecha | Decisión tomada | Por qué |
|---|---|---|
| 2026-06-04 | AGP 9.2.0 + Gradle 9.4.1 + Kotlin 2.3.20 + Compose BOM 2026.05.00, compileSdk/targetSdk 36, minSdk 26 | Versiones estables verificadas en docs oficiales a la fecha |
| 2026-06-04 | Removido plugin `org.jetbrains.kotlin.android`; AGP 9.0+ tiene soporte Kotlin incorporado | Error explícito de AGP 9.x al intentar aplicar ambos |
| 2026-06-04 | `org.gradle.java.home` apunta al JBR de Android Studio (JDK 21); sin `jvmToolchain` | JBR es Java 21; `jvmToolchain(17)` causaba fallo de toolchain lookup |
| 2026-06-04 | AudioRecord PCM 16kHz mono → WavWriter con header placeholder 44 bytes | Whisper/Vosk esperan WAV; MediaPlayer lo reproduce directamente para verificación |
| 2026-06-04 | `AudioRecorder` interfaz en `data/audio/`; `AudioRecorderImpl` con scope IO propio | Permite cambiar motor en Fase 2 sin tocar ViewModel ni UI |
| 2026-06-04 | Sin `Icons.Default.PlayArrow`; se usan TextButton para evitar dependencia `material-icons-extended` | Reduce tamaño del APK; se puede agregar en Fase 5 (pulido) |
