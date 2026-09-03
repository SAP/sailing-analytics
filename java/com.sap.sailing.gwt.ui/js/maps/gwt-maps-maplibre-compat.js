// GWT adapter: exposes branflake GWT Maps wrapper conventions over the Google-style MapLibre facade.
// Keep MapLibre provider behavior in google-maps-maplibre-compat.js.
import { installGoogleMapsCompat } from './google-maps-maplibre-compat.js?v=race-map-feedback-19';

function call(handler, event = {}) {
    if (typeof handler === 'function') handler(event);
    else handler?.onEvent?.(event);
}

function literal(value) {
    return typeof value?.lat === 'function' ? { lat: value.lat(), lng: value.lng() } : value;
}

function isMapWidget(value) {
    return value && typeof value.getElement === 'function' && typeof value.ready === 'function';
}

function unwrapMap(value) {
    return isMapWidget(value) && value.map ? value.map : value;
}

function asElement(value) {
    return value?.getElement ? value.getElement() : value;
}

function addOptionAccessors(options) {
    const names = ['Center', 'Clickable', 'ColorMode', 'ColorProvider', 'Content', 'DisableAutoPan', 'Draggable', 'Editable', 'FillColor', 'FillOpacity', 'Geodesic', 'Icon', 'Map', 'Path', 'Position', 'Radius', 'StrokeColor', 'StrokeOpacity', 'StrokeWeight', 'Title', 'Visible', 'ZIndex', 'Zindex'];
    for (const name of names) {
        const key = name === 'Zindex' ? 'zIndex' : name.charAt(0).toLowerCase() + name.slice(1);
        options[`set${name}`] = value => { options[key] = key === 'map' ? unwrapMap(value) : value; };
        options[`get${name}`] = () => options[key];
    }
    options.getItem = index => options[index];
    return options;
}

function wrapOptions(initial = {}) {
    const options = addOptionAccessors({ ...initial });
    if (options.map) options.map = unwrapMap(options.map);
    return options;
}

function newOverlay(NativeClass, options = {}) {
    const mapWidget = isMapWidget(options.map) ? options.map : null;
    const nativeOptions = { ...options };
    if (mapWidget) {
        if (mapWidget.map) nativeOptions.map = mapWidget.map;
        else delete nativeOptions.map;
    }
    const overlay = new NativeClass(nativeOptions);
    const setMap = overlay.setMap.bind(overlay);
    overlay.setMap = map => {
        if (isMapWidget(map) && !map.map) map.ready(() => setMap(map.map));
        else setMap(unwrapMap(map));
    };
    if (mapWidget && !mapWidget.map) mapWidget.ready(() => overlay.setMap(mapWidget.map));
    return overlay;
}

