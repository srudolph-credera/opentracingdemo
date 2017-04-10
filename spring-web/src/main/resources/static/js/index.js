$(function() {
    // init heatmap
    var canvas = document.getElementById('heatmap');
    if (canvas.getContext) {
        var ctx = canvas.getContext('2d');
        ctx.scale(0.25, 0.25);
        ctx.save();
        ctx.fillStyle = 'rgb(0, 0, 0)';
        ctx.fillRect(0, 0, 1000, 1000);
        ctx.restore();
    }

    // init cross section graph
    var chart = d3.select("#crossSection")
        .attr("width", 250)
        .attr("height", 250);

    // init markers and map
    var markerA = L.marker([500, 450], {
            draggable: true
        });
    var markerB = L.marker([500, 550], {
            draggable: true
        });
    var connectorBackground = L.polyline([[500, 450], [500, 550]], {
            color: 'yellow',
            weight: 3
        });
    var connector = L.polyline([[500, 450], [500, 550]], {
            color: 'black',
            weight: 1,
            dashArray: "5, 5"
        });
    var markers = L.layerGroup([markerA, markerB, connectorBackground, connector]);

    var map = L.map('finalImageMap', {
        crs: L.CRS.Simple,
        attributionControl: false,
        layers: [markers]
    });

    var overlayMaps = {
        "Cross Section Markers": markers
    };

    L.control.layers(null, overlayMaps).addTo(map);

    var bounds = [[0,0], [1000,1000]];
    var image = L.imageOverlay('img/place.png', bounds).addTo(map);
    map.fitBounds(bounds);

    // Add markers
    markerA.addTo(map);
    markerB.addTo(map);
    connectorBackground.addTo(map);
    connector.addTo(map);

    // Request heatmap values on mouse move
    map.on('mousemove', function(e) {
        var x = Math.round(e.latlng.lng)
        var y = Math.round(1000 - e.latlng.lat)
        if (x >= 0 && x < 1000 && y >= 0 && y < 1000) {
            $.ajax({
                url: "/heatmap",
                data: {
                    x: x,
                    y: y
                },
                dataType: "json",
                success: function (result) {
                    var rgb = Math.floor(256 * result);
                    var canvas = document.getElementById('heatmap');
                    if (canvas.getContext) {
                        var ctx = canvas.getContext('2d');
                        ctx.save();
                        ctx.fillStyle = 'rgb(' + rgb + ', ' + rgb + ', ' + rgb + ')';
                        ctx.fillRect(x, y, 1, 1);
                        ctx.restore();
                    }
                }
            });
        }
    });

    map.on('overlayadd', function(e) {
        $("#crossSectionPanel").show();
    });

    map.on('overlayremove', function(e) {
        $("#crossSectionPanel").hide();
    });

    // Query for cross section data and plot result
    var moveEndFunc = function(e) {
        var xA = Math.round(markerA.getLatLng().lng)
        var yA = Math.round(1000 - markerA.getLatLng().lat)

        var xB = Math.round(markerB.getLatLng().lng)
        var yB = Math.round(1000 - markerB.getLatLng().lat)
        $.ajax({
            url: "/section",
            data: {
                minX: xA,
                minY: yA,
                maxX: xB,
                maxY: yB
            },
            dataType: "json",
            success: function (result) {
                // init cross section graph
                d3.select("#crossSection").select("path").remove();
                var data = result.map(function(x, i) {
                    return [i, x];
                });
                var barWidth = 250 / data.length;

                var x = d3.scaleLinear()
                    .domain([0, data.length - 1])
                    .range([0, 250]);

                var y = d3.scaleLinear()
                        .domain([0, 1])
                        .range([250, 0]);

                var chart = d3.select("#crossSection")
                        .attr("width", 250)
                        .attr("height", 250);

                var lineFunc = d3.line()
                    .x(function(d) { return x(d[0]); })
                    .y(function(d) { return y(d[1]); })
                    .curve(d3.curveNatural)

                var lineGraph = chart.append("path")
                    .attr("d", lineFunc(data))
                    .attr("stroke", "gray")
                    .attr("stroke-width", 2)
                    .attr("fill", "none");
            }
        });
    };

    // Calculate initial cross section
    moveEndFunc(null);

    // Listen for moves
    markerA.on('moveend', moveEndFunc);

    markerA.on('move', function(e) {
        var oldPos = connector.getLatLngs();
        connector.setLatLngs([e.latlng, oldPos[1]]);
        connectorBackground.setLatLngs([e.latlng, oldPos[1]]);
    });

    markerB.on('moveend', moveEndFunc);

    markerB.on('move', function(e) {
        var oldPos = connector.getLatLngs();
        connector.setLatLngs([oldPos[0], e.latlng]);
        connectorBackground.setLatLngs([oldPos[0], e.latlng]);
    });
});
