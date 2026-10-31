package com.yirancrazy.minimall.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.entity.AuthRolePo;
import com.yirancrazy.minimall.auth.entity.AuthUserPo;
import com.yirancrazy.minimall.auth.gateway.AuthRemoteGateway;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.common.constant.jwt.JwtType;
import com.yirancrazy.minimall.common.exception.BizException;
import io.jsonwebtoken.Claims;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 刷新令牌并发轮换测试；模拟多个请求消费同一刷新记录，验证原子删除的胜者唯一性。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenRotationConcurrencyTest {

  private static final int CONCURRENT_REQUESTS = 8;

  @Mock private AuthUserManager authUserManager;
  @Mock private AuthRoleManager authRoleManager;
  @Mock private AuthTokenBlacklistManager tokenBlacklistManager;
  @Mock private AuthRemoteGateway authRemoteGateway;
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
    when(claims.get("type", String.class)).thenReturn(JwtType.REFRESH);
    when(jwtUtil.parse("refresh-jwt")).thenReturn(claims);
    when(jwtUtil.sign(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            anyLong(),
            anyString(),
            eq(JwtType.ACCESS),
            eq(900_000L)))
        .thenReturn("access-token");
    when(jwtUtil.sign(
            anyLong(),
            anyLong(),
            anyString(),
            anyString(),
            anyLong(),
            anyString(),
            eq(JwtType.REFRESH),
            eq(604_800_000L)))
        .thenReturn("refresh-token");

    AuthRolePo role = new AuthRolePo();
    role.setId(1L);
    role.setRoleCode("USER");
    when(authRoleManager.getById(1L)).thenReturn(role);
    AuthUserPo user = new AuthUserPo();
    user.setId(1L);
    user.setAccount("alice");
    user.setRoleId(1L);
    user.setUserId(9001L);
    when(authUserManager.getById(1L)).thenReturn(user);

    // Redis 侧比较并删除只可能成功一次，其余并发请求全部拿到 0
    AtomicInteger remainingWinners = new AtomicInteger(1);
    when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
        .thenAnswer(invocation -> remainingWinners.getAndDecrement() > 0 ? 1L : 0L);

    authService =
        new AuthServiceImpl(
            authUserManager,
            authRoleManager,
            tokenBlacklistManager,
            authRemoteGateway,
            jwtUtil,
            redisTemplate,
            900_000L,
            604_800_000L);
  }

  /** 验证并发刷新仅一个请求成功：8 个请求争抢同一记录，放行后预期 1 次签发、7 次刷新令牌无效. */
  @Test
  void testConcurrentRefreshOnlyWinnerRotates() throws Exception {
    AtomicInteger winners = new AtomicInteger();
    AtomicInteger rejected = new AtomicInteger();
    CountDownLatch startGate = new CountDownLatch(1);
    ThreadPoolExecutor pool =
        new ThreadPoolExecutor(
            CONCURRENT_REQUESTS,
            CONCURRENT_REQUESTS,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(CONCURRENT_REQUESTS),
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
    } finally {
      pool.shutdownNow();
    }

    assertEquals(1, winners.get(), "only the winner of the atomic delete may rotate the token");
    assertEquals(CONCURRENT_REQUESTS - 1, rejected.get());
  }

  private Callable<Void> refreshTask(
      CountDownLatch startGate, AtomicInteger winners, AtomicInteger rejected) {
    return () -> {
      startGate.await();
      try {
        TokenVo vo = authService.refreshToken("refresh-jwt");
        if (vo != null) {
          winners.incrementAndGet();
        }
      } catch (BizException ex) {
        assertEquals(AuthCodeEnum.REFRESH_TOKEN_INVALID.getCode(), ex.getCode());
        rejected.incrementAndGet();
      }
      return null;
    };
  }

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 测试专用线程工厂；为并发刷新任务生成带统一前缀和非零序号的守护线程。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  private static final class NamedThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger counter = new AtomicInteger();

    private NamedThreadFactory(String prefix) {
      this.prefix = prefix;
    }

    /**
     * 创建带统一前缀和递增序号的守护线程。
     *
     * @param runnable 线程要执行的任务
     * @return 已命名的守护线程
     */
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    }
  }
}
