package com.portal.enums;

/**
 * 认证结果类
 * 当且仅当响应码为0时是成功，其余的都是失败
 * create by shizhao at 2019/4/16
 *
 * @author shizhao
 * @since 2019/4/16
 */
public enum ResultCode {

    SUCCESS(0, "成功"),


    LOGIN_INFO_NOT_COMPLETE(101, "登录信息不完整"),

    LOGIN_AUTH_FAILED(102, "认证失败"),

    LOGIN_FAILED_NOT_BINDING(103, "认证失败,该账号未绑定"),


    QR_CODE_IS_NOT_SUPPORT_NOW(201, "请求二维码失败"),

    QR_CODE_IS_OUT_OF_DATE(202, "二维码已经过期"),

    QR_CODE_IS_WAITING_FOR_SCAN(203, "等待扫描二维码"),


    ON_LINE_VISITOR_NOT_FOUND(301, "没有找到您的认证信息"),


    PARAMETER_FAILURE(400, "参数错误"),

    RADIUS_AUTH_DID_NOT_RESPONSE(401, "认证服务没有响应"),

    RADIUS_AUTH_THROW_AN_ERROR(402, "认证过程发生了意外,请重试"),

    LOGIN_PHONE_NOT_REGISTER(403, "认证的手机号尚未注册"),

    LOGIN_COLLECT_USER_NOT_EXIST(404, "无法录入该用户的手机号"),

    LOGIN_COLLECT_USER_HAS_REGISTER_PHONE(405, "用户已经绑定了一个手机号"),

    LOGIN_COLLECT_USER_PHONE_HAS_BEEN_REGISTER(406, "该手机号已经被注册"),

    LOGIN_COLLECT_UPDATE_FAILED(407, "服务器出现了一个错误"),

    LOGIN_SECOND_NOT_MATCHED(408, "请用绑定的账户进行扫码"),

    PORTAL_SYSTEM_ERROR(500, "内部服务器错误"),

    PORTAL_AUTH_CONFIG_NOT_FOUND(501, "未正确配置portal认证"),

    BINDDING_MATCHING_FAILURE(601, "用户匹配失败，请绑定已有账号或注册新账号"),

    BINDING_AUTH_FAILURE(602, "绑定失败，请输入正确的用户名密码"),

    BINDING_EXITS_ERROR(603, "绑定失败，该账号已被绑定"),


    NOT_BINGDING(604, "未绑定本地账号，请使用扫码登录进行绑定"),

    NOT_BINDING_ON(606, "绑定失败,自动绑定未开启"),

    AUTO_BINDING_ERROR(605, "自动绑定失败"),

    BIND_ERROR_PHONE_NULL(618, "绑定失败手机号为空，请使用账号密码登录"),

    BIND_ERROR_MAIL_NULL(607, "绑定失败邮箱为空，请使用账号密码登录"),

    BIND_ERROR_NOT_ACCOUNT(608, "绑定失败，用户不存在"),

    BIND_FLOW_NOT_EXIST(609, "请联系管理员，未设置绑定流程"),

    BIND_FLOW_NOT_AUTH_ACCOUNT(610, "绑定失败，验证未通过"),

    BIND_FLOW_NOT_AUTH_PHONE(611, "绑定失败，手机验证未通过"),

    BIND_FLOW_NOT_AUTH_MAIL(612, "绑定失败，邮箱验证未通过"),

    BIND_FLOW_NOT_CONFIG(613, "未正确配置绑定信息"),

    BIND_FLOW_CAN_NOT_FOUND(614, "没有找到任何绑定配置"),

    BIND_FLOW_MAIL_NOT_REGISTER(619, "邮箱尚未绑定"),

    DING_BIND_RESPONSE_ERROR(615, "钉钉响应失败"),

    PHONE_CODE_CHECK_FAILED(616, "验证码校验失败"),

    BIND_FLOW_NOT_COMPLETE(617, "无法完成绑定,请检查是否已正确进行完整身份验证或验证信息是否被更改"),

    REGIST_EXIST_FAILURE(701, "该账号已存在请绑定"),

    REGIST_ERROR(702, "未知错误，请联系管理员"),

    MSG_SEND_ERROR(801, "短信发送失败，请稍后重试"),

    REGIST_EXIST(802, "注册信息已存在"),

    REGIST_EXIST_LOCAL(803, "该账号已存在，请进行绑定"),

    REGIST_FAILD_BY_MAIL(804, "注册失败，邮箱未认证"),

    REGIST_FAILD_BY_PHONE(805, "注册失败，手机未认证"),

