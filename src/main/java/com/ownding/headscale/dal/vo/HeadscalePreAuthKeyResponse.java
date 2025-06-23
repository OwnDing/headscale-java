package com.ownding.headscale.dal.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Headscale PreAuth Key API 响应包装类
 * 用于解析 Headscale API 返回的包装格式
 */
@Data
public class HeadscalePreAuthKeyResponse {

    @JSONField(name = "preAuthKey")
    private HeadscalePreAuthKey preAuthKey;
}
