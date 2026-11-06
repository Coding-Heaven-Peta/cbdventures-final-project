"""CBDVentures map API and live Folium preview."""

from __future__ import annotations

import logging
import os
from html import escape
from pathlib import Path
from typing import Any

import folium
import pymysql
from dotenv import load_dotenv
from flask import Flask, jsonify, request
from folium import Circle, FeatureGroup, LayerControl, Map, Marker, Popup
from folium.plugins import BeautifyIcon
from pymysql.cursors import DictCursor

from map_core import distance_metres, to_geojson_feature, valid_coordinate


BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")


def _env_float(name: str, default: str) -> float:
    try:
        value = float(os.getenv(name, default))
    except ValueError as error:
        raise RuntimeError(f"{name} must be a number.") from error
    if not -90 <= value <= 90 and name.endswith("_LAT"):
        raise RuntimeError(f"{name} must be between -90 and 90.")
    if not -180 <= value <= 180 and name.endswith("_LON"):
        raise RuntimeError(f"{name} must be between -180 and 180.")
    return value


CENTER_LAT = _env_float("MAP_CENTER_LAT", "-25.746400")
CENTER_LON = _env_float("MAP_CENTER_LON", "28.192483")
MAX_RADIUS_METRES = int(os.getenv("MAP_RADIUS_METRES", "1000"))
MAP_STATUSES = tuple(
    status.strip().upper()
    for status in os.getenv("MAP_STATUSES", "APPROVED,PUBLISHED").split(",")
    if status.strip()
)
if not 1 <= MAX_RADIUS_METRES <= 1000:
    raise RuntimeError("MAP_RADIUS_METRES must be between 1 and 1000.")
if not MAP_STATUSES:
    raise RuntimeError("MAP_STATUSES must contain at least one listing status.")


def _database_connection():
    required = ("DB_HOST", "DB_NAME", "DB_USER")
    missing = [key for key in required if not os.getenv(key)]
    if missing:
        raise RuntimeError(
            "Missing database settings in .env: " + ", ".join(missing)
        )
    try:
        port = int(os.getenv("DB_PORT", "3306"))
    except ValueError as error:
        raise RuntimeError("DB_PORT must be an integer.") from error
    return pymysql.connect(
        host=os.environ["DB_HOST"],
        port=port,
        user=os.environ["DB_USER"],
        password=os.getenv("DB_PASSWORD", ""),
        database=os.environ["DB_NAME"],
        connect_timeout=10,
        read_timeout=15,
        write_timeout=15,
        cursorclass=DictCursor,
        charset="utf8mb4",
    )


def fetch_businesses() -> list[dict[str, Any]]:
    """Load currently eligible business locations from the existing MySQL schema."""
    status_placeholders = ", ".join(["%s"] * len(MAP_STATUSES))
    query = f"""
        SELECT
            b.business_id,
            b.name,
            b.description,
            b.address,
            b.latitude,
            b.longitude,
            b.contact_phone,
            b.price_level,
            b.status,
            c.category_name
        FROM business AS b
        LEFT JOIN category AS c
            ON c.category_id = b.category_id
        WHERE UPPER(b.status) IN ({status_placeholders})
          AND b.latitude IS NOT NULL
          AND b.longitude IS NOT NULL
        ORDER BY b.name
    """
    connection = _database_connection()
    try:
        with connection.cursor() as cursor:
            cursor.execute(query, MAP_STATUSES)
            rows = cursor.fetchall()
    finally:
        connection.close()

    businesses: list[dict[str, Any]] = []
    for row in rows:
        if not valid_coordinate(row.get("latitude"), row.get("longitude")):
            continue
        latitude = float(row["latitude"])
        longitude = float(row["longitude"])
        distance = distance_metres(latitude, longitude, CENTER_LAT, CENTER_LON)
        if distance > MAX_RADIUS_METRES:
            continue
        businesses.append(
            {
                "business_id": row["business_id"],
                "name": row["name"],
                "description": row.get("description"),
                "address": row.get("address"),
                "latitude": latitude,
                "longitude": longitude,
                "contact_phone": row.get("contact_phone"),
                "price_level": row.get("price_level"),
                "status": row.get("status"),
                "category": row.get("category_name") or "Uncategorised",
                "distance_m": round(distance, 1),
            }
        )
    return businesses


