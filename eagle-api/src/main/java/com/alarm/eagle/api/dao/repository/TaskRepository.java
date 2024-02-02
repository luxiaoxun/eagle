package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.TaskDo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by luxiaoxun on 18/1/2.
 */
public interface TaskRepository extends JpaRepository<TaskDo, Integer> {
    /**
     * 根据任务id获取task
     * @param taskId
     * @return
     */
    TaskDo getOne(Integer taskId);
}
