package com.ownding.headscale.dal.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * Headscale Create PreAuth Key Request DTO
 */
@Data
public class HeadscaleCreatePreAuthKeyRequest {

    @JSONField(name = "user")
    private String user;

    @JSONField(name = "reusable")
    private Boolean reusable;

    @JSONField(name = "ephemeral")
    private Boolean ephemeral;

    @JSONField(name = "expiration")
    private String expiration;

    @JSONField(name = "acl_tags")
    private List<String> aclTags;

    public HeadscaleCreatePreAuthKeyRequest() {}

    public HeadscaleCreatePreAuthKeyRequest(String user, Boolean reusable, Boolean ephemeral) {
        this.user = user;
        this.reusable = reusable;
        this.ephemeral = ephemeral;
    }
}
