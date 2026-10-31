package com.yirancrazy.minimall.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.user.dto.AddressCreateDto;
import com.yirancrazy.minimall.user.dto.AddressUpdateDto;
import com.yirancrazy.minimall.user.entity.AddressPo;
import com.yirancrazy.minimall.user.manager.AddressManager;
import com.yirancrazy.minimall.user.service.impl.AddressServiceImpl;
import com.yirancrazy.minimall.user.vo.AddressVo;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址服务单元测试，使用 MyBatis-Plus Wrapper 与 Manager 替身验证地址上限、归属校验、删除和默认地址规则.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class AddressServiceImplTest {

  /**
   * 初始化 AddressPo 的 MyBatis-Plus Lambda 元数据缓存。
   *
   * <p>该步骤使服务实现中的 lambda 字段引用可在无 Spring 容器时完成解析。
   */
  @BeforeAll
  static void initLambdaCache() {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), ""), AddressPo.class);
  }

  private AddressManager addressManager;
  private AddressServiceImpl service;

  /**
   * 初始化 AddressManager 替身和待测服务。
   *
   * <p>默认让保存操作回填 ID，并让更新、删除和批量默认标记更新返回成功。
   */
  @BeforeEach
  void setUp() {
    addressManager = mock(AddressManager.class);
    lenient()
        .doAnswer(
            inv -> {
              AddressPo p = inv.getArgument(0);
              if (p.getId() == null) {
                p.setId(System.nanoTime());
              }
              return true;
            })
        .when(addressManager)
        .save(any(AddressPo.class));
    lenient().when(addressManager.updateById(any(AddressPo.class))).thenReturn(true);
    lenient().when(addressManager.removeById(anyLong())).thenReturn(true);
    lenient().when(addressManager.update(any(Wrapper.class))).thenReturn(true);
    service = new AddressServiceImpl(addressManager);
  }

  /**
   * 验证用户没有地址时新增操作会持久化并返回新 ID。
   *
   * <p>前置条件：地址计数为 0。关键步骤：调用 create 并传入合法地址。预期结果：返回非空 ID，且调用
   * save。
   */
  public void createPersistsAndReturnsId() {
    when(addressManager.count(any(Wrapper.class))).thenReturn(0L);

    AddressCreateDto dto = buildCreateDto("张三", "13800000001");
    Long id = service.create(1L, dto);

    assertTrue(id != null);
    verify(addressManager).save(any(AddressPo.class));
  }

  /**
   * 验证用户已有 20 条地址时拒绝继续新增。
   *
   * <p>前置条件：地址计数为 20。关键步骤：调用 create。预期结果：抛出 BizException，且不调用 save。
   */
  public void createThrowsWhenLimitExceeded() {
    when(addressManager.count(any(Wrapper.class))).thenReturn(20L);

    AddressCreateDto dto = buildCreateDto("张三", "13800000001");
    assertThrows(BizException.class, () -> service.create(1L, dto));
    verify(addressManager, never()).save(any(AddressPo.class));
  }

  /**
   * 验证首条地址会自动标记为默认地址。
   *
   * <p>前置条件：地址计数为 0。关键步骤：捕获 create 写入的 AddressPo。预期结果：isDefault=1。
   */
  public void createAutoSetDefaultWhenNoAddresses() {
    when(addressManager.count(any(Wrapper.class))).thenReturn(0L);
    doAnswer(
            inv -> {
              AddressPo saved = inv.getArgument(0);
              assertEquals(1, saved.getIsDefault());
              return true;
            })
        .when(addressManager)
        .save(any(AddressPo.class));

    AddressCreateDto dto = buildCreateDto("张三", "13800000001");
    service.create(1L, dto);

    verify(addressManager).save(any(AddressPo.class));
  }

  /**
   * 验证地址列表会把 Manager 返回的持久化记录转换为视图。
   *
   * <p>前置条件：Manager 返回一条收件人为“张三”的地址。关键步骤：调用 listByUserId。预期结果：
   * 返回一条记录且姓名保持“张三”。
   */
  public void listByUserIdReturnsAddresses() {
    AddressPo addr = new AddressPo();
    addr.setId(1L);
    addr.setUserId(1L);
    addr.setReceiverName("张三");
    when(addressManager.list(any(Wrapper.class))).thenReturn(List.of(addr));

    List<AddressVo> result = service.listByUserId(1L);

    assertEquals(1, result.size());
    assertEquals("张三", result.get(0).getReceiver());
  }

  /**
   * 验证地址属于当前用户时允许更新。
   *
   * <p>前置条件：地址 1 的 userId=1。关键步骤：用户 1 调用 update。预期结果：返回 true，并调用
   * updateById。
   */
  public void updateSucceedsWhenOwned() {
    AddressPo existing = buildOwnedAddress(1L, 1L);
    when(addressManager.getById(1L)).thenReturn(existing);

    AddressUpdateDto dto = buildUpdateDto("李四", "13900000002");
    boolean ok = service.update(1L, 1L, dto);

    assertTrue(ok);
    verify(addressManager).updateById(any(AddressPo.class));
  }

  /**
   * 验证不能更新其他用户拥有的地址。
   *
   * <p>前置条件：地址 1 的 userId=2。关键步骤：用户 1 调用 update。预期结果：抛出 BizException。
   */
  public void updateThrowsWhenNotOwned() {
    AddressPo existing = buildOwnedAddress(1L, 2L);
    when(addressManager.getById(1L)).thenReturn(existing);

    AddressUpdateDto dto = buildUpdateDto("李四", "13900000002");
    assertThrows(BizException.class, () -> service.update(1L, 1L, dto));
  }

  /**
   * 验证更新不存在的地址时抛出业务异常。
   *
   * <p>前置条件：Manager 对地址 999 返回 null。关键步骤：调用 update。预期结果：抛出 BizException。
   */
  public void updateThrowsWhenNotFound() {
    when(addressManager.getById(999L)).thenReturn(null);

    AddressUpdateDto dto = buildUpdateDto("李四", "13900000002");
    assertThrows(BizException.class, () -> service.update(1L, 999L, dto));
  }

  /**
   * 验证地址属于当前用户时允许删除。
   *
   * <p>前置条件：地址 1 的 userId=1。关键步骤：用户 1 调用 delete。预期结果：调用 removeById(1)。
   */
  public void deleteSucceedsWhenOwned() {
    AddressPo existing = buildOwnedAddress(1L, 1L);
    when(addressManager.getById(1L)).thenReturn(existing);

    service.delete(1L, 1L);

    verify(addressManager).removeById(1L);
  }

  /**
   * 验证不能删除其他用户拥有的地址。
   *
   * <p>前置条件：地址 1 的 userId=2。关键步骤：用户 1 调用 delete。预期结果：抛出 BizException，
   * 且不调用 removeById。
   */
  public void deleteThrowsWhenNotOwned() {
    AddressPo existing = buildOwnedAddress(1L, 2L);
    when(addressManager.getById(1L)).thenReturn(existing);

    assertThrows(BizException.class, () -> service.delete(1L, 1L));
    verify(addressManager, never()).removeById(anyLong());
  }

  /**
   * 验证设置默认地址会先清除旧默认标记再保存目标地址。
   *
   * <p>前置条件：目标地址归属当前用户。关键步骤：调用 setDefault。预期结果：同时调用条件更新和目标记录的
   * updateById。
   */
  public void setDefaultClearsOldAndSetsNew() {
    AddressPo existing = buildOwnedAddress(1L, 1L);
    when(addressManager.getById(1L)).thenReturn(existing);

    service.setDefault(1L, 1L);

    verify(addressManager).update(any(Wrapper.class));
    verify(addressManager).updateById(any(AddressPo.class));
  }

  /**
   * 验证不能将其他用户的地址设为默认地址。
   *
   * <p>前置条件：地址 1 的 userId=2。关键步骤：用户 1 调用 setDefault。预期结果：抛出 BizException。
   */
  public void setDefaultThrowsWhenNotOwned() {
    AddressPo existing = buildOwnedAddress(1L, 2L);
    when(addressManager.getById(1L)).thenReturn(existing);

    assertThrows(BizException.class, () -> service.setDefault(1L, 1L));
  }

  /**
   * 构造一份合法的地址创建参数。
   *
   * @param name 收件人
   * @param phone 手机号
   * @return 包含完整省市区和详细地址的创建参数
   */
  private AddressCreateDto buildCreateDto(String name, String phone) {
    AddressCreateDto dto = new AddressCreateDto();
    dto.setReceiver(name);
    dto.setPhone(phone);
    dto.setProvince("江苏省");
    dto.setCity("南京市");
    dto.setDistrict("鼓楼区");
    dto.setDetail("中山路100号");
    return dto;
  }

  /**
   * 构造一份合法的地址更新参数。
   *
   * @param name 收件人
   * @param phone 手机号
   * @return 包含完整省市区和详细地址的更新参数
   */
  private AddressUpdateDto buildUpdateDto(String name, String phone) {
    AddressUpdateDto dto = new AddressUpdateDto();
    dto.setReceiver(name);
    dto.setPhone(phone);
    dto.setProvince("江苏省");
    dto.setCity("南京市");
    dto.setDistrict("玄武区");
    dto.setDetail("珠江路200号");
    return dto;
  }

  /**
   * 构造指定归属关系的地址持久化对象。
   *
   * @param id 地址 ID
   * @param userId 地址所属用户 ID
   * @return 字段完整且 isDefault=0 的地址对象
   */
  private AddressPo buildOwnedAddress(Long id, Long userId) {
    AddressPo po = new AddressPo();
    po.setId(id);
    po.setUserId(userId);
    po.setReceiverName("张三");
    po.setReceiverPhone("13800000001");
    po.setProvince("江苏省");
    po.setCity("南京市");
    po.setDistrict("鼓楼区");
    po.setDetailAddress("中山路100号");
    po.setIsDefault(0);
    return po;
  }
}
