export const MAPLIBRE_CSS = 'https://unpkg.com/maplibre-gl@5.9.0/dist/maplibre-gl.css';
export const MAPLIBRE_JS = 'https://unpkg.com/maplibre-gl@5.9.0/dist/maplibre-gl.js';

export const MARSEILLE_CENTER = { lat: 43.275, lng: 5.322 };
// Client-side fallback default only. The real tile/style server URL is configured at launch time via the server system
// property "map.provider.tileserver", which the com.sap.sailing.gwt.ui Activator reads and the MapLibre provider
// publishes to window.__sapMapTileServerStyleUrl (see MapLibreProvider). This constant is used by createRaceStyle() only
// when that global is unset - i.e. when the server RPC delivering the configured URL has failed (the server itself
// already falls back to the public OpenFreeMap "liberty" style via Activator.DEFAULT_MAP_TILESERVER_STYLE_URL when the
// property is not set, so under normal operation the global is always present).
export const DEFAULT_RACE_VECTOR_STYLE_URL = 'https://tiles.openfreemap.org/styles/liberty';
export const RACE_WATER_COLOR = '#00437d';

export function lngLat(point) {
    const lng = typeof point.lng === 'function' ? point.lng() : point.lng;
    const lat = typeof point.lat === 'function' ? point.lat() : point.lat;
    return [lng, lat];
}

function toMapLibreZoom(googleZoom) {
    return googleZoom - 1;
}

export function createRaceStyle() {
    // Prefer the launch-time-configured style URL (window.__sapMapTileServerStyleUrl, set by MapLibreProvider from the
    // "map.provider.tileserver" system property); fall back to DEFAULT_RACE_VECTOR_STYLE_URL only if that global is unset
    // (e.g. the server RPC failed).
    return (typeof window !== 'undefined' && window.__sapMapTileServerStyleUrl) || DEFAULT_RACE_VECTOR_STYLE_URL;
}

// Returns true iff `url` is same-origin as the configured tile-server style URL, so we only ever attach the tile-server
// access-token headers to requests actually going to the (self-hosted) tile server - never to third parties such as the
// Esri satellite tiles or the OpenSeaMap overlay. Anything unparseable is treated as not same-origin (fail closed).
function sameOrigin(url, styleUrl) {
    try {
        const base = (typeof window !== 'undefined' && window.location) ? window.location.href : undefined;
        return new URL(url, base).origin === new URL(styleUrl, base).origin;
    } catch (e) {
        return false;
    }
}

// MapLibre `transformRequest` hook: attaches the short-lived tile-server access token as the X-OFM-Md5 / X-OFM-Expires /
// X-OFM-Kid request headers, but only for requests going to the configured tile server and only while a token is
// published (window.__sapMapTileTokenMd5, kept fresh by MapTileTokenRefresher). Keeping the token in headers rather than
// the URL preserves tile URL cacheability. When authentication is disabled or no token is available yet, the request is
// passed through unchanged.
export function mapTileServerTransformRequest(url, resourceType) {
    const md5 = typeof window !== 'undefined' ? window.__sapMapTileTokenMd5 : null;
    const styleUrl = (typeof window !== 'undefined' && window.__sapMapTileServerStyleUrl) || '';
    if (!md5 || !styleUrl || !sameOrigin(url, styleUrl)) {
        return { url };
    }
    return {
        url,
        headers: {
            'X-OFM-Md5': md5,
            'X-OFM-Expires': String(window.__sapMapTileTokenExpires),
            'X-OFM-Kid': window.__sapMapTileTokenKid || ''
        }
    };
}

// MapLibre `error` handler: when a tile-server request is rejected because the token is missing/expired (HTTP 401/403),
// force an out-of-schedule token refresh via the global installed by MapTileTokenRefresher so access recovers without
// waiting for the next scheduled refresh. Other errors are ignored here (MapLibre still logs them to the console).
function onMapTileServerError(event) {
    const status = event && event.error && event.error.status;
    if ((status === 401 || status === 403) && typeof window !== 'undefined'
            && typeof window.__sapMapTileTokenRefresh === 'function') {
        window.__sapMapTileTokenRefresh();
    }
}

export function applyRaceStyle(map, seaMarksVisible = false) {
    const apply = () => {
        for (const layer of map.getStyle().layers || []) {
            if (layer.type === 'fill' && /water/i.test(`${layer.id} ${layer['source-layer'] || ''}`)) {
                map.setPaintProperty(layer.id, 'fill-color', RACE_WATER_COLOR);
            }
            if (layer.type === 'line' && /waterway/i.test(`${layer.id} ${layer['source-layer'] || ''}`)) {
                map.setPaintProperty(layer.id, 'line-color', '#2b7eb3');
            }
        }
        if (seaMarksVisible && !map.getSource('openseamap')) {
            map.addSource('openseamap', {
                type: 'raster',
                tiles: ['https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png'],
                tileSize: 256,
                attribution: '© OpenSeaMap contributors',
                maxzoom: 18
            });
        }
        if (seaMarksVisible && !map.getLayer('openseamap')) {
            map.addLayer({ id: 'openseamap', type: 'raster', source: 'openseamap', paint: { 'raster-opacity': 0.75 } });
        } else if (map.getLayer('openseamap')) {
            map.setLayoutProperty('openseamap', 'visibility', seaMarksVisible ? 'visible' : 'none');
        }
    };
    if (map.getLayer('openseamap') || map.isStyleLoaded?.() || map.loaded()) apply();
    else map.once('load', apply);
}

