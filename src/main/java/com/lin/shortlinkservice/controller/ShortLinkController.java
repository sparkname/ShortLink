package com.lin.shortlinkservice.controller;

import com.lin.shortlinkservice.common.Result;
import com.lin.shortlinkservice.dto.CreateShortLinkRequest;
import com.lin.shortlinkservice.dto.ShortLinkResponse;
import com.lin.shortlinkservice.dto.StatsResponse;
import com.lin.shortlinkservice.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 短链接控制器
 * RESTful API接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ShortLinkController {
    
    private final ShortLinkService shortLinkService;
    
    /**
     * 生成短链接
     * POST /api/short-link
     */
    @PostMapping("/api/short-link")
    public Result<ShortLinkResponse> createShortLink(@RequestBody CreateShortLinkRequest request) {
        log.info("生成短链接请求: {}", request.getOriginalUrl());
        
        if (request.getOriginalUrl() == null || request.getOriginalUrl().isEmpty()) {
            return Result.error("原始URL不能为空");
        }
        
        try {
            ShortLinkResponse response = shortLinkService.createShortLink(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("生成短链接失败", e);
            return Result.error("生成短链接失败: " + e.getMessage());
        }
    }
    
    /**
     * 短链接跳转 (核心功能!)
     * GET /{shortCode}
     * 
     * 面试重点: 为什么用302而不是301?
     * 答: 302临时重定向,每次都会经过服务器,可以统计访问量
     *     301永久重定向,浏览器会缓存,导致无法统计
     */
    @GetMapping("/{shortCode}")
    public void redirect(@PathVariable String shortCode,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        log.info("短链接访问: {}", shortCode);
        
        String originalUrl = shortLinkService.getOriginalUrl(shortCode, request);
        
        if (originalUrl == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "短链接不存在或已失效");
            return;
        }
        
        // 使用302临时重定向 (面试考点!)
        response.sendRedirect(originalUrl);
    }
    
    /**
     * 获取访问统计
     * GET /api/stats/{shortCode}
     */
    @GetMapping("/api/stats/{shortCode}")
    public Result<StatsResponse> getStats(@PathVariable String shortCode) {
        log.info("查询统计: {}", shortCode);
        
        StatsResponse stats = shortLinkService.getStats(shortCode);
        
        if (stats == null) {
            return Result.error(404, "短链接不存在");
        }
        
        return Result.success(stats);
    }
    
    /**
     * 健康检查
     * GET /health
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }
}
