"""
Reconstruct official Nuvio brand identity assets from approved reference image.
Generates:
1. Android VectorDrawables (app/src/main/res/drawable/)
2. Scalable Vector Graphics (brand/svg/)
3. High-Resolution Master PNGs (brand/png/)
4. Android Launcher Adaptive Icons & Mipmaps (app/src/main/res/mipmap-*/)
"""
import os
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

def main():
    img_path = r'C:\Users\Lenovoi\.gemini\antigravity\brain\97b8d717-b855-4ea9-9b00-88396e51d57d\.user_uploaded\media_1793514907915_aa6bc6cb.png'
    im_ref = Image.open(img_path).convert('RGBA')
    arr_ref = np.array(im_ref)

    os.makedirs('brand/svg', exist_ok=True)
    os.makedirs('brand/png', exist_ok=True)
    os.makedirs('app/src/main/res/drawable', exist_ok=True)
    os.makedirs('app/src/main/res/mipmap-mdpi', exist_ok=True)
    os.makedirs('app/src/main/res/mipmap-hdpi', exist_ok=True)
    os.makedirs('app/src/main/res/mipmap-xhdpi', exist_ok=True)
    os.makedirs('app/src/main/res/mipmap-xxhdpi', exist_ok=True)
    os.makedirs('app/src/main/res/mipmap-xxxhdpi', exist_ok=True)
    os.makedirs('app/src/main/res/mipmap-anydpi-v26', exist_ok=True)

    # -------------------------------------------------------------
    # 1. Colors
    # -------------------------------------------------------------
    # Extracted from approved reference:
    COLOR_RIBBON_BLUE = "#287FE8"      # Bright primary blue (diagonal ribbon)
    COLOR_STEM_BLUE = "#216DC1"        # Medium blue (left and right upright stems)
    COLOR_YELLOW = "#F9B814"           # Yellow play triangle
    COLOR_CHARCOAL = "#2E373E"         # Dark charcoal slate text & monochrome
    COLOR_DARK_NAVY = "#0C213C"        # Dark navy background
    COLOR_DARK_RIBBON = "#EBF4FB"      # Light ice white/blue for dark theme ribbon
    COLOR_DARK_STEM = "#BCDBFA"        # Soft sky blue for dark theme stems
    COLOR_WHITE = "#FFFFFF"

    # -------------------------------------------------------------
    # 2. Extract and Un-mat Symbol & Wordmark from Reference
    # -------------------------------------------------------------
    # Main symbol bounds: y=[92, 204], x=[284, 425]
    symbol_crop = arr_ref[92:205, 284:426]
    # Wordmark bounds: y=[108, 188], x=[457, 741]
    wordmark_crop = arr_ref[108:189, 457:742]
    # Full logo bounds: y=[92, 204], x=[284, 742]
    full_logo_crop = arr_ref[92:205, 284:742]

    # Un-matting function to remove grid background #EFF3F6 = (239, 243, 246)
    def unmat_crop(crop_arr, bg_rgb=(239.0, 243.0, 246.0), threshold_low=18.0, threshold_high=55.0):
        h, w, _ = crop_arr.shape
        bgr = crop_arr[:, :, :3].astype(float)
        bg = np.array(bg_rgb)
        dist = np.linalg.norm(bgr - bg, axis=2)
        alpha = np.clip((dist - threshold_low) / (threshold_high - threshold_low), 0.0, 1.0)
        
        fg = bgr.copy()
        mask = alpha > 0.05
        for c in range(3):
            fg[mask, c] = (bgr[mask, c] - (1.0 - alpha[mask]) * bg[c]) / alpha[mask]
        fg = np.clip(fg, 0, 255).astype(np.uint8)
        
        rgba = np.dstack([fg, (alpha * 255).astype(np.uint8)])
        return Image.fromarray(rgba, 'RGBA')

    symbol_unmatted = unmat_crop(symbol_crop)
    wordmark_unmatted = unmat_crop(wordmark_crop)
    full_logo_unmatted = unmat_crop(full_logo_crop)

    symbol_unmatted.save('scratch/reconstruct/symbol_unmatted.png')
    wordmark_unmatted.save('scratch/reconstruct/wordmark_unmatted.png')
    full_logo_unmatted.save('scratch/reconstruct/full_logo_unmatted.png')
    print('Unmatted PNGs extracted from reference.')

    # -------------------------------------------------------------
    # 3. High-Precision Vector Geometry Extraction
    # -------------------------------------------------------------
    # Master coordinate space:
    # Symbol viewBox: 0 0 142 113
    # Wordmark viewBox: 0 0 285 81
    # Full Logo viewBox: 0 0 458 113
    # Adaptive Icon: 0 0 108 108

    # Vectorize Wordmark letters with subpixel lanczos 8x:
    wm_gray = cv2.cvtColor(np.array(wordmark_unmatted), cv2.COLOR_RGBA2GRAY)
    wm_alpha = np.array(wordmark_unmatted)[:, :, 3]
    wm_mask = (wm_alpha > 120).astype(np.uint8) * 255
    wm_8x = cv2.resize(wm_mask, (wm_mask.shape[1]*8, wm_mask.shape[0]*8), interpolation=cv2.INTER_LANCZOS4)
    _, wm_thresh = cv2.threshold(wm_8x, 128, 255, cv2.THRESH_BINARY)
    cnts_wm, hier_wm = cv2.findContours(wm_thresh, cv2.RETR_CCOMP, cv2.CHAIN_APPROX_TC89_L1)

    wordmark_paths = []
    for i, c in enumerate(cnts_wm):
        if cv2.contourArea(c) > 30 * 64:
            parent = hier_wm[0][i][3]
            eps = 0.0018 * cv2.arcLength(c, True)
            approx = cv2.approxPolyDP(c, eps, True)
            pts = approx.squeeze().astype(float) / 8.0  # scale back to 1x
            if len(pts) >= 3:
                d = f"M {pts[0][0]:.2f} {pts[0][1]:.2f}"
                for p in pts[1:]:
                    d += f" L {p[0]:.2f} {p[1]:.2f}"
                d += " Z"
                wordmark_paths.append((parent, d))
    print(f'Extracted {len(wordmark_paths)} smooth vector path components for wordmark.')

    # Vectorize Yellow Triangle:
    sym_arr = np.array(symbol_unmatted)
    hsv_sym = cv2.cvtColor(sym_arr[:, :, :3], cv2.COLOR_RGB2HSV)
    mask_y = cv2.inRange(hsv_sym, np.array([15, 120, 120]), np.array([35, 255, 255]))
    # Upscale 8x for subpixel smoothing
    mask_y_8x = cv2.resize(mask_y, (mask_y.shape[1]*8, mask_y.shape[0]*8), interpolation=cv2.INTER_LANCZOS4)
    _, thresh_y = cv2.threshold(mask_y_8x, 128, 255, cv2.THRESH_BINARY)
    cnts_y, _ = cv2.findContours(thresh_y, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_TC89_L1)
    cy = max(cnts_y, key=cv2.contourArea)
    approx_y = cv2.approxPolyDP(cy, 0.006 * cv2.arcLength(cy, True), True)
    pts_y = approx_y.squeeze().astype(float) / 8.0
    path_play_triangle = f"M {pts_y[0][0]:.2f} {pts_y[0][1]:.2f}"
    for p in pts_y[1:]:
        path_play_triangle += f" L {p[0]:.2f} {p[1]:.2f}"
    path_play_triangle += " Z"

    # Precise Paths for Stems and Diagonal Ribbon:
    # Left stem: slanted upright bar from top (18, 1) down to (0, 111) with width ~35px
    # Top-left is rounded.
    path_left_stem = (
        "M 26.50,1.00 "
        "L 55.00,1.00 "
        "L 35.00,111.00 "
        "L 5.50,111.00 "
        "C 2.00,111.00 0.00,108.50 0.50,105.00 "
        "L 19.50,6.50 "
        "C 20.20,2.80 23.00,1.00 26.50,1.00 Z"
    )

    # Right stem: slanted upright bar from top (105, 1) down to (84, 111) with width ~36px
    # Top-right and bottom-right are rounded.
    path_right_stem = (
        "M 105.00,1.00 "
        "L 134.50,1.00 "
        "C 138.00,1.00 141.00,3.50 140.50,7.00 "
        "L 122.50,105.50 "
        "C 122.00,109.00 119.00,111.00 115.50,111.00 "
        "L 84.00,111.00 "
        "L 94.00,51.00 "
        "L 105.00,1.00 Z"
    )

    # Diagonal ribbon: folds from top of left stem down across to bottom of right stem
    path_diagonal_ribbon = (
        "M 20.00,1.00 "
        "L 55.00,1.00 "
        "L 120.00,111.00 "
        "L 84.00,111.00 "
        "Z"
    )

    # Subtle inner shadow under diagonal ribbon
    path_fold_shadow = (
        "M 18.00,1.00 "
        "L 35.00,1.00 "
        "L 45.00,61.00 "
        "L 32.00,61.00 Z"
    )

    # Outer N boundary for monochrome silhouette (with negative play triangle cutout)
    mask_b = cv2.inRange(hsv_sym, np.array([100, 100, 100]), np.array([130, 255, 255]))
    mask_b_8x = cv2.resize(mask_b, (mask_b.shape[1]*8, mask_b.shape[0]*8), interpolation=cv2.INTER_LANCZOS4)
    _, thresh_b = cv2.threshold(mask_b_8x, 128, 255, cv2.THRESH_BINARY)
    cnts_b, _ = cv2.findContours(thresh_b, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_TC89_L1)
    cb = max(cnts_b, key=cv2.contourArea)
    approx_b = cv2.approxPolyDP(cb, 0.004 * cv2.arcLength(cb, True), True)
    pts_b = approx_b.squeeze().astype(float) / 8.0
    path_n_outer = f"M {pts_b[0][0]:.2f} {pts_b[0][1]:.2f}"
    for p in pts_b[1:]:
        path_n_outer += f" L {p[0]:.2f} {p[1]:.2f}"
    path_n_outer += " Z"

    print('Geometry paths synthesized.')

    # -------------------------------------------------------------
    # 4. Generate SVG Files (brand/svg/)
    # -------------------------------------------------------------
    # A. nuvio_symbol.svg
    svg_symbol = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 142 113" width="142" height="113">
  <!-- Nuvio Official Logo Symbol -->
  <path d="{path_left_stem}" fill="{COLOR_STEM_BLUE}"/>
  <path d="{path_right_stem}" fill="{COLOR_STEM_BLUE}"/>
  <path d="{path_diagonal_ribbon}" fill="{COLOR_RIBBON_BLUE}"/>
  <path d="{path_play_triangle}" fill="{COLOR_YELLOW}"/>
