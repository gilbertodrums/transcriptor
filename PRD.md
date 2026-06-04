# PRD — Documento de Requisitos del Producto

## Visión en una frase

Una app Android que graba audio y, **sin conexión y sin que los datos salgan del teléfono**, lo
transcribe a texto y genera un resumen con los puntos clave.

## El problema

Estudiantes y profesionales graban clases y reuniones, pero rara vez vuelven a escucharlas porque
revisar audio es lento. Las apps que transcriben suelen **subir el audio a la nube**, lo que cuesta
dinero, preocupa por privacidad y requiere internet.

## Nuestra solución y diferenciador

Transcripción y resumen **100% en el dispositivo**. Promesa: *"Tu voz nunca sale de tu teléfono."*
Esto nos da privacidad real, funcionamiento sin internet, costo de servidor cero y una historia de
confianza clara en Google Play (podemos declarar honestamente que no recopilamos datos).

## Usuario objetivo (para empezar)

Estudiantes universitarios que graban clases. Un solo tipo de usuario al inicio; nada de cuentas,
nada de login, nada de social.

## Alcance del MVP (lo que SÍ hace la versión 1)

1. Grabar audio desde el micrófono (iniciar / detener).
2. Guardar cada grabación en el almacenamiento privado de la app.
3. Transcribir la grabación a texto, localmente.
4. Mostrar el texto en pantalla.
5. Generar un resumen en puntos del texto, localmente.
6. Una lista simple de grabaciones pasadas, con su transcripción y resumen.

## Fuera de alcance (lo que la versión 1 NO hace)

- Cuentas de usuario, login, o sincronización entre dispositivos.
- Cualquier subida a la nube o backup remoto.
- iOS (vendrá después; este repo es solo Android).
- Edición de audio, traducción, identificación de hablantes ("quién dijo qué").
- Compartir, exportar a otras apps (se evaluará en Fase 5).
- Monetización (gratis al inicio).

> Mantener esta lista corta es lo que hace el proyecto terminable. Toda idea nueva va a un
> "Backlog futuro", no al MVP.

## Criterios de éxito del MVP

- Un usuario puede grabar una clase de ~30 min y obtener una transcripción utilizable y un resumen,
  **en modo avión**, en un teléfono Android de gama media razonable.
- La app nunca hace una petición de red con datos del usuario (verificable).

## Restricciones conocidas (honestas)

- La **calidad de transcripción** local depende del modelo elegido y del ruido ambiente; no será
  perfecta. Es aceptable un margen de error; se mejora iterando.
- El **resumen local** depende de modelos de lenguaje en dispositivo, que son más limitados y solo
  funcionan bien en teléfonos relativamente recientes. La Fase 3 define un plan B para equipos que
  no los soporten (ver `ARCHITECTURE.md`).
- Audios largos consumen batería y tiempo de CPU. Hay que medir y, si hace falta, transcribir por
  segmentos.

## Backlog futuro (ideas para después del MVP, NO ahora)

- Búsqueda dentro de las transcripciones.
- Exportar a PDF o a la app de notas.
- Generar preguntas de estudio a partir del resumen.
- Detección de tareas/fechas mencionadas.
- Versión iOS reutilizando las decisiones de este proyecto.
