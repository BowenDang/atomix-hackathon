package org.ar.atomix.publisher;

import io.atomix.core.collection.CollectionEvent;
import io.atomix.core.map.AsyncAtomicMap;
import io.atomix.core.map.AtomicMap;
import io.atomix.core.map.AtomicMapEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    publishIfDemoBegins(Executors.newSingleThreadScheduledExecutor(), new SecureRandom());

  }


  private void publishIfDemoBegins(ScheduledExecutorService scheduler, SecureRandom changeRandomizer) {

    node.<Boolean>getAtomicValue(DEMO_STARTED)
        .async()
        .get()
        .whenComplete((isDemoStarted, throwable) -> {

          if (isDemoStarted) {
            publishRandomEvent(changeRandomizer);
          }
        });

    scheduler.schedule(() -> publishIfDemoBegins(scheduler, changeRandomizer), 2, TimeUnit.SECONDS);
  }

  private void publishRandomEvent(SecureRandom changeRandomizer) {
      int lUid = changeRandomizer.nextInt(30);
      String source = randomSources[changeRandomizer.nextInt(randomSources.length)];
      logger.info("Publish event: lUid: " + lUid + ", source: " + source);
      node.getAtomicMap(LOCATION_CHANGE_EVENT_MAP)
          .async()
          .put(lUid, source);
  }

}
