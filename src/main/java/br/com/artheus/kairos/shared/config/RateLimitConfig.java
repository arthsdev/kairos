package br.com.artheus.kairos.shared.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dedicated Lettuce connection for Bucket4j (distributed rate limiting via Redis).
 * <p>
 * Separated from the {@code StringRedisTemplate} used elsewhere in the system,
 * because Bucket4j-Lettuce requires direct access to the native Lettuce API
 * ({@link StatefulRedisConnection}), bypassing the Spring Data Redis abstraction layer.
 */
@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Creates and manages the native Lettuce RedisClient lifecycle.
     * The 'shutdown' method ensures thread pools and physical resources are freed on app teardown.
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient() {
        return RedisClient.create(
                RedisURI.Builder.redis(redisHost, redisPort).build()
        );
    }

    /**
     * Creates and manages the dedicated Redis connection for Bucket4j.
     * The 'close' method ensures the physical connection socket is closed on app teardown.
     * * @param bucket4jRedisClient automatically injected by Spring
     */
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(RedisClient bucket4jRedisClient) {
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        return bucket4jRedisClient.connect(codec);
    }

    /**
     * Configures the Bucket4j ProxyManager using the managed Lettuce connection.
     * * @param bucket4jRedisConnection automatically injected by Spring
     */
    @Bean
    public ProxyManager<String> bucket4jProxyManager(StatefulRedisConnection<String, byte[]> bucket4jRedisConnection) {
        return Bucket4jLettuce.casBasedBuilder(bucket4jRedisConnection)
                .build();
    }
}