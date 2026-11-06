const TILE_URL = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';

export function mountOpenStreetMap(container, { zone, businesses = [], focus = null }) {
  if (!window.L) {
    throw new Error('The interactive map could not load. Check your internet connection and try again.');
  }
  if (!zone) {
    throw new Error('The map area is not ready yet. Refresh the page and try again.');
  }

  const L = window.L;
  const center = [zone.anchorLatitude, zone.anchorLongitude];
  const map = L.map(container, { scrollWheelZoom: false, zoomControl: true }).setView(center, 16);
  L.tileLayer(TILE_URL, {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> contributors'
  }).addTo(map);

  const radius = (zone.primaryRadiusKm || 1) * 1000;
  const zoneRing = L.circle(center, {
    radius,
    color: '#d96c3d',
    weight: 2,
    dashArray: '7 6',
    fillColor: '#d96c3d',
    fillOpacity: 0.08
  }).addTo(map);

  L.circleMarker(center, {
    radius: 6,
    color: '#111318',
    weight: 2,
    fillColor: '#e0a33b',
    fillOpacity: 1
  }).bindPopup(textPopup(zone.name || 'Richfield campus', zone.address || '')).addTo(map);

  businesses
    .filter(hasCoordinates)
    .forEach((business) => {
      const isFocus = focus && String(focus.id) === String(business.id);
      const marker = L.circleMarker([Number(business.latitude), Number(business.longitude)], {
        radius: isFocus ? 8 : 6,
        color: '#15161a',
        weight: 2,
        fillColor: isFocus ? '#e0a33b' : '#e77b4c',
        fillOpacity: 1
      });
      marker.bindPopup(businessPopup(business)).addTo(map);
    });

  if (focus && hasCoordinates(focus)) {
    map.setView([Number(focus.latitude), Number(focus.longitude)], 17);
  } else {
    map.fitBounds(zoneRing.getBounds(), { padding: [12, 12], maxZoom: 16 });
  }
  requestAnimationFrame(() => map.invalidateSize());
  return map;
}

function hasCoordinates(business) {
  return business.latitude !== null && business.latitude !== undefined && business.latitude !== ''
    && business.longitude !== null && business.longitude !== undefined && business.longitude !== ''
    && Number.isFinite(Number(business.latitude)) && Number.isFinite(Number(business.longitude));
}

function textPopup(title, detail) {
  const content = document.createElement('div');
  const heading = document.createElement('strong');
  heading.textContent = title;
  content.append(heading);
  if (detail) {
    const address = document.createElement('div');
    address.textContent = detail;
    content.append(address);
  }
  return content;
}

function businessPopup(business) {
  const content = textPopup(business.name || 'Place', business.categoryName || '');
  const details = [];
  if (Number.isFinite(Number(business.distanceKm))) {
    details.push(`${Number(business.distanceKm).toFixed(2)} km from campus`);
  }
  if (business.reviewCount) {
    details.push(`${Number(business.rating || 0).toFixed(1)} ★`);
  }
  if (details.length) {
    const line = document.createElement('div');
    line.textContent = details.join(' · ');
    content.append(line);
  }
  if (business.id !== undefined && business.id !== null) {
    const link = document.createElement('a');
    link.href = `#/business/${encodeURIComponent(business.id)}`;
    link.textContent = 'View place';
    link.style.display = 'inline-block';
    link.style.marginTop = '5px';
    content.append(link);
  }
  return content;
}
