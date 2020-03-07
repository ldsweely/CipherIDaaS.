package com.portal.commons;

/**
 * @Author: cozi
 * @Date: 2019-04-29
 */
public class NewGlobalReturnCode {

    public static final String RETURN_CODE = "return_code";

    public static final String RETURN_MSG = "return_msg";

    public enum MsgCodeEnum {
        SUCCESS(0, "操作成功!"),
        FAILURE(1, "操作失败!"),
        AUTHUSER_SUCCESS(2, "账号状态认证成功,但企业过期"),
        USER_NOT_EXIST(3, "该用户不存在!"),
        ACCOUNT_STATUS_IVALIDE(4, "账号被停用!"),
        AUTH_OUT_OF_DATE(5, "账号授权已过期,或者还未到启用时间!"),
        AUTH_DATE_NOT_ALLOW(6, "该账号没有设置授权有效期!"),
        AUTH_CAUSE_EXCEPTION(7, "认证过程发生异常!"),
        AUTH_USERNAME_PWD_FAILURE(8, "用户名和密码认证失败!"),
        AUTH_USERNAME_TOTP_FAILURE(9, "totp不正确!"),
        INFO_NOT_COMPLETE(10, "必要信息缺失!"),
        USER_ARE_FREEZED(11, "账号被冻结!"),
        USER_ARE_DELETED(12, "该用户已被删除"),
        USER_INFO_NOT_COMPLETE(13,"信息缺失!"),
        AD_PWD_NOT_STRONG_ENOUGH(14,"密码太弱!");

        private int code;
        private String msg;

        MsgCodeEnum(int _code, String _msg) {
            this.code = _code;
            this.msg = _msg;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

    }


}
