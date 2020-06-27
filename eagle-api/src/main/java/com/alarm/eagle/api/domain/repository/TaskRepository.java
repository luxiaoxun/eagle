package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.TaskDo;
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
