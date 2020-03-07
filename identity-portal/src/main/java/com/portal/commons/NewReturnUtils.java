package com.portal.commons;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: cozi
 * @Date: 2019-04-29
 */
public  class NewReturnUtils {

    public static Map<String, Object> failureResponse(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put(NewGlobalReturnCode.RETURN_CODE, NewGlobalReturnCode.MsgCodeEnum.FAILURE.getCode());
        map.put(NewGlobalReturnCode.RETURN_MSG, msg);
        return map;
    }

    public static Map<String, Object> failureResponse(int code, String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put(NewGlobalReturnCode.RETURN_CODE, code);
        map.put(NewGlobalReturnCode.RETURN_MSG, msg);
        return map;
    }

    public static Map<String, Object> successResponse() {
        Map<String, Object> map = new HashMap<>();
        map.put(NewGlobalReturnCode.RETURN_CODE, NewGlobalReturnCode.MsgCodeEnum.SUCCESS.getCode());
        map.put(NewGlobalReturnCode.RETURN_MSG, NewGlobalReturnCode.MsgCodeEnum.SUCCESS.getMsg());
        return map;
    }

    public static Map<String, Object> successResponse(NewGlobalReturnCode.MsgCodeEnum msgCodeEnum) {
        Map<String, Object> map = new HashMap<>();
        map.put(NewGlobalReturnCode.RETURN_CODE, msgCodeEnum.getCode());
        map.put(NewGlobalReturnCode.RETURN_MSG, msgCodeEnum.getMsg());
        return map;
    }


    public static Map<String, Object> successResponse(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put(NewGlobalReturnCode.RETURN_CODE, NewGlobalReturnCode.MsgCodeEnum.SUCCESS.getCode());
        map.put(NewGlobalReturnCode.RETURN_MSG, msg);
        return map;
    }

    public static Map<String, Object> successResponse(String key, Object val) {
        Map<String, Object> map = new HashMap<>();
        map.put(NewGlobalReturnCode.RETURN_CODE, NewGlobalReturnCode.MsgCodeEnum.SUCCESS.getCode());
        map.put(NewGlobalReturnCode.RETURN_MSG, NewGlobalReturnCode.MsgCodeEnum.SUCCESS.getMsg());
        map.put(key, val);
        return map;
    }



}
