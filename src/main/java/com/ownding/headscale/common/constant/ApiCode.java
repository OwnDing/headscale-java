package com.ownding.headscale.common.constant;

public enum ApiCode {
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "非法参数"),
    SERVER_ERROR(500, "服务器内部错误"),
    UPLOAD_FILE_EXCEED_MAX(600, "上传文件超过最大限制限制"),
    FAIL(999, "操作失败"),

    //account
    LOGIN_FAILED(1000, "用户名或密码错误"),
    FAILED_COUNT_EXCEED(1001, "重试已超过最大次数，请联系管理员或24小时后重试"),
    PASSWD_NOT_CORRECT(1002, "密码错误"),
    USERNAME_ALREADY_EXIST(1003, "用户已存在"),
    CUSTOMER_ADMIN_ALREADY_EXIST(1004, "已存在客户管理员"),
    CUSTOMER_OPERATOR_EXCEED_MAX(1005, "操作员个数已达最大"),
    USER_NOT_EXIST(1006, "用户不存在"),
    INVALID_SESSION(1007, "session过期或在其它设备登录"),
    USER_DELETE_FAILED(1008, "用户删除失败"),
    USER_VPN_CONFIG_NOT_EXIST(1009, "用户VPN配置不存在"),
    USER_HOST_IP_WRONG(1010, "IP格式错误"),
    USER_NOT_CUSTOMER_ADMIN(1011, "该用户不是租户管理员"),

    // license
    LICENSE_NOT_FOUND(1100,"授权文件未找到或授权已过期"),

    //device
    DEVICE_ALREADY_EXIST(2000, "设备已录入"),
    DEVICE_FILE_TYPE_NOT_SUPPORT(2001, "文件格式不支持"),
    DEVICE_FILE_PARSE_ERROR(2002, "文件解析出错"),
    DEVICE_FILE_EXCEED_MAX_COUNT(2003, "导入设备条数超过最大值"),
    DEVICE_FILE_DUP_SN(2004, "设备重复"),
    DEVICE_NOT_EXIST(2005, "设备不存在"),
    DEVICE_ALREADY_ACTIVATED(2006, "设备已被其它客户激活"),
    DEVICE_LOG_UPLOAD_FAILED(2007, "设备日志文件上传失败"),
    DEVICE_LOG_DOWNLOAD_FAILED(2008, "设备日志文件下载失败"),
    DEVICE_UPGRADE_COUNT_EXCEED_MAX(2009, "一次更新不能超过100台"),
    DEVICE_AUTH_FAILED(2010, "设备认证失败"),
    DEVICE_LOG_NOT_EXIST(2011, "设备日志不存在"),
    DEVICE_IS_IN_USE(2012, "设备已在使用中"),
    DEVICE_IS_ONLINE(2013, "设备当前在线中"),
    TRUE_IP_EXIST(2014, "该IP已被使用"),

    //firmware
    FIRMWARE_VALID_FAILED(3000, "文件校验失败"),
    FIRMWARE_ALREADY_EXIST(3001, "同名固件已存在"),
    FIRMWARE_UPLOAD_FAILED(3002, "固件上传失败"),
    FIRMWARE_NOT_EXIST(3003, "固件不存在"),
    FIRMWARE_DOWNLOAD_FAILED(3004, "固件下载失败"),
    FIRMWARE_VERSION_ALREADY_EXIST(3005, "固件版本已存在"),
    FIRMWARE_VERSION_FORMAT_WRONG(3006, "固件版本格式错误。正确格式例如:v1.0"),
    FIRMWARE_VALID_USER(3007, "当前登录用户无权限操作"),

    //config
    CONFIG_NOT_EXIST(4000, "配置不存在"),
    CONFIG_ALREADY_EXIST(4001, "同名配置已存在"),
    CONFIG_UPLOAD_FAILED(4002, "配置上传失败"),
    CONFIG_DOWNLOAD_FAILED(4003, "配置下载失败"),
    CONFIG_VALID_USER(4004, "当前登录用户无权限操作"),


    //mqtt
    MQTT_OPERATE_FAILED(5001, "消息发送失败"),

    SYSCONFIG_NOT_EXIST(6000, "系统配置项不存在"),

    NO_AUTHORITY(7000, "无权限"),

    FILE_FORMAT_NOT_CORRECT(8000, "文件格式不正确"),

    //device log
    FILE_NOT_EXIST(9000, "文件不存在"),
    NO_FILE(9001, "无日志文件"),

    LICENSE_LIMITER_EXCEED(9002, "用户租户数量已达上限"),
    ;



    ApiCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public final int code;
    public final String message;
}