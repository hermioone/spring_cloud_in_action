package com.imooc.order.client;

import com.imooc.order.dataObject.ProductInfo;
import com.imooc.order.dto.CartDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product")          //表示要访问哪个应用的接口
public interface ProductClient {

    @GetMapping("/msg")                 //这个要和producer中的一样
    String productMsg();                //方法名可以不一样

    @PostMapping("/product/listForOrder")
    List<ProductInfo> listForOrder(@RequestBody List<String> productIdList);

    @PostMapping("/product/decreaseStock")
    void decreaseStock(@RequestBody List<CartDTO> decreaseStockInputList);
}
