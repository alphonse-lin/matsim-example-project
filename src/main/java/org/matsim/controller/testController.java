package org.matsim.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

//自动装配
@RestController
@RequestMapping("/hello")
public class testController {

    //接口： http://localhost:8080/hello
    @GetMapping("/hello")
    @ResponseBody
    public String hello(){
        //调用业务，接收前端参数
        return "hello, world";
    }
}
