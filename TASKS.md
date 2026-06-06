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
      no coincide con la documentación pública (no se pudo verificar — regla AGENTS.md) y solo
      soporta inglés. Se optó por **MediaPipe LLM Inference + Gemma 3 1B** (ver 3.6).
- [x] **3.4** `Recording` actualizado con campo `summary: Summary?`.
      `RecordingViewModel`: método `summarize()`, estado `_summarizingId`.
- [x] **3.5** UI: botón "Resumir" (aparece tras transcripción), spinner mientras resume,
      lista de puntos + badge "Resumen IA" / "Resumen básico".
- [x] **3.6** **LLM real en dispositivo:** `MediaPipeSummarizer` con `tasks-genai:0.10.27`
      corriendo **Gemma 3 1B int4** (.task, ~530 MB). Prompt en español, parseo de viñetas.
      Degradación automática al `ExtractiveSummarizer` si el modelo no está cargado.
- [x] **3.7** `ExtractiveSummarizer` reescrito: la versión por oraciones devolvía la
      transcripción completa (Vosk no produce puntuación). Ahora divide por bloques de
      40 palabras y puntúa por densidad de palabras clave (stopwords en español).
- [x] **3.8** **Descarga del modelo con un solo botón, sin token:** `MediaPipeModelManager`
      descarga Gemma desde un mirror público no-gated (descarga directa, sin licencia ni
      token para el usuario). El usuario nunca sale de la app. Validación de integridad por tamaño.
- [ ] **3.9** Test manual en dispositivo:
      1. Conectar WiFi. Pulsar "Descargar IA" → esperar barra de progreso (~530 MB).
      2. Esperar "Preparando IA…" (carga en RAM, ~15–30 s).
      3. Grabar y transcribir una clase de 2–5 minutos.
      4. Pulsar "Resumir". Verificar que aparecen 3–5 puntos clave **redactados por la IA**
         (no fragmentos literales), con el badge "Resumen IA".
      5. Verificar que el resumen funciona en **modo avión** (modelo ya en disco).

## Fase 4 — Biblioteca y persistencia (Room)

> Meta: que grabaciones, transcripciones y resúmenes sobrevivan al cerrar la app.
> Hoy todo vive en memoria (`_recordings` en `RecordingViewModel`) y se pierde al cerrar.

- [x] **4.1** Agregar dependencias Room (`room-runtime`, `room-ktx`) + compilador vía **KSP**
      (no kapt). Confirmar versiones estables vigentes antes de fijarlas (regla AGENTS.md).
      **Hecho (2026-06-06).** Room 2.8.4 + KSP 2.3.9 (verificado: su POM depende de Kotlin
      2.3.20, el del proyecto). `assembleDebug` pasa.
- [x] **4.2** `RecordingEntity` en `data/db/` que mapee el modelo de dominio `Recording`.
      `TypeConverter` para `LocalDateTime` (guardar como epoch millis o ISO String) y para
      `Summary` (serializar a JSON — incluir los puntos y el flag IA/básico).
      **Hecho (2026-06-06).** `RecordingEntity` (tabla `recordings`, PK `id`) 1:1 con `Recording`.
      `Converters`: `LocalDateTime` ⇄ epoch millis (zona sistema, para ordenar en SQL) y
      `Summary` ⇄ JSON con `org.json` (sin dependencias nuevas). Compila con KSP.
- [x] **4.3** `RecordingDao`: `insert`, `update`, `delete`, `getAllFlow(): Flow<List<RecordingEntity>>`
      (ordenado por fecha desc) y `getById`.
      **Hecho (2026-06-06).** Escrituras `suspend`; `insert` con `OnConflictStrategy.REPLACE`
      (sirve para guardar cambios); `getAllFlow` ordena `createdAt DESC`. Compila con KSP.
- [x] **4.4** `AppDatabase : RoomDatabase` (singleton, versión 1) + funciones de mapeo
      `Entity ↔ Recording` para que la capa `domain/` no conozca Room.
      **Hecho (2026-06-06).** `AppDatabase` (BD `transcriptor.db`, v1, `@TypeConverters`,
      singleton con doble verificación). Mapeo `toDomain()`/`toEntity()`. Room valida todo:
      `assembleDebug` pasa sin advertencias.
