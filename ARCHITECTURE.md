# ARCHITECTURE — Decisiones técnicas

> Regla para el agente: las elecciones de librerías marcadas como **(VERIFICAR)** debes confirmarlas
> contra la documentación oficial vigente antes de usarlas, porque el ecosistema de IA en dispositivo
> cambia rápido. Si la opción recomendada está deprecada o cambió, propón la alternativa y pregunta.

## Stack base

- **Lenguaje:** Kotlin.
- **UI:** Jetpack Compose.
- **Patrón:** MVVM (UI ↔ ViewModel ↔ casos de uso ↔ data).
- **minSdk:** 26 (Android 8.0). **targetSdk/compileSdk:** la última estable (VERIFICAR).
- **Build:** Gradle con Kotlin DSL (`build.gradle.kts`).
- **Inyección de dependencias:** empezar simple (manual o Hilt si se justifica; preguntar antes de
  meter Hilt).

## Las cuatro capas y su responsabilidad

```
ui/        Pantallas Compose + ViewModels. No sabe NADA de cómo se transcribe; solo pide y muestra.
domain/    Modelos (Recording, Transcript, Summary) y casos de uso (GrabarAudio, Transcribir, Resumir).
data/      Implementaciones reales:
           - audio/      grabación con el micrófono
           - asr/        motor de transcripción local (speech-to-text)
           - llm/        motor de resumen local
           - db/         persistencia con Room
           - files/      manejo de archivos de audio en almacenamiento privado
```

Beneficio: si mañana cambias el motor de transcripción, solo tocas `data/asr/`. La UI y el dominio
no se enteran. Esto es clave para que el agente no rompa todo al iterar.

## Grabación de audio

- Para transcripción se suele necesitar audio crudo PCM (16 kHz, mono) que los modelos esperan.
- Candidatos: `AudioRecord` (control fino del PCM, recomendado para alimentar el modelo) o
  `MediaRecorder` (más simple pero formato comprimido). **Recomendado: `AudioRecord`** para tener
  el PCM listo para el modelo. (VERIFICAR detalles de API actuales.)
- Permiso requerido: `RECORD_AUDIO`, pedido en tiempo de ejecución.

## Transcripción local (ASR / speech-to-text) — el corazón técnico

Necesitamos un motor que corra **offline en el dispositivo**. Opciones candidatas, de mayor a menor
calidad/peso:

1. **Whisper (modelo de OpenAI) corriendo localmente** vía un puerto en C++ (`whisper.cpp` / ggml)
   con un wrapper para Android (JNI), o vía una versión TFLite. Mejor calidad; más peso y trabajo de
   integración. **(VERIFICAR** el wrapper/binding actual recomendado para Android y el tamaño del
   modelo; empezar con un modelo pequeño tipo "tiny" o "base" para validar el flujo.)
2. **Vosk** — librería de reconocimiento de voz offline, más liviana y fácil de integrar, calidad
   algo menor. Buena para validar rápido el MVP. **(VERIFICAR** versión y modelos de idioma.)

**Decisión recomendada para empezar:** integrar **uno solo** primero (Vosk si quieres validar el
flujo rápido; Whisper si priorizas calidad desde el inicio). NO integrar los dos a la vez. La capa
`data/asr/` debe exponer una interfaz `SpeechRecognizer` propia para poder cambiar de motor sin tocar
el resto.

## Resumen local (LLM en dispositivo) — Fase 3

Resumir texto en el teléfono requiere un modelo de lenguaje pequeño local. Candidatos:

1. **MediaPipe LLM Inference API** corriendo un modelo pequeño (familia Gemma) en el dispositivo.
   (VERIFICAR API, modelos soportados y requisitos de RAM.)
2. **ML Kit GenAI / Gemini Nano** en equipos que lo soporten (Pixel y algunos Samsung recientes).
   Limitado a ciertos dispositivos. (VERIFICAR disponibilidad y API.)

**Plan B obligatorio (definido en el PRD):** en teléfonos que no soporten un LLM local decente, el
"resumen" de la v1 puede ser **extractivo y sin modelo pesado** (p. ej. seleccionar frases clave por
heurística sencilla) y marcarse como "resumen básico". Nunca caer a un servicio de nube. La capa
`data/llm/` expone una interfaz `Summarizer` con al menos dos implementaciones intercambiables.

## Persistencia

- **Room** (SQLite) para metadatos: id, título, fecha, ruta del audio, transcripción, resumen.
- Archivos de audio en **almacenamiento privado de la app** (no en almacenamiento compartido), para
  que ninguna otra app los lea.
- Nada de bases de datos en la nube. Nada de Firebase con datos de usuario.

## Red

- **No hay capa de red para datos del usuario.** Única excepción permitida y previa pregunta: la
  descarga inicial del archivo del modelo de IA desde una fuente confiable, **una sola vez**, sin
  enviar nada del usuario. Documentar esto explícitamente si se hace.

## Privacidad y cumplimiento (atado al diseño)

- Declarar `RECORD_AUDIO` en el manifiesto con su justificación.
- En la ficha de Google Play, poder marcar honestamente "no se recopilan datos".
- Incluir una política de privacidad simple (puede ser una página estática gratuita) que diga que el
  procesamiento es local.
- Recordar el requisito de Google Play para cuentas personales: prueba cerrada con testers antes de
  publicar (ver `ROADMAP.md`, Fase 5).

## Lo que NO usamos (para no desviarnos)

- Sin backend propio. Sin servidores. Sin login. Sin analítica de contenido. Sin SDKs de nube que
  procesen el audio.