    PHONE_MAIL_ERROR(901, "手机或邮箱必须验证一个"),

    IDENTITY_NULL(1000, "身份设置为空"),

    AUTH_FAILED_DIFFERENT(1001, "修改密码失败，手机号与本地账号不一致"),


    LOGIN_CONFIG_NOT_COMPLETE(1101, "未正确配置登录项"),

    PASSWORD_CONFIG_NOT_COMPLETE(1102, "密码策略未正确配置"),

    PASSWORD_CAN_NOT_BE_ACCEPT(1103, "密码不符合规则"),

    PASSWORD_RESET_FAILED(1104, "更新密码遇到了一个未知的错误"),

    USER_IS_NOT_EXIST(1105, "用户不存在"),

    PASSWORD_RESET_NOT_COMPLETE(1106, "暂未设置忘记密码的流程"),

    PASSWORD_RESET_NOT_COMPLETE_AUTH(1107, "未完成全部认证流程"),

    PASSWORD_RESET_FAILED_PASSWORD_NOT_ALLOWED(1108, "更新密码失败,请检查密码是否合法"),

    USER_REGISTER_MEET_AN_EXCEPTION(1109, "用户注册登记遇到一个异常"),
    //TODO 状态码冲突
    USER_NO_LIMIT(1114, "用户登录子应用权限限制"),

    USER_NO_SUB(1111, "用户子账号不存在"),

    APPLICATION_NO_URL(1112, "该应用未添加"),

    APPLICATION_NO_SKIP_URL(1113, "应用跳转地址不存在"),

    DING_TALK_INFO_NOT_FOUND(1110, "没有钉钉的配置信息，无法获取钉钉二维码"),

    PHONE_IS_EXIST(1120, "手机号已经被其他人使用"),

    MAIL_IS_EXIST(1121, "邮箱已经被其他人使用"),

    ACCOUNT_STOPED(1122, "该账号已停用"),

    ACCOUNT_LOCAK(1123, "该账号已锁定"),

    LOGIN_COLLECT_USER_MAIL_HAS_BEEN_REGISTER(1124, "该邮箱已经被注册"),

    LOGIN_COLLECT_USER_HAS_REGISTER_MAIL(1125, "用户已经绑定了一个邮箱号"),

    APPLICATION_NO_EXIT(1126, "应用不存在"),

    APPLICATION_RULE_NOT_EXIT(1127, "应用子账号规则未配置"),

    APPLICATION_NO_ADD(1130, "应用未添加"),

    APPLICATION_NO_APPLICATIONURL(1131,"该应用未设置应用路径"),

    COMPANY_UUID(3001,"公司uuid不存在"),

    SIGN_ERROR(3001,"签名有误"),

    NO_AUTH_AUTHORIZATION(3002,"您目前没有该系统的权限，请联系管理员!"),

    CAS_AUTH_URL_NULL(4001,"Cas认证页面地址未配置"),

    RDSG_AUTH_URL_NULL(4002,"RDSG认证页面地址未配置"),

    CAS_OR_RDSG_AUTH_URL_NULL(4003,"RDSG或Cas的认证地址为空"),

    DING_PUSH_FAILE(4004, "钉钉push失败"),

    DING_PUSH_WAIT(4005, "钉钉push等待中"),

    DING_PUSH_TIMEOUT(4006, "钉钉push超时"),

   SUB_NAME_RULE_ERROR(4007, "子账号配置了规则，信息有误"),

    USER_NO_SUBPWD(4008, "用户子账号密码不存在"),

    SUB_NAME_RULE_ERROE(4009, "用户子账号配置了规则，信息有误"),

    OTP_DYNAMEIC_PASSWORD_ERROE(4010, "用户动态秘钥不存在"),

    APP_INFO_LOST(4011, "应用信息缺失"),
    WEIXIN_CONFIG_INFO_NOT_FOUND(4012, "没有微信的配置信息，无法获取微信二维码"),

    NC_RESMAP_CODE(5001,"NC请求失败"),

    NC_AUTH_CODE(5002,"NC认证失败"),

    NC_CONS_PARAMS_CODE(5003,"NC构建返回参数失败"),

    ACCESS_TOKEN_NOT_ESIXT(5004, "用户token不存在"),

    SAMLPARAMS_IS_NULL(6001,"saml请求携带参数为空"),

    SAML_REGIST_IS_ERROR(6002,"saml校验发生错误，请检查携带参数是否符合标准"),

    SAML_RETURN_ERROR(6003,"saml返回断言失败"),

    TOKEN_CHECKED_FAILD(7001,"签名校验失败无法进行单点登录")
    ;

    private int code;

    private String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

