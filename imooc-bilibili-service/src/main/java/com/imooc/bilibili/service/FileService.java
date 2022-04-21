package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.FileDao;
import com.imooc.bilibili.domain.video.File;
import com.imooc.bilibili.service.util.FastDFSUtil;
import com.imooc.bilibili.service.util.MD5Util;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@Service
public class FileService {

    @Autowired
    private FileDao fileDao;

    @Autowired
    private FastDFSUtil fastDFSUtil;

    /**
     * 上传总文件加密名为 fileMd5, 总分片为 totalSliceNo、当前分片为 sliceNo、当前分片对应的文件为 slice 到 fastdfs
     * <p>相同的文件不会再次上传, 秒传</p>
     * <p>返回文件存储路径</p>
     */
    public String uploadFileBySlices(MultipartFile slice,
                                     String fileMD5,
                                     Integer sliceNo,
                                     Integer totalSliceNo) throws Exception {
        File dbFileMD5 = fileDao.getFileByMD5(fileMD5);
        if (dbFileMD5 != null) {
            return dbFileMD5.getUrl(); // 上传的文件已经在 fastdfs 中了, 直接返回 url
        }

        String url = fastDFSUtil.uploadFileBySlices(slice, fileMD5, sliceNo, totalSliceNo);

        if (!StringUtil.isNullOrEmpty(url)) {
            dbFileMD5 = new File();
            dbFileMD5.setCreateTime(new Date());
            dbFileMD5.setMd5(fileMD5);
            dbFileMD5.setUrl(url);
            dbFileMD5.setType(fastDFSUtil.getFileType(slice));
            fileDao.addFile(dbFileMD5); // 文件所有分片全部上传完成后, 这里才会执行
        }

        return url;
    }

    /**
     * 获取文件 md5 加密后的字符串
     */
    public String getFileMD5(MultipartFile file) throws Exception {
        return MD5Util.getFileMD5(file);
    }

    /**
     * 根据文件 md5 加密后的字符串, 查询 fastdfs 已有文件标识
     */
    public File getFileByMd5(String fileMd5) {
        return fileDao.getFileByMD5(fileMd5);
    }
}
