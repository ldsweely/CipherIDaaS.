package com.portal.commons;

import org.apache.commons.lang.StringUtils;

public class ReturnMsg {
    public static final String EMAILSENDSUCCESS="邮件发送成功";

    public static final String EMAILSENDFAILED="邮件发送失败";

    public static final String APPROVALSUCCESS="账号注册审批执行成功";

    public static final String APPROVALFAILED="账号注册审批执行失败";

    public static final String APPROVALEMAIL="请先开启邮件通知服务";

    public static final String FORGETFAILED = "忘记密码中邮箱验证和手机验证必须开启其一";

    public static final String SELECTNO ="无符合条件数据";

    public static final String CHECKUSERACCOUNTNUMBER = "主账号发生重复！";

    public static final String CHECKUSERMAIL = "邮箱发生重复！";

    public static final String CHECKUSERTELEPHONE = "手机号发生重复！";

    public static final String ADDATTRIBUTEFAILED = "属性添加失败！";

    public static final String ADDATTRIBUTETOTAL = "属性最多20个！";

    public static final String ADDATTRIBUTESUCCESS = "属性添加成功";

    public static final String EDITATTRIBUTESUCCESS = "属性编辑成功";

    public static final String EDITATTRIBUTEFAILED = "属性编辑失败！";

    public static final String DELATTRIBUTESUCCESS = "属性删除成功";

    public static final String DELATTRIBUTEFAILED = "属性删除失败！";

    public static final String ATTRIBUTELISTSUCCESS = "属性列表";

    public static final String ATTRIBUTELISTFAILED = "属性列表查出失败！";

    public static final String PLATEWMAUTH = "赛赋扫码发生重复！";

    public static final String DABBYEWMAUTH = "大白扫码发生重复！";

    public static final String DINGEWMAUTH = "钉钉扫码发生重复！";

    public static final String DYNAMICSTATELISTSUCCESS = "获取列表成功";

    public static final String DYNAMICSTATELISTFAILED = "获取列表失败";

    public static final String DYNAMICSTATELISTISNULL = "用户id不能为空";

    public static final String DYNAMICSTATELISTISNOMAIL = "该用户没有绑定邮箱，请前往绑定后下发";

    public static final String DYNAMICSTATELISTISSUE = "种子密钥下发成功";

    public static final String DYNAMICSTATELISTRESET = "种子密钥重置成功";

    public static final String DYNAMICSTATELISTORG= "组织结构获取成功";

    public static final String DYNAMICSTATELISTBATCHFAILED = "种子密钥批量下发失败";

    public static final String EQUIPLISTSUCCESS = "获取列表成功";

    public static final String EQUIPPARAMETERSMISS = "参数缺失！";

    //门户认证设置
    public static String getDoorAuthMsg(Integer code){
        return getPromptResponse(code,"门户认证设置");
    }

    //管理后台认证设置
    public static String getExtmanAuthMsg(Integer code){
        return getPromptResponse(code,"管理后台认证设置");
    }

    //无线认证设置
    public static String getWireAuthMsg(Integer code){
        return getPromptResponse(code,"无线认证设置");
    }

    //身份认证策略
    public static String getAuthStrategyMsg(Integer code){
        return getPromptResponse(code,"身份认证策略");
    }

    //邮件基本通知配置
    public static String getEmailMsg(Integer code){
        return getPromptResponse(code,"邮件基本通知配置");
    }

    //邮件通知随机码配置
    public static String getRandEmailMsg(Integer code){
        return getPromptResponse(code, "邮件通知随机码配置");
    }

    //邮件通知开通账号配置
    public static String getAcEmailMsg(Integer code){
        return getPromptResponse(code, "邮件通知开通账号配置");
    }

    //portal登录页配置
    public static String getDoorPageMsg(Integer code){
        return getPromptResponse(code, "portal登录页配置");
    }

    //portal首页配置
    public static String getApplicationPageMsg(Integer code){
        return getPromptResponse(code, "应用列表页面");
    }

    //管理后台登录页配置
    public static String getManagePageMsg(Integer code){
        return getPromptResponse(code, "管理后台登录页配置");
    }
    public static String getTitleTag(Integer code){
        return getPromptResponse(code, "页面标题LOGO配置");
    }

    //账号注册审批列表
    public static String getApprovalListMsg(Integer code){
        return getPromptResponse(code, "账号注册审批列表");
    }

    //账号注册审批记录
    public static String getRecordsListMsg(Integer code){
        return getPromptResponse(code, "账号注册审批记录");
    }

    //vpn设备信息
    public static String getVpnConfigListMsg(Integer code){
        return getPromptResponse(code,"设备信息");
    }

    //下发种子密钥邮件配置
    public static String getIssueSeedkey(Integer code){
        return getPromptResponse(code,"下发种子密钥邮件配置");
    }


    public static String getPromptResponse(Integer code,String msg){
        if(StringUtils.isNotEmpty(msg)){
            switch (code){
                case 0:
                    return msg+"更新成功！";
                case 1:
                    return msg+"更新失败！";
                case 2:
                    return msg+"修改失败！";
                case 3:
                    return msg+"插入失败！";
                case 4:
                    return msg+"成功查出！";
                case 5:
                    return msg+"查出失败！";
            }
        }
        return "请给出正确的值！";
    }
}
