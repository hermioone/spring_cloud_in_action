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

## Chapter 5.2

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

## Chapter 5.3

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

## Chapter 5.4

可以通过在application.yml中添加以下来改变负载均衡策略：

```yaml
PRODUCT:
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
```

value值为具体的轮询规则的全路径名。

## Chapter 5.5

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

