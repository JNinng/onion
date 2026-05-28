package org.ninng.businesssvc.identity.domain.port;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.DeptSpecification;
import org.ninng.businesssvc.identity.domain.model.SysDept;

public interface DeptPort {

    SysDept create(Fetcher<SysDept> fetcher, org.babyfish.jimmer.Input<SysDept> input);

    Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification);

    Boolean removeById(Long id);
}
