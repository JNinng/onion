package org.ninng.businesssvc.cache.domain;

import org.springframework.lang.Nullable;

import java.util.Optional;

public record CacheKey<ID, TID>(
    CacheDomain<ID, TID> domain,
    @Nullable TID tid,
    @Nullable ID id,
    String[] fields
) {
    public String fullKey() {
        return domain.buildKeyString(tid, id);
    }

    public String batchPattern() {
        return domain.buildBatchPattern(tid);
    }

    public String singlePattern() {
        if (tid != null && id != null) {
            return String.format(domain.keyPattern(), tid, id);
        } else if (tid != null) {
            return String.format(domain.keyPattern(), tid);
        } else if (id != null) {
            return String.format(domain.keyPattern(), id);
        }
        return domain.keyPattern();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T field(int index) {
        return fields != null && index < fields.length ? (T) fields[index] : null;
    }

    public boolean hasFields() {
        return fields != null && fields.length > 0;
    }

    public Optional<String> field(int index, Class<String> type) {
        return Optional.ofNullable(field(index));
    }

    public Class<ID> idType() {
        return domain.idType();
    }

    public Class<TID> tidType() {
        return domain.tidType();
    }

    public KeySpec spec() {
        return domain.spec();
    }
}
