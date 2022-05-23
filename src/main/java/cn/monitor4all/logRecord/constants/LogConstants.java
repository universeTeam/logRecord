package cn.monitor4all.logRecord.constants;

public interface LogConstants {

    interface Public {
        String RESET_KEY =  "reset";
    }

    interface DataPipeline {

        String ROCKET_MQ = "rocketMq";

        String RABBIT_MQ = "rabbitMq";

        String STREAM = "stream";
    }
}
