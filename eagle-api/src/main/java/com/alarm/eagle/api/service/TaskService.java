package com.alarm.eagle.api.service;

import com.alarm.eagle.api.domain.*;
import com.alarm.eagle.api.domain.repository.*;
import com.alarm.eagle.bean.*;
import com.alarm.eagle.constants.AlertConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by luxiaoxun on 18/1/2.
 */
@Service
public class TaskService {
    @Resource
    private TaskRepository taskRepository;

    @Resource
    private DatasourceRepository datasourceRepository;

    @Resource
    private StreamAppRepository streamAppRepository;

    @Resource
    private StreamDefineRepository streamDefineRepository;

    @Resource
    private StreamFieldRepository streamFieldRepository;

    @Resource
    private PolicyRepository policyRepository;

    public Task queryTask(Integer taskId) {
        TaskDo taskDo = taskRepository.getOne(taskId);
        Task task = new Task();
        task.setTaskId(taskDo.getTaskId());
        task.setTaskName(taskDo.getTaskName());
        task.setTimeCharacteristic(AlertConstant.TimeCharacteristic.resolve(taskDo.getTimeCharacteristic()));
        List<Datasource> datasourceList = queryDatasourceByTaskId(taskId);
        task.setDatasourceList(datasourceList);
        List<StreamApp> streamAppList = queryStreamAppByTaskId(taskId);
        task.setStreamAppList(streamAppList);

        return task;
    }

    /**
     * 根据taskId查询数据源配置
     * @param taskId
     * @return
     */
    private List<Datasource> queryDatasourceByTaskId(Integer taskId) {
        List<DatasourceDo> datasourceDoList = datasourceRepository.queryByTaskId(taskId);
        List<Datasource> datasourceList = new ArrayList<>(datasourceDoList.size());
        for(DatasourceDo datasourceDo : datasourceDoList) {
            Datasource datasource = new Datasource();
            datasource.setTopic(datasourceDo.getTopic());
            datasource.setGroupId(datasourceDo.getGroupId());
            datasource.setServers(datasourceDo.getServers());
            datasource.setStreamId(datasourceDo.getStreamId());
            datasource.setEventTimestampField(datasourceDo.getEventTimestampField());
            List<String> keyByFieldList = new ArrayList<>();
            if(StringUtils.isNotBlank(datasourceDo.getKeyByFields())) {
                keyByFieldList = Arrays.asList(datasourceDo.getKeyByFields().split(","));
            }
            datasource.setKeyByFields(keyByFieldList);
            datasource.setFilter(datasourceDo.getFilter());
            datasourceList.add(datasource);
        }
        return datasourceList;
    }


    private List<StreamApp> queryStreamAppByTaskId(Integer taskId) {
        List<StreamAppDo> streamAppDoList = streamAppRepository.queryByTaskIdAndStatus(taskId, AlertConstant.Status.ENABLE.getId());
        List<StreamApp> streamAppList = new ArrayList<>(streamAppDoList.size());

        List<StreamDefineDo> streamDefineDoList = streamDefineRepository.queryByTaskId(taskId);
        List<StreamFieldDo> streamFieldDoList = streamFieldRepository.queryByTaskId(taskId);
        List<PolicyDo> policyDoList = policyRepository.queryByTaskIdAndStatus(taskId, AlertConstant.Status.ENABLE.getId());

        Map<Integer, List<StreamDefineDo>> streamMap = streamDefineDoList.stream().collect(Collectors.groupingBy(StreamDefineDo::getAppId));
        Map<Integer, List<PolicyDo>> policyMap = policyDoList.stream().collect(Collectors.groupingBy(PolicyDo::getAppId));
        Map<Integer, List<StreamFieldDo>> fieldMap = streamFieldDoList.stream().collect(Collectors.groupingBy(StreamFieldDo::getStream_define_id));
        Map<Integer, List<StreamFieldDo>> appFieldMap = streamFieldDoList.stream().collect(Collectors.groupingBy(StreamFieldDo::getAppId));

        for(StreamAppDo streamAppDo : streamAppDoList) {
            Integer appId = streamAppDo.getAppId();
            //是否数据都准备好(streamDefine, streamField, policy)
            if(!streamMap.containsKey(appId) || !policyMap.containsKey(appId) || !appFieldMap.containsKey(appId)) {
                continue;
            }

            StreamApp streamApp = new StreamApp();
            streamApp.setAppId(streamAppDo.getAppId());
            streamApp.setAppName(streamAppDo.getAppName());
            streamApp.setStatus(AlertConstant.Status.resolve(streamAppDo.getStatus()));

            //streamDefine
            List<StreamDefine> streamDefineList = new ArrayList<>(streamMap.get(appId).size());
            for(StreamDefineDo streamDefineDo : streamMap.get(appId)) {
                Integer streamDefineId = streamDefineDo.getStreamDefineId();
                StreamDefine streamDefine = new StreamDefine();
                streamDefine.setStreamDefineId(streamDefineDo.getStreamDefineId());
                streamDefine.setStreamId(streamDefineDo.getStreamId());
                List<Field> fieldList = new ArrayList<>();
                for(StreamFieldDo streamFieldDo : fieldMap.get(streamDefineId)) {
                    Field field = new Field(streamFieldDo.getFieldName(), streamFieldDo.getFieldType());
                    fieldList.add(field);
                }
                streamDefine.setFieldList(fieldList);
                streamDefineList.add(streamDefine);
            }
            streamApp.setStreamDefineList(streamDefineList);

            //policy
            List<Policy> policyList = new ArrayList<>(policyMap.get(appId).size());
            for(PolicyDo policyDo : policyMap.get(appId)) {
                Policy policy = new Policy();
                policy.setPolicyId(policyDo.getPolicyId());
                policy.setPolicyName(policyDo.getPolicyName());
                policy.setMetric(policyDo.getMetric());
                policy.setPrepareStream(policyDo.getPrepareStream());
                policy.setCql(policyDo.getCql());
                policy.setStatus(AlertConstant.Status.resolve(policyDo.getStatus()));
                policy.setAlertLevel(AlertConstant.AlertLevel.resolve(policyDo.getAlertLevel()));
                policyList.add(policy);
            }

            streamApp.setPolicyList(policyList);

            streamAppList.add(streamApp);

        }

        return streamAppList;
    }



}
