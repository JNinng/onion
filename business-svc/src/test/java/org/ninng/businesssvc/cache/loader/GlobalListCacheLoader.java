package org.ninng.businesssvc.cache.loader;

import org.ninng.businesssvc.cache.CacheTestConfig;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.entity.GlobalListItem;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GlobalListCacheLoader implements CacheLoader<String, Void, GlobalListItem> {

    @Override
    public String name() {
        return CacheTestConfig.GLOBAL_TEST;
    }

    static final List<GlobalListItem> MOCK_DATA = List.of(
            new GlobalListItem("notice", "notice", "通知公告", 0),
            new GlobalListItem("agreement", "agreement", "用户协议", 1),
            new GlobalListItem("privacy", "privacy", "隐私政策", 2),
            new GlobalListItem("help", "help", "使用帮助", 3),
            new GlobalListItem("about", "about", "关于我们", 4),
            new GlobalListItem("contact", "contact", "联系方式", 5),
            new GlobalListItem("faq", "faq", "常见问题", 6),
            new GlobalListItem("changelog", "changelog", "版本历史", 7),
            new GlobalListItem("license", "license", "开源许可", 8),
            new GlobalListItem("terms", "terms", "服务条款", 9)
    );

    @Override
    @Nullable
    public GlobalListItem load(CacheKey<String, Void> key) {
        String id = key.id();
        if (id == null) {
            return null;
        }
        return MOCK_DATA.stream()
                .filter(item -> item.getId()
                        .equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public PageResult<GlobalListItem> loadPage(CacheKey<String, Void> key, int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            return new PageResult<>(Collections.emptyList(), page, pageSize, MOCK_DATA.size());
        }
        int from = (page - 1) * pageSize;
        if (from >= MOCK_DATA.size()) {
            return new PageResult<>(Collections.emptyList(), page, pageSize, MOCK_DATA.size());
        }
        int to = Math.min(from + pageSize, MOCK_DATA.size());
        List<GlobalListItem> pageData = new ArrayList<>(MOCK_DATA.subList(from, to));
        return new PageResult<>(pageData, page, pageSize, MOCK_DATA.size());
    }

    @Override
    public Map<String, GlobalListItem> batchLoad(CacheKey<String, Void> pattern, Set<String> ids) {
        Map<String, GlobalListItem> result = new LinkedHashMap<>();
        for (String id : ids) {
            MOCK_DATA.stream()
                    .filter(item -> item.getId()
                            .equals(id))
                    .findFirst()
                    .ifPresent(item -> result.put(id, item));
        }
        return result;
    }
}
