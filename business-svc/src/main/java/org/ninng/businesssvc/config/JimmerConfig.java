package org.ninng.businesssvc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.cache.RedisHashBinder;
import org.babyfish.jimmer.spring.cache.RedisValueBinder;
import org.babyfish.jimmer.sql.cache.*;
import org.babyfish.jimmer.sql.cache.caffeine.CaffeineHashBinder;
import org.babyfish.jimmer.sql.cache.caffeine.CaffeineValueBinder;
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder;
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.babyfish.jimmer.sql.cache.spi.AbstractCacheCreator;
import org.ninng.businesssvc.constant.C;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

@Configuration
public class JimmerConfig {

    @Bean
    public CacheFactory cacheFactory(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        CacheCreator creator = new RedisCacheCreatorWarp(connectionFactory, objectMapper).withKeyPrefixProvider(
                        new PrefixKey(C.ORM_AUTO_KEY_PREFIX))
                .withRemoteDuration(C.ORM_AUTO_REMOTE_CACHE_DURATION)
                .withLocalCache(C.ORM_AUTO_LOCAL_CACHE_SIZE, C.ORM_AUTO_LOCAL_CACHE_DURATION)
                .withMultiViewProperties(C.ORM_AUTO_MULTI_VIEW_LOCAL_CACHE_SIZE,
                        C.ORM_AUTO_MULTI_VIEW_LOCAL_CACHE_DURATION, C.ORM_AUTO_MULTI_VIEW_REMOTTE_CACHE_DURATION);

        return new AbstractCacheFactory() {

            // Id -> Object
            @Override
            public Cache<?, ?> createObjectCache(ImmutableType type) {
                return creator.createForObject(type);
            }

            // Id -> TargetId, for one-to-one/many-to-one
            @Override
            public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
                return creator.createForProp(prop, getFilterState().isAffected(prop.getTargetType()));
            }

            // Id -> TargetId list, for one-to-many/many-to-many
            @Override
            public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                return creator.createForProp(prop, getFilterState().isAffected(prop.getTargetType()));
            }

