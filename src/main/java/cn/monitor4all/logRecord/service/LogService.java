package cn.monitor4all.logRecord.service;

import cn.monitor4all.logRecord.bean.BurialPointDTO;

public interface LogService {

    boolean createLog(BurialPointDTO burialPointDTO);

}
