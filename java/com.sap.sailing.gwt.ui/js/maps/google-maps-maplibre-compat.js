// Core facade: implements the Google Maps JavaScript API surface on MapLibre GL JS.
// Keep GWT wrapper conventions in gwt-maps-maplibre-compat.js.
import { addCollapsedAttributionControl, applyRaceStyle, createArrowSvg, createRaceStyle, lngLat, mapTileServerTransformRequest, onMapTileServerError, setSatelliteVisible } from './maplibre-test-utils.js?v=race-map-feedback-19';

function asLngLatLiteral(value) {
    if (Array.isArray(value)) return { lat: value[1], lng: value[0] };
    if (typeof value.lat === 'function') return { lat: value.lat(), lng: value.lng() };
    return value;
}

const GOOGLE_ZOOM_SCALE_CORRECTION = Math.log2(1.00225);

function toMapLibreZoom(googleZoom) {
    return googleZoom - 1 + GOOGLE_ZOOM_SCALE_CORRECTION;
}

function toGoogleZoom(mapLibreZoom) {
    return mapLibreZoom + 1 - GOOGLE_ZOOM_SCALE_CORRECTION;
}

function headingDelta(from, to) {
    const delta = Math.abs((from - to) % 360);
    return delta > 180 ? 360 - delta : delta;
}

function isSatelliteMapType(mapTypeId) {
    const id = typeof mapTypeId === 'string' ? mapTypeId.toLowerCase() : mapTypeId;
    return id === 'satellite' || id === 'hybrid';
}

function geodesicCoordinates(points) {
    const coordinates = points.map(point => lngLat(asLngLatLiteral(point)));
    if (coordinates.length < 2) return coordinates;
    const result = [coordinates[0]];
    const radians = Math.PI / 180;
    for (let i = 1; i < coordinates.length; i++) {
        const previous = coordinates[i - 1];
        const next = coordinates[i];
        const deltaLng = ((next[0] - previous[0] + 540) % 360) - 180;
        if (Math.abs(next[1] - previous[1]) <= 5 && Math.abs(deltaLng) <= 5) {
            result.push([result.at(-1)[0] + deltaLng, next[1]]);
            continue;
        }
        const [lng1, lat1] = previous.map(value => value * radians);
        const [lng2, lat2] = next.map(value => value * radians);
        const a = [Math.cos(lat1) * Math.cos(lng1), Math.cos(lat1) * Math.sin(lng1), Math.sin(lat1)];
        const b = [Math.cos(lat2) * Math.cos(lng2), Math.cos(lat2) * Math.sin(lng2), Math.sin(lat2)];
        const angle = Math.acos(Math.max(-1, Math.min(1, a[0] * b[0] + a[1] * b[1] + a[2] * b[2])));
        let steps = Math.max(1, Math.ceil(angle / (5 * radians)));
        if (steps > 1 && steps % 2) steps++;
        const sinAngle = Math.sin(angle);
        for (let step = 1; step <= steps; step++) {
            const fraction = step / steps;
            const x = angle < 1e-12 ? a[0] : (Math.sin((1 - fraction) * angle) * a[0] + Math.sin(fraction * angle) * b[0]) / sinAngle;
            const y = angle < 1e-12 ? a[1] : (Math.sin((1 - fraction) * angle) * a[1] + Math.sin(fraction * angle) * b[1]) / sinAngle;
            const z = angle < 1e-12 ? a[2] : (Math.sin((1 - fraction) * angle) * a[2] + Math.sin(fraction * angle) * b[2]) / sinAngle;
            let lng = Math.atan2(y, x) / radians;
            const previousLng = result.at(-1)[0];
            while (lng - previousLng > 180) lng -= 360;
            while (lng - previousLng < -180) lng += 360;
            result.push([lng, Math.atan2(z, Math.hypot(x, y)) / radians]);
        }
    }
    return result;
}

function circleCoordinates(center, radiusMeters, steps = 64) {
    const { lat, lng } = asLngLatLiteral(center);
    const earthRadius = 6371000;
    const distance = radiusMeters / earthRadius;
    const latRad = lat * Math.PI / 180;
    const lngRad = lng * Math.PI / 180;
    const coords = [];
    for (let i = 0; i <= steps; i++) {
        const bearing = i / steps * Math.PI * 2;
        const pointLat = Math.asin(Math.sin(latRad) * Math.cos(distance) + Math.cos(latRad) * Math.sin(distance) * Math.cos(bearing));
        const pointLng = lngRad + Math.atan2(Math.sin(bearing) * Math.sin(distance) * Math.cos(latRad), Math.cos(distance) - Math.sin(latRad) * Math.sin(pointLat));
        coords.push([pointLng * 180 / Math.PI, pointLat * 180 / Math.PI]);
    }
    return coords;
}

