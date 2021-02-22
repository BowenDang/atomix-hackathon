package org.byc.atomix.service.data;

import java.util.List;
import java.util.Map;

public class ServiceNode {
  String serviceName;
  String nodeId;
  boolean alive;
  Map<Integer, String> sources;
  Map<String, List<String>> workQueue;

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

  public boolean isAlive() {
    return alive;
  }

  public ServiceNode setAlive(boolean alive) {
    this.alive = alive;
    return this;
  }

  public static final class ServiceNodeBuilder {
    String serviceName;
    String nodeId;
    boolean alive;
    Map<Integer, String> sources;

    private ServiceNodeBuilder() {
    }

    public static ServiceNodeBuilder aServiceNode() {
      return new ServiceNodeBuilder();
    }

    public ServiceNodeBuilder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public ServiceNodeBuilder nodeId(String nodeId) {
      this.nodeId = nodeId;
      return this;
    }

    public ServiceNodeBuilder alive(boolean alive) {
      this.alive = alive;
      return this;
    }

    public ServiceNodeBuilder sources(Map<Integer, String> sources) {
      this.sources = sources;
      return this;
    }

    public ServiceNode build() {
      ServiceNode serviceNode = new ServiceNode();
      serviceNode.setServiceName(serviceName);
      serviceNode.setNodeId(nodeId);
      serviceNode.setAlive(alive);
      serviceNode.setSources(sources);
      return serviceNode;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("ServiceNodeBuilder{");
      sb.append("serviceName='").append(serviceName).append('\'');
      sb.append(", nodeId='").append(nodeId).append('\'');
      sb.append(", alive=").append(alive);
      sb.append(", sources=").append(sources);
      sb.append('}');
      return sb.toString();
    }
  }
}
