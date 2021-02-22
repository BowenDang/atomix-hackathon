package org.ar.atomix.publisher;

import com.google.common.collect.ImmutableList;
import io.atomix.core.collection.CollectionEvent;
import io.atomix.core.map.AsyncAtomicMap;
import io.atomix.core.map.AtomicMap;
import io.atomix.core.map.AtomicMapEvent;
import io.atomix.core.value.AtomicValue;
import io.atomix.core.workqueue.WorkQueue;
import io.atomix.primitive.protocol.ProxyProtocol;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;
import io.atomix.core.set.DistributedSet;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;

@Component
public class AtomixPublisher {

  private static final Logger logger = Logger.getLogger(AtomixPublisher.class.getName());

  public static final String DEMO_STARTED = "demoStarted";
  public static final String LOCATION_CHANGE_EVENT_MAP = "locationChangeMap";
  public static final String LOCATION_CHANGE_EVENT_WORK_QUEUE = "locationChangeWorkQueue";

  private final Atomix node;

  private static final String[] randomSources = {"GOOGLE", "FACEBOOK", "GLASSDOOR", "BING", "REPUTATION", "INSTAGRAM", "DELETED"};

  public AtomixPublisher(@Value("${atomix.publisher.host}") String host, @Value("${atomix.publisher.port}") int port) {

    Member publishMember = Member.builder("Publisher")
        .withHost(host)
        .withPort(port)
        .build();

    Atomix atomix = Atomix.builder()
        .withClusterId("Service-Cluster")
        .withHost(host)
        .withPort(port)
        .withMemberId(publishMember.id())
        .withManagementGroup(RaftPartitionGroup.builder("system")
            .withMembers(publishMember)
            .withNumPartitions(1)
            .build())
        .withPartitionGroups(
            RaftPartitionGroup.builder("data")
                .withNumPartitions(1)
                .withMembers(publishMember)
                .build(),
            RaftPartitionGroup.builder("Comment-data")
                .withNumPartitions(1)
                .withMembers(publishMember)
                .build(),
            RaftPartitionGroup.builder("Review-data")
                .withNumPartitions(1)
                .withMembers(publishMember)
                .build())
        .build();

    atomix.start().join();

    node = atomix;

    logger.info("Publisher node started on port : " + port);
  }

  public Atomix getNode() {
    return node;
  }

  @PostConstruct
  public void buildResources() {

      buildPartitionQueue();

      buildLocationChangeMap();

      publishIfDemoBegins(Executors.newSingleThreadScheduledExecutor(), new SecureRandom());

  }

  private void buildPartitionQueue() {
      node.workQueueBuilder("Comment-queue")
          .withProtocol(
              protocol("Comment-data"))
          .build();
      node.workQueueBuilder("Review-queue")
          .withProtocol(
              protocol("Review-data"))
          .build();
  }

  private void buildLocationChangeMap() {
      node.atomicMapBuilder(LOCATION_CHANGE_EVENT_MAP)
          .withProtocol(protocol())
          .build();
  }


  private void publishIfDemoBegins(ScheduledExecutorService scheduler, SecureRandom changeRandomizer) {
      AtomicValue<Boolean> started = node.<Boolean>atomicValueBuilder(DEMO_STARTED).withProtocol(protocol()).build();
      started.async()
        .get()
        .whenComplete((isDemoStarted, throwable) -> {

          if (isDemoStarted) {
            //publishRandomEvent(changeRandomizer);
            publishRandomEventToQueue(changeRandomizer);
          }
        });

    scheduler.schedule(() -> publishIfDemoBegins(scheduler, changeRandomizer), 1000, TimeUnit.MILLISECONDS);
  }

  private void publishRandomEvent(SecureRandom changeRandomizer) {
      int lUid = changeRandomizer.nextInt(30);
      String source = randomSources[changeRandomizer.nextInt(randomSources.length)];
      logger.info("Publish event: lUid: " + lUid + ", source: " + source);
      AtomicMap<Integer, String> map = node.<Integer, String>atomicMapBuilder(LOCATION_CHANGE_EVENT_MAP)
          .withProtocol(protocol())
          .build();
      map.async()
          .put(lUid, source);
  }

    private void publishRandomEventToQueue(SecureRandom changeRandomizer) {
        int lUid = changeRandomizer.nextInt(30);
        String source = randomSources[changeRandomizer.nextInt(randomSources.length)];
        logger.info("Publish Queue event: lUid: " + lUid + ", source: " + source);
        AtomicMap<Integer, String> map = node.<Integer, String>atomicMapBuilder(LOCATION_CHANGE_EVENT_MAP)
            .withProtocol(protocol())
            .build();
        map.async()
            .put(lUid, source);

        WorkQueue<List<String>> commentQueue = node.<List<String>>workQueueBuilder("Comment-queue")
            .withProtocol(
                protocol("Comment-data"))
            .build();
        WorkQueue<List<String>> reviewQueue = node.<List<String>>workQueueBuilder("Review-queue")
            .withProtocol(
                protocol("Review-data"))
            .build();
        commentQueue.addOne(ImmutableList.of(String.valueOf(lUid), source));
        reviewQueue.addOne(ImmutableList.of(String.valueOf(lUid), source));
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
