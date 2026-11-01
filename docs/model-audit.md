# Historical MobileNetV2 model audit

This report describes the previous model. The app now uses Plantulus; see
[the migration report](plantulus-migration.md). Raw baseline measurements are
preserved for comparison.

## Verified contract

The bundled model SHA-256 is
`12993cc3e05489be62470803a026c958d918f076f0ff23e3c045d6b8b52dd46e`.
It is byte-for-byte identical to the model in the upstream repository checked
at commit `33dc8985c943175a7a1301fe034c01d69bebb8cf`:
https://github.com/obeshor/Plant-Diseases-Detector/tree/33dc8985c943175a7a1301fe034c01d69bebb8cf

- Input: float32 `[1, 224, 224, 3]`, RGB, pixel values divided by 255.
- Output: float32 `[1, 38]`, softmax scores, no tensor quantization.
- All 38 app labels were compared in index order against upstream
  `GreenDoctor/app/src/main/assets/plant_labels.txt`; species/disease meanings
  agree, including cherry/sour cherry and corn/maize display aliases.
- The upstream V4 notebook uses RGB generators with rescale=1/255. Its Android
  classifier also divides RGB by 255. Feeding raw 0–255 pixels or switching to
  -1–1 would violate this contract.
- Training resizes full images; the app centre-crops first. This is a difference
  worth measuring, rather than assuming either choice improves accuracy.
- Upstream's README specifies Apache 2.0; the old MIT comment was corrected.

Sources:
https://github.com/obeshor/Plant-Diseases-Detector/blob/33dc8985c943175a7a1301fe034c01d69bebb8cf/Plant_Diseases_Detection_with_TF2_V4.ipynb
https://github.com/obeshor/Plant-Diseases-Detector/blob/33dc8985c943175a7a1301fe034c01d69bebb8cf/GreenDoctor/app/src/main/java/isomora/com/greendoctor/Classifier.kt

## PlantDoc comparison

43 photos from the PlantDoc test split, at most ten sorted filenames from each
of five mapped classes. The current model's upstream training uses PlantVillage;
these photos were not used to tune this model in this project. Cross-dataset
near-duplicates have not been audited and source labels were not independently
verified. This is a small diagnostic subset, not a representative estimate of
accuracy across 38 classes. Do not use it to train or tune a replacement and then
report the same images as an independent test.

| Preprocessing | Correct | Label match rate |
| --- | --- | --- |
| Centre crop, bilinear | 14/43 | 32.6% |
| Full image, bilinear | 13/43 | 30.2% |
| Full image, nearest | 9/43 | 20.9% |

With centre cropping: healthy tomato 0/8, tomato early blight 3/9, tomato late
blight 9/10, potato early blight 0/8, potato late blight 2/8. Twelve of the 29
incorrect top predictions had model scores above 80%; these scores are not
calibrated probabilities of a correct diagnosis. Existing cascade thresholds
therefore cannot guarantee reliable acceptance.

Desktop uses LiteRT 2.3.0 CPU and Pillow image resizing. Android bitmap resizing
and JPEG decoding may differ slightly. These crop comparisons are reference
measurements, not exact Android image-pipeline parity; desktop timings are not
phone performance measurements. The raw report includes top three scores,
source URLs and per-class results: `model-audit-results.json`.

Conclusion: no label-order or normalization mismatch was found. Changing full
resize/crop does not explain the poor results on this subset. Keep production
preprocessing unchanged until broader validation. Evaluate field-trained models
on a larger, independently held-out set, especially healthy plants and both
potato blights; measure wrong high-confidence predictions as well as accuracy.

## Reproduce

Use Python 3.12 in an isolated environment:

```bash
python3.12 -m venv /tmp/floralens-audit-venv
/tmp/floralens-audit-venv/bin/pip install -r tools/model_audit/requirements.txt
git show ff90a9d:app/src/main/assets/plant_disease_model.tflite > /tmp/legacy-model.tflite
/tmp/floralens-audit-venv/bin/python tools/model_audit/benchmark.py \
  --profile legacy --model /tmp/legacy-model.tflite \
  --download /tmp/floralens-plantdoc-benchmark \
  --output /tmp/model-audit-results.json
```

Downloaded images stay outside the app. PlantDoc is by Singh et al. (2020),
CC BY 4.0; source paths are preserved in the downloaded manifest and report.
https://github.com/pratikkayal/PlantDoc-Dataset

The current `generate_reference.py` targets Plantulus. At the time of this audit,
it created the instrumentation reference using a synthetic
224×224 RGB image with deterministic pixels. That avoids JPEG decoder and resize
differences while testing channel order, normalization, output mapping and actual
Android inference against desktop scores (absolute tolerance 0.0001).
The fixture also includes five leaf photos converted to 224×224 lossless PNGs
and their desktop scores. These compare identical leaf pixels across runtimes;
they do not validate Android JPEG decoding or resizing. The reference includes
the model hash and must be regenerated for a new model.

```bash
/tmp/floralens-audit-venv/bin/python tools/model_audit/generate_reference.py
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.arbon.vata.floralens.testdata.ModelReferenceTest
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.arbon.vata.floralens.testdata.PlantDocEvaluationTest
```

These tests do not seed or delete database records and do not call cloud APIs.

## Verification status

Desktop benchmark and reference generation completed. Android test compilation
passed. On the connected Xiaomi 23127PN0CG (Android 16),
ModelReferenceTest.androidInferenceMatchesDesktopReference passed: one synthetic
RGB image and five leaf PNG references matched desktop scores within absolute
tolerance 0.0001. The model hash also matched. This validates inference given
identical input pixels; it does not establish parity of original JPEG decoding
or the resizing of arbitrary gallery photos.

The first installation attempt was blocked with INSTALL_FAILED_USER_RESTRICTED;
after retrying, installation and reference execution succeeded. Test XML confirmed
one executed reference test with no failures, errors or skips.

A subsequent separate attempt to run PlantDocEvaluationTest was blocked by a new
INSTALL_FAILED_USER_RESTRICTED install prompt. Those two evaluation regression
tests remain compiled but unexecuted. This does not invalidate the completed
ModelReferenceTest above. The test runner cleaned up the test package after the
first run, so rerunning needs another approved test installation.
