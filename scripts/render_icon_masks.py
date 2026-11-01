"""
Simulate adaptive icon masking across Circle, Squircle, Rounded Square, and Teardrop.
"""
from PIL import Image, ImageDraw, ImageFilter
import os

def render_masks():
    os.makedirs('scratch/icon_masks', exist_ok=True)
    
    # Load 1024x1024 foreground (N symbol on transparent)
    sym = Image.open('brand/png/nuvio_symbol.png').convert('RGBA')
    
    # Canvas size 1080x1080 (10x of 108dp)
    canvas_size = 1080
    bg = Image.new('RGBA', (canvas_size, canvas_size), (255, 255, 255, 255))
    
    # Target symbol width: 520 (52dp * 10), height: 414
    sym_w = 520
    sym_h = int(sym_w * (113.0 / 142.0))
    resized_sym = sym.resize((sym_w, sym_h), Image.Resampling.LANCZOS)
    
    # Paste centered
    tx = (canvas_size - sym_w) // 2
    ty = (canvas_size - sym_h) // 2
    bg.paste(resized_sym, (tx, ty), mask=resized_sym.split()[3])
    
    # Safe zone circle is 720px (72dp * 10) centered at 540, 540
    # OEM Masks (outer diameter 720px centered at 540, 540):
    
    # 1. Circle (Google Pixel)
    mask_circle = Image.new('L', (canvas_size, canvas_size), 0)
    d_c = ImageDraw.Draw(mask_circle)
    d_c.ellipse([180, 180, 900, 900], fill=255)
    img_circle = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
    img_circle.paste(bg, mask=mask_circle)
    img_circle.crop((180, 180, 900, 900)).save('scratch/icon_masks/pixel_circle.png')
    
    # 2. Squircle (Samsung One UI)
    mask_squircle = Image.new('L', (canvas_size, canvas_size), 0)
    d_s = ImageDraw.Draw(mask_squircle)
    d_s.rounded_rectangle([180, 180, 900, 900], radius=190, fill=255)
    img_squircle = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
    img_squircle.paste(bg, mask=mask_squircle)
    img_squircle.crop((180, 180, 900, 900)).save('scratch/icon_masks/samsung_squircle.png')

    # 3. Rounded Square (OnePlus / Xiaomi)
    mask_rsquare = Image.new('L', (canvas_size, canvas_size), 0)
    d_r = ImageDraw.Draw(mask_rsquare)
    d_r.rounded_rectangle([180, 180, 900, 900], radius=130, fill=255)
    img_rsquare = Image.new('RGBA', (canvas_size, canvas_size), (0, 0, 0, 0))
    img_rsquare.paste(bg, mask=mask_rsquare)
    img_rsquare.crop((180, 180, 900, 900)).save('scratch/icon_masks/oneplus_rounded_square.png')

    print("Rendered adaptive icon masks: Pixel circle, Samsung squircle, OnePlus rounded square.")

if __name__ == '__main__':
    render_masks()
