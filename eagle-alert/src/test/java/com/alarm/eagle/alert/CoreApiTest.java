package com.alarm.eagle.alert;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.java.tuple.Tuple2;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;

/**
 * Created by skycrab on 18/1/10.
 */
public class CoreApiTest {
    @Test
    public void testTypeHint() {
        TypeHint<String> typeHint = new TypeHint<String>() {
        };
    }

    @Test
    public void testT() {
        String s = "";
        Type type = s.getClass().getGenericSuperclass();
        System.out.println(type instanceof ParameterizedType);

        Type t2 = Tuple2.of(1, 2).getClass().getGenericSuperclass();
        Tuple2<String, String> tuple2 = new Tuple2<>("1", "1");
        System.out.println(t2 instanceof ParameterizedType);
        System.out.println(tuple2.getClass().getGenericSuperclass() instanceof TypeVariable);

        ArrayList<Integer> a = new ArrayList<>();
        System.out.println(a.getClass().getGenericSuperclass() instanceof ParameterizedType);
    }
}
