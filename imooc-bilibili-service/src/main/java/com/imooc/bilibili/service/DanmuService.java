package com.imooc.bilibili.service;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.dao.DanmuDao;
import com.imooc.bilibili.domain.video.Danmu;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DanmuService {

    private static final String DANMU_KEY = "dm-video-";

    @Autowired
    private DanmuDao danmuDao;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 添加弹幕
     */
    public void addDanmu(Danmu danmu) {
        danmuDao.addDanmu(danmu);
    }

    /**
     * 异步添加弹幕
     */
    @Async
    public void asyncAddDanmu(Danmu danmu) {
        danmuDao.addDanmu(danmu);
    }

    /**
     * 添加弹幕到 redis
     */
    public void addDanmusToRedis(Danmu danmu) {
        // "dm-video-{videoId}" : List<Danmu>
        String key = DANMU_KEY + danmu.getVideoId();
        String value = redisTemplate.opsForValue().get(key);

        List<Danmu> list = new ArrayList<>();
        if (!StringUtil.isNullOrEmpty(value)) {
            list = JSONArray.parseArray(value, Danmu.class);
        }
        list.add(danmu);

        redisTemplate.opsForValue().set(key, JSONObject.toJSONString(list));
    }

    /**
     * <p>根据 videoId 和 startTime <= createTime <= endTime条件查询弹幕</p>
     * 查询策略是优先查 redis 中的弹幕数据, 如果没有的话查询数据库, 然后把查询的数据写入 redis 当中
     */
    public List<Danmu> getDanmus(Long videoId, String startTime, String endTime) throws Exception {
        // "dm-video-{videoId}" : List<Danmu>
        String key = DANMU_KEY + videoId;
        String value = redisTemplate.opsForValue().get(key);

        List<Danmu> list;
        if (!StringUtil.isNullOrEmpty(value)) {
            // redis 中有
            list = JSONArray.parseArray(value, Danmu.class);
            if (!StringUtil.isNullOrEmpty(startTime) && !StringUtil.isNullOrEmpty(endTime)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date startDate = sdf.parse(startTime);
                Date endDate = sdf.parse(endTime);

                List<Danmu> childList = new ArrayList<>(); // 符合 startTime <= createTime <= endTime 的弹幕
                for (Danmu danmu : list) {
                    Date createTime = danmu.getCreateTime();
                    if (createTime.after(startDate) && createTime.before(endDate)) {
                        childList.add(danmu);
                    }
                }
                list = childList;
            }
        } else {
            // redis 中没有
            Map<String, Object> params = new HashMap<>();
            params.put("videoId", videoId);
            params.put("startTime", startTime);
            params.put("endTime", endTime);

            list = danmuDao.getDanmus(params); // 从数据库中查询
            redisTemplate.opsForValue().set(key, JSONObject.toJSONString(list)); // 保存弹幕到 redis
        }

        return list;
    }

}
