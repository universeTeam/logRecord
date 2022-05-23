package cn.monitor4all.logRecord.aop;

import cn.monitor4all.logRecord.annotation.OperationLog;
import cn.monitor4all.logRecord.bean.BurialPointDTO;
import cn.monitor4all.logRecord.bean.ExceptionDTO;
import cn.monitor4all.logRecord.constants.LogConstants;
import cn.monitor4all.logRecord.context.GlobalContext;
import cn.monitor4all.logRecord.context.LogRecordContext;
import cn.monitor4all.logRecord.function.CustomFunctionRegistrar;
import cn.monitor4all.logRecord.service.IOperationLogGetService;
import cn.monitor4all.logRecord.service.IOperatorIdGetService;
import cn.monitor4all.logRecord.service.LogService;
import cn.monitor4all.logRecord.thread.LogRecordThreadPool;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

@Aspect
@Component
@Slf4j
public class SystemLogAspect {

    @Autowired(required = false)
    private LogRecordThreadPool logRecordThreadPool;

    @Autowired(required = false)
    private LogService logService;

    @Autowired(required = false)
    private IOperationLogGetService iOperationLogGetService;

    @Autowired(required = false)
    private IOperatorIdGetService iOperatorIdGetService;

    private final SpelExpressionParser parser = new SpelExpressionParser();

    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(cn.monitor4all.logRecord.annotation.OperationLog) || @annotation(cn.monitor4all.logRecord.annotation.OperationLogs)")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        Object result;
        List<BurialPointDTO> burialPointDTOList = new ArrayList<>();
        Method method = getMethod(pjp);
        OperationLog[] annotations = method.getAnnotationsByType(OperationLog.class);

        // 将前置和后置执行的注解分开处理并保证最终写入顺序
        Map<OperationLog, BurialPointDTO> logDtoMap = new LinkedHashMap<>();

