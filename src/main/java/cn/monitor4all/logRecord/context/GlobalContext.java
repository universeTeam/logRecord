package cn.monitor4all.logRecord.context;

import cn.monitor4all.logRecord.bean.MachineDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author shaozhengmao
 * @create 2022-05-23 21:26
 * @desc
 */
@Slf4j
@Component
public class GlobalContext {

   public static MachineDTO machineDTO;


    @PostConstruct
    public void init(){
        try {
            final String hostAddress = InetAddress.getLocalHost().getHostAddress();
            machineDTO = new MachineDTO();
            machineDTO.setHost(hostAddress);
        } catch (UnknownHostException e) {
            log.error("获取机器IP地址失败");
        }
    }
}
