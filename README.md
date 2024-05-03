# GT使用简要说明

<p style="color:green">该项目为本人从 0->1 自主开发AT模式分布式事务管理,其中数据回滚部分的编写借鉴了alibaba旗下的seata源码,如有冒犯,烦请告知</p>

>依赖采用**jdk17+spring-boot3.0.0**开发

**本项目作为依赖使用,将项目安装到本地仓库中，并引入依赖**

## 1.将项目clone本地使用install安装项目到maven 仓库 或者 直接下载提供好的压缩包将依赖解压到maven仓库，并在项目中引入依赖

[压缩包下载](https://github.com/L-P-F/GlobalTransaction/releases/tag/dependency)
```xml
<dependency>
    <groupId>cn.distributed</groupId>
    <artifactId>distribute-transaction-client</artifactId>
    <version>1.0</version>
</dependency>
```

## 2.在个人项目中参加全局事务的分支节点内注册拦截器,代码如下

```java
@Component
public class InterceptorConfig extends WebMvcConfigurationSupport
{

    @Autowired
    private GTFeignReqReceive gtFeignReqReceive;

    @Override
    protected void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(gtFeignReqReceive)
                .addPathPatterns("/**");
    }
}
```

## 3.在serviceImpl层全局事务入口处的方法上添加@GlobalTransaction注解

## 4.在分支节点的serviceImpl层需要参与全局事务的方法上添加@BranchTransaction注解

------

**@GloabalTransaction和@BranchTransaction注解和Transactional注解一样，都有管理基本本地事务的能力;但是互斥，不能同时使用**

假设A节点下有两个serviceImpl，
我们可以通过@GT或者@BT注解与原生的@Transactional(propagation = Propagation.REQUIRES_NEW)注解联合使用实现**更改事务的传播机制**。

>示例如下

```java
package com.dre.service2;
@Service
public class BookServiceImpl implements BookService{
    @Autowired
    private UserService userService;
    
    @GlobalTransaction 或者 @BranchTransaction 代替@Transactional注解
    public void save(){
    	userService.save();   
    }
}
```

```java
package com.dre.service1;

@Service
public class UserServiceImpl implements UserService{
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(){
    
    }
}
```

**但是@GT和@BT两个注解本身并没有设计Propagation参数去改变事务的传播机制**

<p style="color:cyan">PS:因为作者也有点懒了QAQ,后续有时间再考虑实现吧</p>

>本依赖在处理批量sql操作的时候，不能处理mybatis-plus封装好的batch操作的一系列方法,作者暂时没找到解决方法,如果有批处理操作,还请使用mybatis的动态sql实现
## 5.运行GTServer服务器

[GTServer-2.0.zip下载路径](https://github.com/L-P-F/GlobalTransaction-Server/releases/tag/GTServer)

