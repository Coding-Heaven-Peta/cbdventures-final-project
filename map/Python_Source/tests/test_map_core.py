import unittest

from map_core import distance_metres, to_geojson_feature, valid_coordinate


class MapCoreTests(unittest.TestCase):
    def test_distance_is_zero_for_same_point(self):
        self.assertAlmostEqual(
            distance_metres(-25.7479, 28.2293, -25.7479, 28.2293),
            0,
            places=5,
        )

    def test_distance_is_symmetric(self):
        first = distance_metres(-25.7479, 28.2293, -25.7464, 28.192483)
        reverse = distance_metres(-25.7464, 28.192483, -25.7479, 28.2293)
        self.assertAlmostEqual(first, reverse, places=5)

    def test_valid_coordinate_rejects_invalid_values(self):
        self.assertTrue(valid_coordinate("-25.74", "28.22"))
        self.assertFalse(valid_coordinate(None, "28.22"))
        self.assertFalse(valid_coordinate("91", "28.22"))
        self.assertFalse(valid_coordinate("nan", "28.22"))

    def test_geojson_uses_longitude_then_latitude(self):
        feature = to_geojson_feature(
            {
                "business_id": 7,
                "name": "Example",
                "latitude": -25.7,
                "longitude": 28.2,
                "distance_m": 42.0,
            }
        )
        self.assertEqual(feature["geometry"]["coordinates"], [28.2, -25.7])
        self.assertEqual(feature["properties"]["distance_m"], 42.0)
        self.assertNotIn("latitude", feature["properties"])


if __name__ == "__main__":
    unittest.main()
