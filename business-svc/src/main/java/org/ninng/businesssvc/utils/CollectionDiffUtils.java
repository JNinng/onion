package org.ninng.businesssvc.utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 集合差异比对工具类。
 * <p>
 * 提供多种策略比对两个 {@link Collection} 的差异，计算出存在于新集合但不存在于旧集合的元素（新增），
 * 以及存在于旧集合但不存在于新集合的元素（删除）。
 * </p>
 * <p>
 * 所有方法均对 null 输入安全（将 null 视为空集合处理）。
 * </p>
 */
public class CollectionDiffUtils {

    /**
     * 基于对象的 {@link Object#equals(Object)} 和 {@link Object#hashCode()} 方法比对两个集合的差异。
     *
     * @param <T>     集合元素类型
     * @param oldList 旧数据集合，允许为 null
     * @param newList 新数据集合，允许为 null
     * @return {@link DiffResult} 包含新增和删除的元素列表
     */
    @NonNull
    public static <T> DiffResult<T> diff(@Nullable Collection<T> oldList, @Nullable Collection<T> newList) {
        var safeOld = Objects.requireNonNullElse(oldList, List.<T>of());
        var safeNew = Objects.requireNonNullElse(newList, List.<T>of());

        var oldSet = new HashSet<>(safeOld);
        var newSet = new HashSet<>(safeNew);

        var addList = safeNew.stream()
                .filter(e -> !oldSet.contains(e))
                .toList();

        var delList = safeOld.stream()
                .filter(e -> !newSet.contains(e))
                .toList();

        return new DiffResult<>(addList, delList);
    }

    /**
     * 基于指定的 Key 提取函数比对两个集合的差异。
     * <p>
     * 该方法适用于 DTO/Entity 对象比对，允许通过提取对象的主键（如 ID）来判断对象是否相同，
     * 而不依赖于对象的 {@code equals} 方法。
     * </p>
     * <p>
     * <b>注意：</b>如果列表中存在提取出的 Key 重复的元素，在进行 Map 转换时会进行去重，
     * 保留列表中遇到的第一个该 Key 对应的元素。
     * </p>
     *
     * @param <T>          集合元素类型
     * @param <K>          提取的 Key 类型
     * @param oldList      旧数据集合，允许为 null
     * @param newList      新数据集合，允许为 null
     * @param keyExtractor Key 提取函数，不能为 null
     * @return {@link DiffResult} 包含新增和删除的元素列表
     */
    @NonNull
    public static <T, K> DiffResult<T> diff(@Nullable Collection<T> oldList, @Nullable Collection<T> newList,
                                            @NonNull Function<? super T, ? extends K> keyExtractor) {
        var safeOld = Objects.requireNonNullElse(oldList, List.<T>of());
        var safeNew = Objects.requireNonNullElse(newList, List.<T>of());

        // 将列表转为 Map，以 Key 为索引，处理旧列表中可能存在的 Key 重复问题（保留第一个）
        var oldMap = safeOld.stream()
                .collect(Collectors.toMap(keyExtractor, Function.identity(), (a, b) -> a));
        var newMap = safeNew.stream()
                .collect(Collectors.toMap(keyExtractor, Function.identity(), (a, b) -> a));

        var addList = safeNew.stream()
                .filter(e -> !oldMap.containsKey(keyExtractor.apply(e)))
                .toList();

        var delList = safeOld.stream()
                .filter(e -> !newMap.containsKey(keyExtractor.apply(e)))
                .toList();

        return new DiffResult<>(addList, delList);
    }

    /**
     * 基于指定的 {@link Comparator} 比对两个集合的差异。
     * <p>
     * 底层使用 {@link TreeSet} 进行去重和比对。
     * 如果 {@code comparator.compare(a, b) == 0}，则认为元素 a 和 b 是相同的元素。
     * </p>
     *
     * @param <T>        集合元素类型
     * @param oldList    旧数据集合，允许为 null
     * @param newList    新数据集合，允许为 null
     * @param comparator 比较器，用于判断元素相等性，不能为 null
     * @return {@link DiffResult} 包含新增和删除的元素列表
     */
    @NonNull
    public static <T> DiffResult<T> diff(@Nullable Collection<T> oldList, @Nullable Collection<T> newList,
                                         @NonNull Comparator<? super T> comparator) {
        var safeOld = Objects.requireNonNullElse(oldList, List.<T>of());
        var safeNew = Objects.requireNonNullElse(newList, List.<T>of());

        // TreeSet 使用 Comparator 判断相等 (compare == 0 即视为相同)
        var oldSet = new TreeSet<>(comparator);
        oldSet.addAll(safeOld);

        var newSet = new TreeSet<>(comparator);
        newSet.addAll(safeNew);

        var addList = safeNew.stream()
                .filter(e -> !oldSet.contains(e))
                .toList();

        var delList = safeOld.stream()
                .filter(e -> !newSet.contains(e))
                .toList();

        return new DiffResult<>(addList, delList);
    }

    /**
     * 集合比对结果载体。
     * <p>
     * 包含新增列表和删除列表。返回的列表均为不可变列表，且不为 null。
     * </p>
     *
     * @param <T> 元素类型
     * @param add 新增的元素列表
     * @param del 删除的元素列表
     */
    public record DiffResult<T>(List<T> add, List<T> del) {

        // 紧凑构造函数：处理入参为 null 的情况，并保证列表不可变
        public DiffResult {
            add = add == null ? List.of() : List.copyOf(add);
            del = del == null ? List.of() : List.copyOf(del);
        }

        public List<T> changed() {
            return Stream.concat(add.stream(), del.stream())
                    .toList();
        }
    }
}