class CompatLatLng {
    constructor(lat, lng) {
        this._lat = lat;
        this._lng = lng;
    }
    lat() { return this._lat; }
    lng() { return this._lng; }
    getLatitude() { return this._lat; }
    getLongitude() { return this._lng; }
    getLatDeg() { return this._lat; }
    getLngDeg() { return this._lng; }
    getX() { return this._lng; }
    getY() { return this._lat; }
    equals(other) { const point = asLngLatLiteral(other); return this._lat === point.lat && this._lng === point.lng; }
    add(other) { const point = asLngLatLiteral(other); return new CompatLatLng(this._lat + point.lat, this._lng + point.lng); }
    dotProduct(other) { const point = asLngLatLiteral(other); return this._lng * point.lng + this._lat * point.lat; }
    getNorm() { return Math.hypot(this._lng, this._lat); }
    getBearingGreatCircle(other) {
        const point = asLngLatLiteral(other);
        const toRad = deg => deg * Math.PI / 180;
        const y = Math.sin(toRad(point.lng - this._lng)) * Math.cos(toRad(point.lat));
        const x = Math.cos(toRad(this._lat)) * Math.sin(toRad(point.lat)) - Math.sin(toRad(this._lat)) * Math.cos(toRad(point.lat)) * Math.cos(toRad(point.lng - this._lng));
        return (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;
    }
    getAsSignedDecimalDegrees() { return `${this._lat.toFixed(5)}, ${this._lng.toFixed(5)}`; }
    getAsDegreesAndDecimalMinutesWithCardinalPoints() { return `${Math.abs(this._lat).toFixed(5)}°${this._lat >= 0 ? 'N' : 'S'} ${Math.abs(this._lng).toFixed(5)}°${this._lng >= 0 ? 'E' : 'W'}`; }
    toJSON() { return { lat: this._lat, lng: this._lng }; }
}

class CompatPoint {
    constructor(x, y) {
        this.x = x;
        this.y = y;
    }
    getX() { return this.x; }
    getY() { return this.y; }
    getLatDeg() { return this.y; }
    getLngDeg() { return this.x; }
    equals(other) { return this.x === other.x && this.y === other.y; }
}

class CompatLatLngBounds {
    constructor(sw, ne) {
        this.sw = asLngLatLiteral(sw);
        this.ne = asLngLatLiteral(ne);
    }
    toMapLibreBounds() {
        return [[this.sw.lng, this.sw.lat], [this.ne.lng, this.ne.lat]];
    }
    contains(latLng) {
        const point = asLngLatLiteral(latLng);
        return point.lat >= this.sw.lat && point.lat <= this.ne.lat && point.lng >= this.sw.lng && point.lng <= this.ne.lng;
    }
    extend(latLng) {
        const point = asLngLatLiteral(latLng);
        this.sw = { lat: Math.min(this.sw.lat, point.lat), lng: Math.min(this.sw.lng, point.lng) };
        this.ne = { lat: Math.max(this.ne.lat, point.lat), lng: Math.max(this.ne.lng, point.lng) };
        return this;
    }
    getNorthEast() { return new CompatLatLng(this.ne.lat, this.ne.lng); }
    getSouthWest() { return new CompatLatLng(this.sw.lat, this.sw.lng); }
    equals(other) {
        const bounds = other instanceof CompatLatLngBounds ? other : new CompatLatLngBounds(other.getSouthWest(), other.getNorthEast());
        return this.sw.lat === bounds.sw.lat && this.sw.lng === bounds.sw.lng && this.ne.lat === bounds.ne.lat && this.ne.lng === bounds.ne.lng;
    }
    intersects(other) {
        const bounds = other instanceof CompatLatLngBounds ? other : new CompatLatLngBounds(other.getSouthWest(), other.getNorthEast());
        return !(bounds.sw.lng > this.ne.lng || bounds.ne.lng < this.sw.lng || bounds.sw.lat > this.ne.lat || bounds.ne.lat < this.sw.lat);
    }
}

class CompatMap {
    constructor(element, options = {}) {
        this.element = element;
        this.options = options;
        this.listeners = new Map();
        this.mapTypes = new Map();
        this.loaded = false;
        this.deferred = [];
        this.polylineOwners = new Map();
        this.polylineFeatureCache = new Map();
        this.dirtyPolylineIds = new Set();
        this.removedPolylineIds = new Set();
        this.polylineFlushFrame = null;
        this.nextPolylineSequence = 1;
        this.cameraChangedSinceIdle = true;
        element.style.position = element.style.position || 'relative';
        element.style.isolation = 'isolate';
        this.overlayLayer = document.createElement('div');
        this.markerLayer = document.createElement('div');
        this.overlayMouseTarget = document.createElement('div');
        this.floatPane = document.createElement('div');
        Object.assign(this.overlayLayer.style, { position: 'absolute', inset: '0', zIndex: 5, pointerEvents: 'none', transformOrigin: '50% 50%' });
        Object.assign(this.markerLayer.style, { position: 'absolute', inset: '0', zIndex: 7, pointerEvents: 'none' });
        Object.assign(this.overlayMouseTarget.style, { position: 'absolute', inset: '0', zIndex: 10, pointerEvents: 'none', transformOrigin: '50% 50%' });
        Object.assign(this.floatPane.style, { position: 'absolute', inset: '0', zIndex: 15, pointerEvents: 'none' });
        element.append(this.overlayLayer, this.markerLayer, this.overlayMouseTarget, this.floatPane);
        for (const pane of [this.markerLayer, this.overlayMouseTarget, this.floatPane]) {
            for (const name of ['click', 'dblclick', 'contextmenu']) pane.addEventListener(name, event => event.stopPropagation());
        }
        this.updateOverlayPointerEvents = () => {
            const width = element.clientWidth;
            const height = element.clientHeight;
            for (const canvas of this.overlayMouseTarget.querySelectorAll('canvas')) {
                const fullMapCanvas = width > 0 && height > 0 && canvas.width >= width && canvas.height >= height;
                const pointerEvents = canvas.style.cursor === 'pointer' && !fullMapCanvas ? 'auto' : 'none';
                if (canvas.style.pointerEvents !== pointerEvents) canvas.style.pointerEvents = pointerEvents;
            }
        };
        this.overlayPointerObserver = new MutationObserver(this.updateOverlayPointerEvents);
        this.overlayPointerObserver.observe(this.overlayMouseTarget, { childList: true, subtree: true, attributes: true, attributeFilter: ['width', 'height'] });
        const center = asLngLatLiteral(options.center || { lat: 0, lng: 0 });
        const initialCenter = Number.isFinite(center.lat) && Number.isFinite(center.lng) ? center : { lat: 0, lng: 0 };
        this.map = new maplibregl.Map({
            container: element,
            style: createRaceStyle(),
            center: lngLat(initialCenter),
            zoom: toMapLibreZoom(options.zoom ?? 0),
            bearing: options.heading || 0,
            pitch: 0,
            attributionControl: false,
            transformRequest: mapTileServerTransformRequest
        });
        this.map.on('error', onMapTileServerError);
        addCollapsedAttributionControl(this.map, 'bottom-right');
        // MapLibre listens for gestures on this container, so interactive panes must be descendants.
        this.map.getCanvasContainer().append(this.overlayLayer, this.markerLayer, this.overlayMouseTarget, this.floatPane);
        requestAnimationFrame(() => this.map.resize());
        this.map.on('resize', this.updateOverlayPointerEvents);
        const controlPositions = {
            1: ['10px', '', '', '10px'], 2: ['0px', '', '', '50%'], 3: ['10px', '10px', '', ''],
            // RaceMap's LEFT_TOP control already supplies its own 10px margin; keep overlays and boats untouched.
            4: ['50%', '', '', '10px'], 5: ['60px', '', '', '0px'], 6: ['', '', '23px', '0px'],
            7: ['60px', '10px', '', ''], 8: ['50%', '10px', '', ''], 9: ['', '0px', '10px', ''],
            10: ['', '', '10px', '10px'], 11: ['', '', '10px', '50%'], 12: ['', '10px', '10px', '']
        };
        this.controls = Array.from({ length: 13 }, (_, position) => {
            const container = document.createElement('div');
            const [top, right, bottom, left] = controlPositions[position] || ['', '', '', ''];
            Object.assign(container.style, { position: 'absolute', top, right, bottom, left, zIndex: 20, pointerEvents: 'auto' });
            // Match Google control metrics; MapLibre's 12px/20px font adds baseline space below inline canvases.
            if (position === 5) container.style.font = '400 11px Roboto, Arial, sans-serif';
            if (position === 2 || position === 11) container.style.transform = 'translateX(-50%)';
            if (position === 4 || position === 8) container.style.transform = 'translateY(-50%)';
            element.appendChild(container);
            const items = [];
            return {
                push: child => { items.push(child); container.appendChild(child); return items.length; },
                pop: () => { const child = items.pop(); child?.remove(); return child; },
                getAt: index => items[index],
                getLength: () => items.length,
                insertAt: (index, child) => { items.splice(index, 0, child); container.insertBefore(child, container.children[index] || null); },
                removeAt: index => { const [child] = items.splice(index, 1); child?.remove(); return child; },
                setAt: (index, child) => { const old = items[index]; items[index] = child; old?.replaceWith(child); }
            };
        });
        if (typeof this.map._getUIString !== 'function') this.map._getUIString = key => key;
        if (options.zoomControl !== false) {
            this.map.addControl(new maplibregl.NavigationControl({ visualizePitch: false }), 'top-right');
        }
        applyRaceStyle(this.map, options.seaMarksVisible);
        setSatelliteVisible(this.map, isSatelliteMapType(options.mapTypeId));
        this.map.on('move', () => {
            this.cameraChangedSinceIdle = true;
            this.emit('bounds_changed');
            this.emit('center_changed');
        });
        this.map.on('zoomstart', event => {
            this.cameraChangedSinceIdle = true;
            this.userZoomInProgress = !!event.originalEvent;
            this.emit('zoom_changed');
            if (this.userZoomInProgress) this.emit('dragend');
        });
        this.map.on('rotate', () => this.emit('heading_changed'));
        // A rotation the user performs themselves outranks a heading still being applied.
        this.map.on('rotatestart', event => { if (event?.originalEvent) this.pendingHeading = null; });
        // Google applies a heading instantly, so it always reaches the requested angle. Here the
        // rotation is animated and any camera command arriving mid-flight cancels it, which would
        // strand the map at a partial bearing. Reconcile on idle rather than on moveend: moveend
        // also fires when rotateTo stops a previous animation, before the rotation has begun.
        this.map.on('idle', () => {
            if (this.pendingHeading == null) return;
            if (headingDelta(this.map.getBearing(), this.pendingHeading) < 0.5) {
                this.pendingHeading = null;
                return;
            }
            if (this.map.isMoving()) return;
            const heading = this.pendingHeading;
            this.pendingHeading = null;
            this.map.jumpTo({ bearing: heading });
        });
        this.map.on('idle', () => {
            if (!this.cameraChangedSinceIdle) return;
            this.cameraChangedSinceIdle = false;
            const emitIdle = () => {
                this.userZoomInProgress = false;
                this.emit('bounds_changed');
                this.emit('idle');
            };
            if (this.userZoomInProgress) {
                clearTimeout(this.userZoomIdleTimer);
                this.userZoomIdleTimer = setTimeout(emitIdle, 550);
            }
            else emitIdle();
        });
        this.map.on('dragend', () => this.emit('dragend'));
        for (const name of ['click', 'mousedown', 'mousemove', 'mouseout', 'mouseup', 'dblclick']) {
            this.map.on(name, event => {
                const relatedTarget = event.originalEvent?.relatedTarget;
                if (name === 'mouseout' && relatedTarget && this.element.contains(relatedTarget)) return;
                const mapEvent = {
                    latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng),
                    pixel: event.point && { x: event.point.x, y: event.point.y },
                    pageX: event.originalEvent?.pageX,
                    pageY: event.originalEvent?.pageY,
                    which: event.originalEvent?.which
                };
                if (name === 'mousemove') {
                    clearTimeout(this.mouseMoveTimer);
                    this.mouseMoveTimer = setTimeout(() => {
                        // Layer-filtered queryRenderedFeatures avoids scanning the full style (100+ base-tile layers).
                        // Upgrade path if still hot: rAF-coalesce, or merge all compat polylines into one shared source.
                        const hitLayers = this.getCompatHitLayerIds();
                        const overPolyline = event.point && hitLayers.length > 0 &&
                            this.map.queryRenderedFeatures?.(event.point, { layers: hitLayers }).length > 0;
                        if (!event.__compatOverlayHandled && !overPolyline) this.emit(name, mapEvent);
                    });
                }
                else this.emit(name, mapEvent);
                // mousedown avoids MapLibre dragstart re-entry; upgrade if non-drag clicks matter.
                if (name === 'mousedown') queueMicrotask(() => this.emit('dragstart', mapEvent));
            });
        }
        let lastContextMenu = 0;
        this.map.on('contextmenu', event => {
            // Suppress the browser's native context menu so a real double-right-click registers as two
            // contextmenu events (Google Maps parity: no native menu over the map).
            event.originalEvent?.preventDefault?.();
            // Double-right-click zooms out around the cursor (Google Maps parity). The app binds no
            // rightclick, so a single right-click still emits 'rightclick' and only the fast second click zooms.
            const now = (typeof performance !== 'undefined' ? performance.now() : Date.now());
            if (now - lastContextMenu < 300) {
                lastContextMenu = 0;
                if (this.map.zoomOut) this.map.zoomOut({ around: event.lngLat });
                return;
            }
            lastContextMenu = now;
            this.emit('rightclick', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) });
        });
        this.map.on('load', () => {
            this.initializePolylineBatch();
            this.loaded = true;
            this.deferred.splice(0).forEach(action => action());
        });
    }
    initializePolylineBatch() {
        if (this.map.getSource('compat-polylines')) return;
        this.map.addSource('compat-polylines', {
            type: 'geojson',
            data: { type: 'FeatureCollection', features: [] }
        });
        const sort = ['get', 'sortKey'];
        this.map.addLayer({
            id: 'compat-polylines',
            type: 'line',
            source: 'compat-polylines',
            layout: { 'line-sort-key': sort },
            paint: {
                'line-color': ['get', 'color'],
                'line-opacity': ['case', ['get', 'visible'], ['get', 'opacity'], 0],
                'line-width': ['get', 'width']
            }
        });
        this.map.addLayer({
            id: 'compat-polylines-hit',
            type: 'line',
            source: 'compat-polylines',
            filter: ['all', ['==', ['get', 'visible'], true], ['==', ['get', 'interactive'], true]],
            layout: { 'line-sort-key': sort },
            paint: {
                'line-color': '#000000',
                'line-opacity': 0,
                'line-width': ['max', ['get', 'width'], 8]
            }
        });
        for (const [mapLibreEvent, compatEvent] of POLYLINE_EVENTS) {
            this.map.on(mapLibreEvent, 'compat-polylines-hit', event => this.routePolylineEvent(mapLibreEvent, compatEvent, event));
        }
        // Pointer cursor over any interactive polyline (Google Maps parity). Dedicated handlers cover all
        // hit-layer features, including click-only lines that updatePolylineHover skips (no mouseover listener).
        const polylineCanvas = this.map.getCanvas();
        this.map.on('mouseenter', 'compat-polylines-hit', () => { polylineCanvas.style.cursor = 'pointer'; });
        this.map.on('mouseleave', 'compat-polylines-hit', () => { polylineCanvas.style.cursor = ''; });
    }
    registerPolyline(polyline) {
        if (!polyline.sequence) polyline.sequence = this.nextPolylineSequence++;
        this.polylineOwners.set(polyline.featureId, polyline);
        this.removedPolylineIds.delete(polyline.featureId);
        this.dirtyPolylineIds.add(polyline.featureId);
        this.schedulePolylineFlush();
    }
    markPolylineDirty(polyline) {
        if (this.polylineOwners.get(polyline.featureId) !== polyline) return;
        this.dirtyPolylineIds.add(polyline.featureId);
        this.schedulePolylineFlush();
    }
    unregisterPolyline(polyline) {
        if (this.polylineOwners.get(polyline.featureId) !== polyline) return;
        this.polylineOwners.delete(polyline.featureId);
        this.dirtyPolylineIds.delete(polyline.featureId);
        this.removedPolylineIds.add(polyline.featureId);
        this.schedulePolylineFlush();
    }
    schedulePolylineFlush() {
        if (this.polylineFlushFrame !== null) return;
        this.polylineFlushFrame = requestAnimationFrame(() => {
            this.polylineFlushFrame = null;
            this.flushPolylineBatch();
        });
    }
    flushPolylineBatch() {
        const source = this.map.getSource('compat-polylines');
        if (!source) return;
        // setData full-replace with per-owner feature cache. updateData({newGeometry})
        // triggered a per-feature re-tile that showed up as a distinct blink on ~1 Hz polylines
        // (course middle line). setData is atomic. To keep the fast tail path cheap, only dirty
        // owners re-run feature(); everyone else reuses the cached feature. Same work per frame
        // as the old updateData diff, but the full source is published atomically.
        for (const id of this.dirtyPolylineIds) {
            const owner = this.polylineOwners.get(id);
            if (owner) this.polylineFeatureCache.set(id, owner.feature());
        }
        for (const id of this.removedPolylineIds) this.polylineFeatureCache.delete(id);
        this.dirtyPolylineIds.clear();
        this.removedPolylineIds.clear();
        source.setData({ type: 'FeatureCollection', features: [...this.polylineFeatureCache.values()] });
    }
    addListener(name, handler) {
        if (!this.listeners.has(name)) this.listeners.set(name, new Set());
        this.listeners.get(name).add(handler);
        return { remove: () => this.listeners.get(name)?.delete(handler) };
    }
    emit(name, event = {}) {
        for (const handler of this.listeners.get(name) || []) handler(event);
    }
    getCompatHitLayerIds() {
        return this.map.getLayer('compat-polylines-hit') ? ['compat-polylines-hit'] : [];
    }
    getTopmostPolyline(event, eventNames) {
        if (!event.point || !this.map.getLayer('compat-polylines-hit')) return null;
        const names = Array.isArray(eventNames) ? eventNames : [eventNames];
        return this.map.queryRenderedFeatures(event.point, { layers: ['compat-polylines-hit'] })
            .map(feature => this.polylineOwners.get(feature.properties?.compatId))
            .find(owner => names.some(name => owner?.listeners.get(name)?.size)) || null;
    }
    polylineEvent(owner, event) {
        return owner && event.lngLat ? { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) } : null;
    }
    updatePolylineHover(owner, event) {
        if (owner === this.hoveredPolyline) return;
        const previous = this.hoveredPolyline;
        this.hoveredPolyline = owner;
        const compatEvent = this.polylineEvent(previous || owner, event);
        if (previous && compatEvent) previous.emit('mouseout', compatEvent);
        if (owner && compatEvent) owner.emit('mouseover', compatEvent);
    }
    routePolylineEvent(mapLibreEvent, compatEvent, event) {
        if (mapLibreEvent === 'mouseleave') {
            this.updatePolylineHover(null, event);
            return;
        }
        if (mapLibreEvent === 'mouseenter' || mapLibreEvent === 'mousemove') {
            this.updatePolylineHover(this.getTopmostPolyline(event, ['mouseover', 'mouseout', 'mousemove']), event);
            if (mapLibreEvent === 'mouseenter') return;
        }
        const owner = this.getTopmostPolyline(event, compatEvent);
        if (!owner) return;
        if (mapLibreEvent === 'mousemove') event.__compatOverlayHandled = true;
        const payload = this.polylineEvent(owner, event);
        if (payload) owner.emit(compatEvent, payload);
    }
    ready(action) {
        if (this.loaded) action();
        else this.deferred.push(action);
    }
    getDiv() { return this.element; }
    getBounds() {
        const bounds = this.map.getBounds();
        return new CompatLatLngBounds(
            { lat: bounds.getSouth(), lng: bounds.getWest() },
            { lat: bounds.getNorth(), lng: bounds.getEast() }
        );
    }
    getZoom() { return toGoogleZoom(this.map.getZoom()); }
    setZoom(zoom) {
        const options = { zoom: toMapLibreZoom(zoom) };
        if (this.pendingPanCenter) {
            options.center = this.pendingPanCenter;
            this.pendingPanCenter = null;
        }
        this.map.jumpTo(options);
    }
    getCenter() {
        const center = this.map.getCenter();
        return new CompatLatLng(center.lat, center.lng);
    }
    setCenter(center) { this.map.setCenter(lngLat(asLngLatLiteral(center))); }
    fitBounds(bounds) { this.map.fitBounds(bounds.toMapLibreBounds()); }
    panTo(position) {
        const center = lngLat(asLngLatLiteral(position));
        this.pendingPanCenter = center;
        this.map.once('moveend', () => {
            if (this.pendingPanCenter !== center) return;
            const current = this.map.getCenter();
            if (Math.abs(current.lng - center[0]) < 1e-9 && Math.abs(current.lat - center[1]) < 1e-9) this.pendingPanCenter = null;
        });
        this.map.panTo(center);
    }
    setHeading(degrees) {
        this.pendingHeading = degrees;
        this.map.rotateTo(degrees, { duration: 500, easing: t => t * (2 - t) });
    }
    getHeading() { return (this.map.getBearing() + 360) % 360; }
    setMapTypeId(mapTypeId) { this.options.mapTypeId = mapTypeId; setSatelliteVisible(this.map, isSatelliteMapType(mapTypeId)); }
    getMapTypeId() { return this.options.mapTypeId || 'roadmap'; }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        const camera = {};
        if ('center' in options) camera.center = lngLat(asLngLatLiteral(options.center));
        if ('zoom' in options) camera.zoom = toMapLibreZoom(options.zoom);
        if ('heading' in options) camera.bearing = options.heading;
        if ('heading' in options && !('center' in options) && !('zoom' in options)) this.setHeading(options.heading);
        else if (Object.keys(camera).length) this.map.jumpTo(camera);
        if ('seaMarksVisible' in options) applyRaceStyle(this.map, options.seaMarksVisible);
        if ('draggable' in options) this.map.dragPan[options.draggable ? 'enable' : 'disable']();
        if ('mapTypeId' in options) setSatelliteVisible(this.map, isSatelliteMapType(options.mapTypeId));
    }
    resize() { this.map.resize(); this.emit('resize'); }
}

