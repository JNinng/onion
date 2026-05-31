package org.ninng.businesssvc.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * {@link CollectionDiffUtils} 单元测试
 */
@DisplayName("集合差异工具类测试")
class CollectionDiffUtilsTest {

    @Test
    @DisplayName("测试默认方法: 空列表与 Null 值处理")
    void testDiff_Default_NullAndEmpty() {
        // 1. 两个空列表
        var result1 = CollectionDiffUtils.diff(List.of(), List.of());
        assertThat(result1.add()).isEmpty();
        assertThat(result1.del()).isEmpty();

        // 2. 旧列表为 Null (视为空)
        var result2 = CollectionDiffUtils.diff(null, List.of("A"));
        assertThat(result2.add()).containsExactly("A");
        assertThat(result2.del()).isEmpty();

        // 3. 新列表为 Null (视为空)
        var result3 = CollectionDiffUtils.diff(List.of("A"), null);
        assertThat(result3.add()).isEmpty();
        assertThat(result3.del()).containsExactly("A");

        // 4. 两个都为 Null
        var result4 = CollectionDiffUtils.diff(null, null);
        assertThat(result4.add()).isEmpty();
        assertThat(result4.del()).isEmpty();
    }

    @Test
    @DisplayName("测试默认方法: 基于equals/hashCode的比较")
    void testDiff_Default_EqualHashCode() {
        List<String> oldList = List.of("A", "B", "C");
        List<String> newList = List.of("B", "C", "D");

        var result = CollectionDiffUtils.diff(oldList, newList);

        // D 是新增的，A 是删除的
        assertThat(result.add()).containsExactlyInAnyOrder("D");
        assertThat(result.del()).containsExactlyInAnyOrder("A");

        // 验证结果不可变
        assertThatThrownBy(() -> result.add()
                .add("X"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("测试 Key 提取器: 基于 ID 比较 DTO")
    void testDiff_KeyExtractor() {
        User u1_old = new User(1, "Alice");
        User u2 = new User(2, "Bob");
        User u1_new = new User(1, "Alice Updated"); // ID 相同，内容变了
        User u3 = new User(3, "Charlie");

        List<User> oldList = List.of(u1_old, u2);
        List<User> newList = List.of(u1_new, u3);

        var result = CollectionDiffUtils.diff(oldList, newList, User::id);

        // ID=1 在两边都存在 -> 不在 add/del 中
        // ID=2 被删除
        // ID=3 被新增
        assertThat(result.add()).hasSize(1)
                .allMatch(u -> u.id()
                        .equals(3));
        assertThat(result.del()).hasSize(1)
                .allMatch(u -> u.id()
                        .equals(2));
    }

    @Test
    @DisplayName("测试 Key 提取器: 重复 Key 处理")
    void testDiff_KeyExtractor_Duplicates() {
        User u1_a = new User(1, "A");
        User u1_b = new User(1, "B"); // 重复 ID
        User u2 = new User(2, "C");

        List<User> oldList = List.of(u1_a, u1_b);
        List<User> newList = List.of(u2);

        var result = CollectionDiffUtils.diff(oldList, newList, User::id);

        // 新列表只有 ID=2。
        // 旧列表中 u1_a (ID=1) 和 u1_b (ID=1) 在删除列表。
        assertThat(result.del()).containsExactlyInAnyOrder(u1_a, u1_b);
        assertThat(result.add()).containsExactly(u2);
    }

    @Test
    @DisplayName("测试 Comparator: 基于特定字段比较")
    void testDiff_Comparator() {
        User u1_old = new User(1, "Alice");
        User u2 = new User(2, "Bob");
        User u1_new = new User(1, "Alice V2");

        List<User> oldList = List.of(u1_old, u2);
        List<User> newList = List.of(u1_new, new User(3, "Char"));

        Comparator<User> comparator = Comparator.comparingInt(User::id);

        var result = CollectionDiffUtils.diff(oldList, newList, comparator);

        assertThat(result.add()).hasSize(1)
                .allMatch(u -> u.id() == 3);
        assertThat(result.del()).hasSize(1)
                .allMatch(u -> u.id() == 2);

        // ID=1 的对象在 Comparator 比较下视为相等，不显示变更
        assertThat(result.add()).noneMatch(u -> u.id() == 1);
        assertThat(result.del()).noneMatch(u -> u.id() == 1);
    }

    @Test
    @DisplayName("测试 DiffResult 不可变性")
    void testDiffResult_Immutability() {
        var result = CollectionDiffUtils.diff(List.of("A"), List.of("B"));

        assertThatThrownBy(() -> result.add()
                .add("X"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> result.del()
                .add("Y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    record User(Integer id, String name) {
    }
}