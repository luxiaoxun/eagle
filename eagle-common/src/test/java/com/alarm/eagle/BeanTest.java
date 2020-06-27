package com.alarm.eagle;

import com.alarm.eagle.constants.AlertConstant.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/9.
 */
public class BeanTest {
    @Test
    public void testAlertType() {
        AlertType alertType = AlertType.resolve(1);
        Assert.assertEquals(alertType, AlertType.HttpCallback);

        List<AlertType> alertTypeList = AlertType.resolveAll(1);
        Assert.assertEquals(alertTypeList.size(), 1);

        List<AlertType> alertTypeList2 = AlertType.resolveAll(3);
        Assert.assertEquals(alertTypeList2.size(), 2);

        System.out.println(alertTypeList2);
    }
}
