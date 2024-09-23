# GT使用简要说明

<p style="color:green">该项目为本人从 0->1 自主开发AT分布式事务管理工具,数据回滚部分参考alibaba旗下seata的源码,如有不当行为,烦请告知</p>

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

## 2.AT模式在项目application.yml中配置服务器运行端口以及数据库中回滚表的名字
不配置会使用默认值
```yaml
gt:
  server:
    undo-table-name: table_name
    server-addr: ip:port
```

## 3.在serviceImpl层全局事务入口处的方法上添加@GlobalTransaction注解

## 4.在分支节点的serviceImpl层中需要参与全局事务的   入口方法   上添加@BranchTransaction注解
>GT和BT注解只需要在   入口方法上   添加一个即可

```text
比如一条全局事务链 A(节点1下的a方法) -> B(节点1下的b方法) -> C(节点2下的a方法) -> D(节点2下的b方法)
此时A作为全局事务入口,在方法内部调用B,通过feign远程请求C,C再在方法内调用D
我们只需要在 A 上添加 GT 注解,在 C 上添加 BT 注解即可。
除非B和D也需要作为其他全局事务链的 【成员】 或 【入口】 ,否则不需要添加相应的注解
```

------

**@GloabalTransaction和@BranchTransaction注解和@Transactional注解一样，具备@Transactional注解管理本地事务的能力;**


>**注意**

<p style="font-style: italic; color:indianred">目前本工具不能很好的支持mybatis-plus封装的批处理操作比如"saveBatch,updateBatch"等方法,因此建议批处理操作使用mybatis的动态sql进行完成

## 5.运行GTServer服务器

[GTServer-2.0.zip下载路径](https://github.com/L-P-F/GlobalTransaction-Server/releases/tag/GTServer)
