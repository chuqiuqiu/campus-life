package com.campus.item.client;

import com.campus.common.domain.R;
import com.campus.item.client.dto.SellerVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * user-service Feign 客户端：
 * 1. 发布闲置后跨服务增加积分（Seata 全局事务参与方调用）
 * 2. 查询卖家信息填充展示
 */
@FeignClient("user-service")
public interface UserClient {

    /**
     * 增加积分（跨服务调用，Seata 保证分布式一致性）
     */
    @PostMapping("/users/credit/add")
    R<Void> addCredit(@RequestParam("userId") Long userId, @RequestParam("point") Integer point);

    /**
     * 查询用户信息
     */
    @GetMapping("/users/{id}")
    R<SellerVO> queryUserById(@PathVariable("id") Long id);
}