        StopWatch stopWatch = new StopWatch();
        try {
            // 方法执行前
            for (OperationLog annotation : annotations) {
                if (annotation.executeBeforeFunc()) {
                    BurialPointDTO burialPointDTO = resolveExpress(annotation, pjp);
                    if (burialPointDTO != null) {
                        logDtoMap.put(annotation, burialPointDTO);
                    }
                }
            }
            stopWatch.start();
            result = pjp.proceed();
            stopWatch.stop();
            // 方法执行后
            for (OperationLog annotation : annotations) {
                if (!annotation.executeBeforeFunc()) {
                    BurialPointDTO burialPointDTO = resolveExpress(annotation, pjp);
                    if (burialPointDTO != null) {
                        logDtoMap.put(annotation, burialPointDTO);
                    }
                }
            }
            // 写入成功执行结果
            burialPointDTOList = new ArrayList<>(logDtoMap.values());
            logDtoMap.forEach((annotation, logDTO) -> {
                logDTO.setSuccess(true);
                if (annotation.recordReturnValue()) {
                    logDTO.setReturnStr(JSON.toJSONString(result));
                }
            });
        } catch (Throwable throwable) {
            stopWatch.stop();
            // 方法执行异常后
            for (OperationLog annotation : annotations) {
                if (!annotation.executeBeforeFunc()) {
                    logDtoMap.put(annotation, resolveExpress(annotation, pjp));
                }
            }
            // 写入异常执行结果
            burialPointDTOList = new ArrayList<>(logDtoMap.values());
            burialPointDTOList.forEach(logDTO -> {
                logDTO.setSuccess(false);
                logDTO.setException(throwable.getMessage());
            });
            throw throwable;
        } finally {
            // 清除Context：每次方法执行一次
            LogRecordContext.clearContext();
            // 提交logDTO至主线程或线程池
            Consumer<BurialPointDTO> createLogFunction = logDTO -> {
                try {
                    // 记录执行时间
                    logDTO.setExecutionTime(stopWatch.getTotalTimeMillis());
                    // 发送日志本地监听
                    if (iOperationLogGetService != null) {
                        iOperationLogGetService.createLog(logDTO);
                    }
                    // 发送消息管道
                    if (logService != null) {
                        logService.createLog(logDTO);
                    }
                } catch (Throwable throwable) {
                    log.error("Send logDTO error", throwable);
                }
            };
            if (logRecordThreadPool != null) {
                burialPointDTOList.forEach(logDTO -> logRecordThreadPool.getLogRecordPoolExecutor().submit(() -> createLogFunction.accept(logDTO)));
            } else {
                burialPointDTOList.forEach(createLogFunction);
            }
        }
        return result;
    }

    private BurialPointDTO resolveExpress(OperationLog annotation, JoinPoint joinPoint) {
        BurialPointDTO burialPointDTO = null;
        String bizIdSpel = annotation.bizId();
        String msgSpel = annotation.msg();
        String extraSpel = annotation.extra();
        String operatorIdSpel = annotation.operatorId();
        String conditionSpel = annotation.condition();
        String bizId = bizIdSpel;
        String msg = msgSpel;
        String extra = extraSpel;
        String operatorId = annotation.operatorId();
        try {
            Object[] arguments = joinPoint.getArgs();
            Method method = getMethod(joinPoint);
            String[] params = discoverer.getParameterNames(method);
            StandardEvaluationContext context = LogRecordContext.getContext();
            CustomFunctionRegistrar.register(context);
            if (params != null) {
                for (int len = 0; len < params.length; len++) {
                    context.setVariable(params[len], arguments[len]);
                }
            }

            // condition 处理：SpEL解析
            if (StringUtils.isNotBlank(conditionSpel)) {
                Expression conditionExpression = parser.parseExpression(conditionSpel);
                boolean passed = Boolean.TRUE.equals(conditionExpression.getValue(context, Boolean.class));
                if (!passed) {
                    return null;
                }
            }

            // bizId 处理：SpEL解析
            if (StringUtils.isNotBlank(bizIdSpel)) {
                Expression bizIdExpression = parser.parseExpression(bizIdSpel);
                bizId = bizIdExpression.getValue(context, String.class);
            }

            // msg 处理：SpEL解析 默认写入原字符串
            if (StringUtils.isNotBlank(msgSpel)) {
                Expression msgExpression = parser.parseExpression(msgSpel);
                Object msgObj = msgExpression.getValue(context, Object.class);
                msg = msgObj instanceof String ? (String) msgObj : JSON.toJSONString(msgObj, SerializerFeature.WriteMapNullValue);
            }

            // extra 处理：SpEL解析 默认写入原字符串
            if (StringUtils.isNotBlank(extraSpel)) {
                Expression extraExpression = parser.parseExpression(extraSpel);
                Object extraObj = extraExpression.getValue(context, Object.class);
                extra = extraObj instanceof String ? (String) extraObj : JSON.toJSONString(extraObj, SerializerFeature.WriteMapNullValue);
            }

            // operatorId 处理：优先级 注解传入 > 自定义接口实现
            if (iOperatorIdGetService != null) {
                operatorId = iOperatorIdGetService.getOperatorId();
            }
            if (StringUtils.isNotBlank(operatorIdSpel)) {
                Expression operatorIdExpression = parser.parseExpression(operatorIdSpel);
                Object operatorIdObj = operatorIdExpression.getValue(context, Object.class);
                operatorId = operatorIdObj instanceof String ? (String) operatorIdObj : JSON.toJSONString(operatorIdObj, SerializerFeature.WriteMapNullValue);
            }

            burialPointDTO = new BurialPointDTO();
            burialPointDTO.setMachine(GlobalContext.machineDTO);
            burialPointDTO.setLogId(UUID.randomUUID().toString());
            burialPointDTO.setBizId(bizId);
            burialPointDTO.setBizType(annotation.bizType());
            burialPointDTO.setTag(annotation.tag());
            burialPointDTO.setOperateDate(new Date());
            burialPointDTO.setMsg(msg);
            burialPointDTO.setExtra(extra);
            burialPointDTO.setOperatorId(operatorId);
            burialPointDTO.setDiffDTOList(LogRecordContext.getDiffDTOList());

            // 处理 Reset
            final Map<String, Object> extendContext = LogRecordContext.getExtendContext();
            final Throwable throwable = (Throwable) extendContext.get(LogConstants.Public.RESET_KEY);
            if (throwable != null) {
                burialPointDTO.setUsual(false);
                final String[] stackFrames = ExceptionUtils.getStackFrames(throwable);
                burialPointDTO.setException(throwable.getMessage()+"");
                final ExceptionDTO exceptionDTO = burialPointDTO.getExceptionDTO();
                exceptionDTO.setDescribe(throwable.getMessage());
                exceptionDTO.setClassName(throwable.getClass().getName());
                exceptionDTO.setRootFrame(stackFrames[1]);
                if (stackFrames.length > 2) {
                    exceptionDTO.setSubFrame(stackFrames[2]);
                }
            }
            extendContext.remove(LogConstants.Public.RESET_KEY);

            // 处理结构化扩展信息
            for (Map.Entry<String, Object> entry : extendContext.entrySet()) {
                final Map<String, Object> cusStructExtra = burialPointDTO.getCusStructExtra();
                cusStructExtra.put(entry.getKey(),entry.getValue());
            }


        } catch (Exception e) {
            log.error("OperationLogAspect resolveExpress error", e);
        } finally {
            // 清除Diff实体列表：每次注解执行一次
            LogRecordContext.clearDiffDTOList();
        }
        return burialPointDTO;
    }

    private Method getMethod(JoinPoint joinPoint) {
        Method method = null;
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature ms = (MethodSignature) signature;
            Object target = joinPoint.getTarget();
            method = target.getClass().getMethod(ms.getName(), ms.getParameterTypes());
        } catch (NoSuchMethodException e) {
            log.error("OperationLogAspect getMethod error", e);
        }
        return method;
    }
}
