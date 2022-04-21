package com.imooc.bilibili.service.util;

import com.github.tobato.fastdfs.domain.fdfs.FileInfo;
import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadCallback;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.imooc.bilibili.domain.exception.ConditionException;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@Component
public class FastDFSUtil {

    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    @Autowired
    private AppendFileStorageClient appendFileStorageClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String PATH_KEY = "path-key:";

    private static final String UPLOADED_SIZE_KEY = "uploaded-size-key:";

    private static final String UPLOADED_NO_KEY = "uploaded-no-key:";

    private static final String DEFAULT_GROUP = "group1";

    private static final int SLICE_SIZE = 1024 * 1024 * 2; // 2 MB

    @Value("${fdfs.http.storage-addr}")
    private String httpFdfsStorageAddr;

    /**
     * 获取文件后缀
     */
    public String getFileType(MultipartFile file) {
        if (file == null) {
            throw new ConditionException("非法文件！");
        }

        String fileName = file.getOriginalFilename();
        int index = fileName.lastIndexOf(".");
        return fileName.substring(index + 1);
    }

    /**
     * 上传一般文件, 返回文件存储路径
     */
    public String uploadCommonFile(MultipartFile file) throws Exception {
        Set<MetaData> metaDataSet = new HashSet<>();
        String fileType = this.getFileType(file);

        StorePath storePath = fastFileStorageClient.uploadFile(file.getInputStream(), file.getSize(), fileType, metaDataSet);
        return storePath.getPath();
    }

    /**
     * 上传一般文件, 返回文件存储路径
     */
    public String uploadCommonFile(File file, String fileType) throws Exception {
        Set<MetaData> metaDataSet = new HashSet<>();

        StorePath storePath = fastFileStorageClient.uploadFile(new FileInputStream(file), file.length(), fileType, metaDataSet);
        return storePath.getPath();
    }

    /**
     * 根据 filePath 删除文件
     */
    public void deleteFile(String filePath) {
        fastFileStorageClient.deleteFile(filePath);
    }

    /**
     * 上传可以断点续传的文件, 返回文件存储路径
     */
    private String uploadAppenderFile(MultipartFile file) throws Exception {
        String fileType = this.getFileType(file);

        StorePath storePath = appendFileStorageClient.uploadAppenderFile(DEFAULT_GROUP, file.getInputStream(), file.getSize(), fileType);
        return storePath.getPath();
    }

    /**
     * 修改续传文件的内容
     */
    private void modifyAppenderFile(MultipartFile file, String filePath, long fileOffset) throws Exception {
        appendFileStorageClient.modifyFile(DEFAULT_GROUP, filePath, file.getInputStream(), file.getSize(), fileOffset);
    }

