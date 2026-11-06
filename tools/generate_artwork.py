#!/usr/bin/env python3
"""Generate the CBDVentures illustration set.

Everything is vector (SVG) so the app has zero external asset dependencies:
it renders offline, inside sandboxed previews and on low-bandwidth connections
(a stated non-functional requirement in the Phase 2/3 documents).
"""
import math
import os
import random

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "static", "images")
COVERS = os.path.join(BASE, "covers")
os.makedirs(COVERS, exist_ok=True)

# Category palettes: (dark, mid, light, accent)
PALETTES = {
    "food-drinks": [("#3A1C0B", "#C8551F", "#F2A73B", "#FFE3B0"),
                    ("#2C1206", "#8E3B12", "#E07A2C", "#FFD59B"),
                    ("#241608", "#A34A16", "#F0B44A", "#FFF0CD"),
                    ("#33180A", "#B24B1C", "#ED8F3C", "#FFE7C2")],
    "restaurants": [("#1F0D18", "#7A1F3D", "#C94F6D", "#FFD9E0"),
                    ("#1A0C14", "#5E1730", "#B33F5B", "#F7C7D2"),
                    ("#22101A", "#8E2A46", "#D96A82", "#FFE1E8"),
                    ("#180A12", "#6B1D36", "#C25570", "#FFD2DC")],
    "salons": [("#1B0F33", "#5B2E9E", "#9D6BE0", "#E8DBFF"),
               ("#150C2B", "#4A2482", "#8A5BD6", "#DFCFFF"),
               ("#1E1238", "#6535AB", "#A875E8", "#EFE4FF"),
               ("#120A24", "#3F1F70", "#7E4FCC", "#D9C7FF")],
    "study-spaces": [("#08222B", "#14606F", "#3FA7A0", "#D6F5F0"),
                     ("#071B24", "#10505E", "#2E939B", "#CBEFF2"),
                     ("#0A2630", "#177085", "#48B6AE", "#E0FBF6"),
                     ("#06171F", "#0E4653", "#288088", "#C4E9EC")],
    "events": [("#1A0F2E", "#4B2C8F", "#A83F8C", "#FFD9F2"),
               ("#140B24", "#3B2277", "#93347A", "#FBC9E9"),
               ("#1F1236", "#57349E", "#B84A9B", "#FFE2F7"),
               ("#110821", "#33196A", "#852C6D", "#F5BCE2")],
}

# Simple, tasteful glyphs drawn as paths in a 100x100 box.
GLYPHS = {
    "food": """<path d="M30 18v30a8 8 0 0 0 16 0V18M38 18v64M62 18c-6 0-10 8-10 18s4 16 10 16m0 0v30" 
        fill="none" stroke="currentColor" stroke-width="4.5" stroke-linecap="round"/>""",
    "restaurant": """<circle cx="50" cy="52" r="26" fill="none" stroke="currentColor" stroke-width="4.5"/>
        <circle cx="50" cy="52" r="13" fill="none" stroke="currentColor" stroke-width="3.5"/>
        <path d="M50 16v10M50 78v10M14 52h10M76 52h10" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>""",
    "salon": """<circle cx="32" cy="30" r="10" fill="none" stroke="currentColor" stroke-width="4.5"/>
        <circle cx="32" cy="74" r="10" fill="none" stroke="currentColor" stroke-width="4.5"/>
        <path d="M39 37l38 30M39 67l38-30" stroke="currentColor" stroke-width="4.5" stroke-linecap="round"/>""",
    "study": """<path d="M20 26c10-5 20-5 30 0v50c-10-5-20-5-30 0z" fill="none" stroke="currentColor" stroke-width="4.5" stroke-linejoin="round"/>
        <path d="M80 26c-10-5-20-5-30 0v50c10-5 20-5 30 0z" fill="none" stroke="currentColor" stroke-width="4.5" stroke-linejoin="round"/>""",
    "event": """<path d="M22 34h56v14a8 8 0 0 0 0 16v14H22V64a8 8 0 0 0 0-16z" fill="none" stroke="currentColor" stroke-width="4.5" stroke-linejoin="round"/>
        <path d="M46 34v44" stroke="currentColor" stroke-width="3" stroke-dasharray="5 6" stroke-linecap="round"/>""",
}

CATEGORY_GLYPH = {
    "food-drinks": "food",
    "restaurants": "restaurant",
    "salons": "salon",
    "study-spaces": "study",
    "events": "event",
}


