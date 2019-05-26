package com.code.config;

import feign.Retryer;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;

public class MyFeignClientsConfiguration extends FeignClientsConfiguration {
    @Override
    public Retryer feignRetryer() {
        return super.feignRetryer();
    }
}
