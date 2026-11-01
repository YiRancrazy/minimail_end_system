package com.yirancrazy.minimall.notify.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.entity.CommentPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证评价视图转换对图片对象键、空图片和缺失 MinioUtil 三种场景的处理结果.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class CommentVoTest {

    /**
     * 测试目标：验证评价视图会把逗号分隔的图片对象键转换为公开地址。
     *
     * 前置：images 为 a.jpg,b.jpg，MinioUtil 为两个对象键返回固定 URL。
     * 预期：视图 images 为两个公开 URL 的逗号拼接结果。
     */
    public void fromWithMinioUtilResolvesImages() {
        CommentPo po = new CommentPo();
        po.setImages("a.jpg,b.jpg");
        MinioUtil minioUtil = mock(MinioUtil.class);
        when(minioUtil.resolvePublicUrl("a.jpg")).thenReturn("http://minio/a.jpg?token");
        when(minioUtil.resolvePublicUrl("b.jpg")).thenReturn("http://minio/b.jpg?token");

        CommentVo vo = CommentVo.from(po, minioUtil);

        assertEquals("http://minio/a.jpg?token,http://minio/b.jpg?token", vo.getImages());
    }

    /**
     * 测试目标：验证原评价图片为空时视图仍保持 null。
     *
     * 前置：CommentPo.images 为 null，传入 MinioUtil Mock。
     * 预期：CommentVo.images 为 null。
     */
    public void fromResolvesNullImagesToNull() {
        CommentPo po = new CommentPo();
        po.setImages(null);

        CommentVo vo = CommentVo.from(po, mock(MinioUtil.class));

        assertNull(vo.getImages());
    }

    /**
     * 测试目标：验证没有 MinioUtil 时评价视图保留原始图片对象键。
     *
     * 前置：images 为 a.jpg,b.jpg，调用不传 MinioUtil 的重载。
     * 预期：视图 images 与原始值一致。
     */
    public void fromWithoutMinioUtilKeepsRawImages() {
        CommentPo po = new CommentPo();
        po.setImages("a.jpg,b.jpg");

        CommentVo vo = CommentVo.from(po);

        assertEquals("a.jpg,b.jpg", vo.getImages());
    }
}
