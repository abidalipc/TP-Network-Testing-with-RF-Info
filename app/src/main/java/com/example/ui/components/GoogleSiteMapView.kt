package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.db.SectorEntity
import com.example.data.db.SiteEntity
import com.example.location.LocationData
import org.json.JSONArray
import org.json.JSONObject

class MapBridge(private val onSiteSelected: (String) -> Unit) {
    @JavascriptInterface
    fun onSiteClicked(siteId: String) {
        onSiteSelected(siteId)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleSiteMapView(
    userLocation: LocationData,
    sites: List<SiteEntity>,
    sectorsMap: Map<String, List<SectorEntity>>,
    activeSiteId: String,
    activeSectorId: String,
    onSiteSelected: (SiteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var mapType by remember { mutableStateOf("dark") } // "dark", "satellite", "roadmap"
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Build JSON payload for JS map update
    val mapDataJson = remember(userLocation, sites, sectorsMap, activeSiteId, activeSectorId) {
        val sitesArray = JSONArray()
        sites.forEach { site ->
            val siteObj = JSONObject().apply {
                put("siteId", site.siteId)
                put("siteName", site.siteName)
                put("lat", site.latitude)
                put("lng", site.longitude)

                val sectorsArray = JSONArray()
                sectorsMap[site.siteId]?.forEach { sector ->
                    sectorsArray.put(JSONObject().apply {
                        put("sectorId", sector.sectorId)
                        put("azimuth", sector.azimuth)
                        put("beamwidth", sector.beamwidth)
                        put("band", sector.band)
                    })
                }
                put("sectors", sectorsArray)
            }
            sitesArray.put(siteObj)
        }

        JSONObject().apply {
            put("userLat", userLocation.latitude)
            put("userLng", userLocation.longitude)
            put("activeSiteId", activeSiteId)
            put("activeSectorId", activeSectorId)
            put("sites", sitesArray)
        }.toString()
    }

    // Update JS map whenever JSON data changes
    LaunchedEffect(mapDataJson, isMapLoaded) {
        if (isMapLoaded) {
            webViewRef?.evaluateJavascript("updateData($mapDataJson);", null)
        }
    }

    // Update JS map type when selected
    LaunchedEffect(mapType, isMapLoaded) {
        if (isMapLoaded) {
            webViewRef?.evaluateJavascript("setMapType('$mapType');", null)
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
                body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #0d1b2a; }
                
                .tower-pin-wrapper {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    cursor: pointer;
                    width: 120px;
                    margin-left: -42px;
                    margin-top: -46px;
                }
                .pin-head {
                    position: relative;
                    width: 36px;
                    height: 46px;
                    filter: drop-shadow(0 3px 6px rgba(0, 0, 0, 0.7));
                }
                .antenna-icon {
                    position: absolute;
                    top: 8px;
                    left: 9px;
                    width: 18px;
                    height: 18px;
                }
                .site-name-text {
                    margin-top: 2px;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    font-size: 13px;
                    font-weight: 700;
                    color: #ffffff;
                    text-shadow: -1px -1px 3px #0b1526, 1px -1px 3px #0b1526, -1px 1px 3px #0b1526, 1px 1px 3px #0b1526, 0 2px 6px rgba(0,0,0,0.9);
                    white-space: nowrap;
                    text-align: center;
                    line-height: 1.2;
                }
                
                .user-pulse {
                    width: 20px;
                    height: 20px;
                    background: #38bdf8;
                    border: 3px solid #ffffff;
                    border-radius: 50%;
                    box-shadow: 0 0 16px #38bdf8;
                }
                
                .google-brand {
                    position: absolute;
                    bottom: 12px;
                    left: 12px;
                    z-index: 1000;
                    font-family: -apple-system, BlinkMacSystemFont, "Product Sans", Roboto, sans-serif;
                    font-size: 18px;
                    font-weight: 700;
                    color: #ffffff;
                    letter-spacing: -0.5px;
                    text-shadow: 0 1px 4px rgba(0,0,0,0.8);
                    pointer-events: none;
                    user-select: none;
                }
                .google-g { color: #4285F4; }
                .google-o1 { color: #EA4335; }
                .google-o2 { color: #FBBC05; }
                .google-g2 { color: #4285F4; }
                .google-l { color: #34A853; }
                .google-e { color: #EA4335; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <div class="google-brand">
                <span style="color:#ffffff;">Google</span>
            </div>
            
            <script>
                var map = L.map('map', { zoomControl: false, attributionControl: false }).setView([34.9350, 72.4580], 13);
                
                var tileLayers = {
                    'dark': L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                        maxZoom: 20,
                        subdomains: 'abcd'
                    }),
                    'satellite': L.tileLayer('https://{s}.google.com/vt/lyrs=s,h&x={x}&y={y}&z={z}', {
                        maxZoom: 20,
                        subdomains: ['mt0','mt1','mt2','mt3']
                    }),
                    'roadmap': L.tileLayer('https://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
                        maxZoom: 20,
                        subdomains: ['mt0','mt1','mt2','mt3']
                    })
                };

                var currentLayer = tileLayers['dark'];
                currentLayer.addTo(map);

                function setMapType(type) {
                    if (tileLayers[type] && currentLayer !== tileLayers[type]) {
                        map.removeLayer(currentLayer);
                        currentLayer = tileLayers[type];
                        currentLayer.addTo(map);
                    }
                }

                var markersGroup = L.layerGroup().addTo(map);
                var userMarker = null;
                var connectionLine = null;
                var initialFitDone = false;

                function getDestinationPoint(lat, lng, distMeters, bearingDeg) {
                    var R = 6371000;
                    var rad = Math.PI / 180;
                    var lat1 = lat * rad;
                    var lon1 = lng * rad;
                    var brng = bearingDeg * rad;
                    var d = distMeters;

                    var lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R) + Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
                    var lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1), Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));

                    return [lat2 / rad, lon2 / rad];
                }

                function getSectorWedge(lat, lng, distMeters, azimuth, beamwidth) {
                    var pts = [[lat, lng]];
                    var halfBeam = beamwidth / 2;
                    var startA = azimuth - halfBeam;
                    var endA = azimuth + halfBeam;
                    var step = Math.max(2, (endA - startA) / 8);

                    for (var a = startA; a <= endA; a += step) {
                        pts.push(getDestinationPoint(lat, lng, distMeters, a));
                    }
                    pts.push(getDestinationPoint(lat, lng, distMeters, endA));
                    pts.push([lat, lng]);
                    return pts;
                }

                function updateData(data) {
                    if (typeof data === 'string') {
                        try { data = JSON.parse(data); } catch(e) { return; }
                    }
                    if (!data) return;

                    markersGroup.clearLayers();
                    var bounds = [];

                    // User Location
                    var userPos = (data.userLat && data.userLng && (data.userLat !== 0 || data.userLng !== 0))
                        ? [data.userLat, data.userLng]
                        : [34.9410, 72.4550]; // Default near Khwazakhela

                    bounds.push(userPos);

                    var userIcon = L.divIcon({
                        className: 'user-pulse',
                        iconSize: [20, 20],
                        iconAnchor: [10, 10]
                    });

                    if (userMarker) {
                        userMarker.setLatLng(userPos);
                    } else {
                        userMarker = L.marker(userPos, { icon: userIcon }).addTo(map);
                    }

                    var activeSitePos = null;

                    if (data.sites && data.sites.length > 0) {
                        data.sites.forEach(function(site) {
                            var sitePos = [site.lat, site.lng];
                            bounds.push(sitePos);
                            var isActive = (site.siteId === data.activeSiteId || site.siteName === 'Khwazakhela' && (!data.activeSiteId || data.activeSiteId === 'N/A'));

                            if (isActive) {
                                activeSitePos = sitePos;
                            }

                            // Sector Wedges
                            if (site.sectors) {
                                site.sectors.forEach(function(sector) {
                                    var isSectorActive = isActive && (sector.sectorId === data.activeSectorId);
                                    var wedgePts = getSectorWedge(site.lat, site.lng, 350, sector.azimuth, sector.beamwidth);

                                    var color = isSectorActive ? '#38bdf8' : '#0284c7';
                                    var fillColor = isSectorActive ? '#38bdf8' : '#0284c7';
                                    var fillOpacity = isSectorActive ? 0.35 : 0.15;

                                    var polygon = L.polygon(wedgePts, {
                                        color: color,
                                        weight: isSectorActive ? 2.0 : 1.0,
                                        fillColor: fillColor,
                                        fillOpacity: fillOpacity
                                    });

                                    polygon.on('click', function() {
                                        if (window.AndroidBridge) {
                                            window.AndroidBridge.onSiteClicked(site.siteId);
                                        }
                                    });

                                    markersGroup.addLayer(polygon);
                                });
                            }

                            // Custom Cell Tower Pin HTML with SVG Antenna and Site Name
                            var pinColor = isActive ? '#00a3e0' : '#0090d0';
                            var displayName = site.siteName || site.siteId;
                            if (displayName === 'Khwazakhela') {
                                displayName = 'Khwazakhela<br/><span style="font-size:11px;font-weight:normal;opacity:0.9;">خواجہ خیلہ</span>';
                            }

                            var markerHtml = `
                                <div class="tower-pin-wrapper">
                                    <div class="pin-head">
                                        <svg viewBox="0 0 36 46" fill="none" style="width:36px;height:46px;">
                                            <path d="M18 0C8.05887 0 0 8.05887 0 18C0 29.5 18 46 18 46C18 46 36 29.5 36 18C36 8.05887 27.9411 0 18 0Z" fill="${'$'}{pinColor}"/>
                                            <circle cx="18" cy="18" r="14" fill="#0284c7"/>
                                        </svg>
                                        <svg class="antenna-icon" viewBox="0 0 24 24" fill="white">
                                            <path d="M12 3C7.03 3 3 7.03 3 12h2c0-3.87 3.13-7 7-7s7 3.13 7 7h2c0-4.97-4.03-9-9-9zm0 4c-2.76 0-5 2.24-5 5h2c0-1.66 1.34-3 3-3s3 1.34 3 3h2c0-2.76-2.24-5-5-5zm0 6c-1.1 0-2 .9-2 2v6h4v-6c0-1.1-.9-2-2-2z"/>
                                        </svg>
                                    </div>
                                    <div class="site-name-text">${'$'}{displayName}</div>
                                </div>
                            `;

                            var siteIcon = L.divIcon({
                                className: '',
                                html: markerHtml,
                                iconSize: [120, 70],
                                iconAnchor: [60, 46]
                            });

                            var marker = L.marker(sitePos, { icon: siteIcon });
                            marker.on('click', function() {
                                if (window.AndroidBridge) {
                                    window.AndroidBridge.onSiteClicked(site.siteId);
                                }
                            });
                            markersGroup.addLayer(marker);
                        });
                    }

                    // Cyan Connection Line to Active Cell Tower
                    if (connectionLine) {
                        map.removeLayer(connectionLine);
                        connectionLine = null;
                    }

                    var lineTarget = activeSitePos || [34.9350, 72.4580]; // Khwazakhela
                    if (userPos && lineTarget) {
                        connectionLine = L.polyline([userPos, lineTarget], {
                            color: '#38bdf8',
                            weight: 3.5,
                            opacity: 0.95
                        }).addTo(map);
                    }

                    if (!initialFitDone && bounds.length > 0) {
                        map.fitBounds(bounds, { padding: [50, 50], maxZoom: 14 });
                        initialFitDone = true;
                    }
                }

                function centerUser(lat, lng) {
                    var targetLat = (lat && lat !== 0) ? lat : 34.9410;
                    var targetLng = (lng && lng !== 0) ? lng : 72.4550;
                    map.setView([targetLat, targetLng], 14, { animate: true });
                }

                function fitAll() {
                    var allBounds = [];
                    markersGroup.eachLayer(function(layer) {
                        if (layer.getBounds) {
                            allBounds.push(layer.getBounds());
                        } else if (layer.getLatLng) {
                            allBounds.push(L.latLngBounds([layer.getLatLng()]));
                        }
                    });
                    if (userMarker) {
                        allBounds.push(L.latLngBounds([userMarker.getLatLng()]));
                    }
                    if (allBounds.length > 0) {
                        var combined = allBounds[0];
                        for (var i = 1; i < allBounds.length; i++) {
                            combined.extend(allBounds[i]);
                        }
                        map.fitBounds(combined, { padding: [50, 50], maxZoom: 14 });
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0D1B2A))) {
        // Fullscreen WebView Map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                    }
                    webChromeClient = android.webkit.WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                        }
                    }
                    addJavascriptInterface(
                        MapBridge { siteId ->
                            val selectedSite = sites.firstOrNull { it.siteId == siteId }
                            if (selectedSite != null) {
                                onSiteSelected(selectedSite)
                            }
                        },
                        "AndroidBridge"
                    )
                    loadDataWithBaseURL("https://www.google.com/maps", htmlContent, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { webView ->
                webViewRef = webView
            }
        )

        // Top-Left Orange "BETA" Badge Overlay
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF97316), // Orange box matching screenshot
            shadowElevation = 4.dp
        ) {
            Text(
                text = "BETA",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Bottom Right Target/Crosshair Floating Action Button (White circle with black crosshair icon)
        FloatingActionButton(
            onClick = {
                webViewRef?.evaluateJavascript(
                    "centerUser(${userLocation.latitude}, ${userLocation.longitude});",
                    null
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp)
                .size(60.dp),
            containerColor = Color.White,
            contentColor = Color(0xFF0F172A),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Center Location",
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF0F172A)
            )
        }
    }
}

