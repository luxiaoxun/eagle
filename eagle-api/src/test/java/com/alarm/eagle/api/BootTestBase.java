package com.alarm.eagle.api;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;

/**
 * Created by luxiaoxun on 18/1/17.
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@WebAppConfiguration
public class BootTestBase {
    public static final Logger logger = LoggerFactory.getLogger(BootTestBase.class);

    @Resource
    private WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc = null;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
}
