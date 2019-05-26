package com.code.controller;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/hello")
    public String hello(String name) {
        return "hello," + name;
    }

    @PostMapping("/hello")
    public String helloPost(String str, HttpServletRequest request) {
        System.out.println(str);
        System.out.println(request.getHeader("Content-Type"));
        System.out.println(request.getParameter("name"));
        return "post hello,"+str;
    }



    @GetMapping("/timeout")
    public String timeout() throws InterruptedException {
        System.out.println("timeout method invoke");
        TimeUnit.SECONDS.sleep(10);
        return "timeout method invoke";
    }
}
