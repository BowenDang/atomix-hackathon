package org.byc.atomix.service;

import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;

import java.io.File;

@Component
public class AtomixView {

  private final Atomix node;

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
        .build();

    atomix.start().join();
    node = atomix;
  }

  public Atomix getNode() {

    return node;
  }

}
