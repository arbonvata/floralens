# Scan input and diagnosis review

Branch: `codex/improve-scan-inputs`, based on `codex/fix-scan-review-findings`.

## Changes

- Map the camera guide through CameraX's preview/capture transforms into the
  upright photo. Only the offline model receives that crop. History and cloud
  requests retain the full framing.
- Make the guide responsive to available width and height. Show the supported
  crop count from the labels and replace the fixed latency claim with capture guidance.
- Decode camera, gallery, and saved photos with power-of-two sampling, targeting
  a longest side no greater than 1600 pixels. Preserve gallery EXIF rotation and
  mirroring, use software ARGB_8888 decoding, and release rotation intermediates.
- When cloud identification changes the crop species, clear the previous disease
  and health assessment. Display an unknown diagnosis until a new assessment is
  available. Automatic and manual verification share the same merge logic.
- Display the confidence of the cloud health assessment instead of the larger
  confidence from a different on-device verdict.

## Verification

Run `./gradlew testDebugUnitTest assembleDebug ktlintCheck --offline`.
Regression tests cover species changes, confidence, model-only cropping, crop
bounds, and image sample sizes, alongside the earlier correction/retry tests.

## Phone checks before merging

1. In portrait and landscape, frame one leaf inside the guide and scan it.
   Confirm the saved photo retains the surrounding scene. Use an asymmetric
   scene to check that rotation and guide placement match what was previewed.
2. Import large landscape/portrait photos and photos with EXIF rotation or
   mirroring. Confirm the saved image and classification are upright.
3. Recheck a scan whose cloud species differs from the offline crop. With no
   health assessment, results, history, and tracking should show an unknown
   diagnosis rather than the old crop's disease or a healthy verdict.

Physical camera framing has not been verified on a device. Model normalization,
uncertainty thresholds, and multi-frame inference are unchanged.
