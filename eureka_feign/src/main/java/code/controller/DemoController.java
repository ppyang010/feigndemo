package code.controller;

import code.domain.User;
import code.feign.ApiFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
