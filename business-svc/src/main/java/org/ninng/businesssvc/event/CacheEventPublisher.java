package org.ninng.businesssvc.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class CacheEventPublisher {

    private final ApplicationEventPublisher publisher;

    public CacheEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public Batch batch() {
        return new Batch(publisher);
    }

    public void evict(String cacheName, Object key) {
        batch().evict(cacheName, key)
                .publish();
    }

    public void clear(String cacheName) {
        batch().clear(cacheName)
                .publish();
    }

    public static class Batch {

        private final ApplicationEventPublisher publisher;
        private final Map<String, Set<Object>> evictions = new HashMap<>();

        Batch(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        public Batch evict(String cacheName, Object key) {
            evictions.computeIfAbsent(cacheName, k -> new HashSet<>())
                    .add(key);
            return this;
        }

        public Batch clear(String cacheName) {
            evictions.put(cacheName, null);
            return this;
        }

        public void publish() {
            if (evictions.isEmpty()) {
                return;
            }
            publisher.publishEvent(new CacheInvalidateEvent.Local(this, Map.copyOf(evictions)));
        }
    }
}
