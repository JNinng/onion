package org.ninng.businesssvc.cache.domain;

public record KeySpec(boolean needsTenant, boolean needsId, boolean needsFields) {

    public static final KeySpec NONE = new KeySpec(false, false, false);
    public static final KeySpec TID = new KeySpec(true, false, false);
    public static final KeySpec ID = new KeySpec(false, true, false);
    public static final KeySpec TID_ID = new KeySpec(true, true, false);

    public KeySpec withFields() {
        return new KeySpec(needsTenant, needsId, true);
    }
}