- [x] **4.5** `RecordingRepository` en `data/` que exponga `Recording` de dominio sobre el DAO.
      Es la única puerta a la persistencia; oculta Room al ViewModel.
      **Hecho (2026-06-06).** `recordings: Flow<List<Recording>>` (mapea entidad→dominio),
      `getById`, `save` (insert REPLACE = crear/actualizar) y `delete`. Compila.
- [x] **4.6** Cablear `RecordingViewModel`: cargar desde el repositorio en `init` (colectar el
      `Flow`), y persistir en `stopRecording()`, `transcribe()` y `summarize()` en vez de mutar
      la lista en memoria. `updateRecording` pasa a escribir en BD.
      **Hecho (2026-06-06).** `recordings` ahora es `repository.recordings.stateIn(...)`
      (la BD es la fuente de verdad; la UI se actualiza sola). `stopRecording` y `updateRecording`
      (ahora `suspend`) escriben vía `repository.save`. Eliminada la lista en memoria. Compila.
- [x] **4.7** Borrar grabación: `delete` en DAO **+ borrar el archivo de audio** del
      almacenamiento privado. Botón en la UI con confirmación.
      **Hecho (2026-06-06).** `deleteRecording()` detiene la reproducción si aplica, borra el
      `.wav` y la fila. Botón "Borrar" (rojo) en cada item + `AlertDialog` de confirmación.
      Strings nuevos. `assembleDebug` pasa.
- [x] **4.8** Estrategia de migración: arrancar en versión 1. `fallbackToDestructiveMigration`
      es aceptable **solo** mientras la app no esté publicada; documentarlo en la bitácora.
      **Hecho (2026-06-06).** BD en v1 con `fallbackToDestructiveMigration(dropAllTables = true)`
      y `exportSchema = false`. Documentado en bitácora y en el comentario de `AppDatabase`.
- [x] **4.9** Test manual en dispositivo:
      1. Grabar, transcribir y resumir una grabación.
      2. Cerrar la app por completo (deslizar desde "recientes"), no solo minimizar.
      3. Reabrir → verificar que la grabación, su transcripción y su resumen **siguen ahí**.
      4. Reproducir el audio para confirmar que la ruta del archivo sigue válida.
      5. Borrar una grabación → confirmar que desaparece de la lista **y** que su archivo
         `.wav` ya no existe en el almacenamiento privado.
      **Hecho (2026-06-06).** Probado en Motorola Edge 50 Pro (Android 16). Todo OK; logcat
      sin crashes ni errores. **Fase 4 completa.**

**Criterios de aceptación (de ROADMAP.md):** Room guarda metadatos, transcripción y resumen;
la lista persiste entre reinicios; abrir una grabación muestra audio+transcripción+resumen;
se puede borrar (incluido su archivo).

---

## Fase 4.5 — Robustez de descarga de modelos (hardening) ⚠️ recomendado antes de publicar

> Problema: hoy el modelo Gemma se baja de **un solo mirror de un tercero** (typosbro en
> HuggingFace) y se valida **solo por tamaño**. Si ese mirror cae, la IA queda inservible para
> usuarios nuevos. Vosk (alphacephei.com) es más estable pero comparte la fragilidad.
> **Decisión (2026-06-06): origen primario = GitHub Release del propio repo; HF/alphacephei como fallback.**

- [x] **4.5.1** Subir el `.task` de Gemma (~554 MB) a un **GitHub Release** del repo y calcular
      su **SHA-256**. Hacer lo mismo (registrar SHA-256) para el `.zip` de Vosk.
      **Hecho (2026-06-06).** Release `models-v1` en `gilbertodrums/transcriptor` (repo público,
      descarga sin token, `Accept-Ranges: bytes` → reanudable). Datos para 4.5.2/4.5.4:
      - Gemma: `https://github.com/gilbertodrums/transcriptor/releases/download/models-v1/gemma3-1b-it-int4.task`
        · 554661246 bytes · SHA-256 `ddfaf1210d8b4d1b812b5fadb6652999e852c8be6dd9abe353b9213a25262c10`
        · fallback: `https://huggingface.co/typosbro/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task`
      - Vosk: `https://github.com/gilbertodrums/transcriptor/releases/download/models-v1/vosk-model-small-es-0.42.zip`
        · 39817833 bytes · SHA-256 `09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f`
        · fallback: `https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip`
- [ ] **4.5.2** Modelo `ModelSource(urls: List<String>, sha256: String, sizeBytes: Long)` en
      `data/`: lista ordenada de orígenes (primario = GitHub Release; fallbacks = mirrors HF /
      alphacephei) + hash y tamaño esperados.
