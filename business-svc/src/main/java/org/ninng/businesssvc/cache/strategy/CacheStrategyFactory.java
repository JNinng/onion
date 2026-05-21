package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CacheStrategyFactory {

    private final Map<CacheType, CacheTypeStrategy> strategies;

    public CacheStrategyFactory(List<CacheTypeStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(CacheTypeStrategy::type, Function.identity()));
    }

    public CacheTypeStrategy get(CacheType type) {
        CacheTypeStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy for " + type);
        }
        return strategy;
    }

    public ValueStrategy value() {
        return (ValueStrategy) get(CacheType.VALUE);
    }

    public ListStrategy list() {
        return (ListStrategy) get(CacheType.LIST);
    }

    public SetStrategy set() {
        return (SetStrategy) get(CacheType.SET);
    }

    public HashStrategy hash() {
        return (HashStrategy) get(CacheType.HASH);
    }
}
