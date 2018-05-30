### 1. Spring Cloud和Spring Boot版本的对应关系
到https://projects.spring.io/spring-cloud/中，向下拉，可以看到版本对应关系图。

分布式系统中服务发现是最基础的组件

微服务的特点：异构
*   不同语言
*   不同类型的数据库

SpringCloud的服务调用方式：**REST**

业务形态不适合微服务：
*   系统中包含很多强事务场景
*   业务相对稳定，迭代周期长
*   访问压力不大，可用性要求不高

# Chapter 5

## 5.2

Spring Cloud中服务间两种restful调用方式
* RestTemplate

  RestTemplate总共有三种方法：

  1. 第一种

     ```java
     @GetMapping("/getProductMsg")
     public String getProductMsg() {    
         
     	//1.直接使用RestTemplate
         //缺点：url写死在代码里; 服务提供方可能是个集群
         RestTemplate restTemplate = new RestTemplate();
         String response = restTemplate.getForObject("http://localhost:8080/msg", String.class);
     
         LOGGER.info("response = {}", response);
         
     }
     ```

  2. 第二种

     ```java
     @Autowired
     private LoadBalancerClient loadBalancerClient;
     
     
     @GetMapping("/getProductMsg")
     public String getProductMsg() {
     
         //2.利用LoadBalancerClient通过应用名获取url，然后再使用RestTemplate
         ServiceInstance serviceInstance = loadBalancerClient.choose("PRODUCT");       
         //参数是注册中心显示的application的名字
     
         String.format("http://%s:%s", serviceInstance.getHost(), 			  serviceInstance.getPort() + "/msg");
     
         RestTemplate restTemplate = new RestTemplate();
         String response = restTemplate.getForObject("http://localhost:8080/msg", String.class);
     
         LOGGER.info("response = {}", response);
         return response;
     }
     ```

  3. 第三种

     ```java
     @Configuration
     public class RestTemplateConfig {
     
         @Bean
         @LoadBalanced
         public RestTemplate restTemplate() {
             return new RestTemplate();
         }
     }
     ```

     ```java
     @Autowired
     private RestTemplate restTemplate;*/
     
     @GetMapping("/getProductMsg")
     public String getProductMsg() {
     
         //3.(在config中的RestTemplateConfig中自定义RestTemplate的bean，
         //利用@LoadBalanced注解，可在restTemplate中使用应用名字)
      	String response = restTemplate.getForObject("http://PRODUCT/msg", String.class);
         
         LOGGER.info("response = {}", response);
         return response;
     }
     ```

*   Feign

## 5.3

两种服务发现方式：
*   服务端发现
*   客户端发现

>Eureka属于客户端发现的方式，它的负载均衡属于软负载，客户端会向服务器(例如eureka server)拉取已注册的可用服务信息，然后根据负载均衡策略，直接命中哪台服务器发送请求。
客户端负载均衡组件是Ribbon。

RestTemplate，Feign和Zuul都使用到Ribbon

Ribbon实现负载均衡依据三点：
*   服务发现
  
    根据服务的名字，把该服务下所有的实例都找出来
   
*   服务选择规则

    依据规则策略，如何从多个服务中选择一个有效的服务
    
*   服务监听

    监测失效的服务，做到高效剔除

## 5.4

可以通过在application.yml中添加以下来改变负载均衡策略：

```yaml
PRODUCT:
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
```

value值为具体的轮询规则的全路径名。

## 5.5

使用Feign进行应用间通信

添加依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-feign</artifactId>
    <version>1.4.4.RELEASE</version>