            // Id -> computed value, for transient properties with resolver
            @Override
            public Cache<?, ?> createResolverCache(ImmutableProp prop) {
                return creator.createForProp(prop, true);
            }
        };
    }

    static class PrefixKey implements RemoteKeyPrefixProvider {

        private final String prefix;

        public PrefixKey() {
            this("orm:");
        }

        public PrefixKey(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String typeKeyPrefix(ImmutableType type) {
            return prefix + type.getJavaClass()
                    .getSimpleName() + ":";
        }

        @Override
        public String propKeyPrefix(ImmutableProp prop) {
            return prefix + prop.getDeclaringType()
                    .getJavaClass()
                    .getSimpleName() + '.' + prop.getName() + ':';
        }
    }

    static class RedisCacheCreatorWarp extends AbstractCacheCreator {

        public RedisCacheCreatorWarp(RedisConnectionFactory connectionFactory) {
            this(connectionFactory, null);
        }

        public RedisCacheCreatorWarp(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
            super(new Root(connectionFactory, objectMapper));
        }

        protected RedisCacheCreatorWarp(Cfg cfg) {
            super(cfg);
        }

        @Override
        public <K, V> Cache<K, V> createForObject(ImmutableType type) {
            return new ChainCacheBuilder<K, V>().add(caffeineValueBinder(type))
                    .add(redisValueBinder(type))
                    .build();
        }

        @Override
        public <K, V> Cache<K, V> createForProp(ImmutableProp prop, boolean multiView) {
            if (multiView) {
                return new ChainCacheBuilder<K, V>().add(caffeineHashBinder(prop))
                        .add(redisHashBinder(prop))
                        .build();
            }
            return new ChainCacheBuilder<K, V>().add(caffeineValueBinder(prop))
                    .add(redisValueBinder(prop))
                    .build();
        }

        @NewChain
        @NotNull
        public RedisCacheCreatorWarp withKeyPrefixProvider(@Nullable RemoteKeyPrefixProvider prefixProvider) {
            return new RedisCacheCreatorWarp(new KeyPrefixCfg(super.cfg, prefixProvider));
        }

        @Override
        protected Args newArgs(Cfg cfg) {
            return new Args(cfg);
        }

        @Override
        protected CacheCreator newCacheCreator(Cfg cfg) {
            return new RedisCacheCreatorWarp(cfg);
        }

        private <K, V> LoadingBinder<K, V> caffeineValueBinder(ImmutableType type) {
            Args args = args();
            if (!args.useLocalCache) {
                return null;
            }
            return CaffeineValueBinder.<K, V>forObject(type)
                    .subscribe(args.tracker)
                    .maximumSize(args.localCacheMaximumSize)
                    .duration(args.localCacheDuration)
                    .build();
        }

        private <K, V> LoadingBinder<K, V> caffeineValueBinder(ImmutableProp prop) {
            Args args = args();
            if (!args.useLocalCache) {
                return null;
            }
            return CaffeineValueBinder.<K, V>forProp(prop)
                    .subscribe(args.tracker)
                    .maximumSize(args.localCacheMaximumSize)
                    .duration(args.localCacheDuration)
                    .build();
        }

        private <K, V> SimpleBinder<K, V> caffeineHashBinder(ImmutableProp prop) {
            Args args = args();
            if (!args.useMultiViewLocalCache) {
                return null;
            }
            return CaffeineHashBinder.<K, V>forProp(prop)
                    .subscribe(args.tracker)
                    .maximumSize(args.multiViewLocalCacheMaximumSize)
                    .duration(args.multiViewLocalCacheDuration)
                    .build();
        }

        private <K, V> SimpleBinder<K, V> redisValueBinder(ImmutableType type) {
            Args args = args();
            return RedisValueBinder.<K, V>forObject(type)
                    .publish(args.tracker)
                    .objectMapper(args.objectMapper)
                    .keyPrefixProvider(args.keyPrefixProvider)
                    .duration(args.duration)
                    .randomPercent(args.randomDurationPercent)
                    .redis(args.connectionFactory)
                    .build()
                    .lock(args.locker, args.lockWaitDuration, args.lockLeaseDuration);
        }

        private <K, V> SimpleBinder<K, V> redisValueBinder(ImmutableProp prop) {
            Args args = args();
            return RedisValueBinder.<K, V>forProp(prop)
                    .publish(args.tracker)
                    .objectMapper(args.objectMapper)
                    .keyPrefixProvider(args.keyPrefixProvider)
                    .duration(args.duration)
                    .randomPercent(args.randomDurationPercent)
                    .redis(args.connectionFactory)
                    .build()
                    .lock(args.locker, args.lockWaitDuration, args.lockLeaseDuration);
        }

        private <K, V> SimpleBinder.Parameterized<K, V> redisHashBinder(ImmutableProp prop) {
            Args args = args();
            return RedisHashBinder.<K, V>forProp(prop)
                    .publish(args.tracker)
                    .objectMapper(args.objectMapper)
                    .keyPrefixProvider(args.keyPrefixProvider)
                    .duration(args.multiVewDuration)
                    .randomPercent(args.randomDurationPercent)
                    .redis(args.connectionFactory)
                    .build()
                    .lock(args.locker, args.lockWaitDuration, args.lockLeaseDuration);
        }

        private static class KeyPrefixCfg extends Cfg {

            final RemoteKeyPrefixProvider prefixProvider;

            private KeyPrefixCfg(Cfg prev, RemoteKeyPrefixProvider prefixProvider) {
                super(prev);
                this.prefixProvider = prefixProvider;
            }
        }

        private static class Root extends Cfg {

            final RedisConnectionFactory connectionFactory;

            final ObjectMapper objectMapper;

            private Root(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
                super(null);
                this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory cannot be null");
                this.objectMapper = objectMapper;
            }
        }

        static class Args extends AbstractCacheCreator.Args {

            final RedisConnectionFactory connectionFactory;
            final ObjectMapper objectMapper;
            final RemoteKeyPrefixProvider keyPrefixProvider;

            Args(Cfg cfg) {
                super(cfg);

                Root root = cfg.as(Root.class);

                this.connectionFactory = root.connectionFactory;
                ObjectMapper mapper = root.objectMapper;
                ObjectMapper clonedMapper = mapper != null ? new ObjectMapper(mapper) {
                    @Serial
                    private static final long serialVersionUID = 4803231399318698891L;
                } : new ObjectMapper();
                clonedMapper.registerModule(new JavaTimeModule());
                clonedMapper.registerModule(new ImmutableModule());
                this.objectMapper = clonedMapper;

                KeyPrefixCfg keyPrefixCfg = cfg.as(KeyPrefixCfg.class);
                this.keyPrefixProvider = (keyPrefixCfg != null && keyPrefixCfg.prefixProvider != null) ?
                        keyPrefixCfg.prefixProvider : new PrefixKey();
            }
        }
    }
}
