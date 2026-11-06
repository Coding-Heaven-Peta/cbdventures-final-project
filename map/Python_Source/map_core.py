"""Shared map data validation and distance calculations."""

from __future__ import annotations

from math import asin, cos, isfinite, radians, sin, sqrt
from typing import Any


EARTH_RADIUS_METRES = 6_371_000


def distance_metres(
    latitude: float,
    longitude: float,
    center_latitude: float,
    center_longitude: float,
) -> float:
    """Return the great-circle distance between two latitude/longitude points."""
    lat1 = radians(latitude)
    lat2 = radians(center_latitude)
    latitude_delta = radians(center_latitude - latitude)
    longitude_delta = radians(center_longitude - longitude)
    haversine = (
        sin(latitude_delta / 2) ** 2
        + cos(lat1) * cos(lat2) * sin(longitude_delta / 2) ** 2
    )
    return 2 * EARTH_RADIUS_METRES * asin(sqrt(min(1.0, haversine)))


def valid_coordinate(latitude: Any, longitude: Any) -> bool:
    """Check that a pair of values is a finite geographic coordinate."""
    try:
        lat = float(latitude)
        lon = float(longitude)
    except (TypeError, ValueError):
        return False
    return isfinite(lat) and isfinite(lon) and -90 <= lat <= 90 and -180 <= lon <= 180


def to_geojson_feature(business: dict[str, Any]) -> dict[str, Any]:
    """Convert a normalized business record to a GeoJSON point feature."""
    properties = {
        key: value
        for key, value in business.items()
        if key not in {"latitude", "longitude"}
    }
    return {
        "type": "Feature",
        "id": business["business_id"],
        "geometry": {
            "type": "Point",
            "coordinates": [business["longitude"], business["latitude"]],
        },
        "properties": properties,
    }