</dependency>
```

1. 在主应用上添加 ```@EnableFeignClients```

   ```java
   @SpringBootApplication
   @EnableDiscoveryClient
   @EnableFeignClients
   public class OrderApplication {
   
   	public static void main(String[] args) {
   		SpringApplication.run(OrderApplication.class, args);
   	}
   }
   ```

2. 定义一个Feign接口

   ```java
   @FeignClient(name = "product")          
   //表示要访问哪个应用的接口，name值和配置文件中定义的一样(spring.application.name=product)
   public interface ProductClient {
       @GetMapping("/msg")                 //这个value值要和producer中的一样
       String productMsg();                //方法名可以不一样
   }
   ```

3. 调用

   ```java
   @RestController
   public class ClientController {
       private final static Logger LOGGER = LogManager.getLogger(ClientController.class);
   
       @Autowired
       private ProductClient productClient;
   
       @GetMapping("/getProductMsg")
       public String getProductMsg() {
   
           //使用Feign的方式
           String response = productClient.productMsg();
           LOGGER.info("response = {}", response);
           return response;
       }
   }
   ```

Feign内部也使用Ribbon做负载均衡。

Feign是伪RPC。

# Chapter 6

## 6.1统一配置中心

![](http://static.zybuluo.com/vermouth9/v5vixj6jf9717ybk87uuhn3u/image.png)

## 6.2 Config Server

1. 创建项目，引入以下两个依赖：

   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-config-server</artifactId>
   </dependency>
   
   <!-- 因为也需要把这个注册到注册中心中，所以需要这个依赖，还需要在application.yml中写上配置（eureka server地址等） -->
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
   </dependency>
   ```

2. 在启动项上添加注解：

   ```java
   @SpringBootApplication
   @EnableDiscoveryClient
   @EnableConfigServer
   public class ConfigApplication {
   
   	public static void main(String[] args) {
   		SpringApplication.run(ConfigApplication.class, args);
   	}
   }
   ```

