package code.feign;

import code.domain.User;
import org.springframework.stereotype.Component;

@Component
public class ApiFeignClientFallback implements ApiFeignClient {


    @Override
    public String hello(String name) {
        return "[hello] fallback method invoke";
    }

    @Override
    public String timeout() {
        return "[timeout] fallback method invoke";

    }

    @Override
    public User postMethod(User user) {
        return null;
    }
}
