package com.yirancrazy.minimall.notify.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.entity.CommentPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentVOTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class CommentVOTest {


    /**
     * 执行 from_withMinioUtil_resolvesImages 方法。
     */
    public void from_withMinioUtil_resolvesImages() {
        CommentPO po = new CommentPO();
        po.setImages("a.jpg,b.jpg");
        MinioUtil minioUtil = mock(MinioUtil.class);
        when(minioUtil.resolvePublicUrl("a.jpg")).thenReturn("http://minio/a.jpg?token");
        when(minioUtil.resolvePublicUrl("b.jpg")).thenReturn("http://minio/b.jpg?token");

        CommentVO vo = CommentVO.from(po, minioUtil);

        assertEquals("http://minio/a.jpg?token,http://minio/b.jpg?token", vo.getImages());
    }


    /**
     * 执行 from_resolvesNullImagesToNull 方法。
     */
    public void from_resolvesNullImagesToNull() {
        CommentPO po = new CommentPO();
        po.setImages(null);

        CommentVO vo = CommentVO.from(po, mock(MinioUtil.class));

        assertNull(vo.getImages());
    }


    /**
     * 执行 from_withoutMinioUtil_keepsRawImages 方法。
     */
    public void from_withoutMinioUtil_keepsRawImages() {
        CommentPO po = new CommentPO();
        po.setImages("a.jpg,b.jpg");

        CommentVO vo = CommentVO.from(po);

        assertEquals("a.jpg,b.jpg", vo.getImages());
    }
}