def cover(category: str, index: int, path: str) -> None:
    palette = PALETTES[category][index % 4]
    dark, mid, light, accent = palette
    glyph = GLYPHS[CATEGORY_GLYPH[category]]
    random.seed(hash((category, index)) & 0xFFFFFFFF)

    # Abstract composition: a horizon band, a sun/moon disc, arcs and translucent tiles.
    horizon = 0.58 + random.uniform(-0.08, 0.10)
    horizon_y = int(600 * horizon)
    disc_x = random.randint(180, 620)
    disc_y = horizon_y - random.randint(90, 170)
    disc_r = random.randint(70, 120)
    tiles = []
    x = -40
    while x < 840:
        w = random.randint(60, 130)
        h = random.randint(60, 210)
        opacity = round(random.uniform(0.06, 0.2), 3)
        tiles.append((x, horizon_y - random.randint(0, 40), w, h, opacity))
        x += w + random.randint(8, 30)

    tile_svg = "\n    ".join(
        f'<rect x="{t[0]}" y="{t[1]}" width="{t[2]}" height="{t[3]}" rx="10" fill="{accent}" opacity="{t[4]}"/>'
        for t in tiles
    )
    arcs = "\n    ".join(
        f'<circle cx="{disc_x}" cy="{disc_y}" r="{disc_r + 60 + i * 46}" fill="none" '
        f'stroke="{accent}" stroke-width="1.2" opacity="{0.30 - i * 0.06:.2f}"/>'
        for i in range(4)
    )

    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="800" height="600" role="img" aria-label="{category} illustration {index + 1}">
  <defs>
    <linearGradient id="sky" x1="0" y1="0" x2="0.35" y2="1">
      <stop offset="0%" stop-color="{dark}"/>
      <stop offset="55%" stop-color="{mid}"/>
      <stop offset="100%" stop-color="{dark}"/>
    </linearGradient>
    <linearGradient id="ground" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="{dark}" stop-opacity="0.96"/>
      <stop offset="100%" stop-color="{dark}"/>
    </linearGradient>
    <radialGradient id="glow" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="{accent}" stop-opacity="0.85"/>
      <stop offset="100%" stop-color="{accent}" stop-opacity="0"/>
    </radialGradient>
    <filter id="grain">
      <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" result="noise"/>
      <feColorMatrix in="noise" type="saturate" values="0"/>
      <feComponentTransfer><feFuncA type="linear" slope="0.06"/></feComponentTransfer>
    </filter>
  </defs>
  <rect width="800" height="600" fill="url(#sky)"/>
  <circle cx="{disc_x}" cy="{disc_y}" r="{disc_r * 2.1}" fill="url(#glow)" opacity="0.55"/>
  {arcs}
  <circle cx="{disc_x}" cy="{disc_y}" r="{disc_r}" fill="{light}" opacity="0.92"/>
  <circle cx="{disc_x - disc_r * 0.3}" cy="{disc_y - disc_r * 0.3}" r="{disc_r * 0.65}" fill="{accent}" opacity="0.35"/>
  <rect x="0" y="{horizon_y}" width="800" height="{600 - horizon_y}" fill="url(#ground)"/>
  {tile_svg}
  <g transform="translate({random.choice([90, 610])}, {horizon_y - 150}) scale(1.35)" color="{accent}" opacity="0.28">
    {glyph}
  </g>
  <rect width="800" height="600" filter="url(#grain)" opacity="0.5"/>
  <rect x="0" y="0" width="800" height="600" fill="none" stroke="{accent}" stroke-opacity="0.15" stroke-width="2"/>
