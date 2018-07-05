package com.binfo.monitor.log.point;

import com.binfo.alex.base.system.service.impl.UserServiceImpl;

import com.binfo.monitor.log.dao.base.LoggerDao;
import com.binfo.monitor.log.model.LogRecord;
import com.binfo.monitor.log.model.SysLog;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import tgtools.util.DateUtil;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * AOP配置
 * <p>
 * Created by shi.xiulong
 */

@Component
@Aspect
public class LogPoint {


    @Resource
    private LoggerDao dao;

    @Pointcut("@annotation(com.binfo.monitor.log.model.LogRecord)")
    public void pointCut() {
    }

    /***
     * 方法执行完成后拦截
     * @param joinPoint
     */
    @AfterReturning("pointCut()")
    public void afterReturningLog(JoinPoint joinPoint) {
        try {
            SysLog log = null;
            String identif = getParamterValue(joinPoint, "identif");
            String desc = getParamterValue(joinPoint, "desc");
            String object = getParamterValue(joinPoint,"object");
            String module = getParamterValue(joinPoint,"module");
            String user = getParamterValue(joinPoint,"user");
            Object identifValue = getIdentif(joinPoint, identif);
            Object userValue = getIdentif(joinPoint, user);
            Object descValue = getIdentif(joinPoint, desc);
            Object objectValue = getIdentif(joinPoint,object);
            Object moduleValue = getIdentif(joinPoint,module);
            String[] arrDesc = objectDataFormact(descValue);
            String[] arrObj = objectDataFormact(identifValue);

            if (arrDesc!=null&&arrObj!=null){
                for (int i=0;i<(arrDesc.length>=arrObj.length?arrDesc.length:arrObj.length);i++){
                    log = new SysLog();
                    if (arrDesc!=null&&i<arrDesc.length&&arrDesc[i]!=null){
                        log.setHandleDescription(arrDesc[i]);
                    }
                    if (arrObj!=null&&i<arrObj.length&&arrObj[i]!=null){
                        log.setHandleObjectId(arrObj[i]);
                    }
                    log.setHandlePerson(userValue!=null?userValue.toString():user);
                    log.setHandleObject(objectValue!=null?objectValue.toString():object);
                    log.setHandleModule(moduleValue!=null?moduleValue.toString():module);
                    insert(joinPoint, log);
                }
            }else if (arrDesc!=null||arrObj!=null){
                int arrObjSize = arrObj!=null?arrObj.length:0;
                int arrDescSize = arrDesc!=null?arrDesc.length:0;
                for (int i=0;i<(arrObjSize>=arrDescSize?arrObjSize:arrDescSize);i++){
                    log = new SysLog();
                    if (arrDesc!=null&&i<arrDesc.length&&arrDesc[i]!=null){
                        log.setHandleDescription(arrDesc[i]);
                    }else {
                        log.setHandleDescription(descValue!=null?descValue.toString():desc);
                    }
                    if (arrObj!=null&&i<arrObj.length&&arrObj[i]!=null){
                        log.setHandleObjectId(arrObj[i]);
                    }else {
                        log.setHandleObjectId(identifValue!=null?identifValue.toString():identif);
                    }
                    log.setHandlePerson(userValue!=null?userValue.toString():user);
                    log.setHandleObject(objectValue!=null?objectValue.toString():object);
                    log.setHandleModule(moduleValue!=null?moduleValue.toString():module);
                    insert(joinPoint, log);
                }
            }else {
                log = new SysLog();
                log.setHandlePerson(userValue!=null?userValue.toString():user);
                log.setHandleDescription(descValue!=null?descValue.toString():desc);
                log.setHandleObject(objectValue!=null?objectValue.toString():object);
                log.setHandleObjectId(identifValue != null ? identifValue.toString() : identif);
                log.setHandleModule(moduleValue!=null?moduleValue.toString():module);
                insert(joinPoint, log);
            }

        } catch (Exception e) {
            System.out.println("-----日志报错-----");
            e.printStackTrace();
        }
    }

    /***
     * ArrayList对象转数组
     * @param object
     * @return
     */
    public String[] objectDataFormact(Object object){
        String[] array =null;
        if (object != null && object.getClass().isArray()) {
            array = (String[]) object;
        } else if (object instanceof ArrayList) {
            array = new String[((List) object).size()];
            ((List) object).toArray(array);
        }
        return array;
    }

    /***
     * 插入日志
     * @param joinPoint
     * @param log
     * @throws Exception
     */
    public void insert(JoinPoint joinPoint, SysLog log) throws Exception {
        try {
            InetAddress address = InetAddress.getLocalHost();
            String hostAddress = address.getHostAddress();
            log.setLogId(UUID.randomUUID().toString());
            log.setHandleIp(hostAddress);
            log.setHandleTime(DateUtil.formatLongtime(new Date()));
            log.setHandleType(getParamterValue(joinPoint, "type"));
            dao.insert(log);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /***
     * 根据注解参数名称获取对应方法中实际参数值
     * @param joinPoint
     * @param identif
     * @return
     * @throws Exception
     */
    public Object getIdentif(JoinPoint joinPoint, String identif) throws Exception {
        String targetName = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] arguments = joinPoint.getArgs();
        Class clazz = Class.forName(targetName);
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methodName.equals(methods[i].getName())) {
                String[] params = u.getParameterNames(methods[i]);
                for (int j = 0; j < params.length; j++) {
                    if (identif.equals(params[j])) {
                        if (arguments[j] != null || !"".equals(arguments[j])) {
                            return arguments[j];
                        }
                    }
                }
            }
        }
        return null;
    }

    /***
     * 根据注解参数名称获取值
     * @param joinPoint
     * @param item
     * @return
     * @throws Exception
     */
    public String getParamterValue(JoinPoint joinPoint, String item) throws Exception {
        String targetName = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] arguments = joinPoint.getArgs();
        Class targetClass = Class.forName(targetName);
        Method[] method = targetClass.getMethods();
        String methode = "";
        for (Method m : method) {
            if (m.getName().equals(methodName)) {
                Class[] tmpCs = m.getParameterTypes();
                if (tmpCs.length == arguments.length) {
                    LogRecord methodCache = m.getAnnotation(LogRecord.class);
                    if (methodCache != null) {
                        switch (item) {
                            case "object":
                                methode = methodCache.object();
                                break;
                            case "identif":
                                methode = methodCache.identif();
                                break;
                            case "type":
                                methode = methodCache.type();
                                break;
                            case "module":
                                methode = methodCache.module();
                                break;
                            case "desc":
                                methode = methodCache.desc();
                                break;
                            case "user":
                                methode = methodCache.user();
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                }
            }
        }
        return methode;
    }

}
