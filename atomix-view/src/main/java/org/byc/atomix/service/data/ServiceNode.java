package org.byc.atomix.service.data;

import java.util.Map;

public class ServiceNode {
  String serviceName;
  String nodeId;
  Map<Integer, String> sources;

  public String getServiceName() {
    return serviceName;
  }

  public ServiceNode setServiceName(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  public String getNodeId() {
    return nodeId;
  }

  public ServiceNode setNodeId(String nodeId) {
    this.nodeId = nodeId;
    return this;
  }

  public Map<Integer, String> getSources() {
    return sources;
  }

  public ServiceNode setSources(Map<Integer, String> sources) {
    this.sources = sources;
    return this;
  }

  public static final class ServiceBuilder {
    String serviceName;
    String nodeId;
    Map<Integer, String> sources;

    private ServiceBuilder() {
    }

    public static ServiceBuilder aStatus() {
      return new ServiceBuilder();
    }

    public ServiceBuilder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public ServiceBuilder nodeId(String nodeId) {
      this.nodeId = nodeId;
      return this;
    }

    public ServiceBuilder sources(Map<Integer, String> sources) {
      this.sources = sources;
      return this;
    }

    public ServiceNode build() {
      ServiceNode status = new ServiceNode();
      status.setServiceName(serviceName);
      status.setNodeId(nodeId);
      status.setSources(sources);
      return status;
    }
  }
}
