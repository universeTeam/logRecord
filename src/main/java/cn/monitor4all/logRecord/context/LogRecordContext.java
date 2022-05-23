package cn.monitor4all.logRecord.context;

import cn.monitor4all.logRecord.bean.DiffDTO;
import cn.monitor4all.logRecord.constants.LogConstants;
import org.springframework.core.NamedThreadLocal;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogRecordContext {

    private static final ThreadLocal<StandardEvaluationContext> CONTEXT_THREAD_LOCAL = new NamedThreadLocal<>("ThreadLocal StandardEvaluationContext");
    private static final ThreadLocal<Map<String,Object>> EXTEND_THREAD_LOCAL = new NamedThreadLocal<>("ThreadLocal ExtendThreadLocal");


    public static StandardEvaluationContext getContext() {
        return CONTEXT_THREAD_LOCAL.get() == null ? new StandardEvaluationContext(): CONTEXT_THREAD_LOCAL.get();
    }

    public static void putVariables(String key, Object value) {
        StandardEvaluationContext context = getContext();
        context.setVariable(key, value);
        CONTEXT_THREAD_LOCAL.set(context);
    }

    public static void putExtra(String key, Object value) {
        final Map<String, Object> extraMap = getExtendContext();
        extraMap.put(key,value);
    }

    public static void clearContext() {
        CONTEXT_THREAD_LOCAL.remove();
    }

    /**
     * 异常重置
     * @param throwable
     */
    public static void reset(Throwable throwable) {
        Map<String, Object> ex = getExtendContext();
        ex.put(LogConstants.Public.RESET_KEY,throwable);
        EXTEND_THREAD_LOCAL.set(ex);
    }

    public static Map<String,Object> getExtendContext(){
        return EXTEND_THREAD_LOCAL.get() == null ? new HashMap<>() : EXTEND_THREAD_LOCAL.get();
    }


    private static final ThreadLocal<List<DiffDTO>> DIFF_DTO_LIST_THREAD_LOCAL = new NamedThreadLocal<>("ThreadLocal DiffDTOList");

    public static List<DiffDTO> getDiffDTOList() {
        return DIFF_DTO_LIST_THREAD_LOCAL.get() == null ? new ArrayList<>() : DIFF_DTO_LIST_THREAD_LOCAL.get();
    }

    public static void addDiffDTO(DiffDTO diffDTO) {
        if (diffDTO != null) {
            List<DiffDTO> diffDTOList = getDiffDTOList();
            diffDTOList.add(diffDTO);
            DIFF_DTO_LIST_THREAD_LOCAL.set(diffDTOList);
        }
    }

    public static void clearDiffDTOList() {
        DIFF_DTO_LIST_THREAD_LOCAL.remove();
    }


}
