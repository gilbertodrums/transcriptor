# ROADMAP — Fases de construcción

> El agente trabaja **una fase a la vez** y, dentro de ella, **una tarea a la vez** (ver `docs/TASKS.md`).
> No se empieza una fase hasta que la anterior cumple sus criterios de aceptación y el humano aprueba.

---

## Fase 0 — Esqueleto

**Meta:** un proyecto Android vacío que compila y abre mostrando una pantalla "Hola".

**Criterios de aceptación:**
- `./gradlew assembleDebug` pasa.
- La app abre en emulador/dispositivo y muestra una pantalla Compose con un texto.
- La estructura de paquetes `ui/ domain/ data/` existe (aunque casi vacía).

**Fuera de alcance:** audio, permisos, IA. Nada todavía.

---

## Fase 1 — Grabar y guardar audio (el "hola mundo" real)

**Meta:** poder grabar desde el micrófono y guardar el archivo; verlo en una lista.

**Criterios de aceptación:**
- Se pide el permiso `RECORD_AUDIO` correctamente en tiempo de ejecución.
- Botón Grabar/Detener funcional.
- Al detener, el audio queda guardado en almacenamiento privado.
- Una lista muestra las grabaciones (al menos su fecha/nombre).
- Reproducir una grabación para confirmar que el audio sirve.

**Fuera de alcance:** transcripción. Solo capturar y guardar bien el audio.

---

## Fase 2 — Transcripción local

**Meta:** convertir una grabación en texto, en el dispositivo, sin red.

**Criterios de aceptación:**
- Integrado **un** motor de ASR local (ver `ARCHITECTURE.md`) detrás de la interfaz `SpeechRecognizer`.
- Desde una grabación, se genera la transcripción y se muestra en pantalla.
- Funciona en **modo avión** (prueba clave de que es local).
- Pasos de prueba manual claros, ejecutados en dispositivo físico.

**Fuera de alcance:** resumen, puntuación perfecta, varios idiomas. Texto crudo aceptable.

---

## Fase 3 — Resumen local

**Meta:** a partir de la transcripción, generar un resumen en puntos, en el dispositivo.

**Criterios de aceptación:**
- Implementada la interfaz `Summarizer` con la opción de LLM local **y** el plan B extractivo.
- En un equipo sin soporte de LLM local, la app degrada con elegancia al resumen básico (no falla,
  no usa nube).
- Resumen visible junto a la transcripción.

**Fuera de alcance:** preguntas de estudio, detección de tareas (eso es backlog futuro).

---

## Fase 4 — Biblioteca y persistencia

**Meta:** que todo sobreviva al cerrar la app.

**Criterios de aceptación:**
- Room guarda metadatos, transcripción y resumen.
- La lista de grabaciones persiste entre reinicios.
- Abrir una grabación muestra su audio, transcripción y resumen.
- Poder borrar una grabación (y su archivo de audio).

---

## Fase 5 — Pulido y publicación

**Meta:** dejarlo presentable y listo para testers reales.

**Criterios de aceptación:**
- Estados vacíos, errores y carga bien manejados (p. ej. "transcribiendo…", "sin grabaciones aún").
- Íconos, nombre, textos revisados.
- Política de privacidad enlazada.
- Preparado el flujo de **prueba cerrada de Google Play** (recordar el requisito de testers para
  cuentas personales) y la ficha con "no se recopilan datos".

**Backlog futuro (NO aquí):** búsqueda en transcripciones, exportar PDF, preguntas de estudio,
versión iOS.
