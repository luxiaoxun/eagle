# eagle
Real time log processing system based on flink and drools

### Design:
![design](https://img2020.cnblogs.com/blog/434101/202006/434101-20200627112934739-65447543.png)

eagle-api：基于springboot，作为drools规则引擎的写入和读取API服务。

eagle-common：通用类模块。

eagle-log：基于flink的日志处理服务。

build
```
mvn package -Dmaven.test.skip=true
```

eagle-log
```
java -jar eagle-log.jar --dev
```

eagle-api
```
http://localhost:8080/eagle-api/log/rules
```
