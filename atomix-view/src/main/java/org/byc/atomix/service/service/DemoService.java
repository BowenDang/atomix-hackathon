package org.byc.atomix.service.service;

import org.byc.atomix.service.AtomixView;
import org.byc.atomix.service.data.ServiceNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;

@Service
public class DemoService {

  public static final String DEMO_STARTED = "demoStarted";
  public static final String LOCATION_CHANGE_EVENT_MAP = "locationChangeMap";

  public static final String REGISTRATION_SET_SERVICE_NAME = "serviceSet";


  @Autowired
  private AtomixView atomixView;

  public ServiceNode getStatus() {

    ArrayList<ServiceNode> nodes = new ArrayList<>();

    atomixView.getNode().<String>getSet(REGISTRATION_SET_SERVICE_NAME).forEach(serviceName -> {

      // TODO build status for active nodes
    });

    nodes.sort(Comparator.comparing(ServiceNode::getNodeId));

    return ServiceNode.ServiceBuilder.aStatus()
        .build();
  }

  public void startPublish() {

    atomixView.getNode().<Boolean>getAtomicValue(DEMO_STARTED)
        .async()
        .set(true);
  }

  public void restart() {
    atomixView.getNode().<Boolean>getAtomicValue(DEMO_STARTED)
        .async()
        .set(false);

    // TODO init all the primitives
  }
}
