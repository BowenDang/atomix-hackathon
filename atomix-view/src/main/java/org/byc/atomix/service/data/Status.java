package org.byc.atomix.service.data;

import java.util.List;

public class Status {
    List<ServiceNode> nodes;

    public List<ServiceNode> getNodes() {
        return nodes;
    }

    public Status setNodes(List<ServiceNode> nodes) {
        this.nodes = nodes;
        return this;
    }

    public static final class StatusBuilder {
        List<ServiceNode> nodes;

        private StatusBuilder() {
        }

        public static StatusBuilder aStatus() {
            return new StatusBuilder();
        }

        public StatusBuilder nodes(List<ServiceNode> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Status build() {
            Status status = new Status();
            status.setNodes(nodes);
            return status;
        }
    }
}
