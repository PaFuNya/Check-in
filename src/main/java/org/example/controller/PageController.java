package org.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 前端路由处理：
 * 所有非 /api/** 的请求都返回 index.html，
 * 由 Vue Router 在客户端处理路由。
 */
@Controller
public class PageController {

    @GetMapping(value = {
        "/",
        "/login",
        "/checkin",
        "/records",
        "/chat",
        "/profile"
    })
    public String spa() {
        // 返回 static/index.html（Spring Boot 静态资源）
        return "forward:/index.html";
    }
}
