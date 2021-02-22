package org.byc.atomix.service;

import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.lock.AtomicLock;
import io.atomix.core.map.AtomicMap;
import io.atomix.core.map.AtomicMapEvent;
import io.atomix.core.workqueue.WorkQueue;
import io.atomix.primitive.protocol.ProxyProtocol;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
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

  private final WorkQueue<List<String>> workQueue;

  private final String serviceName;

  private final String nodeId;

  private final Random rand;

  private final String dataStore;

  public AtomixService(@Value("${atomix.service.host}") String host,
                       @Value("${atomix.service.port}") int port,
                       @Value("${atomix.publisher.port}") int publisherPort,
                       @Value("${atomix.service.name}") String name,
                       @Value("${atomix.service.node}") String nodeCode) {

    this.serviceName = name;
    this.nodeId = name + "-" + nodeCode;

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
        .withPartitionGroups(
            RaftPartitionGroup.builder("data")
                .withNumPartitions(1)
                .withMembers(publishMember, localMember)
                .withDataDirectory(new File(".data-" + serviceName))
                .build(),
            RaftPartitionGroup.builder(serviceName + "-data")
                .withNumPartitions(1)
                .withMembers(publishMember, localMember)
                .withDataDirectory(new File(".data-" + serviceName))
                .build())
        .build();

    atomix.start().join();

    node = atomix;


    String lockName = serviceName + "-lock";
    AtomicLock lock = node.atomicLockBuilder(lockName).withProtocol(protocol()).build();
    this.lock = lock;

    String queueName = serviceName + "-queue";
    WorkQueue workQueue = node.<List<String>>workQueueBuilder(queueName)
        .withProtocol(protocol()).build();
    this.workQueue = workQueue;

    logger.info("Service node started on port : " + port);
  }

  public Atomix getNode() {

    return node;
  }

  @PostConstruct
  public void prepareService() {

    registerNewService();

    createDataStoreIfAbsent();

    registerNewWorker();

    startListening();
  }

  private void registerNewService() {

    node.setBuilder(REGISTRATION_SET_SERVICE_NAME)
        .withProtocol(protocol("data"))
        .build()
        .async()
        .add(nodeId);
  }

  private void registerNewWorker() {
      workQueue.async()
        .registerTaskProcessor(event -> {
          int lUid = Integer.parseInt(event.get(0));
          String val = event.get(1);
          logger.info("Location change in queue {" + lUid + ":" + val + "} processed by: " + nodeId);
          boolean failure = 1 == rand.nextInt(10);
          if (failure) {
            logger.info("Location change in queue {" + lUid + ":" + val + "} failed by: " + nodeId);
            throw new IllegalArgumentException("Node failed to process event in queue");
          } else {
            updateDataStore(lUid, val);
          }
        }, 1, Executors.newSingleThreadExecutor());
  }

  private void createDataStoreIfAbsent() {
      AtomicMap<Integer, String> map = node.<Integer, String>atomicMapBuilder(dataStore)
          .withProtocol(protocol("data"))
          .build();
  }

  private void startListening() {
    AtomicMap<Integer, String> atomicMap = node.<Integer, String>atomicMapBuilder(LOCATION_CHANGE_EVENT_MAP).withProtocol(protocol("data")).build();
    atomicMap
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
      boolean failure = 1 == rand.nextInt(100);
      if (failure) {
        logger.info("Location change {" + lUid + ":" + val + "} failed by: " + nodeId);
        throw new IllegalArgumentException("Node failed to process");
      } else {
        updateDataStore(lUid, val);
      }
  }

  private void updateDataStore(int lUid, String val) {
    AtomicMap<Integer, String> map = node.<Integer, String>atomicMapBuilder(dataStore)
        .withProtocol(protocol("data"))
        .build();
    if ("DELETED".equals(val)) {
      map.async().remove(lUid);
    } else {
      map.async().put(lUid, val);
    }
  }

  private ProxyProtocol protocol() {
    MultiRaftProtocol protocol = MultiRaftProtocol.builder(serviceName + "-data")
        .withReadConsistency(ReadConsistency.LINEARIZABLE).build();
    return protocol;
  }

  private ProxyProtocol protocol(String group) {
    MultiRaftProtocol protocol = MultiRaftProtocol.builder(group)
        .withReadConsistency(ReadConsistency.LINEARIZABLE).build();
    return protocol;
  }

}
