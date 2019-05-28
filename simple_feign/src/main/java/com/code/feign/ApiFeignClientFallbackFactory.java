//package com.code.feign;
//
//import feign.FeignException;
//import feign.hystrix.FallbackFactory;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class ApiFeignClientFallbackFactory implements FallbackFactory<ApiFeignClient> {
//
//    @Override
//    public ApiFeignClient create(Throwable cause) {
//        log.warn("ApiFeignClientFallbackFactory = {}", cause instanceof FeignException);
//        System.out.println();
//        return new ApiFeignClient() {
//
//            @Override
//            public String hello(String name) {
////                e.printStackTrace();
//                return "[hello] fallback method invoke";
//            }
//
//            @Override
//            public String timeout() {
////                e.printStackTrace();
//                return "[timeout] fallback method invoke";
//            }
//        };
//    }
//}
