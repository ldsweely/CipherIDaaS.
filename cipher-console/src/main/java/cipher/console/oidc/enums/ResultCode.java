package cipher.console.oidc.enums;

public enum ResultCode {

    SUCCESS(0,"成功"),

    STRATEGY_IS_FRAGMENTARY(1,"信息不完整"),

    STRATEGY_STUFF_AUTH_TYPE_CAN_NOT_BE_ACCEPTED(2,"不支持的认证类型"),

    STRATEGY_STUFF_AUTH_SOURCE_CAN_NOT_BE_ACCEPTED(3,"无法识别的认证源"),

    STRATEGY_STUFF_DYNAMIC_FACTOR_MUST_BE_OPEN(4,"认证方式为用户名时，双因子认证必须开启"),

    STRATEGY_STUFF_CIPHER_AUTH_STATUS_ERROR(5,"赛赋认证开关状态异常"),

    STRATEGY_STUFF_MAC_STATUS_ERROR(6,"MAC认证状态开关异常"),

    STRATEGY_STUFF_TIME_UTIL_ERROR(7,"时间单位异常"),

    STRATEGY_STUFF_TIME_RANGE_ERROR(8,"时间长度不得小于0"),

    STRATEGY_STUFF_UPDATE_FAILED(9,"WIFI策略更新失败"),

    STRATEGY_VISITOR_SOURCE_TYPE(10,"无法识别的访客认证源"),

    STRATEGY_VISITOR_TIME_RANGE_ERROR(11,"访客有效时长异常"),

    STRATEGY_VISITOR_AUTH_TYPE_ERROR(12,"不支持的访客认证类型"),

    STRATEGY_VISITOR_NEED_STAFF_INFO_ERROR(13,"被访人信息校验开关状态异常"),

    STRATEGY_VISITOR_NEED_SCAN_INFO_ERROR(14,"被访人协助扫码开关状态异常"),

    STRATEGY_MAC_DEVICE_BINDING_ERROR(15,"MAC绑定设备数量信息错误"),

    TOKEN_CHECKED_FAILD(16,"Token校验失败"),

    COMPANY_INSERT_ERROR(17,"公司信息插入失败");



    private int code;

    private String message;

    ResultCode(int code,String message){
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
