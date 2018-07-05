package com.binfo.monitor.log.model;
import lombok.Data;

import java.util.Date;

/**
 *
 * model
 * Created by shi.xiulong
 */
@Data
public class SysLog {
    private String logId;
    private String handlePerson;
    private String handleObject;
    private String handleObjectId;
    private String handleType;
    private String handleModule;
    private String handleIp;
    private String handleTime;
    private String handleDescription;

}
