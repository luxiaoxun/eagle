# eagle
Real time log processing system based on flink and drools 

中文详情：[Chinese Details](https://www.cnblogs.com/luxiaoxun/p/13197981.html)

### Design:
![design](https://github.com/luxiaoxun/eagle/blob/master/picture/eagle-design.png)

### Modules:
* eagle-api：基于springboot，提供drools规则引擎的读写服务。
* eagle-common：通用类模块。
* eagle-log：基于flink的日志处理服务。

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
http://localhost:8080/eagle-api/swagger-ui.html
http://localhost:8080/eagle-api/log/rules
```