</svg>
"""
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(svg)


def hero(path: str) -> None:
    """Jacaranda skyline for the home hero."""
    rng = random.Random(20261026)
    petals = []
    for _ in range(70):
        px, py = rng.randint(0, 1200), rng.randint(0, 620)
        pr = rng.choice([3, 4, 5, 6])
        petals.append(f'<ellipse cx="{px}" cy="{py}" rx="{pr * 1.6}" ry="{pr}" '
                      f'fill="#C9A7F5" opacity="{rng.uniform(0.12, 0.4):.2f}" '
                      f'transform="rotate({rng.randint(0, 180)} {px} {py})"/>')
    # Skyline blocks
    blocks = []
    x = -20
    while x < 1220:
        w = rng.randint(50, 110)
        h = rng.randint(70, 230)
        blocks.append((x, 620 - h, w, h))
        x += w + rng.randint(8, 26)
    block_svg = "\n    ".join(
        f'<rect x="{b[0]}" y="{b[1]}" width="{b[2]}" height="{b[3]}" rx="4" fill="#1A1229" opacity="0.92"/>'
        for b in blocks
    )
    windows = []
    for b in blocks:
        for wy in range(b[1] + 16, 600, 26):
            for wx in range(b[0] + 12, b[0] + b[2] - 10, 20):
                if rng.random() < 0.35:
                    windows.append(f'<rect x="{wx}" y="{wy}" width="6" height="9" rx="1.5" '
                                   f'fill="#FFD79A" opacity="{rng.uniform(0.18, 0.7):.2f}"/>')
    window_svg = "\n    ".join(windows)

    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 620" width="1200" height="620" role="img" aria-label="Jacaranda skyline of the Pretoria CBD">
  <defs>
    <linearGradient id="dusk" x1="0" y1="0" x2="0.2" y2="1">
      <stop offset="0%" stop-color="#241640"/>
      <stop offset="45%" stop-color="#4B2A6B"/>
      <stop offset="75%" stop-color="#8B4C86"/>
      <stop offset="100%" stop-color="#E08A6A"/>
    </linearGradient>
    <radialGradient id="sun" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#FFD9A8" stop-opacity="0.95"/>
      <stop offset="60%" stop-color="#F2A73B" stop-opacity="0.35"/>
      <stop offset="100%" stop-color="#F2A73B" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="haze" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#4B2A6B" stop-opacity="0"/>
      <stop offset="100%" stop-color="#2A1740" stop-opacity="0.95"/>
    </linearGradient>
  </defs>
  <rect width="1200" height="620" fill="url(#dusk)"/>
  <circle cx="930" cy="430" r="300" fill="url(#sun)"/>
  <circle cx="930" cy="430" r="72" fill="#FFE3BC" opacity="0.9"/>
  {block_svg}
  {window_svg}
  <g opacity="0.9">
    <ellipse cx="180" cy="470" rx="150" ry="105" fill="#7B4FB5" opacity="0.85"/>
    <ellipse cx="255" cy="430" rx="105" ry="82" fill="#9A6BD6" opacity="0.8"/>
    <ellipse cx="110" cy="425" rx="90" ry="70" fill="#6B3FA0" opacity="0.8"/>
    <rect x="172" y="480" width="14" height="130" fill="#2A1740"/>
  </g>
  <g opacity="0.75">
    <ellipse cx="1040" cy="500" rx="130" ry="88" fill="#7B4FB5" opacity="0.7"/>
    <ellipse cx="960" cy="470" rx="85" ry="64" fill="#9A6BD6" opacity="0.65"/>
    <rect x="1032" y="510" width="12" height="110" fill="#2A1740"/>
  </g>
  {''.join(petals)}
  <rect x="0" y="360" width="1200" height="260" fill="url(#haze)"/>
</svg>
"""
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(svg)


def logo(path: str) -> None:
    svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64" role="img" aria-label="CBDVentures logo">
  <defs>
    <linearGradient id="mark" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#8B5CF6"/>
      <stop offset="100%" stop-color="#6D3BD1"/>
    </linearGradient>
  </defs>
  <rect width="64" height="64" rx="18" fill="url(#mark)"/>
  <g fill="none" stroke="#F5EFFF" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round">
    <path d="M32 47c8.5-5.5 13-12.6 13-20.5A13 13 0 0 0 19 26.5c0 7.9 4.5 15 13 20.5z"/>
    <circle cx="32" cy="27" r="4.6"/>
  </g>
  <circle cx="45" cy="19" r="3.6" fill="#F2A73B"/>
</svg>
"""
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(svg)


def app_icon(path: str) -> None:
    svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#6D3BD1"/>
      <stop offset="100%" stop-color="#3F1F70"/>
    </linearGradient>
  </defs>
  <rect width="512" height="512" fill="url(#bg)"/>
  <g fill="none" stroke="#F5EFFF" stroke-width="26" stroke-linecap="round" stroke-linejoin="round">
    <path d="M256 386c68-44 104-101 104-164a104 104 0 0 0-208 0c0 63 36 120 104 164z"/>
    <circle cx="256" cy="216" r="37"/>
  </g>
  <circle cx="360" cy="152" r="29" fill="#F2A73B"/>
</svg>
"""
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(svg)


def favicon(path: str) -> None:
    svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="32" height="32">
  <rect width="32" height="32" rx="9" fill="#6D3BD1"/>
  <g fill="none" stroke="#F5EFFF" stroke-width="2.4" stroke-linecap="round">
    <path d="M16 24c5-3.2 7.6-7.4 7.6-12A7.6 7.6 0 0 0 8.4 12c0 4.6 2.6 8.8 7.6 12z"/>
    <circle cx="16" cy="12.4" r="2.6"/>
  </g>
</svg>
"""
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(svg)


if __name__ == "__main__":
    for category in PALETTES:
        for index in range(8):
            cover(category, index, os.path.join(COVERS, f"{category}-{index + 1:02d}.svg"))
    hero(os.path.join(BASE, "hero.svg"))
    logo(os.path.join(BASE, "logo.svg"))
    favicon(os.path.join(BASE, "favicon.svg"))
    app_icon(os.path.join(BASE, "icons", "app-icon.svg"))
    print("Generated artwork in", os.path.abspath(BASE))
