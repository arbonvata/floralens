"""Generate a resize-independent Android parity fixture from synthetic and leaf 256px RGB images."""
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageOps
import numpy as np
from ai_edge_litert.interpreter import Interpreter

root = Path(__file__).resolve().parents[2]
model = root / "app/src/main/assets/plant_disease_model.tflite"
y, x = np.indices((256, 256))
pixels = np.stack(((x * 3 + y) % 256, (y * 5 + x) % 256, (x * 7 + y * 11) % 256), axis=-1)
i = Interpreter(model_path=str(model), num_threads=1)
i.allocate_tensors()
i.set_tensor(i.get_input_details()[0]["index"], pixels[16:240, 16:240][None].astype(np.float32))
i.invoke()
output = i.get_tensor(i.get_output_details()[0]["index"])[0]
p = root / "app/src/androidTest/assets/model_audit/reference.json"
p.parent.mkdir(parents=True, exist_ok=True)
p.write_text(json.dumps({"model_sha256": hashlib.sha256(model.read_bytes()).hexdigest(),
                         "image_size": 256, "profile": "plantulus_short256_crop224_rgb_0_255",
                         "pixel_formula": "R=(3*x+y)%256; G=(5*y+x)%256; B=(7*x+11*y)%256",
                         "scores": output.tolist()}, indent=2) + "\n")
samples = json.loads((root / "app/src/debug/assets/plantdoc/samples.json").read_text())
leaves = []
# Keep runtime parity fixtures fixed when the exploratory debug gallery grows.
for sample in samples[:5]:
    with Image.open(root / "app/src/debug/assets/plantdoc" / sample["file"]) as image:
        image = ImageOps.exif_transpose(image).convert("RGB")
        side = min(image.size)
        x, y = (image.width - side) // 2, (image.height - side) // 2
        image = image.crop((x, y, x + side, y + side)).resize((256, 256), Image.Resampling.BILINEAR)
        filename = sample["file"].replace(".jpg", ".png")
        image.save(p.parent / filename)
        i.set_tensor(i.get_input_details()[0]["index"], np.asarray(image.crop((16, 16, 240, 240)), dtype=np.float32)[None])
        i.invoke()
        leaves.append({"file": filename, "source_file": sample["file"], "source": sample["source"],
                       "scores": i.get_tensor(i.get_output_details()[0]["index"])[0].tolist()})
data = json.loads(p.read_text())
data["leaf_references"] = leaves
p.write_text(json.dumps(data, indent=2) + "\n")
(p.parent / "ATTRIBUTION.txt").write_text(
    "PlantDoc (2020), Singh et al., CC BY 4.0. https://github.com/pratikkayal/PlantDoc-Dataset\n"
    "These five test-only PNGs are centre-cropped and resized 256px derivatives of the debug fixtures.\n"
    "Source URLs are in reference.json; full license is in src/debug/assets/plantdoc/LICENSE.txt.\n")
print(p)
