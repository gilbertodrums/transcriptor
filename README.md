# Transcriptor — App Android de transcripción y resumen de audio (100% en el dispositivo)

Este repositorio contiene la **especificación y el arnés de trabajo** para construir, con ayuda de un
agente de IA (Claude Code, Cursor, etc.), una aplicación Android que graba audio (clases, reuniones,
notas), lo **transcribe y resume usando modelos que corren localmente en el teléfono**, sin enviar
nunca los datos del usuario a internet.

> Estos documentos NO son código todavía. Son el "cerebro" que el agente debe leer **antes** de
> escribir una sola línea. Léelos tú también: son tuyos, edítalos cuando una decisión cambie.

## Cómo usar este arnés con un agente de IA

1. Abre el proyecto con tu agente (p. ej. Claude Code) en la raíz de esta carpeta.
2. Dile, literalmente: **"Lee `AGENTS.md`, `docs/PRD.md` y `docs/ARCHITECTURE.md` antes de hacer nada.
   Luego abre `docs/ROADMAP.md` y trabajemos SOLO la fase actual, una tarea a la vez."**
3. El agente sigue las reglas de `AGENTS.md` (el arnés), avanza por `docs/ROADMAP.md` fase por fase,
   y tú apruebas cada paso antes de seguir.

## Mapa de documentos

| Archivo | Para qué sirve | Quién lo usa más |
|---|---|---|
| `README.md` | Este mapa. Punto de entrada. | Tú |
| `AGENTS.md` | **El arnés.** Reglas, límites y forma de trabajar del agente. | El agente |
| `docs/PRD.md` | Qué construimos y qué NO. Alcance y criterios. | Tú y el agente |
| `docs/ARCHITECTURE.md` | Decisiones técnicas: stack, capas, modelos locales. | El agente |
| `docs/ROADMAP.md` | Fases de construcción con criterios de aceptación. | Tú y el agente |
| `docs/TASKS.md` | Backlog detallado de la fase actual. | El agente |

## Principio rector (no negociable)

**Los datos del usuario (audio y texto) nunca salen del teléfono.** Toda la transcripción y el
resumen ocurren en el dispositivo. Esto define la arquitectura, simplifica el cumplimiento legal
con Google Play y es nuestra principal promesa de valor.

## Estado actual

- [ ] Fase 0 — Esqueleto del proyecto (la app vacía compila y abre)
- [ ] Fase 1 — Grabar audio y guardarlo
- [ ] Fase 2 — Transcripción local → mostrar texto
- [ ] Fase 3 — Resumen local del texto
- [ ] Fase 4 — Biblioteca de grabaciones (persistencia)
- [ ] Fase 5 — Pulido y servicios extra

> El agente actualiza estas casillas al cerrar cada fase.
