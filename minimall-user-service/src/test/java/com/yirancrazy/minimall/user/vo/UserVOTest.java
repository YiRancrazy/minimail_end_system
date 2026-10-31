package com.yirancrazy.minimall.user.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.user.entity.UserPo;
import java.time.LocalDateTime;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户视图转换单元测试，覆盖字段映射、头像 URL 解析以及手机号和邮箱脱敏边界.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class UserVoTest {

  /**
   * 验证提供 MinioUtil 时头像对象键会解析为公开 URL。
   *
   * <p>前置条件：头像键为 a1b2c3.png，替身返回带 token 的 MinIO URL。关键步骤：调用双参数 from。
   * 预期结果：视图头像为解析后的 URL。
   */
  public void fromWithMinioUtilResolvesAvatar() {
    UserPo po = new UserPo();
    po.setId(1L);
    po.setAvatar("a1b2c3.png");
    MinioUtil minioUtil = mock(MinioUtil.class);
    when(minioUtil.resolvePublicUrl("a1b2c3.png"))
        .thenReturn("http://minio/mall-files/a1b2c3.png?token");

    UserVo vo = UserVo.from(po, minioUtil);

    assertEquals("http://minio/mall-files/a1b2c3.png?token", vo.getAvatar());
  }

  /**
   * 验证完整用户对象会映射到视图并执行敏感信息脱敏。
   *
   * <p>前置条件：用户包含有效昵称、手机号、邮箱、头像、性别和创建时间。关键步骤：调用 from。
   * 预期结果：普通字段完整映射，手机号和邮箱为脱敏值。
   */
  public void fromMapsAllFields() {
    UserPo po = new UserPo();
    po.setId(1L);
    po.setNickname("Alice");
    po.setPhone("13812345678");
    po.setEmail("test@example.com");
    po.setAvatar("https://example.com/avatar.png");
    po.setGender(1);
    po.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));

    UserVo vo = UserVo.from(po);

    assertNotNull(vo);
    assertEquals(1L, vo.getId());
    assertEquals("Alice", vo.getNickname());
    assertEquals("138****5678", vo.getPhone());
    assertEquals("te***@example.com", vo.getEmail());
    assertEquals("https://example.com/avatar.png", vo.getAvatar());
    assertEquals(1, vo.getGender());
    assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), vo.getCreateTime());
  }

  /**
   * 验证手机号中间四位会被星号替换。
   *
   * <p>前置条件：手机号为 13812345678。关键步骤：调用 from。预期结果：phone=138****5678。
   */
  public void fromMasksPhone() {
    UserPo po = new UserPo();
    po.setId(1L);
    po.setPhone("13812345678");

    UserVo vo = UserVo.from(po);

    assertEquals("138****5678", vo.getPhone());
  }

  /**
   * 验证邮箱前缀仅保留前两个字符。
   *
   * <p>前置条件：邮箱为 test@example.com。关键步骤：调用 from。预期结果：email=te***@example.com。
   */
  public void fromMasksEmail() {
    UserPo po = new UserPo();
    po.setId(1L);
    po.setEmail("test@example.com");

    UserVo vo = UserVo.from(po);

    assertEquals("te***@example.com", vo.getEmail());
  }

  /**
   * 验证手机号为 null 时视图保持 null。
   *
   * <p>前置条件：用户手机号为 null。关键步骤：调用 from。预期结果：视图 phone 为 null。
   */
  public void fromNullPhone() {
    UserPo po = new UserPo();
    po.setId(1L);
    po.setPhone(null);

    UserVo vo = UserVo.from(po);

    assertNull(vo.getPhone());
  }

  /**
   * 验证邮箱为 null 时视图保持 null。
   *
   * <p>前置条件：用户邮箱为 null。关键步骤：调用 from。预期结果：视图 email 为 null。
   */
  public void fromNullEmail() {
    UserPo po = new UserPo();
    po.setId(1L);
    po.setEmail(null);

    UserVo vo = UserVo.from(po);

    assertNull(vo.getEmail());
  }

  /**
   * 验证空手机号不会被脱敏逻辑改写。
   *
   * <p>前置条件：用户手机号为空字符串。关键步骤：调用 from。预期结果：视图 phone 仍为空字符串。
   */
  public void fromEmptyPhone() {
    UserPo po = new UserPo();
    po.setId(1L);
    po.setPhone("");

    UserVo vo = UserVo.from(po);

    assertEquals("", vo.getPhone());
  }

  /**
   * 验证短邮箱前缀仍按最多两个字符规则脱敏。
   *
   * <p>前置条件：邮箱为 ab@c.com。关键步骤：调用 from。预期结果：email=ab***@c.com。
   */
  public void fromShortEmail() {
    UserPo po = new UserPo();
    po.setId(1L);
    po.setEmail("ab@c.com");

    UserVo vo = UserVo.from(po);

    assertEquals("ab***@c.com", vo.getEmail());
  }

  /**
   * 验证单字符邮箱前缀也保留并追加掩码。
   *
   * <p>前置条件：邮箱为 a@x.com。关键步骤：调用 from。预期结果：email=a***@x.com。
   */
  public void fromSingleCharPrefixEmailStillMasked() {
    UserPo po = new UserPo();
    po.setId(1L);
    po.setEmail("a@x.com");

    UserVo vo = UserVo.from(po);

    assertEquals("a***@x.com", vo.getEmail());
  }
}
