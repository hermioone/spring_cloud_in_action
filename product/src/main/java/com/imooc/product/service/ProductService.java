package com.imooc.product.service;

import com.imooc.product.dataObject.ProductInfo;

import java.util.List;

/**
 * 2017-12-09 21:57
 */
public interface ProductService {

    /**
     * 查询所有在架商品列表
     */
    List<ProductInfo> findUpAll();

}
