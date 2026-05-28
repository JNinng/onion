package org.ninng.businesssvc.identity.application;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.DeptCreateInput;
import org.ninng.businesssvc.identity.application.dto.DeptSpecification;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.port.DeptPort;
import org.springframework.stereotype.Service;

@Service
public class DeptApplicationService {

    private final DeptPort deptPort;

    public DeptApplicationService(DeptPort deptPort) {
        this.deptPort = deptPort;
    }

    public SysDept create(Fetcher<SysDept> fetcher, DeptCreateInput input) {
        return deptPort.create(fetcher, input);
    }

    public Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification) {
        return deptPort.list(fetcher, pageReq, specification);
    }

    public Boolean removeById(Long id) {
        return deptPort.removeById(id);
    }
}
