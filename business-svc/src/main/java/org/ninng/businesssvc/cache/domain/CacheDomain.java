package org.ninng.businesssvc.cache.domain;

import org.ninng.businesssvc.cache.exception.CacheKeyArgumentException;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.function.Function;

public record CacheDomain<ID, TID>(String name, CacheType type, String keyPattern, KeySpec spec, Class<ID> idType,
                                   Class<TID> tidType, @Nullable Function<Object, ID> idExtractor,
                                   @Nullable Class<?> valueType) {

    public String buildKeyString(@Nullable TID tid, @Nullable ID id) {
        if (spec.needsTenant() && tid == null) {
            throw new CacheKeyArgumentException("TenantId is required for " + name);
        }
        if (spec.needsId() && id == null) {
            throw new CacheKeyArgumentException("Id is required for " + name);
        }
        return formatKey(tid, id);
    }

    public CacheKey<ID, TID> buildKey(@Nullable TID tid, @Nullable ID id, String... fields) {
        return new CacheKey<>(this, tid, id,
                spec.needsFields() ? (fields.length > 0 ? fields : new String[0]) : null);
    }

    public String buildBatchPattern(@Nullable TID tid) {
        if (spec.needsTenant() && tid == null) {
            throw new CacheKeyArgumentException("TenantId is required for batch pattern of " + name);
        }
        String formatted = formatKey(tid, null);
        if (spec.needsId()) {
            return formatted.substring(0, formatted.lastIndexOf(':') + 1) + "*";
        }
        return formatted;
    }

    private String formatKey(@Nullable TID tid, @Nullable ID id) {
        if (spec.needsTenant() && spec.needsId()) {
            return String.format(keyPattern, tid, id);
        } else if (spec.needsTenant()) {
            return String.format(keyPattern, tid);
        } else if (spec.needsId()) {
            return String.format(keyPattern, id);
        }
        return keyPattern;
    }

    public CacheDomain<ID, TID> withIdExtractor(Function<Object, ID> extractor) {
        return new CacheDomain<>(name, type, keyPattern, spec, idType, tidType, extractor, valueType);
    }

    public CacheDomain<ID, TID> withValueType(Class<?> vt) {
        return new CacheDomain<>(name, type, keyPattern, spec, idType, tidType, idExtractor, vt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CacheDomain<?, ?> that)) {
            return false;
        }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "CacheDomain{" + name + ", " + type + "}";
    }
}
