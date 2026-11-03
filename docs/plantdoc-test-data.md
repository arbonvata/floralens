# PlantDoc database samples

Twenty-five unchanged PlantDoc test images are bundled in debug builds only, under
`app/src/debug/assets/plantdoc`. Release builds exclude the images and tools. Source URLs pinned to a commit, dataset labels,
author attribution and the CC BY 4.0 license accompany them.

In a debug build, open the three-dot menu → Settings (debug). Tap **Import
PlantDoc samples**, then **Open History**. Repeated imports skip existing samples.
**Remove PlantDoc samples** deletes only these named fixture records and their
photos. Real scans are left in place. No cloud calls or inference run during import.

Alternatively, connect an emulator or development phone and run from the repository root:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.arbon.vata.floralens.testdata.PlantDocSeedTest \
  -Pandroid.testInstrumentationRunnerArguments.seedPlantDoc=true
```

Open History to see the samples. Complete onboarding first if needed. The seed
utility skips samples already present and never clears existing records. Delete
sample entries through History when finished; rerunning the command restores them.
The utility is skipped by normal test runs unless seedPlantDoc=true is supplied.

These records use dataset labels, zero confidence, and no cloud verification.
Their names include "PlantDoc sample". They test persistence, photos, history,
and result display; they do not measure classification accuracy. To test inference,
copy the JPEGs to the phone's Pictures directory and choose them in the gallery
picker. Expected labels are in samples.json. Treat dataset labels as expectations,
not independently confirmed diagnoses.

Dataset: https://github.com/pratikkayal/PlantDoc-Dataset
Singh et al., PlantDoc: A Dataset for Visual Plant Disease Detection (2020).

## Run actual offline inference

In Settings (debug), tap **Run on-device AI on samples**. Importing records first
is optional. Each of the 25 bundled photos is decoded using the bounded photo
decoder and passed through the app's actual PlantClassifier (currently Plantulus Doctor). The screen compares
the highest-scoring prediction with the expected label, displays confidence to
one decimal place, and reports errors per image and a total label match count.
No cloud requests are made and History records retain their fixture labels and
zero confidence. The small sample match count is a smoke check, not an accuracy
benchmark. Leaving the screen cancels the run; decoded photos are recycled.

## Sample coverage

The debug set contains five distinct JPEGs for each category: healthy tomato,
tomato early blight, tomato late blight, potato early blight and potato late blight.
The original five sample names are preserved, so another import adds the 20 new
records and skips the originals if they are already present. All 25 have distinct
file hashes and pinned PlantDoc test-split source URLs in samples.json.
These are exploratory fixtures, not an independent accuracy benchmark: some
overlap the 43-photo diagnostic set and dataset labels may contain errors.
The six-case model parity reference is intentionally unchanged.

Validation of the expansion: debug app and test APK builds passed. APK inspection
confirmed all 25 distinct image hashes and five images per category. Both
PlantDocEvaluationTest cases passed on the connected Android phone, including
decoding all 25 photos, releasing bitmaps, and continuing after an inference error.
The updated debug app was installed with adb install -r.
