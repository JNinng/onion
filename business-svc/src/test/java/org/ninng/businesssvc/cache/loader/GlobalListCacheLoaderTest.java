package org.ninng.businesssvc.cache.loader;

import org.junit.jupiter.api.Test;
import org.ninng.businesssvc.cache.CacheTestConfig;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.entity.GlobalListItem;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GlobalListCacheLoaderTest {

    private final GlobalListCacheLoader loader = new GlobalListCacheLoader();

    @Test
    void loadByIdShouldReturnItem() {
        CacheKey<String, Void> key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, "notice");
        GlobalListItem item = loader.load(key);

        assertNotNull(item);
        assertEquals("notice", item.getId());
        assertEquals("通知公告", item.getTitle());
        assertEquals(0, item.getSort());
    }

    @Test
    void loadByUnknownIdShouldReturnNull() {
        CacheKey<String, Void> key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, "nonexistent");
        assertNull(loader.load(key));
    }

    @Test
    void loadByNullIdShouldReturnNull() {
        CacheKey<String, Void> key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, null);
        assertNull(loader.load(key));
    }

    @Test
    void loadPageFirstPageShouldReturn3Items() {
        CacheKey<String, Void> key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, "_");

        PageResult<GlobalListItem> result = loader.loadPage(key, 1, 3);

        assertEquals(3, result.data()
                .size());
        assertEquals("notice", result.data()
                .get(0)
                .getId());
        assertEquals("agreement", result.data()
                .get(1)
                .getId());
        assertEquals("privacy", result.data()
                .get(2)
                .getId());
        assertEquals(1, result.page());
        assertEquals(3, result.pageSize());
        assertEquals(10, result.total());
        assertTrue(result.hasNext());
    }

    @Test
    void loadPageLastPageShouldReturn1Item() {
        CacheKey<String, Void> key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, "_");

        PageResult<GlobalListItem> result = loader.loadPage(key, 4, 3);

        assertEquals(1, result.data()
                .size());
        assertEquals("terms", result.data()
                .getFirst()
                .getId());
        assertFalse(result.hasNext());
    }

    @Test
    void loadPageBeyondRangeShouldReturnEmpty() {
        CacheKey<String, Void> key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, "_");

        PageResult<GlobalListItem> result = loader.loadPage(key, 5, 3);

        assertTrue(result.isEmpty());
        assertFalse(result.hasNext());
    }

    @Test
    void loadPageWithInvalidParamsShouldReturnEmpty() {
        CacheKey<String, Void> key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, "_");

        PageResult<GlobalListItem> result = loader.loadPage(key, 0, 3);
        assertTrue(result.isEmpty());

        result = loader.loadPage(key, 1, 0);
        assertTrue(result.isEmpty());
    }

    @Test
    void batchLoadShouldReturnMatchingItems() {
        CacheKey<String, Void> pattern = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, null);

        Map<String, GlobalListItem> result = loader.batchLoad(pattern,
                Set.of("notice", "privacy", "faq"));

        assertEquals(3, result.size());
        assertNotNull(result.get("notice"));
        assertNotNull(result.get("privacy"));
        assertNotNull(result.get("faq"));
        assertEquals("通知公告", result.get("notice")
                .getTitle());
    }

    @Test
    void batchLoadWithMixedIdsShouldReturnOnlyExisting() {
        CacheKey<String, Void> pattern = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, null);

        Map<String, GlobalListItem> result = loader.batchLoad(pattern,
                Set.of("notice", "nonexistent", "terms"));

        assertEquals(2, result.size());
        assertNotNull(result.get("notice"));
        assertNotNull(result.get("terms"));
        assertNull(result.get("nonexistent"));
    }

    @Test
    void batchLoadWithEmptyIdsShouldReturnEmpty() {
        CacheKey<String, Void> pattern = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, null);

        Map<String, GlobalListItem> result = loader.batchLoad(pattern, Set.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void loadPagePageSize10ShouldReturnAllInOnePage() {
        CacheKey<String, Void> key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKey(null, "_");

        PageResult<GlobalListItem> result = loader.loadPage(key, 1, 10);

        assertEquals(10, result.data()
                .size());
        assertFalse(result.hasNext());
    }
}
