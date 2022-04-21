package com.imooc;

import com.imooc.bilibili.service.websocket.WebSocketService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement // 开启事务注解
@EnableAsync // 开启异步支持
@EnableScheduling // 开启定时任务注解支持
@EnableAspectJAutoProxy(proxyTargetClass = true) // 切面自动代理, AOP 自动生成代理对象
public class ImoocBilibiliApp {

    public static void main(String[] args) {
        ApplicationContext app = SpringApplication.run(ImoocBilibiliApp.class, args);
        WebSocketService.setApplicationContext(app);
    }
}