let nextOverlayId = 1;
const POLYLINE_EVENTS = [
    ['mouseenter', 'mouseover'], ['mousemove', 'mousemove'], ['mouseleave', 'mouseout'],
    ['mousedown', 'mousedown'], ['mouseup', 'mouseup'], ['click', 'click']
];

function createCompatArray(values = []) {
    if (values?.__compatArrayValues && values?.__compatArraySubscribers) return values;
    const items = values?.toArray ? values.toArray() : [...values];
    const subscribers = new Set();
    const notify = () => {
        for (const subscriber of [...subscribers]) subscriber();
    };
    return {
        __compatArrayValues: items,
        __compatArraySubscribers: subscribers,
        getAt: index => items[index],
        get: index => items[index],
        getLength: () => items.length,
        push: value => { const length = items.push(value); notify(); return length; },
        pop: () => { const value = items.pop(); notify(); return value; },
        insertAt: (index, value) => { items.splice(index, 0, value); notify(); },
        removeAt: index => { const value = items.splice(index, 1)[0]; notify(); return value; },
        setAt: (index, value) => { items[index] = value; notify(); },
        add: value => { const length = items.push(value); notify(); return length; },
        clear: () => { items.splice(0); notify(); },
        forEach: callback => items.forEach((value, index) => callback(value, index)),
        getArray: () => items,
        toArray: () => [...items]
    };
}

