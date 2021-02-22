package org.byc.atomix.service.service;

import com.google.common.collect.ImmutableMap;
import io.atomix.cluster.Member;
import io.atomix.core.map.AtomicMap;
import io.atomix.core.set.DistributedSet;
import io.atomix.core.value.AtomicValue;
import io.atomix.core.workqueue.WorkQueue;
import io.atomix.primitive.protocol.ProxyProtocol;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
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
import java.util.stream.Stream;

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

    DistributedSet<String> set = atomixView.getNode().<String>setBuilder(REGISTRATION_SET_SERVICE_NAME)
        .withProtocol(protocol())
        .build();

    set.forEach(nodeId -> {

      // TODO build status for active nodes
      Member node = null;
      try {
        node = atomixView.getNode().getMembershipService().getMember(nodeId);
      } catch (NullPointerException e) {
        // wrap getMember NPE
      }
      ServiceNode.ServiceNodeBuilder nodeBuilder = ServiceNode.ServiceNodeBuilder.aServiceNode();
      nodeBuilder.nodeId(nodeId);
      if (null != node) {
        nodeBuilder.alive(true);
        String serviceName = nodeId.split("-")[0];
        nodeBuilder.serviceName(serviceName);
        String serviceDataStore = serviceName + "-datastore";
        AtomicMap<Integer, String> map = atomixView.getNode().<Integer, String>atomicMapBuilder(serviceDataStore)
            .withProtocol(protocol())
            .build();
        Map<Integer, String> dataStore = map
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().value()));
        nodeBuilder.sources(dataStore);
      }

      nodes.add(nodeBuilder.build());
    });
    AtomicMap<Integer, String> lcMap = atomixView.getNode().<Integer, String>atomicMapBuilder(LOCATION_CHANGE_EVENT_MAP)
        .withProtocol(protocol())
        .build();
    Map<Integer, String> events = lcMap
        .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().value()));
    nodes.sort(Comparator.comparing(ServiceNode::getNodeId));

    String q1State = atomixView.getNode().workQueueBuilder("Comment-queue")
        .withProtocol(
            protocol("Comment-data"))
        .build().stats().toString();
    String q2State = atomixView.getNode().workQueueBuilder("Review-queue")
        .withProtocol(
            protocol("Review-data"))
        .build().stats().toString();

    Map<String, String> queues = ImmutableMap.of("Comment", q1State, "Review", q2State);

    return Status.StatusBuilder.aStatus()
        .nodes(nodes)
        .event(events)
        .queues(queues)
        .build();
  }

  public void startPublish() {

    AtomicValue<Boolean> started = atomixView.getNode().<Boolean>atomicValueBuilder(DEMO_STARTED).withProtocol(protocol()).build();
    started.async()
        .set(true);
  }

  public void restart() {
    // stop publishing
    AtomicValue<Boolean> started = atomixView.getNode().<Boolean>atomicValueBuilder(DEMO_STARTED).withProtocol(protocol()).build();
    started.async()
        .set(false);

    // TODO init all the primitives

    // removed unregisted service
    DistributedSet<String> set = atomixView.getNode().<String>setBuilder(REGISTRATION_SET_SERVICE_NAME)
        .withProtocol(protocol())
        .build();
    Set<String> nodes = set.stream().collect(Collectors.toSet());
    for (String nodeId : nodes) {
      Member node = null;
      try {
        node = atomixView.getNode().getMembershipService().getMember(nodeId);
      } catch (NullPointerException e) {
        // wrap getMember NPE
      }
      if (null == node) {
        set.remove(nodeId);
      }
      String serviceName = nodeId.split("-")[0];
      String serviceDataStore = serviceName + "-datastore";
      AtomicMap<Integer, String> map = atomixView.getNode().<Integer, String>atomicMapBuilder(serviceDataStore)
          .withProtocol(protocol())
          .build();
      map.clear();
    }

    // add registed services
    atomixView.getNode().getMembershipService().getMembers().forEach(node -> {
      String nodeId = node.id().id();
      String[] splitted = nodeId.split("-");
      if (splitted.length == 2) {
        String serviceName = splitted[0];
        String serviceDataStore = serviceName + "-datastore";
        AtomicMap<Integer, String> map = atomixView.getNode().<Integer, String>atomicMapBuilder(serviceDataStore)
            .withProtocol(protocol())
            .build();
        map.clear();
        set.add(nodeId);
      }
    });


    logger.info("Restart completed");

  }

  private ProxyProtocol protocol() {
    MultiRaftProtocol protocol = MultiRaftProtocol.builder("data")
        .withReadConsistency(ReadConsistency.LINEARIZABLE).build();
    return protocol;
  }

  private ProxyProtocol protocol(String group) {
    MultiRaftProtocol protocol = MultiRaftProtocol.builder(group)
        .withReadConsistency(ReadConsistency.LINEARIZABLE).build();
    return protocol;
  }
}
