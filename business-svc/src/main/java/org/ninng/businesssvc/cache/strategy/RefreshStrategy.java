package org.ninng.businesssvc.cache.strategy;

@FunctionalInterface
public interface RefreshStrategy {

    static RefreshStrategy fixed(int pageSize) {
        return () -> pageSize;
    }

    static RefreshStrategy defaultStrategy() {
        return fixed(200);
    }

    int pageSize();
}
