package org.ninng.businesssvc.model.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.*;
import org.ninng.businesssvc.constant.DateConstant;
import org.ninng.businesssvc.model.SysUser;

import java.time.LocalDateTime;

@MappedSuperclass
public interface UpdatedAware {

    /**
     * 更新时间
     */
    @JsonFormat(pattern = DateConstant.DEFAULT_DATE_TIME_FORMAT)
    LocalDateTime updatedAt();

    @IdView(value = "updater")
    @Nullable
    Long updatedBy();

    /**
     * 更新人
     */
    @ManyToOne
    @Nullable
    @JoinColumn(name = "updated_by")
    SysUser updater();

    /**
     * 删除时间
     */
    @Nullable
    @LogicalDeleted("now")
    @JsonIgnore
    LocalDateTime deletedAt();
}
