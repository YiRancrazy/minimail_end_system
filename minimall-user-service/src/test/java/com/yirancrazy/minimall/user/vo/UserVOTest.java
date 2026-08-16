package com.yirancrazy.minimall.user.vo;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.user.entity.UserPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserVO.from() 单元测试，验证字段映射与敏感数据脱敏逻辑
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
public class UserVOTest {

    /**
     * 验证 from 将所有非敏感字段正确映射。
     */
    @Test
    public void from_mapsAllFields() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setUsername("alice");
        po.setNickname("Alice");
        po.setPhone("13812345678");
        po.setEmail("test@example.com");
        po.setAvatar("https://example.com/avatar.png");
        po.setGender(1);
        po.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));

        UserVO vo = UserVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals("alice", vo.getUsername());
        assertEquals("Alice", vo.getNickname());
        assertEquals("138****5678", vo.getPhone());
        assertEquals("te***@example.com", vo.getEmail());
        assertEquals("https://example.com/avatar.png", vo.getAvatar());
        assertEquals(1, vo.getGender());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), vo.getCreateTime());
    }

    /**
     * 验证 from 对手机号进行脱敏：前3 + **** + 后4。
     */
    @Test
    public void from_masksPhone() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setPhone("13812345678");

        UserVO vo = UserVO.from(po);

        assertEquals("138****5678", vo.getPhone());
    }

    /**
     * 验证 from 对邮箱进行脱敏：前2 + *** + @domain。
     */
    @Test
    public void from_masksEmail() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setEmail("test@example.com");

        UserVO vo = UserVO.from(po);

        assertEquals("te***@example.com", vo.getEmail());
    }

    /**
     * 验证 from 在 phone 为 null 时不抛 NPE。
     */
    @Test
    public void from_nullPhone() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setPhone(null);

        UserVO vo = UserVO.from(po);

        assertNull(vo.getPhone());
    }

    /**
     * 验证 from 在 email 为 null 时不抛 NPE。
     */
    @Test
    public void from_nullEmail() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setEmail(null);

        UserVO vo = UserVO.from(po);

        assertNull(vo.getEmail());
    }

    /**
     * 验证 from 在 phone 为空字符串时正确处理。
     */
    @Test
    public void from_emptyPhone() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setPhone("");

        UserVO vo = UserVO.from(po);

        assertEquals("", vo.getPhone());
    }

    /**
     * 验证 from 对短前缀邮箱（2字符）仍执行脱敏，不原样泄漏。
     */
    @Test
    public void from_shortEmail() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setEmail("ab@c.com");

        UserVO vo = UserVO.from(po);

        assertEquals("ab***@c.com", vo.getEmail());
    }

    /**
     * 验证 from 对单字符前缀邮箱至少保留首字符并遮蔽其余部分。
     */
    @Test
    public void from_singleCharPrefixEmail_still_masked() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setEmail("a@x.com");

        UserVO vo = UserVO.from(po);

        assertEquals("a***@x.com", vo.getEmail());
    }
}