class CompatPolyline {
    constructor(options = {}) {
        this.options = options;
        this.path = createCompatArray(options.path || []);
        this.pathChanged = () => this.map?.markPolylineDirty(this);
        this.path.__compatArraySubscribers.add(this.pathChanged);
        this.visible = options.visible !== false;
        this.listeners = new Map();
        this.featureId = `compat-polyline-${nextOverlayId++}`;
        this.id = this.featureId;
        if (options.map) this.setMap(options.map);
    }
    addListener(name, handler) {
        if (!this.listeners.has(name)) this.listeners.set(name, new Set());
        const handlers = this.listeners.get(name);
        handlers.add(handler);
        this.map?.markPolylineDirty(this);
        return { remove: () => {
            const removed = handlers.delete(handler);
            if (removed) this.map?.markPolylineDirty(this);
        } };
    }
    emit(name, event) {
        for (const handler of this.listeners.get(name) || []) handler(event);
    }
    feature() {
        const zIndex = Number(this.options.zIndex) || 0;
        // Fractional insertion order is enough until a map creates one billion polylines.
        const sortKey = zIndex + (this.sequence || 0) / 1e9;
        return {
            type: 'Feature',
            id: this.featureId,
            properties: {
                // MapLibre's queryRenderedFeatures does not preserve string feature.id
                // through the tile pipeline (returns 0). Route hover/click via this properties.compatId.
                compatId: this.featureId,
                color: this.options.strokeColor || '#000000',
                opacity: this.options.strokeOpacity ?? 1,
                width: this.options.strokeWeight || 1,
                visible: this.visible,
                // Google Maps treats clickable polylines (default true) as interactive for hit-testing
                // and the pointer cursor, independent of JS listeners. Tails have no listeners but are clickable,
                // so they must still show the pointer cursor. Click routing checks listeners before dispatching.
                interactive: this.options.clickable !== false,
                sortKey
            },
            geometry: {
                type: 'LineString',
                coordinates: this.options.geodesic
                    ? geodesicCoordinates(this.path.__compatArrayValues)
                    : this.path.__compatArrayValues.map(point => lngLat(asLngLatLiteral(point)))
            }
        };
    }
    setMap(map) {
        if (this.map === map) return;
        if (this.map) {
            this.map.unregisterPolyline(this);
            this.sequence = 0;
        }
        this.map = map || null;
        if (!this.map) return;
        const target = this.map;
        target.ready(() => {
            if (this.map === target) target.registerPolyline(this);
        });
    }
    getPath() { return this.path; }
    setPath(path) {
        this.path.__compatArraySubscribers.delete(this.pathChanged);
        this.path = createCompatArray(path);
        this.path.__compatArraySubscribers.add(this.pathChanged);
        this.map?.markPolylineDirty(this);
    }
    clear() { this.getPath().clear(); }
    insertAt(index, value) { this.getPath().insertAt(index, value); }
    removeAt(index) { return this.getPath().removeAt(index); }
    setAt(index, value) { this.getPath().setAt(index, value); }
    getMap() { return this.map || null; }
    getValue() { return this.options.item ?? this.options.value; }
    setEditable(value) { this.options.editable = value; }
    setEditingEnabled(value) { this.setEditable(value); }
    getEditable() { return !!this.options.editable; }
    addMouseOutMoveHandler(handler) { return this.addListener('mouseout', handler); }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        if ('visible' in options) this.setVisible(options.visible);
        if ('path' in options) this.setPath(options.path);
        else this.map?.markPolylineDirty(this);
    }
    setVisible(visible) {
        if (this.visible === visible) return;
        this.visible = visible;
        this.options.visible = visible;
        this.map?.markPolylineDirty(this);
    }
    getVisible() { return this.visible; }
}

