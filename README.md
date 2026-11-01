# Flora Lens

Android app that diagnoses plant diseases from a leaf photo. Scans are classified
on-device by a TFLite model; only when the on-device verdict is unsure does the
app spend cloud calls on second opinions.

## The cascade

1. **On-device** — the 54-class Plantulus Doctor model (19 plant groups) runs via LiteRT.
   A verdict is trusted outright only when confidence > 0.80 **and** the runner-up
   is > 0.15 behind; otherwise the scan is flagged for cloud help.
2. **Species check (Pl@ntNet)** — free identification. If the plant is one of the
   unsupported species (e.g. Banana, Avocado), the scan is rewritten as a v2.0
   wishlist entry; the user can still request an on-demand Plant.id health check.
3. **Disease check (Plant.id)** — a health assessment confirms or corrects the
   on-device disease verdict. It can also be re-run manually from the result screen.

Every cloud stage is optional: with no API keys or offline, scans complete fully
on-device and the cloud steps silently no-op.

## Features

- Camera scanning (CameraX) and gallery import, photos stored on disk with EXIF handling
- Inline second opinions on the result screens, including for unsupported species
- User corrections: override the species/diagnosis, marked as corrected in history
- Scan history with filters (all / supported / wishlist / cloud-verified)
- "My Plants": track a species across scans to follow disease progression
- "Notify me when supported" wishlist feeding v2.0 prioritization

## Tech stack

Kotlin 2.2, Jetpack Compose (Material 3), Navigation Compose, CameraX, LiteRT
(TFLite) 1.4.2, Ktor 3.3 client, ObjectBox 5.4, Koin 4. AGP 9 / Gradle 9,
minSdk 24, targetSdk 37.

## Setup

1. Open in a current Android Studio (or build with `./gradlew`).
2. Optional API keys in `local.properties` (gitignored). Obtain a species key from
   [Pl@ntNet](https://my.plantnet.org/) and a plant.id health-assessment key from
   [Kindwise](https://admin.kindwise.com/). Provider quotas and credits apply:

   ```
   PLANTNET_API_KEY=...
   PLANT_ID_API_KEY=...
   ```

   Rebuild and reinstall after changing keys; they are read at build time.

3. The model ships in the repo at `app/src/main/assets/plant_disease_model.tflite`
   — no extra download needed.

## Build & test

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests (JUnit + MockK, no emulator needed)
./gradlew ktlintCheck            # lint/format gate
```

## Debug tools and model validation

In a debug build, open **⋮ → Settings (debug)**:

- **Import PlantDoc samples** adds five labelled photos to History, skipping
  existing samples. Imported labels come from the dataset and have 0% confidence;
  importing does not run AI.
- **Run on-device AI on samples** runs the real offline model and displays its
  predictions, confidence scores, and agreement with expected labels. It does not
  change the imported History records or call cloud services.
- **Remove PlantDoc samples** removes the fixture records and their photos.

The tools and sample images are excluded from release builds. Sources, attribution
and usage instructions are in [PlantDoc test data](docs/plantdoc-test-data.md).

The current offline model is **Plantulus Doctor (EfficientNetV2-B0, FP16 weights)**.
It requires RGB float32 pixels in **0–255**, shorter-side resize to 256, and a
central 224×224 crop. Its 54 outputs are mapped into stable app labels so existing
scan records remain readable. New groups include Aloe, Cactus, Money Plant, Snake
Plant and Spider Plant; their condition classes do not yet have reviewed treatment
advice. Cloud scientific-name aliases cover selected representative species, not
every species in these broad plant groups.

On the same small 43-photo PlantDoc subset, Plantulus matched **17/43 (39.5%)**,
versus **14/43 (32.6%)** for the previous MobileNetV2 model. Incorrect predictions
scoring above 80% fell from 12 to 4. These are diagnostic measurements across five
classes, not a representative accuracy guarantee; high scores can still be wrong.
See [replacement details and validation](docs/plantulus-migration.md), the
[Plantulus benchmark](docs/plantulus-benchmark-results.json), and the
[historical MobileNet audit](docs/model-audit.md).

The model is from [KumarjitC/plantulus-doctor](https://huggingface.co/KumarjitC/plantulus-doctor),
with MIT declared in its model card. Its pinned revision, SHA-256, upstream label
list, preprocessing instructions and model card are bundled in
`app/src/main/assets/plantulus/`.

To run the Android reference comparison on a connected device:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.arbon.vata.floralens.testdata.ModelReferenceTest
```

The separate `PlantDocEvaluationTest` checks evaluation logic and bitmap cleanup;
`PlantulusPreprocessingTest` checks RGB scaling, crop placement and panorama memory use.
Current verification status is recorded in [the migration report](docs/plantulus-migration.md).
If installation reports `INSTALL_FAILED_USER_RESTRICTED`, enable **Install via
USB** where available and accept the phone's installation prompt. Confirm actual
test results; this Gradle runner can report build success after installation fails.

## Project structure

Single module, feature-sliced under `app/src/main/java/com/arbon/vata/floralens/`:

| Package | Contents |
| --- | --- |
| `domain/classifier` | Cascade gate, canonical model labels (54 classes) |
| `domain/species` | Pl@ntNet species-identification contract |
| `domain/diagnosis` | Plant.id health-assessment contract |
| `data/plantnet`, `data/plantid` | Ktor clients and DTOs |
| `data/photos` | Scan photo storage and decoding |
| `data/objectbox` | `PlantRecord` entity and model |
| `data/repository` | Persistence + photo write path |
| `presentation` | Compose screens, nav graph, `PlantViewModel` |
| `di` | Koin module |

## Status

v1.0 in development. Known open items: per-disease advice content, Trefle.io care
tips, history search, general settings, and improving field-photo model accuracy.
A debug-only settings screen is available for sample import and model evaluation.