</svg>'''
    with open('brand/svg/nuvio_symbol.svg', 'w') as f:
        f.write(svg_symbol)

    # B. nuvio_wordmark.svg
    wordmark_path_data = " ".join([p[1] for p in wordmark_paths])
    svg_wordmark = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 285 81" width="285" height="81">
  <!-- Nuvio Official Wordmark -->
  <path d="{wordmark_path_data}" fill="{COLOR_CHARCOAL}" fill-rule="evenodd"/>
</svg>'''
    with open('brand/svg/nuvio_wordmark.svg', 'w') as f:
        f.write(svg_wordmark)

    # C. nuvio_logo_light.svg (Symbol + Wordmark on Light Background)
    # Symbol at (0, 0), Wordmark offset by x=173, y=16 (centered vertically)
    # Total width: 142 + 31 (gap) + 285 = 458, height: 113
    svg_logo_light = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 458 113" width="458" height="113">
  <!-- Nuvio Official Logo (Light Background) -->
  <g transform="translate(0, 0)">
    <path d="{path_left_stem}" fill="{COLOR_STEM_BLUE}"/>
    <path d="{path_right_stem}" fill="{COLOR_STEM_BLUE}"/>
    <path d="{path_diagonal_ribbon}" fill="{COLOR_RIBBON_BLUE}"/>
    <path d="{path_play_triangle}" fill="{COLOR_YELLOW}"/>
  </g>
  <g transform="translate(173, 16)">
    <path d="{wordmark_path_data}" fill="{COLOR_CHARCOAL}" fill-rule="evenodd"/>
  </g>