function installGwtWrapperGlobals() {
    class LatLng {
        static newInstance(lat, lng) { return new google.maps.LatLng(lat, lng); }
    }

    class LatLngBounds {
        static newInstance(sw, ne) { return new google.maps.LatLngBounds(literal(sw), literal(ne)); }
    }

    class Point {
        static newInstance(x, y) { return new google.maps.Point(x, y); }
    }

    class Size {
        constructor(width, height) { this.width = width; this.height = height; }
        getWidth() { return this.width; }
        getHeight() { return this.height; }
        setWidth(value) { this.width = value; }
        setHeight(value) { this.height = value; }
        equals(other) { return this.width === other.width && this.height === other.height; }
        static newInstance(width, height) { return new Size(width, height); }
    }

    class SphericalUtils {
        static computeDistanceBetween(from, to, radius = 6371009) {
            const a = literal(from), b = literal(to);
            const toRad = deg => deg * Math.PI / 180;
            const dLat = toRad(b.lat - a.lat);
            const dLng = toRad(b.lng - a.lng);
            const lat1 = toRad(a.lat);
            const lat2 = toRad(b.lat);
            const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
            return 2 * radius * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
        }
    }

    class MVCArray {
        constructor(values = []) {
            this.items = [...values];
            this.__compatArrayValues = this.items;
            this.__compatArraySubscribers = new Set();
        }
        __compatNotify() {
            for (const subscriber of [...this.__compatArraySubscribers]) subscriber();
        }
        getAt(index) { return this.items[index]; }
        get(index) { return this.getAt(index); }
        getLength() { return this.items.length; }
        push(value) {
            const length = this.items.push(value);
            this.__compatNotify();
            return length;
        }
        pop() {
            const value = this.items.pop();
            this.__compatNotify();
            return value;
        }
        insertAt(index, value) { this.items.splice(index, 0, value); this.__compatNotify(); }
        removeAt(index) {
            const value = this.items.splice(index, 1)[0];
            this.__compatNotify();
            return value;
        }
        setAt(index, value) { this.items[index] = value; this.__compatNotify(); }
        add(value) { return this.push(value); }
        clear() { this.items.splice(0); this.__compatNotify(); }
        forEach(callback) { this.items.forEach((value, index) => callback(value, index)); }
        getArray() { return this.items; }
        toArray() { return [...this.items]; }
        static newInstance(values = []) { return new MVCArray(values); }
    }

    class ControlOptions { static newInstance() { return { setPosition(position) { this.position = position; } }; } }
    class PanControlOptions extends ControlOptions {}
    class ScaleControlOptions extends ControlOptions {}
    class ZoomControlOptions extends ControlOptions {}
    class MapTypeStyler {
        static newInstance(initial = {}) { return wrapOptions(initial); }
        static newHueStyler(hue) { return { hue }; }
        static newSaturationStyler(saturation) { return { saturation }; }
        static newLightnessStyler(lightness) { return { lightness }; }
        static newInvertLightnessStyler(invert_lightness) { return { invert_lightness }; }
        static newVisibilityStyler(visibility) { return { visibility }; }
    }
    class MapTypeStyle {
        static newInstance(initial = {}) {
            return Object.assign(wrapOptions(initial), {
                setFeatureType(value) { this.featureType = value; },
                setElementType(value) { this.elementType = value; },
                setStylers(value) { this.stylers = value; }
            });
        }
    }
    class StyledMapTypeOptions { static newInstance() { return { setName(name) { this.name = name; } }; } }
    class StyledMapType {
        constructor(styles = [], options = {}) { this.styles = styles; this.options = options; }
        static newInstance(styles = [], options = {}) { return new StyledMapType(styles, options); }
    }
    const MapTypeStyleFeatureType = { TRANSIT: 'transit', POI: 'poi', ROAD: 'road', WATER: 'water', LANDSCAPE: 'landscape', ADMINISTRATIVE: 'administrative' };
    const MapTypeStyleElementType = { ALL: 'all', LABELS__TEXT__FILL: 'labels.text.fill', LABELS__TEXT__STROKE: 'labels.text.stroke', GEOMETRY__FILL: 'geometry.fill', GEOMETRY__STROKE: 'geometry.stroke' };

    class MapOptions {
        static newInstance(initial = {}) { return new MapOptions(initial); }
        constructor(initial = {}) { this.options = wrapOptions(initial); }
        setCenter(center) { this.options.center = literal(center); }
        setZoom(zoom) { this.options.zoom = zoom; }
        setMapTypeId(mapTypeId) { this.options.mapTypeId = mapTypeId; }
        setStyles(styles) { this.options.styles = styles; }
        setDisableDefaultUi(value) { this.options.disableDefaultUI = value; }
        setDisableDefaultUI(value) { this.setDisableDefaultUi(value); }
        setZoomControl(value) { this.options.zoomControl = value; }
        setZoomControlOptions(value) { this.options.zoomControlOptions = value; }
        setPanControl(value) { this.options.panControl = value; }
        setPanControlOptions(value) { this.options.panControlOptions = value; }
        setMapTypeControl(value) { this.options.mapTypeControl = value; }
        setScaleControl(value) { this.options.scaleControl = value; }
        setScaleControlOptions(value) { this.options.scaleControlOptions = value; }
        setStreetViewControl(value) { this.options.streetViewControl = value; }
        setRotateControl(value) { this.options.rotateControl = value; }
        setFullscreenControl(value) { this.options.fullscreenControl = value; }
        setIsFractionalZoomEnabled(value) { this.options.isFractionalZoomEnabled = value; }
        setHeading(value) { this.options.heading = value; }
        setSeaMarksVisible(value) { this.options.seaMarksVisible = value; }
        setRenderingType(value) { this.options.renderingType = value; }
        setDisableDoubleClickZoom(value) { this.options.disableDoubleClickZoom = value; }
        setDraggable(value) { this.options.draggable = value; }
        setMapTypeStyles(value) { this.options.styles = value; }
        setScrollWheel(value) { this.options.scrollwheel = value; }
        setStrokeWeight(value) { this.options.strokeWeight = value; }
        setVisible(value) { this.options.visible = value; }
        toGoogleOptions() { return { ...this.options }; }
    }

    class MapWidget {
        constructor(options, container) {
            this.options = options?.toGoogleOptions ? options.toGoogleOptions() : { ...(options || {}) };
            this.element = document.createElement('div');
            this.map = null;
            this.deferred = [];
            Object.assign(this.element.style, { width: '100%', height: '100%', position: 'relative' });
            if (container) container.append(this.element);
            queueMicrotask(() => this.init());
        }
        init() {
            if (this.map) return;
            this.map = new google.maps.Map(this.element, this.options);
            this.deferred.splice(0).forEach(action => action());
        }
        ready(action) { this.map ? action() : this.deferred.push(action); }
        getElement() { return this.element; }
        getDiv() { return this.element; }
        getZoom() { return this.map ? this.map.getZoom() : this.options.zoom; }
        setZoom(zoom) { this.ready(() => this.map.setZoom(zoom)); }
        getCenter() { return this.map ? this.map.getCenter() : this.options.center; }
        setCenter(center) { this.ready(() => this.map.setCenter(literal(center))); }
        fitBounds(bounds) { this.ready(() => this.map.fitBounds(bounds)); }
        panTo(position) { this.ready(() => this.map.panTo(literal(position))); }
        setHeading(degrees) { this.ready(() => this.map.setHeading(degrees)); }
        getHeading() { return this.map ? this.map.getHeading() : (this.options.heading || 0); }
        getBounds() { return this.map?.getBounds(); }
        triggerResize() { this.ready(() => this.map.resize()); }
        resize() { this.triggerResize(); }
        setOptions(options = {}) { this.ready(() => this.map.setOptions?.(options?.toGoogleOptions ? options.toGoogleOptions() : options)); }
        setSize(width, height) { Object.assign(this.element.style, { width, height }); this.triggerResize(); }
        setTitle(title) { this.element.title = title; }
        getAbsoluteLeft() { return this.element.getBoundingClientRect().left; }
        getAbsoluteTop() { return this.element.getBoundingClientRect().top; }
        getOffsetWidth() { return this.element.offsetWidth; }
        setDoubleClickZoom(value) { this.setOptions({ disableDoubleClickZoom: !value }); }
        getRenderingType() { return this.options.renderingType || google.maps.RenderingType?.VECTOR; }
        getMapTypeRegistry() { return this.map?.mapTypes || new Map(); }
        setCustomMapType(id, type) { this.map?.mapTypes?.set?.(id, type); }
        addOverlay(overlay) { overlay.setMap(this); }
        getInfoWindow() { return InfoWindow.newInstance(); }
        put(key, value) { this[key] = value; return value; }
        remove(key) { const value = this[key]; delete this[key]; return value; }
        containsKey(key) { return Object.prototype.hasOwnProperty.call(this, key); }
        entrySet() { return Object.entries(this); }
        clear() { for (const key of Object.keys(this)) if (!['options', 'element', 'map', 'deferred'].includes(key)) delete this[key]; }
        addHandler(name, handler) {
            let registration = null;
            this.ready(() => { registration = this.map.addListener(name, event => call(handler, event)); });
            return { remove: () => registration?.remove() };
        }
        addBoundsChangeHandler(handler) { return this.addHandler('bounds_changed', handler); }
        addCenterChangeHandler(handler) { return this.addHandler('center_changed', handler); }
        addZoomChangeHandler(handler) { return this.addHandler('zoom_changed', handler); }
        addHeadingChangeHandler(handler) { return this.addHandler('heading_changed', handler); }
        addIdleHandler(handler) { return this.addHandler('idle', handler); }
        addResizeHandler(handler) { return this.addHandler('resize', handler); }
        addDragEndHandler(handler) { return this.addHandler('dragend', handler); }
        addDragStartHandler(handler) { return this.addHandler('dragstart', handler); }
        addRenderingTypeChangeHandler(handler) { return this.addHandler('renderingtype_changed', handler); }
        addClickHandler(handler) { return this.addHandler('click', handler); }
        addMouseDownHandler(handler) { return this.addHandler('mousedown', handler); }
        addMouseMoveHandler(handler) { return this.addHandler('mousemove', handler); }
        addMouseOutHandler(handler) { return this.addHandler('mouseout', handler); }
        addMouseOutMoveHandler(handler) { return this.addMouseOutHandler(handler); }
        addMouseUpHandler(handler) { return this.addHandler('mouseup', handler); }
        addRightClickHandler(handler) { return this.addHandler('rightclick', handler); }
        addDblClickHandler(handler) { return this.addHandler('dblclick', handler); }
        addListener(name, handler) { return this.addHandler(name, handler); }
        setControls(position, control) {
            const element = asElement(control);
            if (!element) return;
            Object.assign(element.style, { position: 'absolute', zIndex: 20 });
            if (position === google.maps.ControlPosition.RIGHT_BOTTOM) Object.assign(element.style, { right: '10px', bottom: '28px' });
            if (position === google.maps.ControlPosition.TOP_LEFT) Object.assign(element.style, { left: '10px', top: '10px' });
            if (position === google.maps.ControlPosition.TOP_RIGHT) Object.assign(element.style, { right: '10px', top: '10px' });
            if (position === google.maps.ControlPosition.BOTTOM_LEFT) Object.assign(element.style, { left: '10px', bottom: '28px' });
            this.element.append(element);
        }
    }

    class PolylineOptions { static newInstance(initial = {}) { return wrapOptions(initial); } }
    class MarkerOptions { static newInstance(initial = {}) { return wrapOptions(initial); } }
    class MarkerImage {
        constructor(url, size, origin, anchor, scaledSize) { Object.assign(this, { url, size, origin, anchor, scaledSize }); }
        setAnchor(value) { this.anchor = value; }
        setScaledSize(value) { this.scaledSize = value; }
        static newInstance(...args) { return new MarkerImage(...args); }
    }
    class PolygonOptions { static newInstance(initial = {}) { return wrapOptions(initial); } }
    class CircleOptions { static newInstance(initial = {}) { return wrapOptions(initial); } }
    class InfoWindowOptions { static newInstance(initial = {}) { return wrapOptions(initial); } }

    class Polyline { static newInstance(options = {}) { return newOverlay(google.maps.Polyline, options); } }
    class Marker { static newInstance(options = {}) { return newOverlay(google.maps.Marker, options); } }
    class Polygon { static newInstance(options = {}) { return newOverlay(google.maps.Polygon, options); } }
    class Circle { static newInstance(options = {}) { return newOverlay(google.maps.Circle, options); } }
    class InfoWindow {
        static newInstance(options = {}) {
            const infoWindow = new google.maps.InfoWindow(options);
            const open = infoWindow.open.bind(infoWindow);
            infoWindow.open = (map, anchor) => {
                if (isMapWidget(map)) map.ready(() => open(map.map, anchor));
                else open(unwrapMap(map), anchor);
            };
            infoWindow.addDomReadyHandler = handler => infoWindow.addListener('domready', event => call(handler, event));
            infoWindow.addCloseClickHandler = handler => infoWindow.addListener('closeclick', event => call(handler, event));
            return infoWindow;
        }
    }

    class OverlayView extends google.maps.OverlayView {
        setMap(map) {
            if (isMapWidget(map) && !map.map) map.ready(() => super.setMap(map.map));
            else super.setMap(unwrapMap(map));
        }
        static newInstance() {
            const overlay = new google.maps.OverlayView();
            const setMap = overlay.setMap.bind(overlay);
            overlay.setMap = map => {
                if (isMapWidget(map) && !map.map) map.ready(() => setMap(map.map));
                else setMap(unwrapMap(map));
            };
            return overlay;
        }
    }

    Object.assign(globalThis, {
        LatLng, LatLngBounds, Point, Size, SphericalUtils, MVCArray,
        MapOptions, MapWidget,
        PanControlOptions, ScaleControlOptions, ZoomControlOptions,
        PolylineOptions, MarkerOptions, MarkerImage, PolygonOptions, CircleOptions, InfoWindowOptions,
        Polyline, Marker, Polygon, Circle, InfoWindow, OverlayView,
        SymbolPath: google.maps.SymbolPath,
        Animation: google.maps.Animation,
        RenderingType: google.maps.RenderingType,
        MapTypeId: google.maps.MapTypeId,
        MapTypeStyler,
        MapTypeStyle,
        StyledMapTypeOptions,
        StyledMapType,
        MapTypeStyleFeatureType,
        MapTypeStyleElementType,
        ControlPosition: google.maps.ControlPosition
    });
    // GWT's JSNI constructors look these up under $wnd.google.maps.
    Object.assign(google.maps, { MVCArray, MarkerImage, Size, StyledMapType });
}

export function installGwtMapsCompat() {
    installGoogleMapsCompat();
    installGwtWrapperGlobals();
}
