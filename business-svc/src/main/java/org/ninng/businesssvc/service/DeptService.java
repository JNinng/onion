package org.ninng.businesssvc.service;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.model.SysDept;
import org.ninng.businesssvc.model.dto.DeptCreateInput;
import org.ninng.businesssvc.model.dto.DeptSpecification;
import org.ninng.businesssvc.repository.DeptRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DeptService {

    private final DeptRepository deptRepository;

    public DeptService(DeptRepository deptRepository) {
        this.deptRepository = deptRepository;
    }

    public SysDept create(Fetcher<SysDept> fetcher, DeptCreateInput input) {
        return deptRepository.saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    public Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification deptSpecification) {
        return deptRepository.select(fetcher, pageReq, deptSpecification);
    }

    public Boolean deleteById(UUID id) {
        deptRepository.verifyPermissions(UserContextHolder.getUserId(), List.of());
        return deptRepository.delete(id);
    }
}