</svg>'''
    with open('brand/svg/nuvio_logo_light.svg', 'w') as f:
        f.write(svg_logo_light)

    # D. nuvio_logo_dark.svg (Soft Sky Blue stems, Ice White ribbon, White Wordmark, on Dark Navy)
    svg_logo_dark = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 458 113" width="458" height="113">
  <!-- Nuvio Official Logo (Dark Background) -->
  <g transform="translate(0, 0)">
    <path d="{path_left_stem}" fill="{COLOR_DARK_STEM}"/>
    <path d="{path_right_stem}" fill="{COLOR_DARK_STEM}"/>
    <path d="{path_diagonal_ribbon}" fill="{COLOR_DARK_RIBBON}"/>
    <path d="{path_play_triangle}" fill="{COLOR_YELLOW}"/>
  </g>
  <g transform="translate(173, 16)">
    <path d="{wordmark_path_data}" fill="{COLOR_WHITE}" fill-rule="evenodd"/>
  </g>
</svg>'''
    with open('brand/svg/nuvio_logo_dark.svg', 'w') as f:
        f.write(svg_logo_dark)

    # E. nuvio_logo_monochrome.svg (Single dark color with play triangle cutout)
    svg_logo_mono = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 458 113" width="458" height="113">
  <!-- Nuvio Official Logo (Monochrome Dark) -->
  <g transform="translate(0, 0)">
    <!-- Evenodd fill automatically cuts out the play triangle inside the N -->
    <path d="{path_n_outer} {path_play_triangle}" fill="{COLOR_CHARCOAL}" fill-rule="evenodd"/>
  </g>
  <g transform="translate(173, 16)">
    <path d="{wordmark_path_data}" fill="{COLOR_CHARCOAL}" fill-rule="evenodd"/>
  </g>
