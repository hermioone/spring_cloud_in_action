package com.imooc.order.controller;

import com.imooc.order.client.ProductClient;
import com.imooc.order.dataObject.ProductInfo;
import com.imooc.order.dto.CartDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@RestController
public class ClientController {
    private final static Logger LOGGER = LogManager.getLogger(ClientController.class);

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    /*@Autowired
    private RestTemplate restTemplate;*/

    @Autowired
    private ProductClient productClient;

    @GetMapping("/getProductMsg")
    public String getProductMsg() {

        //使用RestTemplate的三种方式
        //1.第一种方式：直接使用RestTemplate
        //缺点：url写死在代码里; 服务提供方可能是个集群
        /*RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject("http://localhost:8080/msg", String.class);
        LOGGER.info("response = {}", response);
        return response;*/

        //2.第二种方式：利用LoadBalancerClient通过应用名获取url，然后再使用RestTemplate
        ServiceInstance serviceInstance = loadBalancerClient.choose("product");       //参数是注册中心显示的application的名字
        String url = String.format("http://%s:%s", serviceInstance.getHost(), serviceInstance.getPort() + "/msg");
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        LOGGER.info("response = {} from {}", response, url);
        return response;

        //3.第三种方式：(在config中的RestTemplateConfig中自定义RestTemplate的bean，利用@LoadBalanced注解，可在restTemplate中使用应用名字)
        /*String response = restTemplate.getForObject("http://PRODUCT/msg", String.class);
        LOGGER.info("response = {}", response);
        return response;*/


        //使用Feign的方式
        /*String response = productClient.productMsg();
        LOGGER.info("response = {}", response);
        return response;*/
    }


    @GetMapping("/getProductList")
    public String getProductList() {
       List<ProductInfo> productInfoList =  productClient.listForOrder(Arrays.asList("164103465734242707"));
       LOGGER.info("response = {}", productInfoList);
       return "okay";
    }

    @GetMapping("/productDecreaseStock")
    public String productDecreaseStock() {
        productClient.decreaseStock(Arrays.asList(new CartDTO("157875196366160022", 3)));
        return "okay";
    }
}
