function WebSocketTest() {

    if ("WebSocket" in window) {
        alert("WebSocket is supported by your Browser!");

        webSocket = new WebSocket('ws://localhost:8080/websocket',
            'subprotocol.demo.websocket');

        webSocket.onopen = function () {
            console.log('Client connection opened');

            console.log('Subprotocol: ' + webSocket.protocol);
            console.log('Extensions: ' + webSocket.extensions);
        };

        webSocket.onmessage = function (event) {
            console.log('Client received: ' + event.data);
            updateStatus(event.data);
        };

        webSocket.onerror = function (event) {
            console.log('Client error: ' + event);
        };

        webSocket.onclose = function (event) {
            console.log('Client connection closed: ' + event.code);
        };
    } else {

        // The browser doesn't support WebSocket
        alert("WebSocket NOT supported by your Browser!");
    }
}


function start() {
    $.post("start", function () {
        console.log('Started');
    });
}

function restart() {
    $.post("restart", function () {
        console.log('Restarted');
    });
}

function updateStatus(status) {
    var element = document.getElementById("state");
    element.innerHTML = status;
}