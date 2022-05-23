package cn.monitor4all.logRecord.bean;


import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class BurialPointDTO {
	/**
	 * 日志唯一ID
	 */
	private String logId;
	/**
	 * 日志追踪ID
	 */
	private String traceId;
	/**
	 * 系统ID
	 */
	private String sysId;
	/**
	 * 机器信息
	 */
	private MachineDTO machine = new MachineDTO();
	/**
	 * 业务ID
	 * 支持SpEL
	 */
	private String bizId;
	/**
	 * 业务类型
	 */
	private String bizType;
	/**
	 * 方法异常信息
	 */
	private String exception;
	/**
	 * 异常详情
	 */
	private ExceptionDTO exceptionDTO = new ExceptionDTO();
	/**
	 * 日志操作时间
	 */
	private Date operateDate;
	/**
	 * 方法是否执行成功
	 */
	private Boolean success = true;
	/**
	 * 业务正常执行成功
	 */
	private Boolean usual = true;
	/**
	 * 日志内容
	 * 支持SpEL
	 */
	private String msg;
	/**
	 * 日志标签
	 */
	private String tag;
	/**
	 * 方法结果（JSON）
	 */
	private String returnStr;
	/**
	 * 方法执行时间
	 */
	private Long executionTime;
	/**
	 * 额外信息
	 * 支持SpEL
	 */
	private String extra;
	/**
	 * 自定义结构体额外信息
	 */
	private Map<String,Object> cusStructExtra;
	/**
	 * 操作人ID
	 */
	private String operatorId;
	/**
	 * 实体DIFF列表
	 */
	private List<DiffDTO> diffDTOList;

}