3. 在远端git仓库中创建配置文件

   ![](http://static.zybuluo.com/vermouth9/em40nlvxd27dnjjnea64bpcr/image.png)

   ```yaml
   spring:
     application:
       name: order
     datasource:
         driver-class-name: com.mysql.jdbc.Driver
         username: root
         password: 123456
         url: jdbc:mysql://127.0.0.1:3306/SpringCloud_Sell?characterEncoding=utf-8&useSSL=false
     jpa:
       show-sql: true
   eureka:
     client:
       service-url:
         defaultZone: http://localhost:8761/eureka/
   PRODUCT:
     ribbon:
       NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
   ```

4. 在application.yml添加如下配置

   ```yaml
   spring:
     cloud:
       config:
         server:
           git:
             uri: https://gitee.com/vermouth9/config-repo
             username: 782550237@qq.com
             password: masn3712238
             basedir: E:\workspace\idea-workspace\imooc\spring cloud微服务实战\spring_cloud_in_action_multiple_module\basedir   # 本地git存放地址
   ```

5. 可以通过浏览器访问以下地址：

   ```html
   http://localhost:8082/order-a.json
   
   http://localhost:8082/order-a.yml
   
   http://localhost:8082/order-a.properties
   ```

   以上url需满足以下条件：必须有-和后面的profile名

   ```
   /{name}-{profile}.xx
   /{label}/{name}-{profiles}.yml}
   ```

   name：项目名

   profile：环境名

   label：分支（branch）

   如果git仓库中有name.xx和name-yy.xx，name访问name-yy.xx时也会加载name.xx

## 6.3 Config Client

1. 引入依赖

   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-config-client</artifactId>
       <version>Finchley.RC1</version>
   </dependency>
   ```

2. 删掉application.yml，添加bootstrap.yml，并添加以下配置：

   ```yaml
   # bootstrap.yml是启动引导的配置文件，最先加载这个，必须保证这个在加载在config server中那些配置之前加载
   spring:
     application:
       name: order
     cloud:
       config:
         discovery:
           enabled: true
           service-id: config
         profile: dev
   eureka:             # cloud.config/discovery是要到配置中心中去找，所以需要把配置中心的地址配置在这里
     client:
       service-url:
         defaultZone: http://localhost:8761/eureka/
   ```

   ## 6.5 Spring Cloud Bus自动更新配置

   如何在修改配置后，Config Server会通知Config Client

   1. 在Config Server和Config Client中都添加以下依赖：

      ```xml
      <dependency>
          <groupId>org.springframework.cloud</groupId>
          <artifactId>spring-cloud-starter-bus-amqp</artifactId>
      </dependency>
      ```

   2. 在Config Server和Config Client中配置RabbitMq的信息

      ```yaml
      spring:
        rabbitmq:
          host: 192.168.1.103
          port: 5672
          username: sherry
          password: 123456
      ```

   3. 在Config Server中配置映射暴露，也就是把```/actuator/bus-refresh```暴露出去

      ```yaml
      management:
        endpoints:
          web:
            exposure:
              include: "*"
      ```

      

   4. 当git远程仓库中的配置发生改变的时候，发送post请求到```http://ip:port/actuator/bus-refresh```。

   5. 在要使用这些配置项的代码上添加注解：```@RefreshScope```

      ```java
      @RestController
      @RefreshScope
      public class EnvController {
      
          @Value("${env}")
          public String env;
      
          @GetMapping("/env")
          public String env() {
              return env;
          }
      }
      ```

# Chapter 7

## 7.10 异步扣库存分析

1. 库存在Redis中保存
2. 收到请求Redis判断是否库存充足，减掉Redis中库存
3. 订单服务创建订单写入数据库，并发送消息

异步的问题：CAP

1. 数据一致性：Consistency
2. 服务可用性：Availability
3. 服务对分区故障的容错性：Partition tolerance

Dubbo + Zookeeper：C、P

Eureka：A、P

# Chapter 8

## 8.1 服务网关和Zuul

 为什么需要网关

![](http://static.zybuluo.com/vermouth9/crskybjj950kieq2lrln9ie2/image.png)

服务网关的要素

* 稳定性、高可用
* 性能、并发性
* 安全性
* 扩展性

网关 可以处理各种非业务功能：协议转发，防刷，流量管控。

常用的网关方案：

* Nginx + Lua
* Spring Cloud Zuul

Zuul的特点：

* 路由 + 过滤器 = Zuul
* 核心是一系列的过滤器

Zuul的四种过滤器API：

* 前置（Pre）
* 后置（Post）
* 路由（Route）
* 错误（Error）：做一些统一异常处理

请求声明周期：

![](http://static.zybuluo.com/vermouth9/iok82wwdk4pkqfi23fagosk8/image.png)

## 8.2 Zuul：路由转发，排除和自定义

此时共有五个项目：

![](http://static.zybuluo.com/vermouth9/7sjcquvkqpp54nfd46kko2s6/image.png)

1. 添加依赖

   ```xml
   <dependency>		<!-- 统一配置中心的依赖 -->
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-config</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
   </dependency>
   
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-test</artifactId>
       <scope>test</scope>
   </dependency>
   ```

2. 把application.yml替换成bootstrap.yml，并添加配置如下：

   ```yaml
   spring:
     application:
       name: api-gateway
     cloud:
       config:
         discovery:
           enabled: true
           service-id: config
         profile: dev
   eureka:
     client:
       service-url:
         defaultZone: http://localhost:8761/eureka/
   zuul:
     routes:
       aaaa:
         path: /myProduct/**		# /myProduct/product/list == /product/product/list
         serviceId: product
       
     ignored-patterns:				# 排除某些路由
       - /**/product/listForOrder
   server:
     port: 8083
   ```

3. 在主启动项上添加注解：```@EnableZuulProxy```

   ```java
   @SpringBootApplication
   @EnableZuulProxy
   public class ApiGatewayApplication {
   	public static void main(String[] args) {
   		SpringApplication.run(ApiGatewayApplication.class, args);
   	}
   }
   ```

4. 访问如下链接测试：

   ```
   http://localhost:8083/product/product/list
   ```

   URL中的第一个product是在注册中心中注册的服务名称，后面的```/product/list```是访问路径，可得结果如下：

   ```json
   {
     "code": 0,
     "msg": "成功",
     "data": [
       {
         "name": "热榜",
         "type": 1,
         "foods": [
           {
             "id": "157875196366160022",
             "name": "皮蛋粥",
             "price": 0.01,
             "description": "好吃的皮蛋粥",
             "icon": "//fuss10.elemecdn.com/0/49/65d10ef215d3c770ebb2b5ea962a7jpeg.jpeg"
           },
           {
             "id": "164103465734242707",
             "name": "蜜汁鸡翅",
             "price": 0.02,
             "description": "好吃",
             "icon": "//fuss10.elemecdn.com/7/4a/f307f56216b03f067155aec8b124ejpeg.jpeg"
           }
         ]
       }
     ]
   }
   ```

​      因为加入了访问控制，url必须加入token，所以应该访问：

```
http://localhost:8083/product/product/list?token=123                                 
```

# Chapter 9

## 9.9 Zuul：跨域

前后端分离的一个问题就是：跨域问题

解决方法：

* 在被调用的类或方法上增加@CrossOrigin注解

  ```java
  @CrossOrigin(origins = "http://localhost:9000")   //参数中allowCredentials = "true"，允许cookie跨域
  @GetMapping("/greeting")
  public Greeting greeting(
      			@RequestParam(required=false, defaultValue="World") String name) {
      System.out.println("==== in greeting ====");
      return new Greeting(counter.incrementAndGet(), String.format(template, name));
  }
  ```

  

* 在Zuul里增加CorsFilter过滤器

普通的前后端分离项目中，可以使用CORS来解决跨域问题，因为HTML5一旦发现Ajax请求跨域，就会自动添加一些附加的头信息，有时还会多出一次附加的请求，但用户不会有感觉。因此，实现CORS通信的关键是服务器，只要服务器实现了CORS接口，就可以跨源通信。

```java
@Component
public class CorsConfigurerAdapter extends WebMvcConfigurerAdapter {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/greeting").allowedOrigins("http://localhost:9000");
    }
}
```

# Chapter 10

## 10.1 服务容错和Hystrix

Spring Cloud Hystrix是防雪崩利器。

具备：

* 服务降级

  优先核心服务，非核心服务不可用或弱可用

  * 通过```@HystrixCommand```注解指定
  * fallbackMethod（回退函数）中具体实现降级逻辑

* 依赖隔离

* 服务熔断

  ```Circuit Breaker```：断路器，当某个服务单元发生故障，通过断路器的故障监测，直接切断主逻辑调用。

* 监控

添加依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-hystrix</artifactId>
    <version>1.4.4.RELEASE</version>
</dependency>
```

添加注解

```java
@EnableFeignClients(basePackages = "com.imooc.product.client")
/*@SpringBootApplication
@EnableDiscoveryClient
@EnableCircuitBreaker*/
@SpringCloudApplication   //可以替换上面三个
public class OrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
}
```

```java
@RestController
@DefaultProperties(defaultFallback = "defaultFallback")			//默认降级处理逻辑
public class HystrixController {

    //@HystrixCommand(fallbackMethod = "fallback")
    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000")     /*超时时间设置*/
    })
    @GetMapping("/getProductInfoList")
    public String getProductInfoList() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject("http://localhost:8080/product/listForOrder", Arrays.asList("157875196366160022"), String.class);
    }

    private String fallback() {         //getProductInfoList()出故障了会访问这个方法
        return "太拥挤了，请稍后再试~";
    }

    private String defaultFallback() {         //getProductInfoList()出故障了会访问这个方法
        return "啊咧咧，默认提示太拥挤了，请稍后再试~";
    }
}
```

**服务熔断**

微服务中的服务故障的处理方法：

* 重试机制
* 断路

```java
(name = "circuitBreaker.enabled", value = "true"),      /*设置熔断*/
(name = "circuitBreaker.requestVolumeThreshold", value = "10"),  滚动时间窗口中，断路器最小的请求数
(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000"),	休闲时间窗
(name = "circuitBreaker.errorThresholdPercentage", value = "60")   断路器打开的错误百分比条件
```

# Chapter 11

## 11.1 服务追踪

1. 引入依赖

   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-sleuth</artifactId>
   </dependency>
   ```
