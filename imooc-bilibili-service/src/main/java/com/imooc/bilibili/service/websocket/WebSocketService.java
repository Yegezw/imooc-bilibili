package com.imooc.bilibili.service.websocket;

import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.domain.constant.UserMomentsConstant;
import com.imooc.bilibili.domain.video.Danmu;
import com.imooc.bilibili.service.DanmuService;
import com.imooc.bilibili.service.util.RocketMQUtil;
import com.imooc.bilibili.service.util.TokenUtil;
import io.netty.util.internal.StringUtil;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component // WebSocketService 是多例模式
@ServerEndpoint("/imserver/{token}")
public class WebSocketService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass()); // slf4j 日志

    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0); // 线程安全

    public static final ConcurrentHashMap<String, WebSocketService> WEBSOCKET_MAP = new ConcurrentHashMap<>(); // 线程安全

    private Session session;

    private String sessionId;

    private Long userId;

    private static ApplicationContext APPLICATION_CONTEXT; // bean 注入, ApplicationContext 是 static 的

    /**
     * 由启动类设置静态属性 ApplicationContext, 方便 bean 注入
     */
    public static void setApplicationContext(ApplicationContext applicationContext) {
        WebSocketService.APPLICATION_CONTEXT = applicationContext;
    }

    /**
     * 连接成功方法
     */
    @OnOpen
    public void openConnection(Session session, @PathParam("token") String token) {
        try {
            this.userId = TokenUtil.verifyToken(token);
        } catch (Exception ignored) {
        }
        this.session = session;
        this.sessionId = session.getId();

        if (WEBSOCKET_MAP.containsKey(sessionId)) {
            WEBSOCKET_MAP.remove(sessionId);
            WEBSOCKET_MAP.put(sessionId, this);
        } else {
            WEBSOCKET_MAP.put(sessionId, this);
            ONLINE_COUNT.getAndIncrement();
        }

        logger.info("用户连接成功: " + sessionId + ", 当前在线人数为: " + ONLINE_COUNT.get());
        try {
            this.sendMessage("0");
        } catch (Exception e) {
            logger.error("连接异常");
        }
    }

    /**
     * 关闭连接方法
     */
    @OnClose
    public void closeConnection() {
        if (WEBSOCKET_MAP.containsKey(sessionId)) {
            WEBSOCKET_MAP.remove(sessionId);
            ONLINE_COUNT.getAndDecrement();
        }
        logger.info("用户退出: " + sessionId + ", 当前在线人数为: " + ONLINE_COUNT.get());
    }

    /**
     * 得到消息方法
     */
    @OnMessage
    public void onMessage(String message) {
        logger.info("用户信息: " + sessionId + ", 报文: " + message);

        // 发送弹幕到 mq, 并保存弹幕到数据库和 redis
        if (!StringUtil.isNullOrEmpty(message)) {
            try {
                // 群发消息, 向所有 webSocketService 发送消息
                for (Map.Entry<String, WebSocketService> entry : WEBSOCKET_MAP.entrySet()) {
                    WebSocketService webSocketService = entry.getValue();

                    // 向 rocketmq 中的 MomentsGroup - Topic-Moments 异步发送消息(弹幕)
                    DefaultMQProducer danmusProducer = (DefaultMQProducer) APPLICATION_CONTEXT.getBean("danmusProducer"); // 弹幕 mq 生产者

                    // 消息内容
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message", message); // 一样的消息
                    jsonObject.put("sessionId", webSocketService.getSessionId()); // 不一样的 sessionId
                    Message msg = new Message(UserMomentsConstant.TOPIC_DANMUS, jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));

                    RocketMQUtil.asyncSendMsg(danmusProducer, msg);
                }

                if (this.userId != null) {
                    // 保存弹幕到数据库
                    Danmu danmu = JSONObject.parseObject(message, Danmu.class);
                    danmu.setUserId(userId);
                    danmu.setCreateTime(new Date());
                    DanmuService danmuService = (DanmuService) APPLICATION_CONTEXT.getBean("danmuService");
                    danmuService.asyncAddDanmu(danmu);
                    // 保存弹幕到 redis
                    danmuService.addDanmusToRedis(danmu);
                }
            } catch (Exception e) {
                logger.error("弹幕接收出现问题");
                e.printStackTrace();
            }
        }
    }

    /**
     * 出现异常方法
     */
    @OnError
    public void onError(Throwable error) {
    }

    /**
     * 发送消息
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * <p>在线人数统计</p>
     * 定时任务, 或直接指定时间间隔, 例如 5 秒
     */
    @Scheduled(fixedRate = 5000)
    private void noticeOnlineCount() throws IOException {
        // 遍历所有的 webSocketService 并给它发送消息(在线人数)
        for (Map.Entry<String, WebSocketService> entry : WebSocketService.WEBSOCKET_MAP.entrySet()) {
            WebSocketService webSocketService = entry.getValue();

            if (webSocketService.session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("onlineCount", ONLINE_COUNT.get());
                jsonObject.put("msg", "当前在线人数为" + ONLINE_COUNT.get());
                webSocketService.sendMessage(jsonObject.toJSONString());
            }
        }
    }

    public Session getSession() {
        return session;
    }

    public String getSessionId() {
        return sessionId;
    }
}
