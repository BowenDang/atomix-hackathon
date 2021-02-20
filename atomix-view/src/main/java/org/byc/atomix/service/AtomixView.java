package org.byc.atomix.service;

import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.atomix.cluster.Member;
import io.atomix.core.Atomix;

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
        .build();

    atomix.start().join();
    node = atomix;
  }

  public Atomix getNode() {

    return node;
  }

}
