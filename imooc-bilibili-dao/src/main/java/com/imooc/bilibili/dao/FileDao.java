package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.video.File;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileDao {

    /**
     * 添加 fastdfs 已有文件标识
     */
    Integer addFile(File file);

    /**
     * 根据文件 md5 加密后的字符串, 查询 fastdfs 已有文件标识
     */
    File getFileByMD5(String md5);
}
