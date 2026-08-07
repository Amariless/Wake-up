# Wake up

App de Android (nativa, Kotlin + Jetpack Compose) que junta en un solo lugar:

- Organizador académico por **carpetas** (semestres o lo que quieras): materias con horarios por día/salón, tareas con fecha de vencimiento opcional y recordatorios configurables (1 semana + 1 día antes por defecto).
- **Reloj**: alarmas generales y por carpeta, temporizador y cronómetro, con **retos para apagarlas** (agitar el celular, resolver una cuenta, conectar puntos en orden, seguir una línea curva, escribir una frase) para que no las apagues dormido.
- Notificación **una hora antes** de cada alarma con botón "apagar solo esta vez".
- **Terminar un semestre**: desactiva todas sus alarmas y recordatorios de un toque, sin borrar el historial.
- 2 **widgets** de pantalla de inicio: próximas clases y próximas tareas.
- **Tiempo de pantalla** por app + avisos configurables (p. ej. "llevas 90 min en redes sociales").
- **Bloqueo tipo Scroll Guard**: limita minutos diarios de Reels de Instagram / TikTok sin tocar los mensajes directos.
- Asistente de **permisos**, con atención especial a lo estricto que es MIUI/Xiaomi con autoinicio y ventanas emergentes en segundo plano.

## Requisitos para abrir y correr el proyecto