- [ ] **4.5.3** `ResilientDownloader` reutilizable en `data/`: recorre la lista de URLs probando
      la siguiente si una falla; **descarga reanudable** con cabecera HTTP `Range` (continúa
      desde el `.tmp` parcial); **verifica SHA-256** al terminar; `renameTo` atómico solo si el
      hash coincide. Si el hash no coincide, descarta y prueba el siguiente origen.
- [ ] **4.5.4** Migrar `MediaPipeModelManager` y `VoskModelManager` a `ResilientDownloader` con
      sus respectivos `ModelSource`. Sustituir la validación por tamaño por la de hash.
- [ ] **4.5.5** UX de error: distinguir "sin conexión" de "todos los orígenes fallaron";
      botón "Reintentar"; al reabrir, ofrecer reanudar una descarga incompleta.
- [ ] **4.5.6** *(opcional, máxima robustez)* **Manifiesto remoto**: un JSON pequeño en GitHub
      raw (`models.json`) con las URLs + hash + tamaño actuales. La app lo lee primero, así se
      puede **cambiar el origen sin publicar una actualización**. Si el manifiesto no carga, usar
      la lista embebida como respaldo.
- [ ] **4.5.7** Test manual: (a) poner una URL primaria inválida a propósito → confirmar que cae
      al fallback; (b) cortar la red a mitad de descarga y reanudar → confirmar que continúa;
      (c) forzar un hash esperado incorrecto → confirmar que rechaza el archivo.

> Las tareas de la Fase 5 (pulido y publicación) se detallarán al llegar.

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
| 2026-06-04 | ASR: Vosk 0.3.47 + modelo `vosk-model-small-es-0.42` (39 MB, Apache 2.0) | Offline, liviano, fácil de integrar, español nativo. Whisper queda para Fase 5 si se quiere más calidad |
| 2026-06-05 | Resumen IA: MediaPipe `tasks-genai:0.10.27` + Gemma 3 1B int4 (~530 MB) | LLM real en dispositivo; el extractivo (Plan B) solo devolvía fragmentos literales |
| 2026-06-05 | `ExtractiveSummarizer` por bloques de 40 palabras, no por oraciones | Vosk no produce puntuación; la regex `[.!?]` nunca cortaba y devolvía todo el texto |
| 2026-06-05 | Modelo Gemma desde mirror público no-gated (typosbro), no el repo oficial gated | Elimina token/licencia para el usuario: un solo botón "Descargar IA". Riesgo: depende de tercero (ver Fase 5) |
| 2026-06-06 | Fase 4 = Room en `data/db/` detrás de un `RecordingRepository`; ViewModel deja de usar lista en memoria | Único punto de acceso a BD; el dominio no conoce Room (igual patrón que ASR/LLM) |
| 2026-06-06 | Persistencia con Room vía **KSP** (no kapt) | KSP es el procesador recomendado/soportado para Room en Kotlin actual; kapt está en mantenimiento |
| 2026-06-06 | Robustez descarga: origen primario = **GitHub Release del repo**; HF/alphacephei como fallback | El repo es propio: no desaparece. CDN estable, hasta 2 GB, sin token. Mirrors de terceros pasan a respaldo |
| 2026-06-06 | Validar modelo por **SHA-256** (no por tamaño) + descarga reanudable + cadena de mirrors | El tamaño no detecta corrupción ni archivo cambiado; Range evita reiniciar 554 MB; fallback elimina el punto único de fallo |
| 2026-06-06 | Modelos alojados en GitHub Release `models-v1` (Gemma + Vosk); descarga sin token, `Accept-Ranges: bytes` | Origen primario propio; URLs y SHA-256 registrados en tarea 4.5.1 para cablear en 4.5.4 |
| 2026-06-06 | Room 2.8.4 + KSP 2.3.9 | Versiones estables verificadas en Maven; KSP 2.3.9 (esquema KSP2 independiente) construido contra Kotlin 2.3.20 del proyecto |
| 2026-06-06 | BD v1 con `fallbackToDestructiveMigration(dropAllTables=true)` y `exportSchema=false` | App aún sin publicar: si cambia el esquema, basta recrear la BD. ⚠️ Antes de publicar: exportar esquema y escribir migraciones reales para no borrar datos de usuarios |