const SATELLITE_TILES = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';
const SATELLITE_ATTRIBUTION = 'Tiles © Esri — Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community';

export function setSatelliteVisible(map, visible) {
    const apply = () => {
        if (visible && !map.getSource('satellite')) {
            map.addSource('satellite', {
                type: 'raster',
                tiles: [SATELLITE_TILES],
                tileSize: 256,
                attribution: SATELLITE_ATTRIBUTION,
                maxzoom: 19
            });
        }
        if (visible && !map.getLayer('satellite')) {
            // Insert below the first symbol layer so vector labels stay on top (Google HYBRID-ish);
            // fall back to below the seamark overlay so seamarks always remain visible.
            const layers = map.getStyle().layers || [];
            const firstSymbol = layers.find(layer => layer.type === 'symbol');
            const before = firstSymbol?.id || (map.getLayer('openseamap') ? 'openseamap' : undefined);
            map.addLayer({ id: 'satellite', type: 'raster', source: 'satellite' }, before);
        } else if (map.getLayer('satellite')) {
            map.setLayoutProperty('satellite', 'visibility', visible ? 'visible' : 'none');
        }
    };
    if (map.isStyleLoaded?.()) apply();
    else map.once('idle', apply);
}

// Adds a compact AttributionControl that starts collapsed. MapLibre's `compact: true` still opens
// the bubble once attributions populate (on styledata/sourcedata), so we strip the "show" class the
// first time it appears. After that the control keeps `maplibregl-compact`, so MapLibre no longer
// re-expands it and user clicks continue to toggle it normally.
export function addCollapsedAttributionControl(map, position = 'bottom-right') {
    const control = new maplibregl.AttributionControl({ compact: true });
    map.addControl(control, position);
    const collapse = () => {
        const attrib = map.getContainer().querySelector('.maplibregl-ctrl-attrib.maplibregl-compact-show');
        if (attrib) {
            attrib.classList.remove('maplibregl-compact-show');
            map.off('styledata', collapse);
            map.off('sourcedata', collapse);
            map.off('idle', collapse);
        }
    };
    map.on('styledata', collapse);
    map.on('sourcedata', collapse);
    map.on('idle', collapse);
    return control;
}

export function createRaceMap(containerId, options = {}) {
    const center = options.center || MARSEILLE_CENTER;
    const map = new maplibregl.Map({
        container: containerId,
        style: createRaceStyle(),
        center: lngLat(center),
        zoom: toMapLibreZoom(options.zoom ?? 15),
        bearing: options.bearing ?? 0,
        pitch: 0,
        attributionControl: false,
        transformRequest: mapTileServerTransformRequest
    });
    map.on('error', onMapTileServerError);
    map.addControl(new maplibregl.NavigationControl({ visualizePitch: false }), 'top-right');
    addCollapsedAttributionControl(map, 'bottom-right');
    applyRaceStyle(map, options.seaMarksVisible);
    return map;
}

export function createArrowSvg(color = '#ff0000', scale = 6, strokeColor = '#fff') {
    const size = scale * 6;
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '-18 -18 36 36');
    svg.setAttribute('width', size);
    svg.setAttribute('height', size);
    svg.style.display = 'block';
    svg.style.filter = 'drop-shadow(0 0 1px #111)';
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', 'M 0 -16 L 13 12 L 0 6 L -13 12 Z');
    path.setAttribute('fill', color);
    path.setAttribute('stroke', strokeColor);
    path.setAttribute('stroke-width', '2');
    path.setAttribute('stroke-linejoin', 'round');
    svg.appendChild(path);
    return svg;
}

export function lineFeature(id, points, properties = {}) {
    return {
        type: 'Feature',
        id,
        properties: { id, ...properties },
        geometry: { type: 'LineString', coordinates: points.map(lngLat) }
    };
}

export function polygonFeature(id, rings, properties = {}) {
    return {
        type: 'Feature',
        id,
        properties: { id, ...properties },
        geometry: { type: 'Polygon', coordinates: rings.map(ring => ring.map(lngLat)) }
    };
}

export function lineCollection(features) {
    return { type: 'FeatureCollection', features };
}

export function setTestState(patch) {
    window.__testState = { ...(window.__testState || {}), ...patch };
    return window.__testState;
}

export function generateBoatTrack(boatIdx, totalSteps = 100) {
    const track = [];
    let lat = 43.270;
    let lng = 5.320 + boatIdx * 0.001;
    let heading = boatIdx % 2 === 0 ? 45 : -45;
    const tackInterval = 8 + boatIdx;
    for (let i = 0; i < totalSteps; i++) {
        if (i > 0 && i % tackInterval === 0) {
            heading = heading > 0 ? -45 : 45;
        }
        const stepSize = 0.00035 + boatIdx * 0.00001;
        const rad = heading * Math.PI / 180;
        lat += stepSize * Math.cos(rad);
        lng += stepSize * Math.sin(rad) * 0.65;
        track.push({ lat, lng, heading: heading + 90, speed: 5 + boatIdx * 0.5 });
    }
    return track;
}

export function makeCircle(center, radiusDeg, steps = 64) {
    const ring = [];
    for (let i = 0; i <= steps; i++) {
        const angle = (i / steps) * Math.PI * 2;
        ring.push({
            lat: center.lat + Math.cos(angle) * radiusDeg,
            lng: center.lng + Math.sin(angle) * radiusDeg
        });
    }
    return ring;
}