class CompatOverlayView {
    setMap(map) {
        if (this.map && map === null) {
            if (this._redraw) this.map.map.off('move', this._redraw);
            this.onRemove?.();
            this.map = null;
            return;
        }
        this.map = map;
        map.ready(() => queueMicrotask(() => {
            if (this.map !== map) return;
            this.onAdd?.();
            this.draw?.();
            this._redraw = () => { if (this.map === map) this.draw?.(); };
            map.map.on('move', this._redraw);
        }));
    }
    getPanes() {
        return {
            mapPane: this.map.overlayLayer,
            overlayLayer: this.map.overlayLayer,
            overlayShadow: this.map.overlayLayer,
            overlayImage: this.map.overlayLayer,
            markerLayer: this.map.markerLayer,
            floatPane: this.map.floatPane,
            overlayMouseTarget: this.map.overlayMouseTarget
        };
    }
    getProjection() {
        const project = latLng => {
            const point = this.map.map.project(lngLat(asLngLatLiteral(latLng)));
            return { x: point.x, y: point.y };
        };
        const unproject = point => {
            const lngLatPoint = this.map.map.unproject([point.x, point.y]);
            return new CompatLatLng(lngLatPoint.lat, lngLatPoint.lng);
        };
        return {
            getWorldWidth: () => 512 * Math.pow(2, this.map.map.getZoom()),
            fromLatLngToDivPixel: project,
            fromDivPixelToLatLng: unproject,
            fromContainerPixelToLatLng: unproject,
            fromLatLngToContainerPixel: project
        };
    }
}

