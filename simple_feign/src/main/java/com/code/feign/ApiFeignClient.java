package com.code.feign;

import com.code.config.MyFeignClientsConfiguration;
import com.code.domain.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "api", url = "http://127.0.0.1:7333", path = "/api", configuration = MyFeignClientsConfiguration.class
//        , fallback = ApiFeignClientFallback.class
//        , fallbackFactory = ApiFeignClientFallbackFactory.class
)
public interface ApiFeignClient {

    @GetMapping("/hello")
    String hello(@RequestParam("name") String name);


    @GetMapping("/timeout")
    String timeout();

    @PostMapping("/post")
    User postMethod(@RequestBody User user);
}
