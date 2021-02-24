package org.byc.atomix.service;

import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.lock.AtomicLock;
import io.atomix.core.map.AtomicMap;
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

  private static final int MAX_ATTACK = 20;
  private static final Logger logger = Logger.getLogger(AtomixService.class.getName());

  private final Atomix node;

  private final AtomicLock lock;

  private final String serviceName;

  private final String nodeId;

  private final Random rand;

  private final String dataStore;

  public AtomixService(@Value("${atomix.service.host}") String host,
                       @Value("${atomix.service.port}") int port,
                       @Value("${atomix.publisher.port}") int publisherPort,
                       @Value("${atomix.service.name}") String name,
                       @Value("${atomix.node.id}") String id) {

    this.serviceName = name;
    this.nodeId = name + "-" + id;

    this.rand = new SecureRandom();

    this.dataStore = name + "-datastore";

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
        .withHost(host)
        .withPort(port)
        .withMemberId(nodeId)
        .withMembershipProvider(BootstrapDiscoveryProvider.builder()
        .withNodes(localMember, publishMember).build())
        .build();

    atomix.start().join();

    node = atomix;

    String lockName = serviceName + "-lock";
    AtomicLock lock = node.getAtomicLock(lockName);
    if (null == lock) {
      MultiRaftProtocol protocol = MultiRaftProtocol.builder().withReadConsistency(ReadConsistency.LINEARIZABLE).build();

      lock = node.atomicLockBuilder(lockName).withProtocol(protocol).build();
    }

    this.lock = lock;

    logger.info("Service node started on port : " + port);
  }

  public Atomix getNode() {

    return node;
  }

  @PostConstruct
  public void prepareService() {

    registerNewService();

    createDataStoreIfAbsent();

    startListening();
  }

  private void registerNewService() {

    node.<String>getSet(REGISTRATION_SET_SERVICE_NAME)
        .async()
        .add(nodeId);

  }

  private void createDataStoreIfAbsent() {

    if (null == node.getAtomicMap(dataStore)) {
      MultiRaftProtocol protocol = MultiRaftProtocol.builder()
          .withReadConsistency(ReadConsistency.LINEARIZABLE)
          .build();

      AtomicMap<Integer, String> map = node.<Integer, String>atomicMapBuilder(dataStore)
          .withProtocol(protocol)
          .build();
    }
  }

  private void startListening() {

    node.<Integer, String>getAtomicMap(LOCATION_CHANGE_EVENT_MAP)
        .async()
        .addListener(this::changeListener);
  }

  private void changeListener(AtomicMapEvent<Integer, String> event) {

    //logger.info("location change: lUid: " + event.key() + ", type: " + event.type());

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
      logger.info("Location change {" + lUid + ":" + val + "} processed by: " + nodeId);
      boolean failure = 1 == rand.nextInt(10);
      if (failure) {
        logger.info("Location change {" + lUid + ":" + val + "} failed by: " + nodeId);
        throw new IllegalArgumentException("Node failed to process");
      } else {
        updateDataStore(lUid, val);
      }
  }

  private void updateDataStore(int lUid, String val) {
    if ("DELETED".equals(val)) {
      node.getAtomicMap(dataStore).async().remove(lUid);
    } else {
      node.getAtomicMap(dataStore).async().put(lUid, val);
    }
  }

}