</svg>'''
    with open('brand/svg/nuvio_logo_monochrome.svg', 'w') as f:
        f.write(svg_logo_mono)

    # F. nuvio_app_icon.svg (Standalone App Icon in white rounded squircle)
    # Squircle size: 1024x1024, corner radius: 220
    # Centered N symbol: width: 620, height: 493.4, at x=(1024-620)/2=202, y=(1024-493.4)/2=265.3
    # Scale: 620 / 142 = 4.3662
    scale_icon = 620.0 / 142.0
    tx_icon = (1024 - 142 * scale_icon) / 2
    ty_icon = (1024 - 113 * scale_icon) / 2

    svg_app_icon = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">
  <!-- Nuvio Official App Icon (Squircle Reference Direction) -->
  <defs>
    <filter id="icon-shadow" x="-5%" y="-5%" width="110%" height="115%" filterUnits="userSpaceOnUse">
      <feDropShadow dx="0" dy="16" stdDeviation="24" flood-color="#000000" flood-opacity="0.12"/>
    </filter>
  </defs>
  <rect x="48" y="48" width="928" height="928" rx="220" ry="220" fill="#FFFFFF" filter="url(#icon-shadow)"/>
  <g transform="translate({tx_icon:.1f}, {ty_icon:.1f}) scale({scale_icon:.4f})">
    <path d="{path_left_stem}" fill="{COLOR_STEM_BLUE}"/>
    <path d="{path_right_stem}" fill="{COLOR_STEM_BLUE}"/>
    <path d="{path_diagonal_ribbon}" fill="{COLOR_RIBBON_BLUE}"/>
    <path d="{path_play_triangle}" fill="{COLOR_YELLOW}"/>
  </g>
</svg>'''
    with open('brand/svg/nuvio_app_icon.svg', 'w') as f:
        f.write(svg_app_icon)

    print('Generated all 6 SVG master vector files in brand/svg/.')

    # -------------------------------------------------------------
    # 5. Generate Android VectorDrawables (app/src/main/res/drawable/)
    # -------------------------------------------------------------
    # Android VectorDrawable XML files:
    # A. nuvio_symbol.xml
    vd_symbol = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="142dp"
    android:height="113dp"
    android:viewportWidth="142"
    android:viewportHeight="113">
    <!-- Nuvio Official Symbol -->
    <path
        android:fillColor="{COLOR_STEM_BLUE}"
        android:pathData="{path_left_stem}" />
    <path
        android:fillColor="{COLOR_STEM_BLUE}"
        android:pathData="{path_right_stem}" />
    <path
        android:fillColor="{COLOR_RIBBON_BLUE}"
        android:pathData="{path_diagonal_ribbon}" />
    <path
        android:fillColor="{COLOR_YELLOW}"
        android:pathData="{path_play_triangle}" />
