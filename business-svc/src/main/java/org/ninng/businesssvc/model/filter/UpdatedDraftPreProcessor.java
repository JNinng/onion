package org.ninng.businesssvc.model.filter;

import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.model.common.UpdatedAwareDraft;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UpdatedDraftPreProcessor implements DraftPreProcessor<UpdatedAwareDraft> {

    @Override
    public void beforeSave(@NonNull UpdatedAwareDraft draft) {
        draft.setUpdatedBy(UserContextHolder.getUserId());
        draft.setUpdatedAt(LocalDateTime.now());
    }
}
