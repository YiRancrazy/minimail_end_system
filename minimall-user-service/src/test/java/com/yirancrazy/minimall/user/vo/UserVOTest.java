package com.yirancrazy.minimall.user.vo;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.user.entity.UserPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserVOTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class UserVOTest {


    /**
     * 执行 from_withMinioUtil_resolvesAvatar 方法。
     */
    public void from_withMinioUtil_resolvesAvatar() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setAvatar("a1b2c3.png");
        MinioUtil minioUtil = mock(MinioUtil.class);
        when(minioUtil.resolvePublicUrl("a1b2c3.png")).thenReturn("http://minio/mall-files/a1b2c3.png?token");

        UserVO vo = UserVO.from(po, minioUtil);

        assertEquals("http://minio/mall-files/a1b2c3.png?token", vo.getAvatar());
    }


    /**
     * 执行 from_mapsAllFields 方法。
     */
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
     * 执行 from_masksPhone 方法。
     */
    public void from_masksPhone() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setPhone("13812345678");

        UserVO vo = UserVO.from(po);

        assertEquals("138****5678", vo.getPhone());
    }


    /**
     * 执行 from_masksEmail 方法。
     */
    public void from_masksEmail() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setEmail("test@example.com");

        UserVO vo = UserVO.from(po);

        assertEquals("te***@example.com", vo.getEmail());
    }


    /**
     * 执行 from_nullPhone 方法。
     */
    public void from_nullPhone() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setPhone(null);

        UserVO vo = UserVO.from(po);

        assertNull(vo.getPhone());
    }


    /**
     * 执行 from_nullEmail 方法。
     */
    public void from_nullEmail() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setEmail(null);

        UserVO vo = UserVO.from(po);

        assertNull(vo.getEmail());
    }


    /**
     * 执行 from_emptyPhone 方法。
     */
    public void from_emptyPhone() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setPhone("");

        UserVO vo = UserVO.from(po);

        assertEquals("", vo.getPhone());
    }


    /**
     * 执行 from_shortEmail 方法。
     */
    public void from_shortEmail() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setEmail("ab@c.com");

        UserVO vo = UserVO.from(po);

        assertEquals("ab***@c.com", vo.getEmail());
    }


    /**
     * 执行 from_singleCharPrefixEmail_still_masked 方法。
     */
    public void from_singleCharPrefixEmail_still_masked() {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setEmail("a@x.com");

        UserVO vo = UserVO.from(po);

        assertEquals("a***@x.com", vo.getEmail());
    }
}
