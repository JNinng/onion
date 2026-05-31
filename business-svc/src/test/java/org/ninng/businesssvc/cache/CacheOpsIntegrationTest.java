package org.ninng.businesssvc.cache;

import org.junit.jupiter.api.*;
import org.ninng.businesssvc.cache.ops.CacheOps;
import org.ninng.businesssvc.cache.strategy.RefreshStrategy;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheOpsIntegrationTest {

    private static final String KEY_PATTERN = "onion:global:test:*";

    @Autowired
    private CacheOps cacheOps;

    @Autowired
    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() {
        var keys = redissonClient.getKeys();
        for (String key : keys.getKeys(KeysScanOptions.defaults()
                .pattern(KEY_PATTERN))) {
            keys.delete(key);
        }
    }

    @Test
    @Order(1)
    void refreshShouldPopulateAllItems() {
        cacheOps.refresh(CacheTestConfig.GLOBAL_TEST_DOMAIN, null,
                RefreshStrategy.fixed(3));

        List<String> keyList = new ArrayList<>();
        redissonClient.getKeys()
                .getKeys(KeysScanOptions.defaults()
                        .pattern(KEY_PATTERN))
                .forEach(keyList::add);

        assertEquals(10, keyList.size());
    }

    @Test
    @Order(2)
    void getWithEmptyCacheShouldFallbackToLoader() {
        Optional<GlobalListItem> result =
                cacheOps.<String, Void, GlobalListItem>get(CacheTestConfig.GLOBAL_TEST_DOMAIN, null, "faq");

        assertTrue(result.isPresent());
        GlobalListItem item = result.get();
        assertEquals("faq", item.getId());
        assertEquals("常见问题", item.getTitle());
        assertEquals(6, item.getSort());

        // Verify L2 was populated
        String key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKeyString(null, "faq");
        assertNotNull(redissonClient.getBucket(key)
                .get());
    }

    @Test
    @Order(3)
    void getAfterRefreshShouldHitL2() {
        cacheOps.refresh(CacheTestConfig.GLOBAL_TEST_DOMAIN, null,
                RefreshStrategy.fixed(3));

        Optional<GlobalListItem> result =
                cacheOps.<String, Void, GlobalListItem>get(CacheTestConfig.GLOBAL_TEST_DOMAIN, null, "changelog");

        assertTrue(result.isPresent());
        assertEquals("changelog", result.get()
                .getId());
        assertEquals("版本历史", result.get()
                .getTitle());
    }

    @Test
    @Order(4)
    void getWithUnknownIdShouldReturnEmpty() {
        Optional<GlobalListItem> result =
                cacheOps.<String, Void, GlobalListItem>get(CacheTestConfig.GLOBAL_TEST_DOMAIN, null, "no-such-id");

        assertFalse(result.isPresent());
    }

    @Test
    @Order(5)
    void getTwiceShouldHitL1OnSecondCall() {
        // First call: L2 miss → L3 hit → populates L1 + L2
        Optional<GlobalListItem> first =
                cacheOps.<String, Void, GlobalListItem>get(CacheTestConfig.GLOBAL_TEST_DOMAIN, null, "help");
        assertTrue(first.isPresent());

        // Evict only L2 (bypass CacheOps to keep L1 intact)
        String key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKeyString(null, "help");
        redissonClient.getBucket(key)
                .delete();

        // Second call: L2 miss → L1 hit (Caffeine)
        Optional<GlobalListItem> second =
                cacheOps.<String, Void, GlobalListItem>get(CacheTestConfig.GLOBAL_TEST_DOMAIN, null, "help");
        assertTrue(second.isPresent());
        assertEquals("help", second.get()
                .getId());
        assertEquals("使用帮助", second.get()
                .getTitle());
    }

    @Test
    @Order(6)
    void batchGetShouldReturnAllRequested() {
        cacheOps.refresh(CacheTestConfig.GLOBAL_TEST_DOMAIN, null,
                RefreshStrategy.fixed(3));

        Set<String> ids = new LinkedHashSet<>();
        ids.add("notice");
        ids.add("agreement");
        ids.add("privacy");

        Map<String, GlobalListItem> items =
                cacheOps.<String, Void, GlobalListItem>batchGet(CacheTestConfig.GLOBAL_TEST_DOMAIN, null, ids);

        assertEquals(3, items.size());
        assertTrue(items.containsKey("notice"));
        assertTrue(items.containsKey("agreement"));
        assertTrue(items.containsKey("privacy"));
    }

    @Test
    @Order(7)
    void batchGetWithEmptyCacheShouldFallbackToLoader() {
        Set<String> ids = new LinkedHashSet<>();
        ids.add("terms");
        ids.add("license");
        ids.add("nonexistent");

        Map<String, GlobalListItem> items =
                cacheOps.<String, Void, GlobalListItem>batchGet(CacheTestConfig.GLOBAL_TEST_DOMAIN, null, ids);

        assertEquals(2, items.size());
        assertTrue(items.containsKey("terms"));
        assertTrue(items.containsKey("license"));
        assertFalse(items.containsKey("nonexistent"));

        // Verify L2 was populated for found items
        String termsKey = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKeyString(null, "terms");
        String licenseKey = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKeyString(null, "license");
        assertNotNull(redissonClient.getBucket(termsKey)
                .get());
        assertNotNull(redissonClient.getBucket(licenseKey)
                .get());
    }

    @Test
    @Order(8)
    void evictShouldRemoveSingleItem() {
        cacheOps.refresh(CacheTestConfig.GLOBAL_TEST_DOMAIN, null,
                RefreshStrategy.fixed(3));

        String key = CacheTestConfig.GLOBAL_TEST_DOMAIN.buildKeyString(null, "about");
        assertNotNull(redissonClient.getBucket(key)
                .get());

        cacheOps.evict(CacheTestConfig.GLOBAL_TEST_DOMAIN, null, "about");

        assertNull(redissonClient.getBucket(key)
                .get());
    }

    @Test
    @Order(9)
    void endToEndRefreshGetEvictGet() {
        // Refresh → populate all
        cacheOps.refresh(CacheTestConfig.GLOBAL_TEST_DOMAIN, null,
                RefreshStrategy.fixed(3));

        // Get → should hit L2
        Optional<GlobalListItem> hit = cacheOps.<String, Void, GlobalListItem>get(
                CacheTestConfig.GLOBAL_TEST_DOMAIN, null, "contact");
        assertTrue(hit.isPresent());
        assertEquals("联系方式", hit.get()
                .getTitle());

        // Evict
        cacheOps.evict(CacheTestConfig.GLOBAL_TEST_DOMAIN, null, "contact");

        // Get after evict → L3 fallback (still in mock data)
        Optional<GlobalListItem> reload = cacheOps.<String, Void, GlobalListItem>get(
                CacheTestConfig.GLOBAL_TEST_DOMAIN, null, "contact");
        assertTrue(reload.isPresent());
        assertEquals("contact", reload.get()
                .getId());
    }

    @Test
    @Order(10)
    void refreshWithPageSize5ShouldWork() {
        cacheOps.refresh(CacheTestConfig.GLOBAL_TEST_DOMAIN, null,
                RefreshStrategy.fixed(5));

        List<String> keyList = new ArrayList<>();
        redissonClient.getKeys()
                .getKeys(KeysScanOptions.defaults()
                        .pattern(KEY_PATTERN))
                .forEach(keyList::add);

        assertEquals(10, keyList.size());
    }
}
