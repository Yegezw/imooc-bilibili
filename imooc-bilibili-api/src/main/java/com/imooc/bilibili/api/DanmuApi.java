package com.imooc.bilibili.api;

import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.video.Danmu;
import com.imooc.bilibili.service.DanmuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DanmuApi {

    @Autowired
    private DanmuService danmuService;

    @Autowired
    private UserSupport userSupport;

    /**
     * 查询弹幕
     */
    @GetMapping("/danmus")
    public JsonResponse<List<Danmu>> getDanmus(@RequestParam Long videoId, String startTime, String endTime) throws Exception {
        List<Danmu> list;
        try {
            userSupport.getCurrentUserId(); // 判断当前是游客模式还是用户登录模式
            list = danmuService.getDanmus(videoId, startTime, endTime); // 若是用户登录模式, 则允许用户进行时间段筛选
        } catch (Exception ignored) {
            list = danmuService.getDanmus(videoId, null, null); // 若为游客模式, 则不允许用户进行时间段筛选
        }
        return new JsonResponse<>(list);
    }

}