</vector>'''
    with open('app/src/main/res/drawable/nuvio_symbol.xml', 'w') as f:
        f.write(vd_symbol)

    # B. nuvio_wordmark.xml
    vd_wordmark = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="285dp"
    android:height="81dp"
    android:viewportWidth="285"
    android:viewportHeight="81">
    <!-- Nuvio Official Wordmark -->
    <path
        android:fillColor="{COLOR_CHARCOAL}"
        android:fillType="evenOdd"
        android:pathData="{wordmark_path_data}" />
</vector>'''
    with open('app/src/main/res/drawable/nuvio_wordmark.xml', 'w') as f:
        f.write(vd_wordmark)

    # C. nuvio_logo_light.xml
    vd_logo_light = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="458dp"
    android:height="113dp"
    android:viewportWidth="458"
    android:viewportHeight="113">
    <!-- Nuvio Official Logo (Light Background) -->
    <group>
        <path
            android:fillColor="{COLOR_STEM_BLUE}"
            android:pathData="{path_left_stem}" />
        <path
            android:fillColor="{COLOR_STEM_BLUE}"
            android:pathData="{path_right_stem}" />
        <path
            android:fillColor="{COLOR_RIBBON_BLUE}"
            android:pathData="{path_diagonal_ribbon}" />
        <path
            android:fillColor="{COLOR_YELLOW}"
            android:pathData="{path_play_triangle}" />
    </group>
    <group
        android:translateX="173"
        android:translateY="16">
        <path
            android:fillColor="{COLOR_CHARCOAL}"
            android:fillType="evenOdd"
            android:pathData="{wordmark_path_data}" />
    </group>
</vector>'''
    with open('app/src/main/res/drawable/nuvio_logo_light.xml', 'w') as f:
        f.write(vd_logo_light)

    # D. nuvio_logo_dark.xml
    vd_logo_dark = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="458dp"
    android:height="113dp"
    android:viewportWidth="458"
    android:viewportHeight="113">
    <!-- Nuvio Official Logo (Dark Background) -->
    <group>
        <path
            android:fillColor="{COLOR_DARK_STEM}"
            android:pathData="{path_left_stem}" />
        <path
            android:fillColor="{COLOR_DARK_STEM}"
            android:pathData="{path_right_stem}" />
        <path
            android:fillColor="{COLOR_DARK_RIBBON}"
            android:pathData="{path_diagonal_ribbon}" />
        <path
            android:fillColor="{COLOR_YELLOW}"
            android:pathData="{path_play_triangle}" />
    </group>
    <group
        android:translateX="173"
        android:translateY="16">
        <path
            android:fillColor="{COLOR_WHITE}"
            android:fillType="evenOdd"
            android:pathData="{wordmark_path_data}" />
    </group>
</vector>'''
    with open('app/src/main/res/drawable/nuvio_logo_dark.xml', 'w') as f:
        f.write(vd_logo_dark)

    # E. nuvio_logo_monochrome.xml
    vd_logo_mono = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="458dp"
    android:height="113dp"
    android:viewportWidth="458"
    android:viewportHeight="113">
    <!-- Nuvio Official Logo (Monochrome Dark) -->
    <group>
        <path
            android:fillColor="{COLOR_CHARCOAL}"
            android:fillType="evenOdd"
            android:pathData="{path_n_outer} {path_play_triangle}" />
    </group>
    <group
        android:translateX="173"
        android:translateY="16">
        <path
            android:fillColor="{COLOR_CHARCOAL}"
            android:fillType="evenOdd"
            android:pathData="{wordmark_path_data}" />
    </group>
</vector>'''
    with open('app/src/main/res/drawable/nuvio_logo_monochrome.xml', 'w') as f:
        f.write(vd_logo_mono)

    # F. nuvio_icon_background.xml (Android Adaptive Icon Background)
    vd_icon_bg = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Clean White Adaptive Icon Background -->
    <path
        android:fillColor="{COLOR_WHITE}"
        android:pathData="M0,0h108v108h-108z"/>
</vector>'''
    with open('app/src/main/res/drawable/nuvio_icon_background.xml', 'w') as f:
        f.write(vd_icon_bg)

    # G. nuvio_icon_foreground.xml (Android Adaptive Icon Foreground)
    # Canvas: 108dp x 108dp
    # Safe area: 72dp circle in center (center at 54, 54)
    # We scale the 142x113 symbol to width=52dp, height=41.4dp
    # Scale factor: 52 / 142 = 0.366197
    # Position: translateX = (108 - 142*0.366197)/2 = (108 - 52)/2 = 28dp
    # translateY = (108 - 113*0.366197)/2 = (108 - 41.38)/2 = 33.31dp
    vd_icon_fg = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Nuvio Official Adaptive Icon Foreground (Centered in Safe Zone) -->
    <group
        android:scaleX="0.3662"
        android:scaleY="0.3662"
        android:translateX="28.0"
        android:translateY="33.31">
        <path
            android:fillColor="{COLOR_STEM_BLUE}"
            android:pathData="{path_left_stem}" />
        <path
            android:fillColor="{COLOR_STEM_BLUE}"
            android:pathData="{path_right_stem}" />
        <path
            android:fillColor="{COLOR_RIBBON_BLUE}"
            android:pathData="{path_diagonal_ribbon}" />
        <path
            android:fillColor="{COLOR_YELLOW}"
            android:pathData="{path_play_triangle}" />
    </group>
