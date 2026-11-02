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
   Keys are embedded in `BuildConfig` and can be extracted from any APK, including
   release builds. Do not distribute a build containing personal keys; use a
   backend proxy for production credentials.

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

- **Import PlantDoc samples** adds 25 labelled photos to History, skipping
  existing samples. Imported labels come from the dataset and have 0% confidence;
  importing does not run AI. Open a sample from History and tap **Run on-device AI
  again** to save a new offline prediction and model score. The original sample
  stays unchanged for comparison; this action does not call cloud services.
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

## Python model evaluation tools

Python is used for offline evaluation and Android test-fixture generation; the
Android app runs inference through LiteRT and does not require Python.

Use Python 3.12 and an isolated environment from the repository root:

```bash
python3.12 -m venv /tmp/floralens-audit-venv
/tmp/floralens-audit-venv/bin/pip install -r tools/model_audit/requirements.txt

# Evaluate the bundled model on the debug gallery without changing app records.
/tmp/floralens-audit-venv/bin/python tools/model_audit/benchmark.py \
  --samples app/src/debug/assets/plantdoc \
  --output /tmp/floralens-gallery-results.json

# Download and evaluate the larger, five-class diagnostic subset.
/tmp/floralens-audit-venv/bin/python tools/model_audit/benchmark.py \
  --download /tmp/floralens-plantdoc-benchmark \
  --output /tmp/floralens-plantdoc-results.json
```

`benchmark.py` records the model hash, preprocessing profile, top three predictions,
inference time, per-image errors, and overall/per-class label agreement. Check the
per-image errors as well as the summary: failed images are not counted in the
summary totals. Desktop timings do not measure phone performance, and Pillow
decoding/resizing does not establish Android image-pipeline parity. The debug
gallery and downloaded subset are different sample sets; compare models on the
same photos and preprocessing contract.

`generate_reference.py` regenerates tracked instrumentation assets under
`app/src/androidTest/assets/model_audit/`, using a synthetic RGB image and the
first five entries in the debug sample manifest:

```bash
/tmp/floralens-audit-venv/bin/python tools/model_audit/generate_reference.py
```

Review the generated JSON and PNG changes before committing. These fixtures
check runtime agreement on prepared pixels; they do not measure recognition
accuracy. Keep evaluation photos separate from any data used to train models or
choose confidence thresholds.

### Updating the recognition model

Treat model weights, output labels, and preprocessing as one versioned contract:

1. Record the source revision, license, and SHA-256 alongside the bundled model.
2. Verify tensor shapes/types, RGB scaling, resize/crop rules, and every output
   index. Update canonical labels and the plant catalog together, preserving the
   meaning of labels stored in existing scans.
3. Run the Python comparison on the same diagnostic photos and inspect class
   regressions and incorrect high-score predictions, not only the overall score.
4. Regenerate reference fixtures, run unit tests, and run the model reference,
   preprocessing, and evaluation instrumentation tests on a device. See the
   [migration report](docs/plantulus-migration.md) for commands and recorded results.
5. Review cascade thresholds and advice coverage for new classes. The current
   0.80 confidence / 0.15 margin rules are not calibrated for Plantulus.
6. Check camera and gallery flows manually, including offline use and unavailable
   cloud services, and document the results and remaining limitations.

## Proposed UI improvements

These are follow-up ideas, not features already implemented:

- **Clearer History:** replace implementation wording such as “saved in ObjectBox”
  with a simple scan count; distinguish an empty history from a filter with no
  matches; add search by plant or diagnosis.
- **Layouts for smaller screens and larger text:** make History filters scroll
  horizontally, allow result badges to wrap, and let long result/advice content
  scroll while keeping actions reachable.
- **A clearer result hierarchy:** emphasize the plant, possible condition, and
  next step; explain model scores in plain language and distinguish on-device,
  cloud-assessed, and user-corrected results without relying on color alone.
- **Better photo guidance:** provide concise camera tips for lighting, focus,
  and leaf framing, with an easy path to try another photo.
- **More useful plant tracking:** make changes across scans easier to compare
  with dated photos and a readable progression view.

Start with History wording/empty states and adaptable result layouts, then review
camera, result, and History screens on a device at larger font sizes. Those changes
can improve everyday use without changing model behavior.

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

## Language support

The v1 interface is English-only. Android string resources are used for the
scan-progress and unsupported-result screens; other screens and domain advice
still contain English literals. Full string extraction and translation remain
localization work; Android lint alone does not detect every Compose text literal.

## Status

v1.0 in development. Known open items: per-disease advice content, Trefle.io care
tips, history search, general settings, and improving field-photo model accuracy.
A debug-only settings screen is available for sample import and model evaluation.
