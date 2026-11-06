import unittest
from unittest.mock import patch

from map_api import CENTER_LAT, CENTER_LON, MAP_STATUSES, app, fetch_businesses


class FakeCursor:
    def __init__(self, rows):
        self.rows = rows
        self.query = None
        self.parameters = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        return False

    def execute(self, query, parameters):
        self.query = query
        self.parameters = parameters

    def fetchall(self):
        return self.rows


class FakeConnection:
    def __init__(self, rows):
        self.fake_cursor = FakeCursor(rows)
        self.closed = False

    def cursor(self):
        return self.fake_cursor

    def close(self):
        self.closed = True


class MapApiTests(unittest.TestCase):
    def setUp(self):
        self.client = app.test_client()
        self.businesses = [
            {
                "business_id": 1,
                "name": "Central Cafe",
                "description": "Coffee and study",
                "address": "Pretoria CBD",
                "latitude": CENTER_LAT,
                "longitude": CENTER_LON,
                "contact_phone": None,
                "price_level": "R",
                "status": "APPROVED",
                "category": "Food & Drinks",
                "distance_m": 0.0,
            },
            {
                "business_id": 2,
                "name": "Queen Hair",
                "description": None,
                "address": "CBD",
                "latitude": CENTER_LAT,
                "longitude": CENTER_LON,
                "contact_phone": None,
                "price_level": None,
                "status": "PUBLISHED",
                "category": "Salons",
                "distance_m": 0.0,
            },
        ]

    @patch("map_api.fetch_businesses")
    def test_business_endpoint_filters_by_category_and_search(self, fetch):
        fetch.return_value = self.businesses
        response = self.client.get("/api/map/businesses?category=Salons&q=queen")
        self.assertEqual(response.status_code, 200)
        payload = response.get_json()
        self.assertEqual(payload["count"], 1)
        self.assertEqual(payload["businesses"][0]["business_id"], 2)

    @patch("map_api.fetch_businesses")
    def test_geojson_uses_geojson_coordinate_order(self, fetch):
        fetch.return_value = self.businesses[:1]
        response = self.client.get("/api/map/geojson")
        self.assertEqual(response.status_code, 200)
        feature = response.get_json()["features"][0]
        self.assertEqual(feature["geometry"]["coordinates"], [CENTER_LON, CENTER_LAT])

    def test_radius_cannot_exceed_pilot_limit(self):
        response = self.client.get("/api/map/businesses?radius_m=1001")
        self.assertEqual(response.status_code, 400)
        self.assertIn("radius_m", response.get_json()["error"])

    @patch("map_api._database_connection")
    def test_database_loader_filters_bad_coordinates_and_outside_radius(self, connect):
        connection = FakeConnection(
            [
                {
                    "business_id": 1,
                    "name": "Inside",
                    "latitude": CENTER_LAT,
                    "longitude": CENTER_LON,
                    "status": "APPROVED",
                    "category_name": "Food",
                },
                {
                    "business_id": 2,
                    "name": "Outside",
                    "latitude": CENTER_LAT + 0.02,
                    "longitude": CENTER_LON,
                    "status": "APPROVED",
                    "category_name": "Food",
                },
                {
                    "business_id": 3,
                    "name": "Invalid",
                    "latitude": None,
                    "longitude": CENTER_LON,
                    "status": "APPROVED",
                    "category_name": "Food",
                },
            ]
        )
        connect.return_value = connection

        businesses = fetch_businesses()

        self.assertEqual([item["name"] for item in businesses], ["Inside"])
        self.assertTrue(connection.closed)
        placeholders = ", ".join(["%s"] * len(MAP_STATUSES))
        self.assertIn(
            f"UPPER(b.status) IN ({placeholders})",
            connection.fake_cursor.query,
        )
        self.assertEqual(connection.fake_cursor.parameters, MAP_STATUSES)

    @patch("map_api.fetch_businesses")
    def test_map_preview_escapes_business_text(self, fetch):
        fetch.return_value = [
            {
                **self.businesses[0],
                "name": "<script>alert(1)</script>",
            }
        ]
        response = self.client.get("/map")
        self.assertEqual(response.status_code, 200)
        self.assertNotIn("<script>alert(1)</script>", response.get_data(as_text=True))
        self.assertIn("&lt;script&gt;", response.get_data(as_text=True))

    def test_map_preview_without_database_shows_boundary_and_no_fake_businesses(self):
        response = self.client.get("/map/preview")
        page = response.get_data(as_text=True)
        self.assertEqual(response.status_code, 200)
        self.assertIn("1000 m pilot boundary around the campus reference point.", page)
        self.assertIn("No business pins are shown until MySQL is connected.", page)


if __name__ == "__main__":
    unittest.main()
