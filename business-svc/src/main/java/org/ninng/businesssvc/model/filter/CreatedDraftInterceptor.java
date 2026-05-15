package org.ninng.businesssvc.model.filter;

import org.babyfish.jimmer.sql.DraftInterceptor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.CreatedAwareDraft;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;

@Component
public class CreatedDraftInterceptor implements DraftInterceptor<CreatedAware, CreatedAwareDraft> {

    @Override
    public void beforeSave(@NonNull CreatedAwareDraft draft, @Nullable CreatedAware original) {
        if (original == null) {
            draft.setCreatedBy(UserContextHolder.getUserId());
            draft.setCreatedAt(LocalDateTime.now());
        }
    }

    @Override
    public void beforeSaveAll(@NonNull Collection<Item<CreatedAware, CreatedAwareDraft>> items) {
        items.stream()
                .filter(item -> item.getOriginal() == null)
                .forEach(item -> {
                    item.getDraft()
                            .setCreatedBy(UserContextHolder.getUserId());
                    item.getDraft()
                            .setCreatedAt(LocalDateTime.now());
                });
    }
}