class CompatPolygon {
    constructor(options = {}) {
        this.options = options;
        this.visible = options.visible !== false;
        this.listeners = new Map();
        this.id = `compat-polygon-${nextOverlayId++}`;
        this.outerPathsChanged = () => {
            this.subscribeToRings();
            this.publish();
        };
        this.ringChanged = () => this.publish();
        this.ringSubscriptions = new Set();
        this.paths = createCompatArray([]);
        this.setPaths(options.paths ?? (options.path ? [options.path] : []));
        if (options.map) this.setMap(options.map);
    }
    addListener(name, handler) {
        if (!this.listeners.has(name)) this.listeners.set(name, new Set());
        this.listeners.get(name).add(handler);
        return { remove: () => this.listeners.get(name)?.delete(handler) };
    }
    emit(name, event) {
        for (const handler of this.listeners.get(name) || []) handler(event);
    }
    isLatLng(value) {
        return value && ((typeof value.lat === 'function' && typeof value.lng === 'function') ||
            (typeof value.lat === 'number' && typeof value.lng === 'number'));
    }
    normalizePaths(paths) {
        const candidate = createCompatArray(paths || []);
        if (!candidate.getLength()) return candidate;
        const outer = this.isLatLng(candidate.getAt(0)) ? createCompatArray([candidate]) : candidate;
        const values = outer.__compatArrayValues;
        for (let index = 0; index < values.length; index++) values[index] = createCompatArray(values[index]);
        return outer;
    }
    subscribeToRings() {
        for (const ring of this.ringSubscriptions) ring.__compatArraySubscribers.delete(this.ringChanged);
        this.ringSubscriptions.clear();
        const values = this.paths.__compatArrayValues;
        for (let index = 0; index < values.length; index++) {
            const ring = values[index] = createCompatArray(values[index]);
            ring.__compatArraySubscribers.add(this.ringChanged);
            this.ringSubscriptions.add(ring);
        }
    }
    feature() {
        const rings = this.paths.__compatArrayValues.map(path => {
            const ring = createCompatArray(path).__compatArrayValues
                .map(point => lngLat(asLngLatLiteral(point)));
            if (ring.length && (ring[0][0] !== ring.at(-1)[0] || ring[0][1] !== ring.at(-1)[1])) ring.push([...ring[0]]);
            return ring;
        });
        return {
            type: 'Feature',
            properties: {
                strokeColor: this.options.strokeColor || '#000000',
                strokeOpacity: this.options.strokeOpacity ?? 1,
                strokeWeight: this.options.strokeWeight || 1,
                fillColor: this.options.fillColor || this.options.strokeColor || '#000000',
                fillOpacity: this.options.fillOpacity ?? 0
            },
            geometry: { type: 'Polygon', coordinates: rings }
        };
    }
    publish() {
        this.map?.map.getSource(this.id)?.setData(this.feature());
    }
    setMap(map) {
        if (map === null) {
            for (const layer of [`${this.id}-fill`, `${this.id}-line`]) if (this.map?.map.getLayer(layer)) this.map.map.removeLayer(layer);
            if (this.map?.map.getSource(this.id)) this.map.map.removeSource(this.id);
            this.map = null;
            return;
        }
        this.map = map;
        map.ready(() => {
            if (this.map !== map) return;
            map.map.addSource(this.id, { type: 'geojson', data: this.feature() });
            map.map.addLayer({ id: `${this.id}-fill`, type: 'fill', source: this.id, paint: { 'fill-color': ['get', 'fillColor'], 'fill-opacity': ['get', 'fillOpacity'] } });
            map.map.addLayer({ id: `${this.id}-line`, type: 'line', source: this.id, paint: { 'line-color': ['get', 'strokeColor'], 'line-opacity': ['get', 'strokeOpacity'], 'line-width': ['get', 'strokeWeight'] } });
            map.map.on('mouseenter', `${this.id}-fill`, event => this.emit('mouseover', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) }));
            map.map.on('mouseleave', `${this.id}-fill`, event => this.emit('mouseout', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) }));
            this.setVisible(this.visible);
        });
    }
    getPath() {
        if (!this.paths.getLength()) this.paths.push(createCompatArray([]));
        return this.paths.getAt(0);
    }
    setPath(path) { this.setPaths([path]); }
    getPaths() { return this.paths; }
    setPaths(paths) {
        this.paths.__compatArraySubscribers.delete(this.outerPathsChanged);
        for (const ring of this.ringSubscriptions) ring.__compatArraySubscribers.delete(this.ringChanged);
        this.ringSubscriptions.clear();
        this.paths = this.normalizePaths(paths);
        this.paths.__compatArraySubscribers.add(this.outerPathsChanged);
        this.subscribeToRings();
        this.publish();
    }
    addMouseOutMoveHandler(handler) { return this.addListener('mouseout', handler); }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        if ('visible' in options) this.setVisible(options.visible);
        if ('paths' in options) this.setPaths(options.paths);
        else if ('path' in options) this.setPath(options.path);
        else this.publish();
    }
    setVisible(visible) {
        this.visible = visible;
        for (const layer of [`${this.id}-fill`, `${this.id}-line`]) {
            if (this.map?.map.getLayer(layer)) this.map.map.setLayoutProperty(layer, 'visibility', visible ? 'visible' : 'none');
        }
    }
    getVisible() { return this.visible; }
}

