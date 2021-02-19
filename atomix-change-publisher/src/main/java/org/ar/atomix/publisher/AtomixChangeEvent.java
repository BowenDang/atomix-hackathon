package org.ar.atomix.publisher;

public class AtomixChangeEvent {
    private Type changeType;
    private String before;
    private String after;

    public String getAfter() {
        return after;
    }

    public AtomixChangeEvent setAfter(String after) {
        this.after = after;
        return this;
    }

    public Type getChangeType() {
        return changeType;
    }

    public AtomixChangeEvent setChangeType(Type changeType) {
        this.changeType = changeType;
        return this;
    }

    public String getBefore() {
        return before;
    }

    public AtomixChangeEvent setBefore(String before) {
        this.before = before;
        return this;
    }

    public static final class AtomixChangeEventBuilder {
        private Type changeType;
        private String before;
        private String after;

        private AtomixChangeEventBuilder() {
        }

        public static AtomixChangeEventBuilder anAtomixChangeEvent() {
            return new AtomixChangeEventBuilder();
        }

        public AtomixChangeEventBuilder changeType(Type changeType) {
            this.changeType = changeType;
            return this;
        }

        public AtomixChangeEventBuilder before(String before) {
            this.before = before;
            return this;
        }

        public AtomixChangeEventBuilder after(String after) {
            this.after = after;
            return this;
        }

        public AtomixChangeEvent build() {
            AtomixChangeEvent atomixChangeEvent = new AtomixChangeEvent();
            atomixChangeEvent.setChangeType(changeType);
            atomixChangeEvent.setBefore(before);
            atomixChangeEvent.setAfter(after);
            return atomixChangeEvent;
        }
    }
}
