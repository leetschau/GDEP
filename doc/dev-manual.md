# 指标获取算法

* ostype: System.getProperty("os.name")

* freq: 

* mem:

* disk:

* jdk:

* fd:

* cpu-perf:

* mem-io:

* disk-io:

* net-io:

* multi-thread:

# 插件加载方法

对于Java程序型插件，用户需要事先把Java代码编译为Jar文件，对于Shell脚本型插件，直接保存在.sh文件中，然后放置在GDEP的plugins目录中，在程序启动时，启动shell脚本会将plugins目录中的所有jar文件加入到classpath中，执行所有.sh文件，程序或者脚本运行的控制台输出将作为检测结果加入到最终报告中；