class CompatCircle {
    constructor(options = {}) {
        this.options = options;
        this.center = options.center;
        this.radius = options.radius || 0;
        this.visible = options.visible !== false;
        this.id = `compat-circle-${nextOverlayId++}`;
        if (options.map) this.setMap(options.map);
    }
    feature() {
        return {
            type: 'Feature',
            properties: {
                strokeColor: this.options.strokeColor || '#000000',
                strokeOpacity: this.options.strokeOpacity ?? 1,
                strokeWeight: this.options.strokeWeight || 1,
                fillColor: this.options.fillColor || this.options.strokeColor || '#000000',
                fillOpacity: this.options.fillOpacity ?? 0
            },
            geometry: { type: 'Polygon', coordinates: [circleCoordinates(this.center, this.radius)] }
        };
    }
    setMap(map) {
        if (map === null) {
            for (const layer of [`${this.id}-fill`, `${this.id}-line`]) if (this.map?.map.getLayer(layer)) this.map.map.removeLayer(layer);
            if (this.map?.map.getSource(this.id)) this.map.map.removeSource(this.id);
            this.map = null;
            return;
        }
        this.map = map;
        map.ready(() => {
            map.map.addSource(this.id, { type: 'geojson', data: this.feature() });
            map.map.addLayer({ id: `${this.id}-fill`, type: 'fill', source: this.id, paint: { 'fill-color': ['get', 'fillColor'], 'fill-opacity': ['get', 'fillOpacity'] } });
            map.map.addLayer({ id: `${this.id}-line`, type: 'line', source: this.id, paint: { 'line-color': ['get', 'strokeColor'], 'line-opacity': ['get', 'strokeOpacity'], 'line-width': ['get', 'strokeWeight'] } });
            this.setVisible(this.visible);
        });
    }
    setCenter(center) {
        this.center = center;
        this.map?.map.getSource(this.id)?.setData(this.feature());
    }
    setRadius(radius) {
        this.radius = radius;
        this.map?.map.getSource(this.id)?.setData(this.feature());
    }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        if ('center' in options) this.center = options.center;
        if ('radius' in options) this.radius = options.radius;
        if ('visible' in options) this.setVisible(options.visible);
        this.map?.map.getSource(this.id)?.setData(this.feature());
    }
    setVisible(visible) {
        this.visible = visible;
        for (const layer of [`${this.id}-fill`, `${this.id}-line`]) {
            if (this.map?.map.getLayer(layer)) this.map.map.setLayoutProperty(layer, 'visibility', visible ? 'visible' : 'none');
        }
    }
    getVisible() { return this.visible; }
}

class CompatInfoWindow {
    constructor(options = {}) {
        this.content = options.content || '';
        this.position = options.position;
        this.options = options;
        this.listeners = new Map();
    }
    addListener(name, handler) {
        if (!this.listeners.has(name)) this.listeners.set(name, new Set());
        this.listeners.get(name).add(handler);
        return { remove: () => this.listeners.get(name)?.delete(handler) };
    }
    emit(name) { for (const handler of this.listeners.get(name) || []) handler(); }
    addDomReadyHandler(handler) { return this.addListener('domready', handler); }
    addCloseClickHandler(handler) { return this.addListener('closeclick', handler); }
    open(map, anchor) {
        this.close(false);
        const position = this.position || anchor?.getPosition?.() || map.getCenter();
        const element = document.createElement('div');
        element.className = 'maplibregl-popup maplibregl-popup-anchor-bottom';
        element.style.position = 'absolute';
        const content = document.createElement('div');
        content.className = 'maplibregl-popup-content';
        if (this.content instanceof Node) content.appendChild(this.content);
        else content.innerHTML = this.content;
        const closeButton = document.createElement('button');
        closeButton.className = 'maplibregl-popup-close-button';
        closeButton.type = 'button';
        closeButton.textContent = '×';
        closeButton.addEventListener('click', () => this.close());
        content.appendChild(closeButton);
        element.appendChild(content);
        const draw = () => {
            const point = map.map.project(lngLat(asLngLatLiteral(position)));
            element.style.left = `${point.x}px`;
            element.style.top = `${point.y}px`;
        };
        this.popup = { element, draw, remove: () => element.remove() };
        map.floatPane.appendChild(element);
        map.map.on('move', draw);
        this.popup.off = () => map.map.off('move', draw);
        draw();
        this.emit('domready');
    }
    close(emit = true) { this.popup?.off?.(); this.popup?.remove(); this.popup = null; if (emit) this.emit('closeclick'); }
    setContent(content) { this.content = content; }
    setOptions(options = {}) { Object.assign(this.options, options); if ('content' in options) this.content = options.content; if ('position' in options) this.position = options.position; }
    setPosition(position) { this.position = position; }
}

function createSvgSymbol(pathData, icon = {}) {
    const numbers = [...pathData.matchAll(/-?\d+(?:\.\d+)?/g)].map(match => Number(match[0]));
    const xs = numbers.filter((_, index) => index % 2 === 0);
    const ys = numbers.filter((_, index) => index % 2 === 1);
    const minX = Math.min(...xs, -8);
    const maxX = Math.max(...xs, 8);
    const minY = Math.min(...ys, -8);
    const maxY = Math.max(...ys, 8);
    const padding = Math.max(icon.strokeWeight ?? 1, 1) + 1;
    const width = maxX - minX + padding * 2;
    const height = maxY - minY + padding * 2;
    const scale = icon.scale ?? 1;
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', `${minX - padding} ${minY - padding} ${width} ${height}`);
    svg.setAttribute('width', width * scale);
    svg.setAttribute('height', height * scale);
    svg.style.display = 'block';
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', pathData);
    path.setAttribute('fill', icon.fillColor || '#ff0000');
    path.setAttribute('fill-opacity', icon.fillOpacity ?? 1);
    path.setAttribute('stroke', icon.strokeColor || '#111');
    path.setAttribute('stroke-width', icon.strokeWeight ?? 1);
    path.setAttribute('stroke-linejoin', 'round');
    svg.appendChild(path);
    return svg;
}

