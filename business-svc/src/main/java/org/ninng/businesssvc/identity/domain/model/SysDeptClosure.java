package org.ninng.businesssvc.identity.domain.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface SysDeptClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 祖先节点ID
     */
    long ancestorId();

    /**
     * 后代节点ID
     */
    long descendantId();

    /**
     * 深度/距离（0表示自身，1表示直接子节点，2表示孙子节点...）
     */
    int depth();
}
