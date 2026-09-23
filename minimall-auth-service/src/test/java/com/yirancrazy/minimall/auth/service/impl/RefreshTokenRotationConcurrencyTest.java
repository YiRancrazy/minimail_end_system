package com.yirancrazy.minimall.auth.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.jsonwebtoken.Claims;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.entity.AuthRolePO;
import com.yirancrazy.minimall.auth.entity.AuthUserPO;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 刷新令牌轮换并发测试，校验同一刷新令牌并发重放时只有原子删除成功者能换到新令牌。
 * @Version: 1.0
 * @DateTime: 2026/09/23
 **/
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenRotationConcurrencyTest {

    private static final int CONCURRENT_REQUESTS = 8;

    @Mock private AuthUserManager authUserManager;
    @Mock private AuthRoleManager authRoleManager;
    @Mock private AuthTokenBlacklistManager tokenBlacklistManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("jti", String.class)).thenReturn("race-jti");
        when(jwtUtil.parse("refresh-jwt")).thenReturn(claims);
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyLong(), anyString()))
            .thenReturn("access-token");
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyLong()))
            .thenReturn("refresh-token");

        AuthRolePO role = new AuthRolePO();
        role.setId(1L);
        role.setRoleCode("USER");
        when(authRoleManager.getById(1L)).thenReturn(role);
        AuthUserPO user = new AuthUserPO();
        user.setId(1L);
        user.setAccount("alice");
        user.setRoleId(1L);
        when(authUserManager.getById(1L)).thenReturn(user);

        // Redis 侧比较并删除只可能成功一次，其余并发请求全部拿到 0
        AtomicInteger remainingWinners = new AtomicInteger(1);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
            .thenAnswer(invocation -> remainingWinners.getAndDecrement() > 0 ? 1L : 0L);

        authService = new AuthServiceImpl(authUserManager, authRoleManager, tokenBlacklistManager,
            jwtUtil, redisTemplate, 900L, 604800L);
    }

    @Test
    void concurrent_refresh_only_winner_rotates() throws Exception {
        AtomicInteger winners = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch startGate = new CountDownLatch(1);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(CONCURRENT_REQUESTS, CONCURRENT_REQUESTS,
            0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(CONCURRENT_REQUESTS),
            new NamedThreadFactory("refresh-race"));
        try {
            List<Future<Void>> futures = new ArrayList<>(CONCURRENT_REQUESTS);
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                futures.add(pool.submit(refreshTask(startGate, winners, rejected)));
            }
            startGate.countDown();
            for (Future<Void> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        }
        finally {
            pool.shutdownNow();
        }

        assertEquals(1, winners.get(), "only the winner of the atomic delete may rotate the token");
        assertEquals(CONCURRENT_REQUESTS - 1, rejected.get());
    }

    private Callable<Void> refreshTask(CountDownLatch startGate, AtomicInteger winners,
                                      AtomicInteger rejected) {
        return () -> {
            startGate.await();
            try {
                TokenVO vo = authService.refreshToken("refresh-jwt");
                if (vo != null) {
                    winners.incrementAndGet();
                }
            }
            catch (BizException ex) {
                assertEquals(AuthCodeEnum.REFRESH_TOKEN_INVALID.getCode(), ex.getCode());
                rejected.incrementAndGet();
            }
            return null;
        };
    }

    /**
     * 带命名前缀的线程工厂，便于并发测试线程定位。
     */
    private static final class NamedThreadFactory implements ThreadFactory {

        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger();

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