def filter_businesses(
    businesses: list[dict[str, Any]], category: str | None, search: str | None, radius: int
) -> list[dict[str, Any]]:
    category_key = category.strip().casefold() if category else ""
    search_key = search.strip().casefold() if search else ""
    return [
        business
        for business in businesses
        if distance_metres(
            business["latitude"], business["longitude"], CENTER_LAT, CENTER_LON
        )
        <= radius
        and (not category_key or business["category"].casefold() == category_key)
        and (
            not search_key
            or search_key in str(business["name"]).casefold()
            or search_key in str(business.get("address") or "").casefold()
        )
    ]


def _parse_radius() -> int:
    value = request.args.get("radius_m", str(MAX_RADIUS_METRES))
    try:
        radius = int(value)
    except ValueError as error:
        raise ValueError("radius_m must be a whole number of metres.") from error
    if not 1 <= radius <= MAX_RADIUS_METRES:
        raise ValueError(f"radius_m must be between 1 and {MAX_RADIUS_METRES}.")
    return radius


def _api_response(businesses: list[dict[str, Any]], radius: int) -> dict[str, Any]:
    return {
        "center": {"latitude": CENTER_LAT, "longitude": CENTER_LON},
        "radius_m": radius,
        "count": len(businesses),
        "businesses": businesses,
    }


def _marker_color(category: str) -> str:
    value = category.casefold()
    if "salon" in value or "personal care" in value:
        return "purple"
    if "study" in value or "social" in value or "library" in value:
        return "blue"
    if "event" in value:
        return "orange"
    if "food" in value or "drink" in value or "eat" in value or "restaurant" in value:
        return "green"
    return "cadetblue"


def create_map(businesses: list[dict[str, Any]], radius: int) -> folium.Map:
    map_view = Map(
        location=[CENTER_LAT, CENTER_LON],
        zoom_start=16,
        control_scale=True,
        tiles="OpenStreetMap",
    )
    Circle(
        location=[CENTER_LAT, CENTER_LON],
        radius=radius,
        color="#2563eb",
        weight=2,
        fill=True,
        fill_color="#3b82f6",
        fill_opacity=0.08,
        tooltip=f"{radius} m CBDVentures pilot boundary",
    ).add_to(map_view)
    Marker(
        location=[CENTER_LAT, CENTER_LON],
        tooltip="Richfield Pretoria campus (map centre)",
        icon=BeautifyIcon(
            icon="graduation-cap",
            icon_shape="marker",
            border_color="#1d4ed8",
            text_color="#1d4ed8",
        ),
    ).add_to(map_view)

    category_groups: dict[str, FeatureGroup] = {}
    for business in businesses:
        category = business["category"]
        group = category_groups.get(category)
        if group is None:
            group = FeatureGroup(name=category, show=True)
            group.add_to(map_view)
            category_groups[category] = group

        name = escape(str(business["name"]))
        category_label = escape(category)
        address = escape(str(business.get("address") or "Not available"))
        description = escape(str(business.get("description") or "Not available"))
        phone = escape(str(business.get("contact_phone") or "Not available"))
        price = escape(str(business.get("price_level") or "Not available"))
        directions_url = (
            "https://www.google.com/maps/dir/?api=1"
            f"&destination={business['latitude']},{business['longitude']}"
        )
        popup_html = (
            f"<strong>{name}</strong><br>"
            f"Category: {category_label}<br>"
            f"Distance from campus: {business['distance_m']} m<br>"
            f"Address: {address}<br>"
            f"Phone: {phone}<br>"
            f"Price level: {price}<br>"
            f"{description}<br>"
            f'<a href="{directions_url}" target="_blank" rel="noopener noreferrer">'
            "Get directions</a>"
        )
        Marker(
            location=[business["latitude"], business["longitude"]],
            tooltip=f"{name} · {business['distance_m']} m",
            popup=Popup(popup_html, max_width=320),
            icon=BeautifyIcon(
                icon="map-marker",
                icon_shape="marker",
                border_color=_marker_color(category),
                text_color=_marker_color(category),
            ),
        ).add_to(group)
    LayerControl(collapsed=False).add_to(map_view)
    return map_view


