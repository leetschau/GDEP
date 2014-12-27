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

## 用户扩展模块

通过编辑plugins/user.script文件，可以将自定义的shell命令和Java文件挂载到环境检测工具上，具体方法是：

在[shell]小节下添加要运行的shell命令，每个命令一行；
在[java]小节下添加要运行的Java入口类名，同时需要将运行Java程序的jar包及其依赖包保存在plugins/lib文件夹下。

# 报告格式

DepResult.txt文件内容为静态指标(basic)和性能指标(performance)的测试结果，

user_shell.log文件内容为自定义shell命令运行结果。

user_java.log文件内容是自定义Java程序运行结果。

# 目录结构

* bin: 放置运行脚本；

* lib: 保存GDEP运行需要的第三方类库；

* plugins: 自定义脚本目录；

* plugins/lib: 自定义Java程序及其依赖包目录。
