package cn.monitor4all.logRecord.service;

import cn.monitor4all.logRecord.bean.BurialPointDTO;

public interface IOperationLogGetService {

    /**
     * 自定义日志监听
     * @param burialPointDTO 日志传输实体
     */
    void createLog(BurialPointDTO burialPointDTO);

}
