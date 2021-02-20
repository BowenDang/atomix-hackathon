package org.byc.atomix.service.data;

import com.google.common.base.Objects;

import java.util.List;
import java.util.Map;

public class Status {
    List<ServiceNode> nodes;
    Map<Integer, String> event;

    public List<ServiceNode> getNodes() {
        return nodes;
    }

    public Status setNodes(List<ServiceNode> nodes) {
        this.nodes = nodes;
        return this;
    }

    public Map<Integer, String> getEvent() {
        return event;
    }

    public Status setEvent(Map<Integer, String> event) {
        this.event = event;
        return this;
    }

    public static final class StatusBuilder {
        List<ServiceNode> nodes;
        Map<Integer, String> event;

        private StatusBuilder() {
        }

        public static StatusBuilder aStatus() {
            return new StatusBuilder();
        }

        public StatusBuilder nodes(List<ServiceNode> nodes) {
            this.nodes = nodes;
            return this;
        }

        public StatusBuilder event(Map<Integer, String> event) {
            this.event = event;
            return this;
        }

        public Status build() {
            Status status = new Status();
            status.setNodes(nodes);
            status.setEvent(event);
            return status;
        }
    }
}
