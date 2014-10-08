# 使用方法

GDEP有两种使用场景：基准测试和比较测试，

当前目录下有dep.conf文件和DepStandard.txt两个文件时，运行比较测试，
测试范围由模板DepStandard.txt定义，网络测试的对端定义在dep.conf文件的[client]节中；

当前目录下只有dep.conf文件时，运行基准测试，测试范围和网络测试对端都由dep.conf文件定义。
如果希望将测试结果作为后续测试的基准，需要将结果文件DepResult.txt改名为DepStandard.txt。

GDEP运行需要JDK 6或以上版本，安装和使用过程包含下面的步骤：

1. 安装GDEP：将gdep-*.zip文件解压到任意位置；

1. 在GDEP根目录(gdep-*)下编写配置文件dep.conf（实例可参考功能说明文档的“场景说明”部分），定义扩展插件（可选）；

1. 运行GDEP：执行"bin/gdep"；

1. 分析检测报告：见项目根目录下的DepResult.txt文件；

## 网络相关测试

当使用网络相关测试时，需要在配置文件中指定目标主机的IP地址和用户，并需要在测试主机和目标主机间建立SSH密钥登录互信机制：

```
$ ssh-keygen
$ ssh-copy-id user1@10.2.0.61
```

对应在配置文件中的定义是：

```
[client]
user: user1
ip: 10.2.0.61
```

# 配置文件

配置文件分为3节：

# 命令行格式

# 报告格式

# 目录结构

* bin: 放置运行脚本；

* lib: 保存GDEP运行需要的第三方类库；

* plugins: 放置扩展插件；
