package org.ninng.businesssvc.model.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.IdView;
import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.ninng.businesssvc.constant.DateConstant;
import org.ninng.businesssvc.identity.domain.model.SysUser;

import java.time.LocalDateTime;

@MappedSuperclass
public interface CreatedAware {

    /**
     * 创建时间
     */
    @JsonFormat(pattern = DateConstant.DEFAULT_DATE_TIME_FORMAT)
    LocalDateTime createdAt();

    @IdView(value = "creator")
    @Nullable
    Long createdBy();

    /**
     * 创建人
     */
    @ManyToOne
    @Nullable
    @JoinColumn(name = "created_by")
    SysUser creator();
}
