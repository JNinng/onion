package org.ninng.businesssvc.event;

import org.springframework.context.ApplicationEvent;

import java.util.Map;
import java.util.Set;

public sealed class CacheInvalidateEvent extends ApplicationEvent
        permits CacheInvalidateEvent.Local, CacheInvalidateEvent.Redis {

    protected final Map<String, Set<Object>> evictions;

    private CacheInvalidateEvent(Object source, Map<String, Set<Object>> evictions) {
        super(source);
        this.evictions = Map.copyOf(evictions);
    }

    public Map<String, Set<Object>> evictions() {
        return evictions;
    }

    public static final class Local extends CacheInvalidateEvent {
        public Local(Object source, Map<String, Set<Object>> evictions) {
            super(source, evictions);
        }
    }

    public static final class Redis extends CacheInvalidateEvent {

        private final String sourceInstanceId;

        public Redis(Object source, Map<String, Set<Object>> evictions, String sourceInstanceId) {
            super(source, evictions);
            this.sourceInstanceId = sourceInstanceId;
        }

        public String sourceInstanceId() {
            return sourceInstanceId;
        }
    }
}