Necesitas **Android Studio** (gratis, [descárgalo aquí](https://developer.android.com/studio)) — trae el JDK y buena parte del SDK de Android integrados, así que es la forma más simple de tener "modo dev en el PC".

Esta sesión ya generó y **verificó que el proyecto compila** (`gradlew assembleDebug` y `gradlew testDebugUnitTest`, ambos en verde) usando un JDK 17 y un Android SDK instalados por consola en `C:\Users\User\.android-dev-tools\` — eso confirma que el código es correcto, pero **no reemplaza tener Android Studio** para el día a día (emulador, Logcat, Live Edit, depuración visual).

## Cómo correrlo (modo dev)

1. Instala Android Studio y ábrelo.
2. `Open` → selecciona la carpeta `Wake up` (esta misma).
3. Espera el "Gradle sync" (la primera vez descarga dependencias, tarda unos minutos).
4. Crea un emulador: `Device Manager` → `Create device` → cualquier Pixel con **API 34 o 35**. (El *tiempo de pantalla* casi no genera datos reales en un emulador — para eso está el panel de desarrollador, ver abajo.)
5. Click en ▶ **Run** con el emulador (o tu Xiaomi por USB con "Depuración USB" activada) seleccionado.

### Probar en tu Xiaomi real

1. Activa "Opciones de desarrollador" (Ajustes → Acerca del teléfono → toca 7 veces "Versión de MIUI") y luego "Depuración USB".
2. Conecta el cable, acepta el diálogo de autorización en el teléfono.
3. Click en ▶ Run en Android Studio con tu teléfono seleccionado como destino.
4. La primera vez que abras la app, ve a **Ajustes → Permisos** dentro de la app y sigue el checklist — en MIUI hay 2 permisos extra que Android normal no tiene (ver más abajo).

### Panel de desarrollador

Solo existe en el build `debug` (no en `release`). Se abre desde **Ajustes → Panel de desarrollador** dentro de la app. Sirve para probar todo sin esperar horarios reales:

- **Disparar alarma ya mismo**: activa el flujo completo de alarma sonando (pantalla completa, reto de apagado) sin esperar la hora.
- **Simular notificación T-60**: prueba el aviso previo con el botón "apagar solo esta vez".
- **Poblar datos de ejemplo**: crea una carpeta demo con materia, horario, tarea y alarma.
- **Simular datos de uso de pantalla**: como los emuladores casi no generan `UsageStats` reales, esto inserta datos falsos para poder ver el dashboard de tiempo de pantalla funcionando.
- **Forzar overlay de bloqueo**: muestra el aviso de "límite diario alcanzado" sin tener que agotar minutos reales de Reels.
- **Inspector de nodos**: muestra en vivo los `resource-id` que el servicio de accesibilidad está viendo — útil para recalibrar el detector de Reels (ver "Limitaciones" abajo).

## Checklist de permisos (Xiaomi/MIUI)

MIUI es más agresivo que Android "puro" matando apps en segundo plano. La app tiene un asistente en **Ajustes → Permisos** que revisa y enlaza a cada uno, pero en resumen, para que las alarmas nunca fallen:

| Permiso | Por qué | Dónde |
|---|---|---|
| Notificaciones | Avisos de alarma, tareas, tiempo de pantalla | Ajustes de Android |
| Alarmas y recordatorios exactos | Que suenen a la hora exacta | Ajustes de Android (Android 12+) |
| Mostrar sobre otras apps | Aviso de bloqueo de Reels/TikTok | Ajustes de Android |
| Acceso a datos de uso | Medir tiempo de pantalla por app | Ajustes de Android |
| Servicio de accesibilidad | Detectar y limitar Reels/TikTok | Ajustes de Android |
| Ignorar optimización de batería | Que el sistema no mate la app | Ajustes de Android |
| **Autoinicio** | Que las alarmas se reprogramen solas tras reiniciar el teléfono | **Seguridad de MIUI** (solo Xiaomi) |
| **Mostrar ventanas emergentes en segundo plano** | Que la alarma se vuelva a abrir sola si la mandas a segundo plano sin apagarla | **Seguridad de MIUI** (solo Xiaomi) |

Los dos últimos son justo el tipo de permiso que, si falta, produce el bug de "la alarma se queda sonando en segundo plano y hay que buscar la app a mano" (el problema que mencionaste de "Shake it"). `RingingForegroundService` ya hace todo lo que puede desde el código (foreground service + wakelock + notificación de pantalla completa + un "watchdog" que reintenta reabrir la pantalla de alarma cada pocos segundos mientras suena), pero en MIUI específicamente, sin el permiso de "ventanas emergentes en segundo plano" el sistema puede bloquear ese reintento.

## Estructura del proyecto

```
app/src/main/java/com/fritangui/wakeup/
  data/           Room (entidades, DAOs, DB), repositorios, DataStore de settings
  domain/         Lógica pura y testeable (próximas clases/tareas, cálculo de alarmas, recordatorios)
  alarm/          AlarmManager, foreground services de alarma/temporizador, retos de apagado
  notifications/  Construcción centralizada de todas las notificaciones
  widget/         Widgets Glance (próximas clases, próximas tareas)
  usage/          UsageStatsManager + worker periódico de tiempo de pantalla
  blocking/       AccessibilityService + overlay del bloqueo tipo Scroll Guard
  permissions/    Chequeos e intents de permisos (incluye los específicos de MIUI)
  ui/             Pantallas Compose (carpetas, materias, tareas, reloj, ajustes, etc.)
  di/             Módulos de Hilt
app/src/test/     Unit tests de la lógica de dominio (domain/)
```

## Limitaciones conocidas (honestas)

- **El detector de Reels es una heurística**: busca `resource-id` conocidos de la interfaz de Instagram/TikTok. Si esas apps actualizan su UI, puede dejar de detectar correctamente hasta que se ajusten las listas en `blocking/ReelsNodeDetector.kt` — para eso está el "Inspector de nodos" del panel de desarrollador.
- El servicio de accesibilidad usado para el bloqueo **no cumple la política de Google Play** para ese uso (Play solo permite Accessibility Service para apps cuya función principal sea de accesibilidad real). Está pensado para instalar el APK directamente en tu teléfono, no para publicar en Play Store.
- El **cronómetro** vive en el ViewModel de la pantalla (no en un foreground service): sobrevive a rotar la pantalla y a navegar por la app, pero se reinicia si Android mata el proceso completo de la app en segundo plano por mucho tiempo. Alarmas y temporizador sí son 100% resistentes a esto porque corren en foreground service.
- No pude probar el flujo completo (alarma sonando sobre pantalla bloqueada, detección real de Reels, comportamiento exacto de MIUI) en un teléfono físico desde este entorno — sí verifiqué que **todo el proyecto compila limpio** (`gradlew assembleDebug`) y que la lógica pura tiene tests unitarios en verde (`gradlew testDebugUnitTest`). La primera vez que lo abras en Android Studio y lo corras en tu Xiaomi, revisa sobre todo: que la alarma suene con la pantalla bloqueada, y que el detector de Reels reconozca la versión de Instagram que tengas instalada.

## Generar un APK para instalar directo (sideload)

Desde Android Studio: `Build` → `Build App Bundle(s) / APK(s)` → `Build APK(s)`. El archivo queda en `app/build/outputs/apk/debug/`. Cópialo al teléfono e instálalo (activa "Instalar apps de fuentes desconocidas" para el explorador de archivos que uses).

## Ideas para seguir mejorando

- Snooze configurable (hoy es fijo a 5 min) y sonidos de alarma personalizados desde un selector nativo.
- Historial/estadísticas semanales de tiempo de pantalla (hoy solo muestra el día actual).
- Extender el detector de bloqueo a más apps (YouTube Shorts, X/Twitter) siguiendo el mismo patrón de `ReelsNodeDetector`.
- Publicar el detector de nodos como una lista remota configurable (JSON) para no depender de una actualización de la app cada vez que Instagram cambie su UI.
