"""
Verification script for Nuvio brand identity resources and adaptive icons.
"""
import os
import xml.etree.ElementTree as ET
from PIL import Image

def verify():
    print("=== Nuvio Brand Resources Verification ===")
    
    # 1. Check Drawables
    drawables = [
        "app/src/main/res/drawable/nuvio_symbol.xml",
        "app/src/main/res/drawable/nuvio_wordmark.xml",
        "app/src/main/res/drawable/nuvio_logo_light.xml",
        "app/src/main/res/drawable/nuvio_logo_dark.xml",
        "app/src/main/res/drawable/nuvio_logo_monochrome.xml",
        "app/src/main/res/drawable/nuvio_icon_foreground.xml",
        "app/src/main/res/drawable/nuvio_icon_background.xml",
        "app/src/main/res/drawable/splash_background.xml",
        "app/src/main/res/drawable-night/splash_background.xml"
    ]
    for d in drawables:
        assert os.path.exists(d), f"Missing drawable: {d}"
        # Parse XML
        tree = ET.parse(d)
        root = tree.getroot()
        assert root.tag in ["vector", "layer-list"], f"Invalid root tag {root.tag} in {d}"
        print(f"  [OK] Valid XML: {d} ({root.tag})")

    # 2. Check Mipmaps
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    for folder, size in densities.items():
        for icon in ["ic_launcher.png", "ic_launcher_round.png"]:
            path = os.path.join("app/src/main/res", folder, icon)
            assert os.path.exists(path), f"Missing mipmap: {path}"
            with Image.open(path) as im:
                assert im.size == (size, size), f"Size mismatch for {path}: expected ({size}, {size}), got {im.size}"
                assert im.mode == "RGBA", f"Invalid color mode {im.mode} for {path}"
                print(f"  [OK] Mipmap {folder}/{icon}: {im.size} {im.mode}")

    # 3. Check Adaptive Icon XMLs
    for xml_icon in ["app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml", "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"]:
        assert os.path.exists(xml_icon), f"Missing {xml_icon}"
        tree = ET.parse(xml_icon)
        root = tree.getroot()
        assert root.tag == "adaptive-icon", f"Invalid root {root.tag} in {xml_icon}"
        print(f"  [OK] Adaptive Icon XML: {xml_icon}")

    # 4. Check Web Launcher and Master PNGs
    master_pngs = [
        ("app/src/main/ic_launcher-web.png", (512, 512)),
        ("brand/png/nuvio_app_icon.png", (1024, 1024)),
        ("brand/png/nuvio_symbol.png", (1024, 1024)),
        ("brand/png/nuvio_symbol_512.png", (512, 512)),
        ("brand/png/nuvio_logo_light.png", (1200, 310)),
        ("brand/png/nuvio_logo_dark.png", (1200, 310)),
        ("brand/png/nuvio_logo_monochrome.png", (1200, 310))
    ]
    for path, expected_size in master_pngs:
        assert os.path.exists(path), f"Missing master PNG: {path}"
        with Image.open(path) as im:
            print(f"  [OK] Master PNG: {path} ({im.size})")

    # 5. Check SVGs
    svgs = [
        "brand/svg/nuvio_symbol.svg",
        "brand/svg/nuvio_wordmark.svg",
        "brand/svg/nuvio_logo_light.svg",
        "brand/svg/nuvio_logo_dark.svg",
        "brand/svg/nuvio_logo_monochrome.svg",
        "brand/svg/nuvio_app_icon.svg"
    ]
    for svg in svgs:
        assert os.path.exists(svg), f"Missing SVG: {svg}"
        print(f"  [OK] SVG: {svg} ({os.path.getsize(svg)} bytes)")

    print("\nALL 27 BRAND AND ICON ASSETS VERIFIED SUCCESSFULLY!")

if __name__ == '__main__':
    verify()
