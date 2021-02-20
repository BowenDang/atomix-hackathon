package org.byc.atomix.service.service;

import io.atomix.cluster.Member;
import org.byc.atomix.service.AtomixView;
import org.byc.atomix.service.data.ServiceNode;
import org.byc.atomix.service.data.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class DemoService {
  private static final Logger logger = Logger.getLogger(DemoService.class.getName());

  public static final String DEMO_STARTED = "demoStarted";
  public static final String LOCATION_CHANGE_EVENT_MAP = "locationChangeMap";

  public static final String REGISTRATION_SET_SERVICE_NAME = "serviceSet";


  @Autowired
  private AtomixView atomixView;

  public Status getStatus() {

    ArrayList<ServiceNode> nodes = new ArrayList<>();

    atomixView.getNode().<String>getSet(REGISTRATION_SET_SERVICE_NAME).forEach(nodeId -> {

      // TODO build status for active nodes
      Member node = null;
      try {
        node = atomixView.getNode().getMembershipService().getMember(nodeId);
      } catch (NullPointerException e) {
        // wrap getMember NPE
      }
      ServiceNode.ServiceNodeBuilder nodeBuilder = ServiceNode.ServiceNodeBuilder.aServiceNode();
      if (null != node) {
        nodeBuilder.alive(true);
        nodeBuilder.nodeId(nodeId);
        String serviceName = nodeId.split("-")[0];
        nodeBuilder.serviceName(serviceName);
        String serviceDataStore = serviceName + "-datastore";
        Map<Integer, String> dataStore = atomixView.getNode().<Integer, String>getAtomicMap(serviceDataStore)
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().value()));
        nodeBuilder.sources(dataStore);
      }

      nodes.add(nodeBuilder.build());
    });
    Map<Integer, String> events = atomixView.getNode().<Integer, String>getAtomicMap(LOCATION_CHANGE_EVENT_MAP)
        .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().value()));
    nodes.sort(Comparator.comparing(ServiceNode::getNodeId));

    return Status.StatusBuilder.aStatus()
        .nodes(nodes)
        .event(events)
        .build();
  }

  public void startPublish() {

    atomixView.getNode().<Boolean>getAtomicValue(DEMO_STARTED)
        .async()
        .set(true);
  }

  public void restart() {
    // stop publishing
    atomixView.getNode().<Boolean>getAtomicValue(DEMO_STARTED)
        .async()
        .set(false);

    // TODO init all the primitives

    // removed unregisted service
    Set<String> nodes = atomixView.getNode().<String>getSet(REGISTRATION_SET_SERVICE_NAME).stream().collect(Collectors.toSet());
    for (String nodeId : nodes) {
      Member node = null;
      try {
        node = atomixView.getNode().getMembershipService().getMember(nodeId);
      } catch (NullPointerException e) {
        // wrap getMember NPE
      }
      if (null == node) {
        atomixView.getNode().<String>getSet(REGISTRATION_SET_SERVICE_NAME).remove(nodeId);
      }
      String serviceName = nodeId.split("-")[0];
      String serviceDataStore = serviceName + "-datastore";
      atomixView.getNode().<Integer, String>getAtomicMap(serviceDataStore).clear();
    }

    // add registed services
    atomixView.getNode().getMembershipService().getMembers().forEach(node -> {
      String nodeId = node.id().id();
      String[] splitted = nodeId.split("-");
      if (splitted.length == 2) {
        String serviceName = splitted[0];
        String serviceDataStore = serviceName + "-datastore";
        atomixView.getNode().<Integer, String>getAtomicMap(serviceDataStore).clear();
        atomixView.getNode().<String>getSet(REGISTRATION_SET_SERVICE_NAME).add(nodeId);
      }
    });


    logger.info("Restart completed");

  }
}
