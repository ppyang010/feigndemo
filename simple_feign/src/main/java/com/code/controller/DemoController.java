package com.code.controller;

import cn.hutool.json.JSONUtil;
import com.code.domain.User;
import com.code.feign.ApiFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
public class DemoController {
    @Autowired
    private ApiFeignClient apiFeignClient;

    @GetMapping("/hello")
    public String hello(String name) {
        return apiFeignClient.hello(name);
    }

    @GetMapping("/timeout")
    public String timeout() {
        return apiFeignClient.timeout();
    }

    @GetMapping("/post")
    public User postMethod(){
        User user = new User();
        user.setUsername("ccy");
        user.setPhone("186186186");
        return apiFeignClient.postMethod(user);
    }
}
