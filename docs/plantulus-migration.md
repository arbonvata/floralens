# Plantulus replacement

Floralens now bundles Plantulus Doctor, an EfficientNetV2-B0 model with FP16
weights and float32 input/output. Source:
https://huggingface.co/KumarjitC/plantulus-doctor

Pinned revision: `ee2cb607a33fefa85ccc567988c98fc605fa36bd`.
Model SHA-256: `3f9b1ef6e7bcc603c6844429ae4ac28607cbfb48fd7b1f856362c824a29a3e65`.
Upstream declares MIT in its model card; the original model card, labels,
preprocessing instructions, canonical mapping and provenance are bundled under
`app/src/main/assets/plantulus/`. The model remains at
`app/src/main/assets/plant_disease_model.tflite` for the existing loader.

## Integration

- Input is RGB float32 `[1,224,224,3]` in **0–255**, with no division by 255.
- Resize the shorter side to 256 with bilinear filtering, centre-crop to 256,
  then centre-crop to 224. Equivalent central offsets are used to render only
  the final 224 pixels, avoiding large allocations for panoramas.
- The upstream JSON does not specify interpolation; bilinear is the explicit
  app/reference choice. Integer resized dimensions use truncation.
- Interpreter tensor shapes/types are checked on initialization. A mutex
  serializes inference because the shared interpreter is not thread-safe.
- Output order has changed. All 54 indices map to canonical app labels. The
  original 38 species/disease label strings are preserved at their new indices,
  so saved scans and user corrections remain readable without database migration.
- Aloe, Cactus, Money Plant, Snake Plant and Spider Plant add 16 classes.
  Supported-plants and onboarding screens now derive counts from the catalog.
  These are broad dataset groups, not a claim to cover every species or disease.
- Selected scientific-name aliases are added for cloud species matching. These
  cover representative named species, not whole genera or families.
- The 11 new non-healthy conditions have no reviewed treatment advice. The app
  uses its existing no-advice fallback rather than inventing treatment steps.
- Cloud services and keys are unchanged. Existing cascade thresholds remain
  unchanged; they have not been calibrated for this replacement.

## Same-set desktop comparison

| Model | Correct labels | Match rate | Incorrect scores above 80% |
| --- | --- | --- | --- |
| Previous MobileNetV2, central crop | 14/43 | 32.6% | 12 |
| Plantulus, required preprocessing | 17/43 | 39.5% | 4 |

Plantulus results by class: healthy tomato 0/8; tomato early blight 7/9;
tomato late blight 6/10; potato early blight 2/8; potato late blight 2/8.
The overall result improved, but tomato late blight declined and healthy tomato
still failed on all examples. This is not a reliable field diagnostic model.
The author's high accuracy on their own test split does not establish accuracy
on our images. Our 43-image, five-class, non-random diagnostic subset does not
validate the other classes, and source labels have not been independently checked.

Original measurements: `model-audit-results.json` (historical model).
New measurements: `plantulus-benchmark-results.json`.
Images stay outside the app at the path used for benchmarking; source URLs and
expected labels are preserved in the reports. Desktop uses Pillow; Android image
resampling can differ. Runtime parity uses identical prepared PNG pixels instead.

## Reproduce and validate

```bash
/tmp/floralens-audit-venv/bin/python tools/model_audit/benchmark.py \
  --download /tmp/floralens-plantdoc-benchmark \
  --output /tmp/plantulus-results.json
/tmp/floralens-audit-venv/bin/python tools/model_audit/generate_reference.py
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin compileReleaseKotlin
```

See `tools/model_audit/requirements.txt` for Python 3.12 dependencies. The reference
uses a synthetic 256×256 image and five 256×256 leaf PNGs, with desktop inference
on their central 224 crops. On Android those same PNGs go through actual
Plantulus preprocessing, then inference; scores must match within 0.0001.
This isolates pixel scaling, crop placement, label mapping and runtime behavior.
It does not compare arbitrary original JPEG decoding between platforms.

Device tests: `ModelReferenceTest`, `PlantulusPreprocessingTest` and
`PlantDocEvaluationTest`, all under `com.arbon.vata.floralens.testdata`.
The optional seeding test requires `seedPlantDoc=true` and is skipped otherwise.
Prefer `adb install -r` when updating the development app to preserve its data.

## Verification status

- All 73 JVM unit tests passed.
- Debug app and instrumentation APKs built; release Kotlin compilation passed.
- APK inspection confirms the exact pinned Plantulus model is bundled uncompressed
  and its 54 source labels are present.
- Installed the debug app and test APK on the Xiaomi 23127PN0CG (Android 16)
  using adb install -r. Existing app data was preserved.
- Five device tests passed: model reference parity (synthetic plus five leaf
  inputs), two evaluation tests, and two preprocessing tests. The optional seed
  test was skipped, as intended, so no fixture records were added or removed.
- The installed replacement model runs under the existing LiteRT Android runtime;
  no runtime dependency upgrade was required.

To rerun the installed instrumentation suite without reinstalling or clearing data:

```bash
adb shell am instrument -w -r \
  -e package com.arbon.vata.floralens.testdata \
  com.arbon.vata.floralens.test/androidx.test.runner.AndroidJUnitRunner
```

The runner prints six discovered tests, with one assumption skip for seeding.
The debug application remains installed for manual gallery/camera checks.
