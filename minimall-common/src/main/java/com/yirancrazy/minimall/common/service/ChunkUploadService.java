package com.yirancrazy.minimall.common.service;

import com.yirancrazy.minimall.common.constant.UploadCodeEnum;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.util.MinioUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 管理分片上传生命周期，包括参数校验、任务归属、临时分片落盘、配额检查、
 * 顺序合并及上传 MinIO。元数据保存在本地临时目录，任务超过 TTL 后可批量清理。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class ChunkUploadService {

  private static final String DEFAULT_ROOT = System.getProperty("java.io.tmpdir") + "/mm-chunk";

  private static final String OWNER_FILE = ".owner";

  private static final UploadLimits DEFAULT_LIMITS =
      new UploadLimits(
          1000,
          10L * 1024 * 1024,
          2L * 1024 * 1024 * 1024,
          10L * 1024 * 1024 * 1024,
          Duration.ofHours(24));

  private final MinioUtil minioUtil;
  private final Path root;
  private final UploadLimits limits;
  private final Clock clock;

  /**
   * 使用系统临时目录、默认上传限额和 UTC 时钟创建服务。
   *
   * @param minioUtil 合并完成后执行对象上传的工具
   */
  public ChunkUploadService(MinioUtil minioUtil) {
    this(minioUtil, Paths.get(DEFAULT_ROOT), DEFAULT_LIMITS, Clock.systemUTC());
  }

  /**
   * 创建可注入临时目录、限额和时钟的分片上传服务，便于测试和部署时覆盖默认值。
   *
   * @param minioUtil 合并完成后执行对象上传的工具
   * @param root 分片任务临时目录根路径
   * @param limits 分片数量、单分片大小、任务大小、用户配额和任务 TTL
   * @param clock 计算任务过期时间使用的时钟
   */
  public ChunkUploadService(MinioUtil minioUtil, Path root, UploadLimits limits, Clock clock) {
    this.minioUtil = minioUtil;
    this.root = root.toAbsolutePath().normalize();
    this.limits = limits;
    this.clock = clock;
  }

  /**
   * 保存单个分片并检查分片、任务和用户三级容量限制；当所有分片到齐时自动合并并上传对象存储。
   * 写入失败会删除本次新增分片，成功合并后会删除任务临时目录。
   *
   * @param userId 上传用户 ID，必须为正数
   * @param uploadId 8 到 64 位字母、数字或短横线组成的上传标识
   * @param chunkIndex 从 0 开始的当前分片序号
   * @param totalChunks 总分片数，必须不小于 1
   * @param contentType 合并上传时使用的内容类型
   * @param fileName 原始文件名，仅用于提取安全扩展名
   * @param in 当前分片输入流，由调用方负责关闭
   * @return 当前接收进度；完成时包含最终对象键
   * @throws BizException 参数非法、归属不符、超限、写入或合并上传失败时抛出
   */
  public ChunkResult saveChunk(
      Long userId,
      String uploadId,
      int chunkIndex,
      int totalChunks,
      String contentType,
      String fileName,
      InputStream in) {
    validateParams(uploadId, chunkIndex, totalChunks);
    if (userId == null || userId <= 0) {
      throw new BizException(UploadCodeEnum.CHUNK_PARAM_INVALID);
    }
    Path dir = taskDir(uploadId);
    Path chunkPath = dir.resolve("chunk-" + chunkIndex);
    try {
      Files.createDirectories(dir);
      ensureOwner(userId, dir);
      try (OutputStream out =
          Files.newOutputStream(
              chunkPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
        in.transferTo(out);
      }
      if (Files.size(chunkPath) > limits.maxChunkBytes()) {
        Files.deleteIfExists(chunkPath);
        throw new BizException(UploadCodeEnum.CHUNK_SIZE_LIMIT_EXCEEDED);
      }
      long taskBytes = storedBytes(dir);
      if (taskBytes > limits.maxTaskBytes()) {
        Files.deleteIfExists(chunkPath);
        throw new BizException(UploadCodeEnum.TASK_SIZE_LIMIT_EXCEEDED);
      }
      if (userStoredBytes(userId) > limits.maxUserBytes()) {
        Files.deleteIfExists(chunkPath);
        throw new BizException(UploadCodeEnum.USER_QUOTA_EXCEEDED);
      }
    } catch (BizException e) {
      throw e;
    } catch (IOException e) {
      throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
    }
    List<Integer> received = receivedChunks(userId, uploadId, totalChunks);
    if (received.size() == totalChunks) {
      String objectKey = mergeAndUpload(uploadId, totalChunks, contentType, fileName);
      return new ChunkResult(true, received.size(), objectKey);
    }
    return new ChunkResult(false, received.size(), null);
  }

  /**
   * 延迟打开分片输入流并复用主保存流程，确保流在保存结束后关闭。
   *
   * @param userId 上传用户 ID
   * @param uploadId 上传标识
   * @param chunkIndex 当前分片序号
   * @param totalChunks 总分片数
   * @param contentType 合并上传内容类型
   * @param fileName 原始文件名
   * @param streamSupplier 延迟创建分片输入流的供应器
   * @return 当前接收进度；完成时包含最终对象键
   * @throws BizException 打开流、参数校验或保存失败时抛出
   */
  public ChunkResult saveChunk(
      Long userId,
      String uploadId,
      int chunkIndex,
      int totalChunks,
      String contentType,
      String fileName,
      ChunkInputStreamSupplier streamSupplier) {
    try (InputStream in = streamSupplier.open()) {
      return saveChunk(userId, uploadId, chunkIndex, totalChunks, contentType, fileName, in);
    } catch (IOException e) {
      throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
    }
  }

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 延迟创建分片输入流的函数式接口，使调用方可以在分片校验完成后再打开底层资源。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  @FunctionalInterface
  public interface ChunkInputStreamSupplier {

    /**
     * 打开一个新的分片输入流，调用方负责关闭。
     *
     * @return 分片输入流
     * @throws IOException 打开底层资源失败时抛出
     */
    InputStream open() throws IOException;
  }

  /**
   * 校验任务归属后列出已落盘分片序号，缺失或非法的文件名会被忽略。
   *
   * @param userId 请求用户 ID，用于校验任务归属
   * @param uploadId 上传标识
   * @param totalChunks 总分片数，用于过滤越界序号
   * @return 已接收分片序号，按升序返回；任务不存在时返回空列表
   * @throws BizException 任务归属不符或读取目录失败时抛出
   */
  public List<Integer> receivedChunks(Long userId, String uploadId, int totalChunks) {
    Path dir = taskDir(uploadId);
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    try {
      /* 首次写入创建归属文件，后续请求必须与记录的用户一致。 */
      ensureOwner(userId, dir);
    } catch (IOException e) {
      throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
    }
    /* 只统计 chunk-数字 形式的文件，并保证返回序号升序。 */
    try (var stream = Files.list(dir)) {
      return stream
          .filter(p -> p.getFileName().toString().startsWith("chunk-"))
          .map(p -> p.getFileName().toString().substring("chunk-".length()))
          .filter(s -> s.matches("\\d+"))
          .map(Integer::parseInt)
          .filter(i -> i >= 0 && i < totalChunks)
          .sorted()
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
    }
  }

  /**
   * 递归删除指定上传任务的临时分片和归属文件，任务不存在时直接返回。
   *
   * @param uploadId 待删除的上传标识
   */
  public void deleteTask(String uploadId) {
    deleteDir(taskDir(uploadId));
  }

  /**
   * 删除最后修改时间早于任务 TTL 的上传目录，并返回成功清理的目录数。
   *
   * @return 本次清理的任务目录数量；根目录不存在时返回 0
   * @throws BizException 遍历或删除根目录失败时抛出
   */
  public int cleanupExpiredTasks() {
    if (!Files.isDirectory(root)) {
      return 0;
    }
    long expireBefore = clock.instant().minus(limits.taskTtl()).toEpochMilli();
    int cleaned = 0;
    /* 仅选择最后修改时间早于 TTL 的任务目录，活跃任务保持不变。 */
    try (var dirs = Files.list(root)) {
      List<Path> expired =
          dirs
              .filter(Files::isDirectory)
              .filter(
                  dir -> {
                    try {
                      return Files.getLastModifiedTime(dir).toMillis() < expireBefore;
                    } catch (IOException e) {
                      return false;
                    }
                  })
              .collect(Collectors.toList());
      for (Path dir : expired) {
        deleteDir(dir);
        cleaned++;
      }
      return cleaned;
    } catch (IOException e) {
      throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
    }
  }

  /**
   * 校验上传标识格式、分片序号边界和总分片数上限，防止路径穿越和资源滥用。
   *
   * @param uploadId 上传标识，只允许 8 到 64 位字母、数字和短横线
   * @param chunkIndex 当前分片序号
   * @param totalChunks 总分片数
   * @throws BizException 参数非法或分片总数超限时抛出
   */
  private void validateParams(String uploadId, int chunkIndex, int totalChunks) {
    // uploadId 限定为字母数字与短横线，杜绝路径分隔符/相对路径穿越字符，同时要求 8~64 位
    if (totalChunks < 1
        || chunkIndex < 0
        || chunkIndex >= totalChunks
        || uploadId == null
        || !uploadId.matches("[a-zA-Z0-9-]{8,64}")) {
      throw new BizException(UploadCodeEnum.CHUNK_PARAM_INVALID);
    }
    if (totalChunks > limits.maxTotalChunks()) {
      throw new BizException(UploadCodeEnum.CHUNK_COUNT_LIMIT_EXCEEDED);
    }
  }

  /**
   * 将上传标识解析为根目录下的唯一任务目录，并再次校验规范化后的路径未越界。
   *
   * @param uploadId 已通过格式校验的上传标识
   * @return 规范化后的任务目录
   * @throws BizException 解析结果越出临时根目录时抛出
   */
  private Path taskDir(String uploadId) {
    Path dir = root.resolve(uploadId).normalize();
    // 双保险：即使字符集校验被绕过，normalize 后越出 ROOT 的路径也直接拒绝
    if (!dir.startsWith(root) || dir.getNameCount() != root.getNameCount() + 1) {
      throw new BizException(UploadCodeEnum.CHUNK_PARAM_INVALID);
    }
    return dir;
  }

  /**
   * 读取或创建任务归属文件，确保上传标识只能由首次创建者继续写入。
   *
   * @param userId 当前请求用户 ID
   * @param dir 任务目录
   * @throws IOException 归属文件读写失败时抛出
   * @throws BizException 任务已归属于其他用户时抛出
   */
  private void ensureOwner(Long userId, Path dir) throws IOException {
    Path ownerFile = dir.resolve(OWNER_FILE);
    try {
      /* CREATE_NEW 保证归属只在任务首次创建时写入，避免并发覆盖。 */
      Files.writeString(
          ownerFile,
          String.valueOf(userId),
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE);
    } catch (FileAlreadyExistsException e) {
      String owner = Files.readString(ownerFile).trim();
      if (!String.valueOf(userId).equals(owner)) {
        throw new BizException(UploadCodeEnum.UPLOAD_FORBIDDEN);
      }
    }
  }

  /**
   * 汇总任务目录中所有 chunk-N 文件的大小，无法读取的单个文件按 0 字节处理。
   *
   * @param dir 任务目录
   * @return 当前任务分片总字节数
   * @throws IOException 遍历任务目录失败时抛出
   */
  private long storedBytes(Path dir) throws IOException {
    /* 单个分片不可读时按 0 字节处理，避免阻塞其他分片的配额统计。 */
    try (var files = Files.list(dir)) {
      return files
          .filter(path -> path.getFileName().toString().startsWith("chunk-"))
          .mapToLong(
              path -> {
                try {
                  return Files.size(path);
                } catch (IOException e) {
                  return 0L;
                }
              })
          .sum();
    }
  }

  /**
   * 遍历根目录下归属于指定用户的任务，并汇总其全部分片大小。
   *
   * @param userId 目标用户 ID
   * @return 用户当前占用的临时分片总字节数；根目录不存在时返回 0
   * @throws IOException 读取任务目录或归属文件失败时抛出
   */
  private long userStoredBytes(Long userId) throws IOException {
    if (!Files.isDirectory(root)) {
      return 0L;
    }
    long total = 0L;
    /* 任务归属文件缺失或无法读取时跳过该目录，不计入该用户的配额。 */
    try (var dirs = Files.list(root)) {
      for (Path dir : dirs.filter(Files::isDirectory).collect(Collectors.toList())) {
        Path ownerFile = dir.resolve(OWNER_FILE);
        if (!Files.isRegularFile(ownerFile)
            || !String.valueOf(userId).equals(Files.readString(ownerFile).trim())) {
          continue;
        }
        total += storedBytes(dir);
      }
    }
    return total;
  }

  /**
   * 按分片序号顺序合并临时文件并上传为对象键 {@code uploadId + 安全扩展名}，
   * 无论上传成功与否都会清理任务临时目录。
   *
   * @param uploadId 上传标识，同时作为对象键主体
   * @param totalChunks 总分片数
   * @param contentType 上传内容类型
   * @param fileName 原始文件名，用于提取安全扩展名
   * @return 对象存储中的最终对象键
   * @throws BizException 合并文件或上传对象失败时抛出
   */
  private String mergeAndUpload(
      String uploadId, int totalChunks, String contentType, String fileName) {
    Path dir = taskDir(uploadId);
    Path merged = dir.resolve("merged.bin");
    try {
      // 按序号拼接分片到合并文件，避免一次性持有多个流
      try (OutputStream out =
          Files.newOutputStream(
              merged, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
        for (int i = 0; i < totalChunks; i++) {
          Path chunk = dir.resolve("chunk-" + i);
          try (InputStream in = Files.newInputStream(chunk)) {
            in.transferTo(out);
          }
        }
      }
      return minioUtil.upload(merged, uploadId + extensionOf(fileName), contentType);
    } catch (IOException e) {
      throw new BizException(UploadCodeEnum.CHUNK_MERGE_FAIL);
    } finally {
      /* 无论上传结果如何，都清理分片，防止敏感临时文件长期滞留。 */
      deleteDir(dir);
    }
  }

  /**
   * 提取最多 10 位字母或数字的小写扩展名；无扩展名或格式不安全时返回空字符串。
   *
   * @param fileName 原始文件名，可为 null
   * @return 带点号的扩展名或空字符串
   */
  private static String extensionOf(String fileName) {
    if (fileName == null) {
      return "";
    }
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) {
      return "";
    }
    String ext = fileName.substring(dot + 1).toLowerCase();
    return ext.matches("[a-z0-9]{1,10}") ? "." + ext : "";
  }

  /**
   * 递归删除目录内容，按逆序处理文件和子目录；清理失败时忽略异常，留待后续过期清理重试。
   *
   * @param dir 待删除目录，可为 null
   */
  private static void deleteDir(Path dir) {
    if (dir == null || !Files.isDirectory(dir)) {
      return;
    }
    /* 逆序删除可先清理子项再删除目录；失败时交给下一次过期清理重试。 */
    try (var stream = Files.walk(dir)) {
      stream
          .sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException ignored) {
                  // ignore cleanup failure and retry during the next cleanup
                }
              });
    } catch (IOException ignored) {
      // ignore directory cleanup failure
    }
  }

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 单个分片保存结果，标识任务是否完成、已接收分片数以及完成后的对象键。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  public record ChunkResult(boolean done, int received, String objectKey) {}

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 分片上传资源限制，包含分片数量、单分片大小、任务总大小、用户总配额和任务 TTL。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  public record UploadLimits(
      int maxTotalChunks,
      long maxChunkBytes,
      long maxTaskBytes,
      long maxUserBytes,
      Duration taskTtl) {}
}
