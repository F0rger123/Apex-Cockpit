package com.example.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.CarMeet
import com.example.data.DriverPresence

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RealMapView(
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Float, // e.g., 12.0f..18.0f
    mapMode: String,  // SATELLITE, TERRAIN, DARK MODE, ROAD VIEW
    onlineDrivers: List<DriverPresence> = emptyList(),
    meetsList: List<CarMeet> = emptyList(),
    racePinsList: List<MapRacePin> = emptyList(),
    shopPinsList: List<MapShopPin> = emptyList(),
    isRealSensorsMode: Boolean = false,
    realLocation: android.location.Location? = null,
    onMapClick: ((Double, Double) -> Unit)? = null,
    onMarkerClick: ((String, String) -> Unit)? = null,
    onBrowserLocationUpdate: ((Double, Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    // Map Center lat/lon from input or active live sensors
    val currentLat = if (isRealSensorsMode && realLocation != null) realLocation.latitude else centerLat
    val currentLon = if (isRealSensorsMode && realLocation != null) realLocation.longitude else centerLon

    // Compile dynamic map helper actions
    LaunchedEffect(currentLat, currentLon, isMapLoaded) {
        val webView = webViewInstance ?: return@LaunchedEffect
        if (isMapLoaded) {
            webView.evaluateJavascript("updateUserLocation($currentLat, $currentLon)", null)
            webView.evaluateJavascript("setView($currentLat, $currentLon, ${zoomLevel + 12f})", null)
        }
    }

    LaunchedEffect(mapMode, isMapLoaded) {
        val webView = webViewInstance ?: return@LaunchedEffect
        if (isMapLoaded) {
            webView.evaluateJavascript("updateMapMode('$mapMode')", null)
        }
    }

    LaunchedEffect(onlineDrivers, meetsList, racePinsList, shopPinsList, isMapLoaded) {
        val webView = webViewInstance ?: return@LaunchedEffect
        if (isMapLoaded) {
            webView.evaluateJavascript("clearAllMarkers()", null)

            // Loop and add live elements
            onlineDrivers.forEach { d ->
                webView.evaluateJavascript("addPilotMarker('${d.username}', ${d.latitude}, ${d.longitude})", null)
            }
            meetsList.forEach { m ->
                webView.evaluateJavascript("addMeetMarker('${m.title.replace("'", "\\'")}', ${m.latitude}, ${m.longitude})", null)
            }
            racePinsList.forEach { r ->
                webView.evaluateJavascript("addRaceMarker('${r.name.replace("'", "\\'")}', ${r.latitude}, ${r.longitude}, ${r.wagerCredits})", null)
            }
            shopPinsList.forEach { s ->
                webView.evaluateJavascript("addShopMarker('${s.name.replace("'", "\\'")}', ${s.latitude}, ${s.longitude}, '${s.specialization.replace("'", "\\'")}')", null)
            }
        }
    }

    val htmlContent = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    background: #02090F;
                }
                .user-pulsing-marker {
                    display: block;
                    width: 12px;
                    height: 12px;
                    border-radius: 50%;
                    background: #00FFCC;
                    border: 3px solid #fff;
                    box-shadow: 0 0 12px #00FFCC;
                    animation: userPulse 1.6s infinite;
                }
                @keyframes userPulse {
                    0% {
                        box-shadow: 0 0 0 0 rgba(0, 255, 204, 0.7);
                    }
                    70% {
                        box-shadow: 0 0 0 14px rgba(0, 255, 204, 0);
                    }
                    100% {
                        box-shadow: 0 0 0 0 rgba(0, 255, 204, 0);
                    }
                }
                .pilot-marker-label, .meet-marker-label {
                    text-shadow: 1px 1px 2px #000;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map;
                var userMarker;
                var markersGroup;
                var currentMode = 'SATELLITE';

                function initMap() {
                    map = L.map('map', {
                        zoomControl: false,
                        attributionControl: false
                    }).setView([$currentLat, $currentLon], ${zoomLevel + 12f});
                    
                    // Google Satellite map by default
                    L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
                        maxZoom: 20
                    }).addTo(map);
                    
                    markersGroup = L.layerGroup().addTo(map);
                    
                    map.on('click', function(e) {
                        if (window.MapAppInterface) {
                            window.MapAppInterface.handleMapClick(e.latlng.lat, e.latlng.lng);
                        }
                    });
                }

                function updateMapMode(mode) {
                    currentMode = mode;
                    var tileUrl = 'https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}'; // Google Satellite/Hybrid
                    if (mode === 'ROAD VIEW') {
                        tileUrl = 'https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}'; // Google road map
                    } else if (mode === 'TERRAIN') {
                        tileUrl = 'https://mt1.google.com/vt/lyrs=t,r&x={x}&y={y}&z={z}'; // Google terrain
                    } else if (mode === 'DARK_MODE' || mode === 'DARK MODE') {
                        tileUrl = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'; // dark Carto tiles
                    }

                    // Remove existing tile layers and apply updated mapping styles
                    map.eachLayer(function(layer) {
                        if (layer !== markersGroup && layer !== userMarker) {
                            map.removeLayer(layer);
                        }
                    });

                    L.tileLayer(tileUrl, { maxZoom: 20 }).addTo(map);
                }

                function updateUserLocation(lat, lon) {
                    if (userMarker) {
                        userMarker.setLatLng([lat, lon]);
                    } else {
                        var userIcon = L.divIcon({
                            className: 'user-pulsing-marker',
                            iconSize: [18, 18],
                            iconAnchor: [9, 9]
                        });
                        userMarker = L.marker([lat, lon], { icon: userIcon }).addTo(map);
                    }
                }

                function setView(lat, lon, zoom) {
                    if (map) {
                        map.setView([lat, lon], zoom);
                    }
                }

                function clearAllMarkers() {
                    if (markersGroup) {
                        markersGroup.clearLayers();
                    }
                }

                function addPilotMarker(name, lat, lon) {
                    var icon = L.divIcon({
                        className: 'pilot-marker',
                        html: '<div style="background-color:#FF3333; width:12px; height:12px; border-radius:50%; border:2px solid #fff; box-shadow:0 0 6px #FF3333;"></div><div class="pilot-marker-label" style="color:#FFF; font-size:10px; font-weight:bold; background:rgba(0,0,0,0.85); padding:2px 5px; border-radius:4px; margin-top:2px; white-space:nowrap; border:1px solid #FF3333;">🏎️ ' + name + '</div>',
                        iconSize: [12, 12],
                        iconAnchor: [6, 6]
                    });
                    var marker = L.marker([lat, lon], { icon: icon });
                    marker.on('click', function() {
                        if (window.MapAppInterface) {
                            window.MapAppInterface.handleMarkerClick('pilot', name);
                        }
                    });
                    markersGroup.addLayer(marker);
                }

                function addMeetMarker(title, lat, lon) {
                    var icon = L.divIcon({
                        className: 'meet-marker',
                        html: '<div style="background-color:#00FFCC; width:14px; height:14px; border-radius:50%; border:2px solid #fff; box-shadow:0 0 10px #00FFCC; display:flex; align-items:center; justify-content:center; color:#000; font-weight:black; font-size:9px;">🏁</div><div class="meet-marker-label" style="color:#00FFCC; font-size:10px; font-weight:black; background:rgba(0,0,0,0.85); padding:2px 5px; border-radius:4px; margin-top:2px; white-space:nowrap; border:1px solid #00FFCC;">🏆 ' + title + '</div>',
                        iconSize: [14, 14],
                        iconAnchor: [7, 7]
                    });
                    var marker = L.marker([lat, lon], { icon: icon });
                    marker.on('click', function() {
                        if (window.MapAppInterface) {
                            window.MapAppInterface.handleMarkerClick('meet', title);
                        }
                    });
                    markersGroup.addLayer(marker);
                }

                function addRaceMarker(name, lat, lon, bounty) {
                    var icon = L.divIcon({
                        className: 'race-marker',
                        html: '<div style="background-color:#FF9F0A; width:14px; height:14px; border-radius:50%; border:2px solid #fff; box-shadow:0 0 8px #FF9F0A; display:flex; align-items:center; justify-content:center; color:#000; font-weight:black; font-size:9px;">⚡</div><div style="color:#FF9F0A; font-size:10px; font-weight:bold; background:rgba(0,0,0,0.85); padding:2px 5px; border-radius:4px; margin-top:2px; white-space:nowrap; border:1px solid #FF9F0A;">' + name + ' (' + bounty + ' Cr)</div>',
                        iconSize: [14, 14],
                        iconAnchor: [7, 7]
                    });
                    var marker = L.marker([lat, lon], { icon: icon });
                    marker.on('click', function() {
                        if (window.MapAppInterface) {
                            window.MapAppInterface.handleMarkerClick('race', name);
                        }
                    });
                    markersGroup.addLayer(marker);
                }

                function addShopMarker(name, lat, lon, spec) {
                    var icon = L.divIcon({
                        className: 'shop-marker',
                        html: '<div style="background-color:#0A84FF; width:14px; height:14px; border-radius:50%; border:2px solid #fff; box-shadow:0 0 8px #0A84FF; display:flex; align-items:center; justify-content:center; color:#fff; font-weight:bold; font-size:9px;">🔧</div><div style="color:#0A84FF; font-size:10px; font-weight:bold; background:rgba(0,0,0,0.85); padding:2px 5px; border-radius:4px; margin-top:2px; white-space:nowrap; border:1px solid #0A84FF;">' + name + '</div>',
                        iconSize: [14, 14],
                        iconAnchor: [7, 7]
                    });
                    var marker = L.marker([lat, lon], { icon: icon });
                    marker.on('click', function() {
                        if (window.MapAppInterface) {
                            window.MapAppInterface.handleMarkerClick('shop', name);
                        }
                    });
                    markersGroup.addLayer(marker);
                }

                function startBrowserGeolocation() {
                    if (navigator.geolocation) {
                        navigator.geolocation.watchPosition(function(position) {
                            var lat = position.coords.latitude;
                            var lon = position.coords.longitude;
                            var speedMps = position.coords.speed || 0.0;
                            var speedMph = speedMps * 2.23694;
                            
                            updateUserLocation(lat, lon);
                            
                            if (window.MapAppInterface) {
                                window.MapAppInterface.handleBrowserLocationUpdate(lat, lon, speedMph);
                            }
                        }, function(error) {
                            console.error("Browser location error: " + error.message);
                        }, {
                            enableHighAccuracy: true,
                            maximumAge: 1000,
                            timeout: 10000
                        });
                    }
                }

                // Initializer
                window.onload = function() {
                    initMap();
                    updateUserLocation($currentLat, $currentLon);
                    startBrowserGeolocation();
                    
                    // Inject initial loaded pins
                    window.setTimeout(function() {
                        if (window.MapAppInterface) {
                            // Notify Compose that map is ready to receive lists
                        }
                    }, 200);
                };
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: android.webkit.GeolocationPermissions.Callback?
                    ) {
                        callback?.invoke(origin, true, false)
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun handleMapClick(lat: Double, lon: Double) {
                        onMapClick?.invoke(lat, lon)
                    }

                    @JavascriptInterface
                    fun handleMarkerClick(type: String, name: String) {
                        onMarkerClick?.invoke(type, name)
                    }

                    @JavascriptInterface
                    fun handleBrowserLocationUpdate(lat: Double, lon: Double, speedMph: Double) {
                        onBrowserLocationUpdate?.invoke(lat, lon, speedMph)
                    }
                }, "MapAppInterface")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isMapLoaded = true
                    }
                }
                loadDataWithBaseURL("https://leafletjs.com", htmlContent, "text/html", "UTF-8", null)
                webViewInstance = this
            }
        },
        update = {
            // Updated state parameters are processed reactive of the LaunchedEffects above
        },
        modifier = modifier.fillMaxSize()
    )
}