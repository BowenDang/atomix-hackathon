package org.byc.atomix.service;

import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.lock.AtomicLock;
import io.atomix.core.map.AtomicMapEvent;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;

@Component
public class AtomixService {

  public static final String REGISTRATION_SET_SERVICE_NAME = "serviceSet";
  public static final String LOCATION_CHANGE_EVENT_MAP = "locationChangeMap";
  public static final String SERVICE_DATA_STORE = "Comment";

  private static final int MAX_ATTACK = 20;
  private static final Logger logger = Logger.getLogger(AtomixService.class.getName());

  private final Atomix node;

  private final AtomicLock lock;

  private final String serviceName;

  private final String nodeId;

  private final Random rand;

  public AtomixService(@Value("${atomix.service.host}") String host,
                       @Value("${atomix.service.port}") int port,
                       @Value("${atomix.publisher.port}") int publisherPort,
                       @Value("${atomix.service.name}") String name) {

    this.serviceName = name;
    this.nodeId = name + "-" + UUID.randomUUID();

    this.rand = new SecureRandom();

    Member localMember = Member.builder(nodeId)
        .withHost(host)
        .withPort(port)
        .build();

    Member publishMember = Member.builder("Publisher")
        .withHost(host)
        .withPort(publisherPort)
        .build();


    Atomix atomix = Atomix.builder()
        .withClusterId("Service-Cluster")
        .withMemberId(name)
        .withMembershipProvider(BootstrapDiscoveryProvider.builder()
        .withNodes(localMember, publishMember).build())
        .build();

    atomix.start().join();

    node = atomix;


    MultiRaftProtocol protocol = MultiRaftProtocol.builder()
        .withReadConsistency(ReadConsistency.LINEARIZABLE)
        .build();

    AtomicLock lock = node.atomicLockBuilder(serviceName)
        .withProtocol(protocol)
        .build();

    this.lock = lock;

    logger.info("Service node started on port : " + port);
  }

  public Atomix getNode() {

    return node;
  }

  @PostConstruct
  public void prepareService() {

    registerNewService();


    startListening();
  }

  private void registerNewService() {

    node.<String>getSet(REGISTRATION_SET_SERVICE_NAME)
        .async()
        .add(nodeId);
  }

  private void startListening() {

    node.<Integer, String>getAtomicMap(LOCATION_CHANGE_EVENT_MAP)
        .async()
        .addListener(this::changeListener);
  }

  private void changeListener(AtomicMapEvent<Integer, String> event) {

    logger.info("location change: lUid: " + event.key() + ", type: " + event.type());

    if (AtomicMapEvent.Type.INSERT.equals(event.type()) || AtomicMapEvent.Type.UPDATE.equals(event.type())) {
      int lUid = event.key();
      String val = event.newValue().value();
      lock.lock();
      try {
        processLocationChanges(lUid, val);

      } finally {
        lock.unlock();
      }
    }
  }

  private void processLocationChanges(int lUid, String val) {
      boolean failure = 1 == rand.nextInt(1000);
      if (failure) {
        throw new IllegalArgumentException("Node failed to process");
      } else {
        updateDataStore(lUid, val);
      }
  }

  private void updateDataStore(int lUid, String val) {
    if ("DELETED".equals(val)) {
      node.getAtomicMap(SERVICE_DATA_STORE).async().remove(lUid);
    } else {
      node.getAtomicMap(SERVICE_DATA_STORE).async().put(lUid, val);
    }
  }

}
