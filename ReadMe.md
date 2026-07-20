# eagle
Real time data processing system based on flink and CEP(drools/siddhi) 

中文详情：[基于flink和drools的实时日志处理](https://www.cnblogs.com/luxiaoxun/p/13197981.html)

### Design:
![design](https://github.com/luxiaoxun/eagle/blob/master/picture/eagle-design.png)

### Modules:
* eagle-api：基于springboot，提供策略规则的读写服务。
* eagle-common：通用类模块。
* eagle-alert：基于flink和[siddhi](https://github.com/siddhi-io/siddhi)的数据告警服务。
* eagle-log：基于flink和[drools](https://github.com/kiegroup/drools)的日志处理服务。
* eagle-detection：基于flink的交易欺诈检测系统(Dynamic CEP)。

build
```
mvn clean package
```

debug mode:
```
"Run/Debug" => "Edit Configurations...",
"Build and run" section select "Modify options" => Java => Add dependencies with "Provided" scope to classpath.
```
cluster mode:
```
flink run -m yarn-cluster -ynm eagle-log -j eagle-log.jar -c com.alarm.eagle.EagleLogApp -arg "--mode test" 
```

eagle-api
```
http://localhost:8080/eagle-api/swagger-ui/index.html
```
