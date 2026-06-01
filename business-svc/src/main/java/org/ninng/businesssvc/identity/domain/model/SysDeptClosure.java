package org.ninng.businesssvc.identity.domain.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;

/**
 * 部门闭包表实体
 * <p>
 * <p>实现树形结构的闭包表（Closure Table）模式，用于高效查询部门层级关系。
 * 每一行记录表示一对「祖先-后代」关系，{@code depth}字段表示两者之间的距离。</p>
 * <p>
 * <p>典型数据示例：</p>
 * <pre>
 * 祖先ID=1, 后代ID=1, depth=0  （自身关系）
 * 祖先ID=1, 后代ID=2, depth=1  （直接父子）
 * 祖先ID=1, 后代ID=3, depth=2  （祖父-孙子）
 * </pre>
 * <p>
 * <p>通过闭包表可以避免递归查询，快速获取一个部门的所有子部门
 * （{@code SELECT descendantId WHERE ancestorId = ?}）或所有祖先部门
 * （{@code SELECT ancestorId WHERE descendantId = ?}）。</p>
 * <p>
 * <p>主键使用数据库自增策略（{@link GenerationType#IDENTITY}），
 * 由数据库负责ID生成。</p>
 */
@Entity
public interface SysDeptClosure {

    /**
     * 闭包表记录唯一标识
     * <p>
     * <p>使用数据库自增主键策略</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * 祖先节点ID
     * <p>
     * <p>对应{@link SysDept}中层级较高的部门</p>
     */
    long ancestorId();

    /**
     * 后代节点ID
     * <p>
     * <p>对应{@link SysDept}中层级较低的部门（子孙节点）</p>
     */
    long descendantId();

    /**
     * 深度/距离
     * <p>
     * <p>0表示自身关系，1表示直接父子，2表示孙子节点，以此类推。
     * 用于控制查询范围（例如仅查询直接子部门时过滤 {@code depth = 1}）。</p>
     */
    int depth();
}
