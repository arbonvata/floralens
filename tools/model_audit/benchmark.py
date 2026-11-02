"""Reference inference. Pillow resize is an approximation of Android resizing.
Run with --samples app/src/debug/assets/plantdoc --output /tmp/model-results.json.
Use --download /tmp/plantdoc-audit to fetch up to 10 images per mapped test class.
"""
import argparse
import hashlib
import json
import re
import time
import urllib.parse
import urllib.request
from pathlib import Path

import numpy as np
from PIL import Image, ImageOps
from ai_edge_litert.interpreter import Interpreter

ROOT = Path(__file__).resolve().parents[2]
COMMIT = "5467f6012d78d1c446145d5f582da6096f852ae8"
MAPPING = {
    "Tomato leaf": "Tomato___healthy",
    "Tomato Early blight leaf": "Tomato___Early_blight",
    "Tomato leaf late blight": "Tomato___Late_blight",
    "Potato leaf early blight": "Potato___Early_blight",
    "Potato leaf late blight": "Potato___Late_blight",
}


def download(destination):
    destination.mkdir(parents=True, exist_ok=True)
    url = f"https://api.github.com/repos/pratikkayal/PlantDoc-Dataset/git/trees/{COMMIT}?recursive=1"
    tree = json.load(urllib.request.urlopen(url, timeout=60))["tree"]
    rows = []
    for folder, label in MAPPING.items():
        paths = sorted(x["path"] for x in tree if x["type"] == "blob" and x["path"].startswith(f"test/{folder}/"))[:10]
        for path in paths:
            name = f"sample_{len(rows):03d}.jpg"
            url = f"https://raw.githubusercontent.com/pratikkayal/PlantDoc-Dataset/{COMMIT}/{urllib.parse.quote(path)}"
            target = destination / name
            if not target.exists():
                target.write_bytes(urllib.request.urlopen(url, timeout=60).read())
            rows.append({"file": name, "label": label, "source": url})
    (destination / "samples.json").write_text(json.dumps(rows, indent=2) + "\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--samples", type=Path, default=ROOT / "app/src/debug/assets/plantdoc")
    parser.add_argument("--download", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.download:
        download(args.download)
        args.samples = args.download
    model = ROOT / "app/src/main/assets/plant_disease_model.tflite"
    text = (ROOT / "app/src/main/java/com/arbon/vata/floralens/domain/classifier/PlantLabels.kt").read_text()
    labels = re.findall(r'"([^"]+)"', text.split("/** Species")[0])
    interpreter = Interpreter(model_path=str(model), num_threads=1)
    interpreter.allocate_tensors()
    inp, out = interpreter.get_input_details()[0], interpreter.get_output_details()[0]
    assert list(inp["shape"]) == [1, 224, 224, 3] and inp["dtype"] == np.float32
    assert list(out["shape"]) == [1, len(labels)] and out["dtype"] == np.float32
    rows = []
    for sample in json.loads((args.samples / "samples.json").read_text()):
        try:
            with Image.open(args.samples / sample["file"]) as image:
                image = ImageOps.exif_transpose(image).convert("RGB")
                # Match PhotoDecoding's power-of-two bound before resizing.
                sample_size = 1
                while max(image.size) > 1600 * sample_size:
                    sample_size *= 2
                if sample_size > 1:
                    image = image.resize((image.width // sample_size, image.height // sample_size))
                for mode in ["center_crop_bilinear", "full_resize_bilinear", "full_resize_nearest"]:
                    photo = image
                    if mode.startswith("center_crop"):
                        side = min(photo.size)
                        x, y = (photo.width - side) // 2, (photo.height - side) // 2
                        photo = photo.crop((x, y, x + side, y + side))
                    resampling = Image.Resampling.NEAREST if mode.endswith("nearest") else Image.Resampling.BILINEAR
                    tensor = np.asarray(photo.resize((224, 224), resampling), dtype=np.float32)[None] / np.float32(255)
                    interpreter.set_tensor(inp["index"], tensor)
                    start = time.perf_counter()
                    interpreter.invoke()
                    elapsed = (time.perf_counter() - start) * 1000
                    scores = interpreter.get_tensor(out["index"])[0]
                    order = np.argsort(scores)[::-1][:3]
                    rows.append({**sample, "mode": mode, "predicted": labels[order[0]],
                                 "confidence": float(scores[order[0]]), "matches": labels[order[0]] == sample["label"],
                                 "milliseconds": elapsed, "top3": [{"label": labels[i], "score": float(scores[i])} for i in order]})
        except Exception as error:
            rows.append({**sample, "error": str(error)})
    summary = {}
    for mode in sorted({r["mode"] for r in rows if "mode" in r}):
        selected = [r for r in rows if r.get("mode") == mode]
        summary[mode] = {"correct": sum(r["matches"] for r in selected), "total": len(selected),
                         "per_class": {label: {"correct": sum(r["matches"] for r in selected if r["label"] == label),
                                               "total": sum(r["label"] == label for r in selected)} for label in MAPPING.values()}}
    result = {"model_sha256": hashlib.sha256(model.read_bytes()).hexdigest(),
              "input": [1, 224, 224, 3], "output": [1, 38], "normalization": "RGB / 255 float32",
              "runtime": "ai-edge-litert 2.3.0 CPU; desktop timings are not phone timings",
              "limitations": "Small non-random subset, five classes, dataset labels not independently verified. Pillow resize is not Android parity.",
              "summary": summary, "results": rows}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
