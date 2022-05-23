package cn.monitor4all.logRecord.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * @author shaozhengmao
 * @create 2022-05-23 20:47
 * @desc
 */
@Data
public class ExceptionDTO implements Serializable {

    private String describe;

    private String className;

    private String rootFrame;

    private String subFrame;


}
