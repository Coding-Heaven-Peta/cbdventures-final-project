"""Generate a refreshed HTML preview and GeoJSON handoff from MySQL."""

from __future__ import annotations

import json
from pathlib import Path

from map_api import MAX_RADIUS_METRES, create_map, fetch_businesses
from map_core import to_geojson_feature


def main() -> None:
    output_dir = Path(__file__).resolve().parent / "output"
    output_dir.mkdir(parents=True, exist_ok=True)
    businesses = fetch_businesses()

    map_path = output_dir / "cbdventures_map_preview.html"
    geojson_path = output_dir / "businesses.geojson"
    create_map(businesses, MAX_RADIUS_METRES).save(map_path)
    feature_collection = {
        "type": "FeatureCollection",
        "features": [to_geojson_feature(item) for item in businesses],
    }
    geojson_path.write_text(
        json.dumps(feature_collection, indent=2, ensure_ascii=False),
        encoding="utf-8",
    )
    print(f"Generated map preview: {map_path}")
    print(f"Exported {len(businesses)} businesses: {geojson_path}")


if __name__ == "__main__":
    main()
