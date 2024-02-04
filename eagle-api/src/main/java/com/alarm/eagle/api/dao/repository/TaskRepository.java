package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.TaskDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by luxiaoxun on 18/1/2.
 */
@Repository
public interface TaskRepository extends JpaRepository<TaskDo, Integer> {
    /**
     * 根据任务id获取task
     *
     * @param taskId
     * @return
     */
    TaskDo getOne(Integer taskId);
}