    /**
     * 上传总分片为 totalSliceNo、当前分片为 sliceNo、当前分片对应的文件为 file 到 fastdfs, 返回文件存储路径
     */
    public String uploadFileBySlices(MultipartFile file, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws Exception {
        if (file == null || sliceNo == null || totalSliceNo == null) {
            throw new ConditionException("参数异常！");
        }

        // redis 中, fileMd5 是为了生成唯一 key
        String pathKey = PATH_KEY + fileMd5;                  // 文件存储路径 key
        String uploadedSizeKey = UPLOADED_SIZE_KEY + fileMd5; // 文件当前大小 key
        String uploadedNoKey = UPLOADED_NO_KEY + fileMd5;     // 文件当前分片 key

        String uploadedSizeStr = redisTemplate.opsForValue().get(uploadedSizeKey); // 文件当前大小
        Long uploadedSize = 0L;
        if (!StringUtil.isNullOrEmpty(uploadedSizeStr)) {
            uploadedSize = Long.valueOf(uploadedSizeStr);
        }

        if (sliceNo == 1) {   // 上传的是第一个分片
            String path = this.uploadAppenderFile(file);
            if (StringUtil.isNullOrEmpty(path)) {
                throw new ConditionException("上传失败！");
            }
            redisTemplate.opsForValue().set(pathKey, path);
            redisTemplate.opsForValue().set(uploadedNoKey, "1");
        } else {              // 上传的不是第一个分片
            String filePath = redisTemplate.opsForValue().get(pathKey);
            if (StringUtil.isNullOrEmpty(filePath)) {
                throw new ConditionException("上传失败！");
            }
            this.modifyAppenderFile(file, filePath, uploadedSize);
            redisTemplate.opsForValue().increment(uploadedNoKey);
        }
        // 修改历史上传分片文件大小(已上传的文件大小)
        uploadedSize += file.getSize();
        redisTemplate.opsForValue().set(uploadedSizeKey, String.valueOf(uploadedSize));

        // 如果所有分片全部上传完毕, 则清空 redis 里面相关的 key 和 value
        String uploadedNoStr = redisTemplate.opsForValue().get(uploadedNoKey);
        Integer uploadedNo = Integer.valueOf(uploadedNoStr);
        String resultPath = "";
        if (uploadedNo.equals(totalSliceNo)) {
            resultPath = redisTemplate.opsForValue().get(pathKey);
            List<String> keyList = Arrays.asList(uploadedNoKey, pathKey, uploadedSizeKey);
            redisTemplate.delete(keyList);
        }
        return resultPath;
    }

    /**
     * 将文件进行分片, 并将分片文件存储到 tmpfile 目录下
     */
    public void convertFileToSlices(MultipartFile multipartFile) throws Exception {
        String fileType = this.getFileType(multipartFile);

        // 生成临时文件, 将 MultipartFile 转为 File
        File file = this.multipartFileToFile(multipartFile);

        long fileLength = file.length(); // 文件大小, 单位：B
        int count = 1;                   // 当前分片
        // 将文件分片, 每片大小为 SLICE_SIZE(2 MB), i 代表偏移量
        for (int i = 0; i < fileLength; i += SLICE_SIZE) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(i);

            byte[] bytes = new byte[SLICE_SIZE];
            int len = randomAccessFile.read(bytes); // 将 2 MB 大小的文件内容读入到 bytes 中

            String path = "F:\\tmpfile\\imooc-bilibili\\" + count + "." + fileType;
            File slice = new File(path);
            FileOutputStream fos = new FileOutputStream(slice);
            fos.write(bytes, 0, len);

            fos.close();
            randomAccessFile.close();
            count++;
        }

        // 删除临时文件
        file.delete();
    }

    /**
     * 将 MultipartFile 转为 File 并返回
     */
    public File multipartFileToFile(MultipartFile multipartFile) throws Exception {
        String originalFileName = multipartFile.getOriginalFilename();
        String[] fileName = originalFileName.split("\\.");

        File file = File.createTempFile(fileName[0], "." + fileName[1]);
        multipartFile.transferTo(file);
        return file;
    }

    /**
     * 视频在线观看
     */
    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String path) throws Exception {
        FileInfo fileInfo = fastFileStorageClient.queryFileInfo(DEFAULT_GROUP, path);
        long totalFileSize = fileInfo.getFileSize(); // 文件大小
        String url = httpFdfsStorageAddr + path; // 请求路径

        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, Object> headers = new HashMap<>(); // 请求头
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            headers.put(header, request.getHeader(header));
        }

        String rangeStr = request.getHeader("Range");
        String[] range; // 获取请求头中的 range(Range: bytes=0-1048575)
        if (StringUtil.isNullOrEmpty(rangeStr)) {
            rangeStr = "bytes=0-" + (totalFileSize - 1);
        }
        range = rangeStr.split("bytes=|-");

        long begin = 0;
        if (range.length >= 2) {
            begin = Long.parseLong(range[1]);
        }
        long end = totalFileSize - 1;
        if (range.length >= 3) {
            end = Long.parseLong(range[2]);
        }
        long len = (end - begin) + 1;

        // (Content-Range: bytes 6029312-7177100/7177101)
        String contentRange = "bytes " + begin + "-" + end + "/" + totalFileSize;
        response.setHeader("Content-Range", contentRange);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Type", "video/mp4");
        response.setContentLength((int) len);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        HttpUtil.get(url, headers, response);
    }

    /**
     * 下载 fastdfs 上 url 位置的文件到 localPath
     */
    public void downLoadFile(String url, String localPath) {
        fastFileStorageClient.downloadFile(DEFAULT_GROUP, url,
                new DownloadCallback<String>() {
                    @Override
                    public String recv(InputStream ins) throws IOException {
                        File file = new File(localPath);
                        OutputStream os = new FileOutputStream(file);
                        int len;
                        byte[] buffer = new byte[1024];
                        while ((len = ins.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                        os.close();
                        ins.close();
                        return "success";
                    }
                });
    }

    public static void main(String[] args) {
        String[] range1 = "bytes=0-1048575".split("bytes=|-"); // [, 0, 1048575]
        String[] range2 = "bytes=0-".split("bytes=|-");        // [, 0]
    }
}
