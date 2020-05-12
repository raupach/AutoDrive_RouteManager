package de.adEditor;

import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.*;
import org.ehcache.config.units.MemoryUnit;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.awt.*;
import java.io.File;
import java.time.Duration;
import java.util.concurrent.ConcurrentMap;

@Configuration
@ComponentScan(basePackages = "de.adEditor")
public class AppConfig {

    public static final String IMAGES_CACHE_L1 = "images_l1";
    public static final String IMAGES_CACHE_L2 = "images_l2";
    public static final String IMAGES_CACHE_L3 = "images_l3";

    @Bean (destroyMethod = "close")
    public CacheManager cacheConfig() {
        PersistentCacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(new File("cache")))
                .withCache(IMAGES_CACHE_L1,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Image.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(300, MemoryUnit.MB)
                        )
                )
                .withCache(IMAGES_CACHE_L2,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(5, MemoryUnit.MB)
                                        .disk(10, MemoryUnit.GB, true)
                        )
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(30)))
                )
                .withCache(IMAGES_CACHE_L3,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(5, MemoryUnit.MB)
                                        .disk(10, MemoryUnit.GB, true)
                        )
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofDays(2)))
                )
                .build(true);

        return cacheManager;
    }

}