def create_app() -> Flask:
    app = Flask(__name__)
    app.config["JSON_SORT_KEYS"] = False
    app.logger.setLevel(logging.INFO)
    allowed_origin = os.getenv("CORS_ORIGIN", "*")

    @app.after_request
    def add_cors_headers(response):
        response.headers["Access-Control-Allow-Origin"] = allowed_origin
        response.headers["Access-Control-Allow-Headers"] = "Content-Type"
        response.headers["Access-Control-Allow-Methods"] = "GET, OPTIONS"
        return response

    @app.errorhandler(ValueError)
    def invalid_request(error):
        return jsonify({"error": str(error)}), 400

    @app.errorhandler(pymysql.MySQLError)
    @app.errorhandler(RuntimeError)
    def service_error(error):
        app.logger.exception("Map request failed")
        return jsonify({"error": "Map data is temporarily unavailable."}), 503

    @app.get("/api/health")
    def health():
        connection = _database_connection()
        try:
            with connection.cursor() as cursor:
                cursor.execute("SELECT 1 AS database_connected")
                cursor.fetchone()
        finally:
            connection.close()
        return jsonify({"status": "ok", "database": "connected"})

    @app.get("/api/map/businesses")
    def businesses_endpoint():
        radius = _parse_radius()
        result = filter_businesses(
            fetch_businesses(),
            request.args.get("category"),
            request.args.get("q"),
            radius,
        )
        return jsonify(_api_response(result, radius))

    @app.get("/api/map/geojson")
    def geojson_endpoint():
        radius = _parse_radius()
        result = filter_businesses(
            fetch_businesses(),
            request.args.get("category"),
            request.args.get("q"),
            radius,
        )
        return jsonify(
            {
                "type": "FeatureCollection",
                "features": [to_geojson_feature(item) for item in result],
                "center": {"latitude": CENTER_LAT, "longitude": CENTER_LON},
                "radius_m": radius,
            }
        )

    @app.get("/api/map/metadata")
    def metadata_endpoint():
        categories = sorted(
            {item["category"] for item in fetch_businesses()},
            key=str.casefold,
        )
        return jsonify(
            {
                "center": {"latitude": CENTER_LAT, "longitude": CENTER_LON},
                "radius_m": MAX_RADIUS_METRES,
                "categories": categories,
            }
        )

    @app.get("/map")
    def map_preview():
        radius = _parse_radius()
        result = filter_businesses(
            fetch_businesses(),
            request.args.get("category"),
            request.args.get("q"),
            radius,
        )
        return create_map(result, radius).get_root().render()

    @app.get("/map/preview")
    def map_preview_without_database():
        radius = _parse_radius()
        map_view = create_map([], radius)
        map_view.get_root().html.add_child(
            folium.Element(
                '<div style="position:fixed;top:10px;left:50px;z-index:9999;'
                'background:white;padding:10px 14px;border-radius:6px;'
                'box-shadow:0 1px 5px #777;font:14px Arial,sans-serif">'
                '<strong>CBDVentures map preview</strong><br>'
                f'{radius} m pilot boundary around the campus reference point.<br>'
                'No business pins are shown until MySQL is connected.'
                '</div>'
            )
        )
        return map_view.get_root().render()

    return app


app = create_app()
