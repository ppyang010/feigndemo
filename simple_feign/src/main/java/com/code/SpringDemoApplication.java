package com.code;

import com.code.config.MyFeignClientsConfiguration;
import org.springframework.boot.SpringApplication;
        import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author ccy
 */
@SpringBootApplication
@EnableFeignClients(defaultConfiguration={MyFeignClientsConfiguration.class})
public class SpringDemoApplication {

    public static void main(String[] args) {
        //org.springframework.cloud.openfeign.FeignClient
//        System.out.println(FeignClient.class.getCanonicalName());
        SpringApplication.run(SpringDemoApplication.class, args);
    }
}
