# GT使用简要说明

<p style="color:green">该项目为本人从 0->1 自主开发AT|XA模式分布式事务管理工具,数据回滚部分参考alibaba旗下seata的源码,如有不当行为,烦请告知</p>

>依赖采用**jdk17+spring-boot3.0.0**开发

**本项目作为依赖使用,将项目安装到本地仓库中，并引入依赖**

## 1.引入依赖

将项目clone本地使用install安装项目到maven 仓库 或者 直接下载提供好的压缩包将依赖解压到maven仓库

[压缩包下载](https://github.com/L-P-F/GlobalTransaction/releases/tag/dependency)

引入依赖
```xml
<dependency>
    <groupId>cn.aurora</groupId>
    <artifactId>ara-distribute-transaction-client</artifactId>
    <version>2.0</version>
</dependency>
```

## 2.XA模式的工具需要自己注册一个拦截器(由于作者偷懒,没做更改)

在项目中参加全局事务的分支节点内注册拦截器,全局事务入口所在节点不需要注册(除非该节点也需要作为某个全局事务的分支节点)

代码如下,类名 InterceptorConfig 可自定义
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

## 4.在分支节点的serviceImpl层中需要参与全局事务的方法上添加@BranchTransaction注解

------

**@GloabalTransaction和@BranchTransaction注解和Transactional注解一样，都有管理基本本地事务的能力;**


>XA模式下;

假设A节点下有两个serviceImpl，我们可以通过@GT或者@BT注解与原生的@Transactional(propagation = Propagation.REQUIRES_NEW)注解联合使用实现**更改事务的传播机制**。

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

>**注意**

<p style="font-style: italic; color:indianred">本工具不能很好的支持mybatis-plus封装的批处理操作比如"saveBatch,updateBatch"等方法,因此建议批处理操作使用mybatis的动态sql进行完成

## 5.运行GTServer服务器

[GTServer-2.0.zip下载路径](https://github.com/L-P-F/GlobalTransaction-Server/releases/tag/GTServer)

