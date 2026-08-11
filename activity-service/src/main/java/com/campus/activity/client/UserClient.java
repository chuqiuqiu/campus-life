package com.campus.activity.client;

import com.campus.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * user-service Feign 客户端：查询组织者信息
 */
@FeignClient("user-service")
public interface UserClient {

    /**
     * 查询用户信息（返回字段裁剪后的 UserVO）
     */
    @GetMapping("/users/{id}")
    R<Map<String, Object>> queryUserById(@PathVariable("id") Long id);
}