function createMarkerVisual(icon = {}) {
    if (icon.url) {
        const img = document.createElement('img');
        img.src = icon.url;
        img.style.width = `${icon.scaledSize?.width || icon.size?.width || 18}px`;
        img.style.height = `${icon.scaledSize?.height || icon.size?.height || 18}px`;
        img.style.display = 'block';
        return img;
    }
    if (icon.path === 'forward-closed-arrow') {
        return createArrowSvg(icon.fillColor || '#ff0000', icon.scale || 6, icon.strokeColor || '#fff');
    }
    if (icon.path && icon.path !== 'circle') {
        return createSvgSymbol(icon.path, icon);
    }
    const element = document.createElement('div');
    element.style.width = `${(icon.scale || 6) * 2}px`;
    element.style.height = element.style.width;
    element.style.borderRadius = '50%';
    element.style.background = icon.fillColor || '#ff0000';
    element.style.opacity = icon.fillOpacity ?? 1;
    element.style.border = `${icon.strokeWeight ?? 1}px solid ${icon.strokeColor || '#111'}`;
    return element;
}

class CompatMarker {
    constructor(options = {}) {
        this.options = options;
        this.position = options.position;
        this.title = options.title || '';
        this.visible = options.visible !== false;
        this.element = document.createElement('div');
        this.element.title = this.title;
        this.element.style.lineHeight = '0';
        this.element.style.cursor = 'pointer';
        if (options.zIndex !== undefined) this.element.style.zIndex = options.zIndex;
        this.visual = createMarkerVisual(options.icon || {});
        this.visual.style.transform = `rotate(${options.icon?.rotation || 0}deg)`;
        this.visual.style.transformOrigin = 'center';
        this.element.appendChild(this.visual);
        if (options.map) this.setMap(options.map);
    }
    setMap(map) {
        if (map == null) {
            this.marker?.off?.();
            this.marker?.remove();
            this.marker = null;
            this.map = null;
            return;
        }
        while (map.map?.map) map = map.map;
        this.map = map;
        map.ready(() => {
            let rawMap = map.map;
            while (rawMap?.map) rawMap = rawMap.map;
            Object.assign(this.element.style, { position: 'absolute', zIndex: this.options.zIndex ?? '', pointerEvents: 'auto', transform: 'translate(-50%, -50%)' });
            const draw = () => {
                const point = rawMap.project(lngLat(asLngLatLiteral(this.position)));
                this.element.style.left = `${point.x}px`;
                this.element.style.top = `${point.y}px`;
            };
            this.marker = { remove: () => this.element.remove(), draw };
            map.markerLayer.appendChild(this.element);
            rawMap.on('move', draw);
            this.marker.off = () => rawMap.off('move', draw);
            draw();
            this.setVisible(this.visible);
        });
    }
    setPosition(position) {
        this.position = position;
        this.marker?.draw?.();
    }
    setIcon(icon = {}) {
        this.options.icon = icon;
        const visual = createMarkerVisual(icon);
        visual.style.transform = `rotate(${icon.rotation || 0}deg)`;
        visual.style.transformOrigin = 'center';
        this.visual.replaceWith(visual);
        this.visual = visual;
    }
    getPosition() { return new CompatLatLng(asLngLatLiteral(this.position).lat, asLngLatLiteral(this.position).lng); }
    getIcon() { return this.options.icon; }
    getIcon_MarkerImage() { return this.options.icon; }
    setTitle(title) { this.title = title || ''; this.element.title = this.title; }
    setZindex(zIndex) { this.options.zIndex = zIndex; this.element.style.zIndex = zIndex; }
    setZIndex(zIndex) { this.setZindex(zIndex); }
    setVisible(visible) {
        this.visible = visible;
        this.element.style.display = visible ? '' : 'none';
    }
    getVisible() { return this.visible; }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        if ('visible' in options) this.setVisible(options.visible);
    }
    setAnimation(animation) {
        this.element.classList.toggle('compat-marker-bounce', animation === 'bounce');
    }
    addListener(name, handler) {
        this.element.addEventListener(name, handler);
        return { remove: () => this.element.removeEventListener(name, handler) };
    }
    addClickHandler(handler) { return this.addListener('click', handler); }
    addMarkerMouseOverHandler(handler) { return this.addListener('mouseover', handler); }
    addMarkerMouseOutHandler(handler) { return this.addListener('mouseout', handler); }
}

function ensureCompatStyles() {
    if (document.getElementById('google-maps-maplibre-compat-styles')) return;
    const style = document.createElement('style');
    style.id = 'google-maps-maplibre-compat-styles';
    style.textContent = `@keyframes compat-marker-bounce { 0%, 100% { translate: 0 0; } 50% { translate: 0 -14px; } } .compat-marker-bounce { animation: compat-marker-bounce 450ms ease-in-out infinite; }`;
    document.head.appendChild(style);
}

export function installGoogleMapsCompat() {
    ensureCompatStyles();
    const google = globalThis.google || (globalThis.google = {});
    google.maps = {
        Map: CompatMap,
        LatLng: CompatLatLng,
        LatLngBounds: CompatLatLngBounds,
        Point: CompatPoint,
        Polyline: CompatPolyline,
        Polygon: CompatPolygon,
        Marker: CompatMarker,
        OverlayView: CompatOverlayView,
        Circle: CompatCircle,
        InfoWindow: CompatInfoWindow,
        SymbolPath: { CIRCLE: 'circle', FORWARD_CLOSED_ARROW: 'forward-closed-arrow' },
        Animation: { BOUNCE: 'bounce' },
        RenderingType: { RASTER: 'raster', VECTOR: 'vector', UNINITIALIZED: 'uninitialized' },
        MapTypeId: { ROADMAP: 'roadmap', SATELLITE: 'satellite', HYBRID: 'hybrid' },
        ControlPosition: { RIGHT_BOTTOM: 'RIGHT_BOTTOM', TOP_LEFT: 'TOP_LEFT', TOP_RIGHT: 'TOP_RIGHT', BOTTOM_LEFT: 'BOTTOM_LEFT' },
        event: {
            addListener: (target, name, handler) => target.addListener(name, handler),
            removeListener: (listener) => { if (listener && listener.remove) listener.remove(); },
            clearInstanceListeners: (target) => { if (target && target.listeners instanceof Map) target.listeners.clear(); },
            trigger: (target, name, ...args) => { if (target && typeof target.emit === 'function') target.emit(name, ...args); }
        }
    };
    return google.maps;
}
