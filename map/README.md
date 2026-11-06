# CBDVentures Map Handover

This folder contains the map module and current live-data demo for the
CBDVentures frontend team.

## What is included

- Python_Source/ — Flask API, MySQL query, Folium map builder, dependencies,
  safe configuration example, tests, and Windows setup/run scripts.
- Map_Demo/cbdventures_map_preview.html — standalone interactive map generated
  from the current MySQL records.
- Map_Data/businesses.geojson — current business pin data for Leaflet.

## Quick start on Windows

1. Install Python 3.10 or later and copy this whole handover folder to the
   developer's computer.
2. Double-click setup_windows.bat in the handover folder. It installs the
   dependencies and opens Python_Source\.env for configuration.
3. In the .env file opened by setup, enter the developer's own MySQL
   connection values. Do not copy another person's password. Use a read-only
   MySQL account with access to cbdventures_db.
4. Double-click run_map_api.bat.
5. Open <http://127.0.0.1:5000/map> in a browser.

The API host is local to the computer running it. Other teammates cannot call
that computer's 127.0.0.1 address. For frontend development, run the API on
the same computer as the frontend or configure an agreed shared development
server. Use the GeoJSON file for a quick static integration without a database
connection.

## API routes

- GET /api/health — verifies the MySQL connection.
- GET /api/map/businesses — JSON businesses, campus centre, radius, and count.
- GET /api/map/geojson — GeoJSON FeatureCollection for Leaflet.
- GET /api/map/metadata — centre, radius, and available categories.
- GET /map — live interactive map.
- GET /map/preview — map centre and 1 km boundary without database pins.

Optional query parameters for the business and GeoJSON endpoints:

- category — exact category label, e.g. Salons & Personal Care.
- q — search business name/address.
- radius_m — reduce the radius from 1,000 m; values above 1,000 are rejected.


## Confirmed live map settings

- Database: cbdventures_db
- Listings included: status APPROVED
- Campus point: latitude -25.746400, longitude 28.192483
- Pilot radius: 1,000 metres
- Current live listings: three approved businesses, at 227.6 m, 311.4 m, and
  329.4 m from the campus point.

Coordinates in Leaflet/Folium use latitude then longitude. GeoJSON correctly
stores coordinates as longitude then latitude.

The Phase 4 report contains a different campus point. The map uses the point
confirmed against the database's SQL distance audit.We need to fix that too.

## Schema and responsibilities

The frontend should call the API or load the GeoJSON file; it must not connect
directly to MySQL. Keep credentials private and do not use a MySQL root account
for a shared deployment. The main project design specifies Java/Spring Boot, we need to also confirm which one we going with.

## Refresh the demo/export

After setting up the Python environment and .env, run
Python_Source\generate_map_files.bat. It retrieves fresh data from MySQL and
updates Python_Source\output\. The two files in Map_Demo and Map_Data are
the snapshot generated.

OpenStreetMap tiles need internet access. Keep the map's visible
OpenStreetMap attribution; do not bulk-download public tile data for offline
use.

## Tests

From Python_Source, run:

powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
