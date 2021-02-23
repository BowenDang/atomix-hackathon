(function() {
    if ('WebSocket' in window) {
        webSocket = new WebSocket('ws://localhost:8080/websocket', 'subprotocol.demo.websocket');

        webSocket.onmessage = function(event) {
            updateStatus(event.data);
        };
    } else {
        // The browser doesn't support WebSocket
        console.error('WebSocket NOT supported by your Browser.');
    }
})();

function start() {
    $.post('start', function () {
        console.log('Started');
    });
}

function restart() {
    $.post('restart', function () {
        console.log('Restarted');
    });
}

function updateStatus(response) {
    if (response) {
        var status = JSON.parse(response),
            nodes = status.nodes,
            publisher = status.event;

        updateCluster();
        updateTable(nodes, publisher);
    }
}

function updateCluster(nodes, publisher) {
    var clusterTable = document.getElementById('cluster-table');

    if (!clusterTable) {

    }
}

function updateTable(nodes, publisher) {
    if (!document.getElementById('node-table')) {
        createNewTable(nodes, publisher);
    }

    var nodeTable = document.getElementById('node-table');

    for (const [location, source] of Object.entries(publisher)) {
        // var publisherData = document.getElementById(location);

        // if (publisherData) {
        //     publisherData.innerHTML = source || '';
        // }

        nodes.forEach((currentNode) => {
            if (currentNode.alive) {
                var currentNodeData = document.getElementById(currentNode.nodeId + '+' + location);
                currentNodeData.innerHTML = currentNode.sources && currentNode.sources[location] ? currentNode.sources[location] : '';
                
                if (currentNode.sources[location] === source || source === 'DELETED') {
                    currentNodeData.style.backgroundColor = 'transparent';
                } else {
                    currentNodeData.style.backgroundColor = 'red';
                }
            }
        });
    }
}

function createNewTable(nodes, publisher) {
    var table = document.getElementById('table'),
        newTable = document.createElement('table'),
        services = {};

    nodes.forEach((node) => {
        if (node.serviceName) {
            if (!services[node.serviceName]) {
                services[node.serviceName] = [];
            }

            services[node.serviceName].push(node);
        }
    });
        
    newTable.setAttribute('id', 'node-table');

    // Table Header
    var thead = document.createElement('thead');
    var trTopHead = document.createElement('tr');
    var trSubHead = document.createElement('tr');

    trTopHead.setAttribute('class', 'top-header');
    trSubHead.setAttribute('class', 'sub-header');

    var locationHeader = document.createElement('th');
    var locationText = document.createTextNode('Locations');
    locationHeader.appendChild(locationText);
    locationHeader.setAttribute('rowspan', 2);
    trTopHead.appendChild(locationHeader);

    // Append publisher service
    // var publisherHeader = document.createElement('th');
    // var publisherText = document.createTextNode('Publisher');
    // publisherHeader.appendChild(publisherText);
    // publisherHeader.setAttribute('rowspan', 2);
    // trTopHead.appendChild(publisherHeader);
    // mainHeaderColspan++;

    for (const [key, nodeList] of Object.entries(services)) {
        var serviceHeader = document.createElement('th');
        var serviceNameText = document.createTextNode(key);

        serviceHeader.setAttribute('colspan', nodeList.length);
        serviceHeader.appendChild(serviceNameText);
        trTopHead.appendChild(serviceHeader);

        // Sub Header
        nodeList.forEach((currentNode) => {
            var nodeHeader = document.createElement('th');
            var nodeNameText = document.createTextNode(currentNode.nodeId);
            nodeHeader.appendChild(nodeNameText);
            trSubHead.appendChild(nodeHeader);
        });
    }

    thead.appendChild(trTopHead);
    thead.appendChild(trSubHead);

    // Table Body
    var tbody = document.createElement('tbody');

    for (const [location, source] of Object.entries(publisher)) {
        var trbody = document.createElement('tr')

        // Locations
        var locationData = document.createElement('td');
        var locationDataText = document.createTextNode(location);
        locationData.appendChild(locationDataText);
        trbody.appendChild(locationData);

        // Publisher
        // var publisherData = document.createElement('td');
        // var publisherDataText = document.createTextNode(source);
        // publisherData.appendChild(publisherDataText);
        // publisherData.setAttribute('id', location);
        // trbody.appendChild(publisherData);

        for (const [key, nodeList] of Object.entries(services)) {
            nodeList.forEach((currentNode) => {
                if (currentNode.alive) {
                    // Node
                    var currentNodeData = document.createElement('td');
                    var currentNodeDataText = document.createTextNode('');
                    currentNodeData.appendChild(currentNodeDataText);
                    currentNodeData.setAttribute('id', currentNode.nodeId + '+' + location);
                    trbody.appendChild(currentNodeData);
                }
            });
        }

        tbody.appendChild(trbody);
    }

    newTable.appendChild(thead);
    newTable.appendChild(tbody);
    table.appendChild(newTable);
}
