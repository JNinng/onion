package org.ninng.businesssvc.cache.loader;

import java.util.List;

public record PageResult<V>(List<V> data, int page, int pageSize, long total) {

    public boolean hasNext() {
        return (long) page * pageSize < total;
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }
}
