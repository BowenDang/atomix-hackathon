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
            publisher = status.event,
            services = {};

        nodes.forEach((node) => {
            if (node.serviceName) {
                if (!services[node.serviceName]) {
                    services[node.serviceName] = [];
                }

                services[node.serviceName].push(node);
            }
        });

        updateTable(nodes, publisher, services);
        updateCluster(nodes, publisher, services);
    }
}

function updateCluster(nodes, publisher, services) {
    if (!document.getElementById('cluster-canvas')) {
        createNewCanvas(nodes, services);
    }

    var clusterCanvas = document.getElementById('cluster-canvas');

    for (const [location, source] of Object.entries(publisher)) {
        nodes.forEach((currentNode) => {
            var serviceCluster = document.getElementById(currentNode.serviceName + '-cluster');
            var circle = document.getElementById(currentNode.nodeId + '+node');

            if (circle) {
                if (currentNode.alive) {
                    if (currentNode.sources[location] === source || source === 'DELETED') {
                        circle.setAttributeNS(null, 'fill', serviceCluster.getAttribute('service-color'));
                        circle.setAttributeNS(null, 'opacity', 0.3);
                    } else {
                        circle.setAttributeNS(null, 'fill', 'red');
                        circle.setAttributeNS(null, 'opacity', 1);
                    }
                } else {
                    circle.setAttributeNS(null, 'fill', '#dddddd');
                }
            } else if (!circle && currentNode.serviceName) {
                var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                svg.setAttributeNS(null, 'width', 150);
                svg.setAttributeNS(null, 'height', 150);

                var newCircle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                newCircle.setAttributeNS(null, 'cx', '50%');
                newCircle.setAttributeNS(null, 'cy', '50%');
                newCircle.setAttributeNS(null, 'r', 70);
                newCircle.setAttributeNS(null, 'fill', serviceCluster.getAttribute('service-color'));
                newCircle.setAttributeNS(null, 'opacity', 0.3);
                newCircle.setAttributeNS(null, 'id', currentNode.nodeId + '+node');
                svg.appendChild(newCircle);

                var text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                var nodeText = document.createTextNode(currentNode.nodeId);
                text.setAttributeNS(null, 'x', '50%');
                text.setAttributeNS(null, 'y', '50%');
                text.setAttributeNS(null, 'text-anchor', 'middle');
                text.setAttributeNS(null, 'stroke', 'black');
                text.appendChild(nodeText);
                svg.appendChild(text);

                serviceCluster.appendChild(svg);
            }
        });
    }
}

function updateTable(nodes, publisher, services) {
    if (!document.getElementById('node-table')) {
        createNewTable(nodes, publisher, services);
    }

    for (const [location, source] of Object.entries(publisher)) {
        var publisherData = document.getElementById('publisher-' + location);

        if (publisherData && source !== publisherData.innerHTML) {
            publisherData.innerHTML = source || '';
            publisherData.classList.remove('publisher');
            void publisherData.offsetWidth;
            publisherData.classList.add('publisher');
        }

        nodes.forEach((currentNode) => {
            var currentNodeData = document.getElementById(currentNode.nodeId + '+' + location);

            if (currentNodeData) {
                if (currentNode.alive) {
                    currentNodeData.innerHTML = currentNode.sources && currentNode.sources[location] ? currentNode.sources[location] : '';
                    currentNodeData.style.opacity = 1;

                    if (currentNode.sources[location] === source || source === 'DELETED') {
                        currentNodeData.style.backgroundColor = 'transparent';
                    } else {
                        currentNodeData.style.backgroundColor = 'red';
                    }
                } else {
                    var currentNodeHeader = document.getElementById(currentNode.nodeId);
                    currentNodeData.innerHTML = '';
                    currentNodeData.style.backgroundColor = '#dddddd';
                    currentNodeData.style.opacity = 0.5;
                }
            } else if (!currentNodeData && currentNode.serviceName) {
                var trTopHead = document.getElementById(currentNode.serviceName + '-service');
                var order = Number(trTopHead.getAttribute('order'));

                if (!document.getElementById(currentNode.nodeId)) {
                    var trSubHead = document.getElementById('sub-header');
                    var newNode = trSubHead.insertCell(order);
                    newNode.innerHTML = currentNode.nodeId;
                    trTopHead.setAttribute('colspan', order + 1);
                    newNode.setAttribute('class', 'node-name');
                    newNode.setAttribute('id', currentNode.nodeId);
                    trTopHead.setAttribute('order', order + 1);
                    order = order + 1;
                }

                var trbody = document.getElementById('location-' + location);
                var newNodeData = trbody.insertCell(1 + order - 1);
                newNodeData.innerHTML = currentNode.sources && currentNode.sources[location] ? currentNode.sources[location] : '';
                newNodeData.setAttribute('id', currentNode.nodeId + '+' + location);
            }
        });
    }
}

