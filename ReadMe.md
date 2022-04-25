# eagle
Real time data processing system based on flink and CEP(drools/siddhi) 

中文详情：[基于flink和drools的实时日志处理](https://www.cnblogs.com/luxiaoxun/p/13197981.html)

### Design:
![design](https://github.com/luxiaoxun/eagle/blob/master/picture/eagle-design.png)

### Modules:
* eagle-api：基于springboot，提供drools规则引擎的读写服务。
* eagle-common：通用类模块。
* eagle-log：基于flink和[drools](https://github.com/kiegroup/drools)的日志处理服务。
* eagle-policy：基于flink和[siddhi](https://github.com/siddhi-io/siddhi)的数据处理服务。
* eagle-fraud-detection：基于flink的交易欺诈检测系统(fraud detection based on dynamic rules)。 
    1. [Flink 高级应用模式第一辑：欺诈检测系统案例研究](https://www.infoq.cn/article/dR1m9FfB1gEtvggKvlVX)
    2. [Flink 高级应用模式第二辑：应用模式的动态更新](https://www.infoq.cn/article/KFT2f79afVkNfIy6MRGg)
    3. [Flink 中的应用部署（三）：自定义窗口处理](https://www.infoq.cn/article/3Xiw36wSyK6J9G40jA9F)

build
```
mvn package -Dmaven.test.skip=true
```

eagle-log
```
flink run -m yarn-cluster -ynm eagle-log -j eagle-log.jar -c com.alarm.eagle.App -arg "--mode test" 
```

eagle-api
```
http://localhost:8080/eagle-api/swagger-ui/index.html
http://localhost:8080/eagle-api/log/rules
```
