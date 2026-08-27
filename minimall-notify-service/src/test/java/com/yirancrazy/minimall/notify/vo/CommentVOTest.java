package com.yirancrazy.minimall.notify.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.entity.CommentPO;

/**
 * CommentVO 单元测试，覆盖评论图片 objectKey 解析为预签名 URL 的逻辑。
 */
public class CommentVOTest {

    /**
     * 验证逗号分隔的多个图片 objectKey 逐个解析为可访问 URL 后以逗号重拼。
     */
    @Test
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
     * 验证 images 为 null 时原样返回 null。
     */
    @Test
    public void from_resolvesNullImagesToNull() {
        CommentPO po = new CommentPO();
        po.setImages(null);

        CommentVO vo = CommentVO.from(po, mock(MinioUtil.class));

        assertNull(vo.getImages());
    }

    /**
     * 验证单参版本（minioUtil 为 null）时 images 原样透传。
     */
    @Test
    public void from_withoutMinioUtil_keepsRawImages() {
        CommentPO po = new CommentPO();
        po.setImages("a.jpg,b.jpg");

        CommentVO vo = CommentVO.from(po);

        assertEquals("a.jpg,b.jpg", vo.getImages());
    }
}