function createNewCanvas(nodes, services) {
    var cluster = document.getElementById('cluster'),
        clusterCanvas = document.createElement('div'),
        thead = document.getElementById('header'),
        usedColors = [
            '#DDDDDD',
            '#FF0000',
            '#FFFF00'
        ];

    function getRandomColor(takenColors) {
        var letters = '0123456789ABCDEF';
        var color;

        while (!color || takenColors.includes(color)) {
            color = '#';
            for (var i = 0; i < 6; i++) {
                color += letters[Math.floor(Math.random() * 16)];
            }
        }

        return color;
    }

    for (const [key, nodeList] of Object.entries(services)) {
        var serviceCluster = document.createElement('div'),
            serviceColor = getRandomColor(usedColors);

        usedColors.push(serviceColor);
        serviceCluster.setAttribute('service-color', serviceColor);
        serviceCluster.setAttribute('id', key + '-cluster');
        serviceCluster.setAttribute('class', 'service-cluster');

        nodeList.forEach((node) => {
            var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.setAttributeNS(null, 'width', 150);
            svg.setAttributeNS(null, 'height', 150);

            var circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            circle.setAttributeNS(null, 'cx', '50%');
            circle.setAttributeNS(null, 'cy', '50%');
            circle.setAttributeNS(null, 'r', 70);
            circle.setAttributeNS(null, 'fill', serviceColor);
            circle.setAttributeNS(null, 'opacity', 0.3);
            circle.setAttributeNS(null, 'id', node.nodeId + '+node');
            svg.appendChild(circle);

            var text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            var nodeText = document.createTextNode(node.nodeId);
            text.setAttributeNS(null, 'x', '50%');
            text.setAttributeNS(null, 'y', '50%');
            text.setAttributeNS(null, 'text-anchor', 'middle');
            text.setAttributeNS(null, 'stroke', 'black');
            text.appendChild(nodeText);
            svg.appendChild(text);

            serviceCluster.appendChild(svg);
        });

        clusterCanvas.appendChild(serviceCluster);
    }

    clusterCanvas.setAttribute('id', 'cluster-canvas');
    cluster.appendChild(clusterCanvas);
}

function createNewTable(nodes, publisher, services) {
    var table = document.getElementById('table'),
        newTable = document.createElement('table');
        
    newTable.setAttribute('id', 'node-table');

    // Table Header
    var thead = document.createElement('thead');
    var trTopHead = document.createElement('tr');
    var trSubHead = document.createElement('tr');

    thead.setAttribute('id', 'header');
    trTopHead.setAttribute('id', 'top-header');
    trSubHead.setAttribute('id', 'sub-header');

    // Append publisher service
    var publisherHeader = document.createElement('th');
    var publisherText = document.createTextNode('');
    publisherHeader.appendChild(publisherText);
    publisherHeader.setAttribute('rowspan', 2);
    publisherHeader.style.border = 'none';
    trTopHead.appendChild(publisherHeader);

    var locationHeader = document.createElement('th');
    var locationText = document.createTextNode('Locations');
    locationHeader.appendChild(locationText);
    locationHeader.setAttribute('rowspan', 2);
    trTopHead.appendChild(locationHeader);

    var order = 0;
    for (const [key, nodeList] of Object.entries(services)) {
        var serviceHeader = document.createElement('th');
        var serviceNameText = document.createTextNode(key);

        serviceHeader.setAttribute('colspan', nodeList.length);
        serviceHeader.setAttribute('id', key + '-service');
        serviceHeader.setAttribute('order', order + nodeList.length);
        serviceHeader.appendChild(serviceNameText);
        trTopHead.appendChild(serviceHeader);
        order = order + nodeList.length;

        // Sub Header
        nodeList.forEach((currentNode) => {
            var nodeHeader = document.createElement('td');
            var nodeNameText = document.createTextNode(currentNode.nodeId);
            nodeHeader.setAttribute('class', 'node-name');
            nodeHeader.setAttribute('id', currentNode.nodeId);
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

        // Publisher
        var publisherData = document.createElement('td');
        var publisherDataText = document.createTextNode(source);
        publisherData.appendChild(publisherDataText);
        publisherData.setAttribute('id', 'publisher-' + location);
        publisherData.style.border = 'none';
        publisherData.style.opacity = 0;
        trbody.appendChild(publisherData);

        // Locations
        var locationData = document.createElement('td');
        var locationDataText = document.createTextNode(location);
        locationData.appendChild(locationDataText);
        trbody.setAttribute('id', 'location-' + location);
        trbody.appendChild(locationData);

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