</vector>'''
    with open('app/src/main/res/drawable/nuvio_icon_foreground.xml', 'w') as f:
        f.write(vd_icon_fg)

    print('Generated all VectorDrawables in app/src/main/res/drawable/.')

    # Update app/src/main/res/drawable/ic_launcher_background.xml and ic_launcher_foreground.xml
    with open('app/src/main/res/drawable/ic_launcher_background.xml', 'w') as f:
        f.write(vd_icon_bg)
    with open('app/src/main/res/drawable/ic_launcher_foreground.xml', 'w') as f:
        f.write(vd_icon_fg)

    # -------------------------------------------------------------
    # 6. Generate High-Resolution Master PNGs (brand/png/)
    # -------------------------------------------------------------
    # Generate high-resolution transparent master PNGs from unmatted reference + vector rendering
    # A. nuvio_symbol.png (1024x1024 and 512x512)
    # Master high-res symbol
    sym_1024 = Image.new('RGBA', (1024, 1024), (255, 255, 255, 0))
    # resize unmatted symbol smoothly
    sym_w = 800
    sym_h = int(800 * (113.0 / 142.0))
    resized_sym = symbol_unmatted.resize((sym_w, sym_h), Image.Resampling.LANCZOS)
    sym_1024.paste(resized_sym, ((1024 - sym_w)//2, (1024 - sym_h)//2), mask=resized_sym.split()[3])
    sym_1024.save('brand/png/nuvio_symbol.png')
    sym_1024.resize((512, 512), Image.Resampling.LANCZOS).save('brand/png/nuvio_symbol_512.png')

    # B. nuvio_wordmark.png (1024x290)
    wm_w = 900
    wm_h = int(900 * (81.0 / 285.0))
    wm_png = Image.new('RGBA', (1024, wm_h + 60), (255, 255, 255, 0))
    resized_wm = wordmark_unmatted.resize((wm_w, wm_h), Image.Resampling.LANCZOS)
    wm_png.paste(resized_wm, ((1024 - wm_w)//2, 30), mask=resized_wm.split()[3])
    wm_png.save('brand/png/nuvio_wordmark.png')

    # C. nuvio_logo_light.png (1200x296)
    logo_w = 1100
    logo_h = int(1100 * (113.0 / 458.0))
    logo_png = Image.new('RGBA', (1200, logo_h + 40), (255, 255, 255, 0))
    resized_logo = full_logo_unmatted.resize((logo_w, logo_h), Image.Resampling.LANCZOS)
    logo_png.paste(resized_logo, ((1200 - logo_w)//2, 20), mask=resized_logo.split()[3])
    logo_png.save('brand/png/nuvio_logo_light.png')

    # D. nuvio_logo_dark.png
    # Extract dark version directly from reference!
    # Dark crop in reference: y: 500 to 680, x: 500 to 980
    dark_logo_crop = arr_ref[520:645, 590:950]
    dark_bg_rgb = (12.0, 33.0, 60.0) # #0C213C
    dark_logo_unmatted = unmat_crop(dark_logo_crop, bg_rgb=dark_bg_rgb, threshold_low=12.0, threshold_high=45.0)
    dark_logo_png = Image.new('RGBA', (1200, logo_h + 40), (12, 33, 60, 255))
    resized_dark = dark_logo_unmatted.resize((logo_w, logo_h), Image.Resampling.LANCZOS)
    dark_logo_png.paste(resized_dark, ((1200 - logo_w)//2, 20), mask=resized_dark.split()[3])
    dark_logo_png.save('brand/png/nuvio_logo_dark.png')

    # E. nuvio_logo_monochrome.png
    mono_crop = arr_ref[280:410, 590:950]
    mono_unmatted = unmat_crop(mono_crop)
    mono_logo_png = Image.new('RGBA', (1200, logo_h + 40), (255, 255, 255, 0))
    resized_mono = mono_unmatted.resize((logo_w, logo_h), Image.Resampling.LANCZOS)
    mono_logo_png.paste(resized_mono, ((1200 - logo_w)//2, 20), mask=resized_mono.split()[3])
    mono_logo_png.save('brand/png/nuvio_logo_monochrome.png')

    # F. nuvio_app_icon.png (1024x1024)
    # White squircle with subtle drop shadow + centered N symbol
    icon_1024 = Image.new('RGBA', (1024, 1024), (255, 255, 255, 0))
    # Draw rounded squircle
    draw = ImageDraw.Draw(icon_1024)
    # Soft shadow
    shadow_layer = Image.new('RGBA', (1024, 1024), (0, 0, 0, 0))
    draw_shadow = ImageDraw.Draw(shadow_layer)
    draw_shadow.rounded_rectangle([72, 88, 952, 968], radius=210, fill=(0, 0, 0, 40))
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(18))
    icon_1024.paste(shadow_layer, (0, 0), mask=shadow_layer)

    draw.rounded_rectangle([72, 72, 952, 952], radius=210, fill=(255, 255, 255, 255))
    # Center N symbol
    sym_icon_w = 540
    sym_icon_h = int(540 * (113.0 / 142.0))
    icon_sym = symbol_unmatted.resize((sym_icon_w, sym_icon_h), Image.Resampling.LANCZOS)
    icon_1024.paste(icon_sym, ((1024 - sym_icon_w)//2, (1024 - sym_icon_h)//2), mask=icon_sym.split()[3])
    icon_1024.save('brand/png/nuvio_app_icon.png')
    icon_1024.resize((512, 512), Image.Resampling.LANCZOS).save('app/src/main/ic_launcher-web.png')

    print('Generated master PNGs in brand/png/ and ic_launcher-web.png.')

    # -------------------------------------------------------------
    # 7. Generate Android Mipmaps for All Densities
    # -------------------------------------------------------------
    densities = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192,
    }

    for dir_name, size in densities.items():
        target_dir = os.path.join('app/src/main/res', dir_name)
        
        # 1. ic_launcher.png (Squircle style matching approved reference)
        launcher_img = Image.new('RGBA', (size, size), (255, 255, 255, 0))
        d = ImageDraw.Draw(launcher_img)
        # Margin: 2px
        pad = max(1, int(size * 0.04))
        r = int(size * 0.22)
        d.rounded_rectangle([pad, pad, size - pad, size - pad], radius=r, fill=(255, 255, 255, 255))
        
        # Centered symbol
        target_sym_w = int(size * 0.58)
        target_sym_h = int(target_sym_w * (113.0 / 142.0))
        scaled_sym = symbol_unmatted.resize((target_sym_w, target_sym_h), Image.Resampling.LANCZOS)
        launcher_img.paste(scaled_sym, ((size - target_sym_w)//2, (size - target_sym_h)//2), mask=scaled_sym.split()[3])
        launcher_img.save(os.path.join(target_dir, 'ic_launcher.png'))

        # 2. ic_launcher_round.png (Circular launcher style)
        round_img = Image.new('RGBA', (size, size), (255, 255, 255, 0))
        d_round = ImageDraw.Draw(round_img)
        d_round.ellipse([pad, pad, size - pad, size - pad], fill=(255, 255, 255, 255))
        round_img.paste(scaled_sym, ((size - target_sym_w)//2, (size - target_sym_h)//2), mask=scaled_sym.split()[3])
        round_img.save(os.path.join(target_dir, 'ic_launcher_round.png'))

    # Adaptive icon XMLs:
    adaptive_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/nuvio_icon_background" />
    <foreground android:drawable="@drawable/nuvio_icon_foreground" />
</adaptive-icon>'''
    with open('app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml', 'w') as f:
        f.write(adaptive_xml)
    with open('app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml', 'w') as f:
        f.write(adaptive_xml)

    print('All mipmap icons generated successfully across all density buckets.')

if __name__ == '__main__':
    main()
