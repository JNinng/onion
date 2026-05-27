package org.ninng.businesssvc.identity.service;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.port.DeptPort;
import org.ninng.businesssvc.model.dto.DeptCreateInput;
import org.ninng.businesssvc.model.dto.DeptSpecification;
import org.springframework.stereotype.Service;

@Service
public class DeptApplicationService {

    private final DeptPort deptPort;

    public DeptApplicationService(DeptPort deptPort) {
        this.deptPort = deptPort;
    }

    public SysDept create(Fetcher<SysDept> fetcher, DeptCreateInput input) {
        return deptPort.create(fetcher, (Input<SysDept>) input);
    }

    public Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification) {
        return deptPort.list(fetcher, pageReq, specification);
    }

    public Boolean deleteById(Long id) {
        return deptPort.deleteById(id);
    }
}
