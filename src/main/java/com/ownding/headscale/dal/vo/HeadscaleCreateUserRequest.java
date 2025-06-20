package com.ownding.headscale.dal.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Headscale Create User Request DTO
 */
@Data
public class HeadscaleCreateUserRequest {

    @JSONField(name = "name")
    private String name;

    public HeadscaleCreateUserRequest() {}

    public HeadscaleCreateUserRequest(String name) {
        this.name = name;
    }
}
