package com.yirancrazy.minimall.common.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证内置角色权限映射、跨角色隔离、不可变约束和 Redis 覆盖回退规则。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class RolePermissionMappingTest {

  /** * 验证用户默认权限；读取用户映射；检查数量与关键权限；预期八项均匹配. */
  @Test
  void testUserHasExpectedPermissions() {
    Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(RoleEnum.USER);
    assertEquals(8, perms.size());
    assertTrue(perms.contains(PermissionEnum.GOODS_VIEW));
    assertTrue(perms.contains(PermissionEnum.CART_MANAGE));
    assertTrue(perms.contains(PermissionEnum.ORDER_CREATE));
    assertTrue(perms.contains(PermissionEnum.ORDER_VIEW));
    assertTrue(perms.contains(PermissionEnum.ORDER_CANCEL));
    assertTrue(perms.contains(PermissionEnum.PAY_VIEW));
    assertTrue(perms.contains(PermissionEnum.NOTIFY_VIEW));
    assertTrue(perms.contains(PermissionEnum.PROFILE_MANAGE));
  }

  /** * 验证商家默认权限；读取商家映射；检查数量与经营权限；预期八项均匹配. */
  @Test
  void testMerchantHasExpectedPermissions() {
    Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(RoleEnum.MERCHANT);
    assertEquals(8, perms.size());
    assertTrue(perms.contains(PermissionEnum.GOODS_MANAGE));
    assertTrue(perms.contains(PermissionEnum.STOCK_MANAGE));
    assertTrue(perms.contains(PermissionEnum.ORDER_SHIP));
    assertTrue(perms.contains(PermissionEnum.QUALIFICATION_SUBMIT));
  }

  /** * 验证平台默认权限；读取平台映射；检查九项治理权限；预期全部存在. */
  @Test
  void testPlatformHasExpectedPermissions() {
    Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(RoleEnum.PLATFORM);
    assertTrue(perms.contains(PermissionEnum.MERCHANT_AUDIT));
    assertTrue(perms.contains(PermissionEnum.GOODS_AUDIT));
    assertTrue(perms.contains(PermissionEnum.ORDER_VIEW_ALL));
    assertTrue(perms.contains(PermissionEnum.ORDER_DELETE));
    assertTrue(perms.contains(PermissionEnum.REFUND_AUDIT));
    assertTrue(perms.contains(PermissionEnum.ROLE_VIEW));
    assertTrue(perms.contains(PermissionEnum.ROLE_MANAGE));
    assertTrue(perms.contains(PermissionEnum.NOTIFY_BROADCAST));
    assertTrue(perms.contains(PermissionEnum.PLATFORM_FINANCE));
  }

  /** * 验证用户不能越权；检查三项平台权限；调用权限判断；预期均为false. */
  @Test
  void testUserCannotAccessPlatformPermissions() {
    assertFalse(RolePermissionMapping.hasPermission(RoleEnum.USER, PermissionEnum.MERCHANT_AUDIT));
    assertFalse(RolePermissionMapping.hasPermission(RoleEnum.USER, PermissionEnum.ORDER_DELETE));
    assertFalse(RolePermissionMapping.hasPermission(RoleEnum.USER, PermissionEnum.ROLE_VIEW));
  }

  /** * 验证商家不能越权；检查商品审核与财务权限；调用权限判断；预期均为false. */
  @Test
  void testMerchantCannotAccessPlatformPermissions() {
    assertFalse(RolePermissionMapping.hasPermission(RoleEnum.MERCHANT, PermissionEnum.GOODS_AUDIT));
    assertFalse(
        RolePermissionMapping.hasPermission(RoleEnum.MERCHANT, PermissionEnum.PLATFORM_FINANCE));
  }

  /** * 验证空角色处理；角色参数为null；调用权限查询；预期返回空集合. */
  @Test
  void testNullRoleReturnsEmptyPermissions() {
    Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(null);
    assertTrue(perms.isEmpty());
  }

  /** * 验证空参数处理；角色或权限为null；调用权限判断；预期均为false. */
  @Test
  void testHasPermissionWithNullArgsReturnsFalse() {
    assertFalse(RolePermissionMapping.hasPermission(null, PermissionEnum.GOODS_VIEW));
    assertFalse(RolePermissionMapping.hasPermission(RoleEnum.USER, null));
  }

  /** * 验证权限集合不可变；尝试向用户权限加入新项；调用集合添加；预期抛不支持操作异常. */
  @Test
  void testPermissionsSetIsImmutable() {
    Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(RoleEnum.USER);
    try {
      perms.add(PermissionEnum.ORDER_DELETE);
      assertFalse(true, "should have thrown UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }
  }

  /** * 验证Redis覆盖优先；商家覆盖为两项商品权限；查询有效权限；预期只返回这两项. */
  @Test
  void testGetEffectivePermissionsUsesRedisOverrideWhenPresent() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> ops = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.get(RolePermissionMapping.KEY_PREFIX + "MERCHANT"))
        .thenReturn("[\"GOODS_VIEW\",\"GOODS_MANAGE\"]");

    Set<PermissionEnum> perms =
        RolePermissionMapping.getEffectivePermissions(RoleEnum.MERCHANT, redis);

    assertEquals(2, perms.size());
    assertTrue(perms.contains(PermissionEnum.GOODS_VIEW));
    assertFalse(perms.contains(PermissionEnum.STOCK_MANAGE));
  }

  /** * 验证Redis缺失回退；平台覆盖键为null；查询有效权限；预期含内置角色查看权限. */
  @Test
  void testGetEffectivePermissionsFallsBackToStaticWhenRedisEmpty() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> ops = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.get(RolePermissionMapping.KEY_PREFIX + "PLATFORM")).thenReturn(null);

    Set<PermissionEnum> perms =
        RolePermissionMapping.getEffectivePermissions(RoleEnum.PLATFORM, redis);

    assertTrue(perms.contains(PermissionEnum.ROLE_VIEW));
  }
}
