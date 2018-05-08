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