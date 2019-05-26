package com.code.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "api", url = "http://127.0.0.1:7333", path = "/api"
//        , fallback = ApiFeignClientFallback.class
        , fallbackFactory = ApiFeignClientFallbackFactory.class
)
public interface ApiFeignClient {

    @GetMapping("/hello")
    String hello(@RequestParam("name") String name);


    @GetMapping("/timeout")
    String timeout();
}
