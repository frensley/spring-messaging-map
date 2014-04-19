
function ApplicationModel(stompClient, map, cfg) {
    var self = this;

    self.map = map;
    self.username = ko.observable();
    self.notifications = ko.observableArray();
    self.markers = [];


    self.defaults = {
        userMarker: {
            location: {lat: 30.2500, lng: -97.7500},
            otherUserIconColor: 'red',
            myUserIconColor: 'green'
        }
    };

    self.createMarker = function(name, lat, lng, draggable, zindex, iconColor) {
        var marker = new MarkerWithLabel({
            position: new google.maps.LatLng(lat, lng),
            draggable: draggable,
            raiseOnDrag: false,
            icon: 'http://maps.google.com/mapfiles/ms/icons/' + iconColor + '-dot.png',
            labelContent: name,
            zIndex: zindex,
            labelAnchor: new google.maps.Point(20, 0),
            //labelClass: "labels", // the CSS class for the label
            labelInBackground: false
        });
        marker.setMap(self.map);
        google.maps.event.addListener(marker, 'drag', function(evt){
            //document.getElementById('current').innerHTML = '<p>Marker dragginfg: Current Lat: ' + evt.latLng.lat().toFixed(3) + ' Current Lng: ' + evt.latLng.lng().toFixed(3) + '</p>';
            self.pushNotification("Lat: " + evt.latLng.lat().toFixed(3) + " Lng: " + evt.latLng.lng().toFixed(3));
            stompClient.send(cfg.MAP_COMMAND_PATH,{}, JSON.stringify({action : 'm', coords: [evt.latLng.lat().toFixed(3), evt.latLng.lng().toFixed(3)]}));

        });
        self.markers[name] = marker;
    };

    self.createUserMarker = function(username, coords) {
        var defaults = self.defaults.userMarker;
        var iconColor = defaults.otherUserIconColor;
        var zindex = 1;
        var draggable = false;
        if (coords === null) {
            coords = [defaults.location.lat,defaults.location.lng];
        } else {
            coords[0] = coords[0] === null ? defaults.location.lat : coords[0];
            coords[1] = coords[1] === null ? defaults.location.lng : coords[1];
        }
        if (username === self.username()) {
            iconColor = 'green';
            zindex = google.maps.Marker.MAX_ZINDEX + 1;
            draggable = true;
        }
        self.createMarker(username, coords[0], coords[1], draggable, zindex, iconColor);
    };

    self.connect = function() {
        stompClient.connect({},function(frame) {

            console.log('Connected ' + frame);
            self.username(frame.headers['user-name']);

            stompClient.subscribe(cfg.MAP_ITEM_PATH, function(message) {
                var items = JSON.parse(message.body);
                $.each(items, function(index, item) {
                    self.createUserMarker(item.username, item.coords);
                });
                console.log('message',message);
            });
            stompClient.subscribe(cfg.MAP_UPDATE_PATH, function(message) {

                var command = JSON.parse(message.body);
                var marker = self.markers[command.username];
                //skip myself
                if (command.username === self.username()) {
                    console.log("not me!");
                    return;
                }
                if (command.action === 'c') {
                    console.log('create marker!');
                    self.createUserMarker(command.username, command.coords);
                }
                if (command.action === 'm') {
                    console.log('move marker!');
                    if (marker === undefined) {
                        console.log('no marker!');
                        //marker = self.createMarker(command.username, command.coords[0], command.coords[1], false, 10);

                    } else {
                        marker.setPosition( new google.maps.LatLng( command.coords[0], command.coords[1]) );
                    }
                }
                if (command.action === 'd') {
                    console.log('delete marker!');
                    if (marker !== undefined) {
                        console.log('delete marker');
                        marker.setMap(null);
                    }
                }
                console.log("map update!" + message);
            });


            stompClient.subscribe(cfg.MAP_UPDATE_PATH, function(message) {
                self.pushNotification("Error " + message.body);
            });
            self.goOnline();
        }, function(error) {
            console.log("STOMP protocol error " + error);
            self.pushNotification("Error detected: " + error);
            self.goOffline();
        });
    };

    self.goOffline = function() {
        $('#online-indicator')
            .removeClass('glyphicon-signal')
            .addClass('glyphicon-warning-sign')
            .prop('title', 'Offline disconnected');
    };

    self.goOnline = function() {
        $('#online-indicator')
            .removeClass('glyphicon-warning-sign')
            .addClass('glyphicon-signal')
            .prop('title', 'Offline disconnected');
    }

    self.pushNotification = function(text) {
        self.notifications.push({notification: text});
        if (self.notifications().length > 5) {
            self.notifications.shift();
        }
    };

    self.logout = function() {
        stompClient.disconnect();
        window.location.href = "../logout.html";
    };
}
