package org.ninng.businesssvc.model.filter;

import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.exception.ServiceException;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.TenantAwareDraft;
import org.ninng.businesssvc.model.common.TenantAwareProps;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class TenantDraftInterceptor implements DraftInterceptor<TenantAware, TenantAwareDraft> {

    private final I18nUtil i18nUtil;

    public TenantDraftInterceptor(I18nUtil i18nUtil) {
        this.i18nUtil = i18nUtil;
    }

    @Override
    public @Nullable Collection<TypedProp<TenantAware, ?>> dependencies() {
        return List.of(TenantAwareProps.TENANT);
    }

    @Override
    public void beforeSave(@NonNull TenantAwareDraft draft, @Nullable TenantAware original) {
        if (original == null) {
            draft.setTenantId(UserContextHolder.getTenantId());
        } else if (!Objects.equals(UserContextHolder.getTenantId(), original.tenantId())) {
            throw new ServiceException(i18nUtil.getMessage("exception.permissionErr"), 500);
        }
    }

    @Override
    public void beforeSaveAll(@NonNull Collection<Item<TenantAware, TenantAwareDraft>> items) {
        for (Item<TenantAware, TenantAwareDraft> item : items) {
            if (item.getOriginal() == null) {
                item.getDraft()
                        .setTenantId(UserContextHolder.getTenantId());
            } else if (!Objects.equals(UserContextHolder.getTenantId(), item.getOriginal()
                    .tenantId())) {
                throw new ServiceException(i18nUtil.getMessage("exception.permissionErr"), 500);
            }
        }
    }
}
