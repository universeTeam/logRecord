package cn.monitor4all.logRecord.test.operationLogNameTest;

import cn.monitor4all.logRecord.bean.BurialPointDTO;
import cn.monitor4all.logRecord.configuration.LogRecordAutoConfiguration;
import cn.monitor4all.logRecord.service.IOperationLogGetService;
import cn.monitor4all.logRecord.test.operationLogNameTest.bean.TestUser;
import cn.monitor4all.logRecord.test.operationLogNameTest.service.OperationLogGetService;
import cn.monitor4all.logRecord.test.operationLogNameTest.service.TestService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

/**
 * 单元测试
 */
@Slf4j
@SpringBootTest
@ContextConfiguration(classes = {
        LogRecordAutoConfiguration.class,
        LogOperationTest.CustomFuncTestOperationLogGetService.class,
        OperationLogGetService.class,
        TestService.class,})
@PropertySource("classpath:test.properties")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class LogOperationTest {

    @Autowired
    private TestService testService;

    @Test
    public void logRecordFuncTest() {
        testService.testBizId("1");
        testService.testReturnStr();
        testService.testRecordReturnValue();
        testService.testReturnObject();
        try {
            testService.testException();
        } catch (Exception ignored) {}
        testService.testMsgAndExtra("李四", new TestUser(1, "name"));
        testService.testCustomFunc();
        testService.testOperatorId("001");
        testService.testExecuteBeforeFunc();
        testService.testObjectDiff(new TestUser(2, "李四"));
        testService.testCondition(new TestUser(1, "张三"));
    }

    @TestComponent
    public static class CustomFuncTestOperationLogGetService implements IOperationLogGetService {

        @Override
        public void createLog(BurialPointDTO burialPointDTO) {
            log.info("burialPointDTO: [{}]", JSON.toJSONString(burialPointDTO));

            if ("testBizIdWithSpEL".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getBizId(), "1");
            }
            if ("testBizIdWithRawString".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getBizId(), "2");
            }

            if ("testReturnStr".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getReturnStr(), "\"returnStr\"");
            }

            if ("testRecordReturnValueTrue".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getReturnStr(), "\"returnStr\"");
            }

            if ("testRecordReturnValueFalse".equals(burialPointDTO.getBizType())) {
                Assertions.assertNull(burialPointDTO.getReturnStr());
            }

            if ("testReturnObject".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getReturnStr(), "{\"id\":1,\"name\":\"张三\"}");
            }

            if ("testException".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getException(), "testException");
            }

            if ("testMsgAndExtraWithSpEL".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getMsg(), "将旧值张三更改为新值李四");
                Assertions.assertEquals(burialPointDTO.getExtra(), "将旧值张三更改为新值李四");
            }
            if ("testMsgAndExtraWithRawString".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getMsg(), "str");
                Assertions.assertEquals(burialPointDTO.getExtra(), "str");
            }
            if ("testMsgAndExtraWithObject".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getMsg(), "{\"id\":1,\"name\":\"name\"}");
                Assertions.assertEquals(burialPointDTO.getExtra(), "{\"id\":1,\"name\":\"name\"}");
            }

            if ("testMethodWithCustomName".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getBizId(), "testMethodWithCustomName");
            }
            if ("testMethodWithoutCustomName".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getBizId(), "testMethodWithoutCustomName");
            }

            if ("testOperatorIdWithSpEL".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getOperatorId(), "001");
            }
            if ("testOperatorIdWithCustomOperatorIdGetService".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getOperatorId(), "操作人");
            }

            if ("testExecuteBeforeFunc1".equals(burialPointDTO.getBizType())) {
                Assertions.assertNull(burialPointDTO.getBizId());
            }
            if ("testExecuteAfterFunc".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getBizId(), "valueInBiz");
            }
            if ("testExecuteBeforeFunc2".equals(burialPointDTO.getBizType())) {
                Assertions.assertNull(burialPointDTO.getBizId());
            }

            if ("testObjectDiff".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getMsg(), "【用户工号】从【1】变成了【2】 【name】从【张三】变成了【李四】");
                Assertions.assertEquals(burialPointDTO.getDiffDTOList().get(0).getOldClassName(), "cn.monitor4all.logRecord.test.operationLogNameTest.bean.TestUser");
                Assertions.assertEquals(burialPointDTO.getDiffDTOList().get(0).getOldClassAlias(), "用户信息实体");
                Assertions.assertEquals(burialPointDTO.getDiffDTOList().get(0).getDiffFieldDTOList().get(0).getFieldName(), "id");
                Assertions.assertEquals(burialPointDTO.getDiffDTOList().get(0).getDiffFieldDTOList().get(0).getOldFieldAlias(), "用户工号");
                Assertions.assertEquals(burialPointDTO.getDiffDTOList().get(0).getDiffFieldDTOList().get(0).getNewFieldAlias(), "用户工号");
                Assertions.assertEquals(burialPointDTO.getDiffDTOList().get(0).getDiffFieldDTOList().get(0).getOldValue(), 1);
                Assertions.assertEquals(burialPointDTO.getDiffDTOList().get(0).getDiffFieldDTOList().get(0).getNewValue(), 2);
            }

            if ("testCondition1".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getBizId(), "1");
            }
            if ("testCondition2".equals(burialPointDTO.getBizType())) {
                Assertions.assertEquals(burialPointDTO.getBizId(), "2");
            }
        }
    }
}
