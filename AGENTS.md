# AGENTS.md — Arnés de trabajo para el agente de IA

Este archivo define **cómo debes trabajar** en este proyecto. Tiene prioridad sobre cualquier
impulso de "avanzar rápido". Si una instrucción del usuario contradice una regla de seguridad de
aquí, **detente y pregunta**.

---

## 1. Reglas de oro (nunca las rompas)

1. **Todo en el dispositivo.** No agregues ninguna llamada de red que envíe audio, transcripciones
   o datos del usuario a un servidor. Sin analítica que filtre contenido, sin APIs de nube para
   procesar el audio. Si crees que algo necesita red, **detente y pregunta primero**.
2. **Sin secretos ni llaves.** Este proyecto no usa API keys. Si propones algo que las necesite,
   es señal de que te saliste del diseño: detente.
3. **Una tarea a la vez.** Toma la siguiente tarea sin terminar de `docs/TASKS.md`, complétala,
   verifícala, repórtala, y **espera aprobación** antes de la siguiente.
4. **No inventes dependencias ni versiones.** Antes de añadir cualquier librería, verifica su
   nombre, versión estable actual y API real en su documentación oficial. La tecnología de IA en
   dispositivo cambia rápido; lo que "recuerdas" puede estar desactualizado. Si no puedes
   verificarlo, dilo y pregunta.
5. **Nunca dejes el build roto.** Cada tarea termina con el proyecto compilando. Si no compila,
   no está hecha.
6. **No borres ni reescribas trabajo aprobado** sin explicar por qué y pedir confirmación.

---

## 2. Flujo obligatorio por cada tarea

Sigue SIEMPRE este ciclo:

1. **Leer contexto.** Relee la tarea en `docs/TASKS.md` y la fase activa en `docs/ROADMAP.md`.
   Si la tarea toca decisiones técnicas, relee `docs/ARCHITECTURE.md`.
2. **Plan corto.** Antes de codificar, escribe en 3–6 líneas qué archivos vas a crear/editar y por
   qué. Si hay ambigüedad o más de una forma razonable de hacerlo, **pregunta antes de elegir**.
3. **Implementar el corte más pequeño posible** que cumpla la tarea. Nada de "ya que estoy, agrego
   tres cosas más". Mínimo viable y funcional.
4. **Verificar** (ver sección 4). El build debe pasar y debes describir cómo probarlo a mano.
5. **Reportar** (ver sección 5) y esperar luz verde.

---

## 3. Definición de "Hecho" (Definition of Done)

Una tarea solo está hecha si TODO esto se cumple:

- [ ] El proyecto compila: `./gradlew assembleDebug` termina sin errores.
- [ ] El lint no introduce errores nuevos: `./gradlew lint` (los avisos preexistentes no cuentan,
      pero no agregues nuevos sin justificar).
- [ ] La app abre y la función nueva se puede probar siguiendo los **pasos manuales** que tú redactas.
- [ ] El código respeta la estructura y el estilo de la sección 6.
- [ ] No se introdujo ninguna llamada de red sobre datos del usuario.
- [ ] Actualizaste el estado en `docs/TASKS.md` (marcar la tarea) y, si cerraste una fase, en
      `README.md` y `docs/ROADMAP.md`.

---

## 4. Cómo verificar

- **Compilar:** `./gradlew assembleDebug`
- **Lint:** `./gradlew lint`
- **Tests (cuando existan):** `./gradlew testDebugUnitTest`
- **Prueba manual:** describe los pasos exactos en el emulador o teléfono real, p. ej.:
  "1) Abrir app. 2) Pulsar Grabar. 3) Hablar 5 s. 4) Pulsar Detener. 5) Verificar que aparece el
  archivo en la lista." Si una función depende de hardware (micrófono) o de modelos pesados,
  prueba en **dispositivo físico**, no solo emulador, y dilo.

> Tú no tienes acceso al micrófono ni puedes "ver" la pantalla del usuario. Por eso, cuando no
> puedas verificar algo tú mismo, entrega **pasos de prueba claros** para que el humano lo confirme.

---

## 5. Cómo reportar al terminar una tarea

Responde siempre con este formato breve:

- **Qué hice:** 1–2 frases.
- **Archivos tocados:** lista.
- **Cómo probarlo:** pasos manuales numerados.
- **Decisiones o dudas:** cualquier suposición que hiciste o cosa que conviene revisar.
- **Siguiente sugerido:** cuál sería la próxima tarea.

Luego **detente y espera**.

---

## 6. Convenciones de código y estructura

- **Lenguaje:** Kotlin. **UI:** Jetpack Compose. **Arquitectura:** MVVM con capas separadas.
- **Estructura de paquetes** (dentro de `app/src/main/java/com/<tuorg>/transcriptor/`):
  - `ui/` — pantallas y componentes Compose, ViewModels.
  - `domain/` — lógica de negocio y modelos (sin dependencias de Android donde se pueda).
  - `data/` — grabación de audio, acceso a modelos de IA, persistencia (Room), almacenamiento de archivos.
- Funciones cortas y con nombre claro. Comenta el **porqué**, no el qué.
- Strings de UI en `res/values/strings.xml` (nunca texto "quemado" en el código).
- Cada permiso del sistema (p. ej. micrófono) se pide en tiempo de ejecución y se declara en el
  `AndroidManifest.xml` con su justificación.
- No subas binarios de modelos pesados al repo sin avisar; documenta de dónde se descargan.

---

## 7. Cuándo DETENERTE y preguntar

- La tarea es ambigua o admite varias soluciones razonables.
- Necesitarías agregar red, una API key, o un permiso nuevo no contemplado.
- Una librería que pensabas usar está deprecada, cambió de API, o no puedes verificar su versión.
- Algo te obligaría a romper una Regla de Oro.
- El usuario te pide saltarte una fase o una verificación.

Preguntar no es fallar. Avanzar a ciegas, sí.
