package code.feign;

import code.domain.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "eureka-feign", path = "/api")
public interface ApiFeignClient {

    @GetMapping("/hello")
    String hello(@RequestParam("name") String name);


    @GetMapping("/timeout")
    String timeout();

    @PostMapping("/post")
    User postMethod(@RequestBody User user);
}
