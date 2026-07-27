# Freno

App Android **anti-dopamina** para reducir el uso de redes sociales y apps adictivas. Estricta pero muy
personalizable: sistema de **tokens** (presupuesto diario), reglas por app (tiempo, cooldown, horario,
aperturas) y **bloqueo del scroll infinito en Reels/Shorts** (cuota configurable). UI minimalista en escala
de grises. Todo funciona **100% en tu teléfono**, sin servidores.

> ⚠️ Realidad técnica: Android no permite "solo ejecutar código". Hay que **compilar un APK** e instalarlo,
> y conceder permisos especiales (Accesibilidad, Superposición). El bloqueo no "mata" otras apps: detecta la
> app/función en primer plano y muestra una pantalla de bloqueo. Sin root, ningún bloqueador es imposible de
> saltar; la rigidez "media" añade fricción (PIN + periodo de reflexión).

---

## Cómo obtener el APK (sin instalar nada en el PC)

El repositorio incluye un flujo de **GitHub Actions** que compila el APK automáticamente.

1. Crea una cuenta en [github.com](https://github.com) (si no tienes).
2. Crea un repositorio nuevo (privado o público) y **sube este proyecto**. Desde esta carpeta:
   ```bash
   git init
   git add .
   git commit -m "Freno inicial"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
   git push -u origin main
   ```
3. En GitHub, abre la pestaña **Actions**. Verás el workflow **build-apk** ejecutándose (~3–6 min).
4. Al terminar, entra al run y descarga el artifact **`freno-debug-apk`** (contiene `app-debug.apk`).
   - Para descargarlo directo al teléfono: crea un tag de versión y usa el **Release**:
     ```bash
     git tag v1.0
     git push origin v1.0
     ```
     Esto adjunta el `app-debug.apk` a un Release; ábrelo desde el navegador del teléfono y descárgalo.

## Instalar en el teléfono

1. Copia/descarga `app-debug.apk` al teléfono.
2. Ábrelo; Android pedirá permitir **instalar apps de orígenes desconocidos** para tu navegador/archivos → acéptalo.
3. Instala y abre **Freno**.

## Primer uso (onboarding)

La app te guía a conceder:
- **Accesibilidad** (obligatorio) — detecta la app en primer plano y aplica el bloqueo.
- **Superposición** (obligatorio) — muestra la pantalla de bloqueo sobre otras apps.
- **Notificaciones** (recomendado) — notificación del servicio de monitoreo.
- **Batería sin restricciones** (recomendado) — evita que el sistema detenga el monitoreo.
- **PIN** — protege los cambios de reglas.

Luego, desde el **Dashboard** → **Agregar**, elige apps o funciones (Reels/Shorts) y ajusta sus reglas.

## Cómo funciona

- **Tokens (presupuesto global):** cada día tienes un presupuesto (def. 60). Abrir una app cuesta tokens y
  cada minuto consume tokens (costos configurables por app). Al llegar a 0, se bloquean todas las apps
  monitoreadas hasta el reinicio diario.
- **Reglas duras por app:** horario, cooldown, límite de aperturas y de tiempo. Cualquiera que se cumpla
  bloquea la app, incluso si quedan tokens.
- **Reels/Shorts:** al detectar el feed de scroll infinito, se cuentan los swipes. Tras la cuota (def. 5),
  el feed se bloquea durante la ventana de uso (hasta el reinicio, o unas horas si lo configuras). El resto
  de la app sigue usable.
- **Rigidez media:** apretar restricciones se aplica al instante; aflojarlas (subir presupuesto, quitar una
  app, relajar una regla) entra en cola y se aplica tras el **periodo de reflexión** (def. 120 min). Todo
  cambio requiere el PIN.
- **Widget:** añade el widget de Freno a tu pantalla de inicio para ver tokens restantes, cuenta atrás al
  reinicio y nº de apps bloqueadas.

## Compilar localmente (alternativa)

Abre la carpeta en **Android Studio** (Giraffe o superior). Android Studio genera el wrapper de Gradle y
descarga el SDK necesario. Luego *Run* sobre un dispositivo/emulador, o *Build > Build APK(s)*.

Stack: Kotlin · Jetpack Compose · Room · WorkManager · minSdk 26 · targetSdk 34 · Gradle 8.9 · AGP 8.5.2.

## Limitaciones conocidas

- Los detectores de Reels/Shorts dependen de identificadores internos de YouTube/Instagram, que cambian con
  las actualizaciones de esas apps. Las firmas de detección están sembradas por defecto y viven en la base
  de datos local (`feature_signatures`), pensadas para poder actualizarse.
- No se puede desactivar el gesto de swipe en sí; se detecta el feed y se bloquea tras la cuota.
- El reinicio diario y los cambios diferidos se aplican de forma perezosa (al usar la app y cada ~15 min por
  un worker de respaldo), no con precisión de reloj al segundo.
- APK **debug**: ideal para uso personal por sideload; no está firmado para Play Store.

## Estructura

```
app/src/main/java/com/freno/app/
  core/       Notifications, PermissionsHelper
  data/       Room (entities, dao), repo/AppRepository, prefs/SettingsStore, FeatureCatalog, TargetJson
  domain/     BlockPolicy, Restrictiveness, model/, util/TimeUtils
  service/    MonitorAccessibilityService, MonitoringService, detect/FeatureDetector
  ui/         MainActivity, BlockActivity, MainViewModel, screens/, components/, theme/
  widget/     StatusWidgetProvider
  work/       HeartbeatWorker, BootReceiver
.github/workflows/build.yml   → compila el APK
```
