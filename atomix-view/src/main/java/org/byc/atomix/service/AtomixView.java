package org.byc.atomix.service;

import com.google.common.collect.ImmutableMap;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.workqueue.WorkQueue;
import io.atomix.primitive.protocol.ProxyProtocol;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;

import java.io.File;
import java.util.Map;

@Component
public class AtomixView {

  private final Atomix node;

  private final WorkQueue q1;

  private final WorkQueue q2;

  public AtomixView(@Value("${atomix.viewer.host}") String host,
                    @Value("${atomix.viewer.port}") int port,
                    @Value("${atomix.publisher.port}") int publisherPort) {

    Member localMember = Member.builder("View")
        .withHost(host)
        .withPort(port)
        .build();

    Member publishMember = Member.builder("Publisher")
        .withHost(host)
        .withPort(publisherPort)
        .build();

    Atomix atomix = Atomix.builder()
        .withClusterId("Service-Cluster")
        .withMemberId(localMember.id())
        .withHost(host)
        .withPort(port)
        .withMembershipProvider(BootstrapDiscoveryProvider.builder()
            .withNodes(localMember, publishMember)
            .build())
        .withPartitionGroups(
            RaftPartitionGroup.builder("data")
                .withNumPartitions(1)
                .withMembers(localMember, publishMember)
                .build(),
            RaftPartitionGroup.builder("Comment-data")
                .withNumPartitions(1)
                .withMembers(localMember, publishMember)
                .build(),
            RaftPartitionGroup.builder("Review-data")
                .withNumPartitions(1)
                .withMembers(localMember, publishMember)
                .build())
        .withMulticastEnabled()
        .build();

    atomix.start().join();
    node = atomix;


    q1 = node.workQueueBuilder("Comment-queue")
        .withProtocol(
            protocol("Comment-data"))
        .build();
    q2 = node.workQueueBuilder("Review-queue")
        .withProtocol(
            protocol("Review-data"))
        .build();
  }

  public Atomix getNode() {

    return node;
  }

  public Map<String, String> getWorkQueue() {
    Map<String, String> queues = ImmutableMap.of("Comment", q1.stats().toString(), "Review", q2.stats().toString());
    return queues;
  }


  private ProxyProtocol protocol(String group) {
    MultiRaftProtocol protocol = MultiRaftProtocol.builder(group)
        .withReadConsistency(ReadConsistency.LINEARIZABLE).build();
    return protocol;
  }

}
