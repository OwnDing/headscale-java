package com.ownding.headscale.dal.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class HeadscalePreAuthKey {

    @JSONField(name = "id")
    private String id;

    @JSONField(name = "key")
    private String key;

    @JSONField(name = "user")
    private String user;

    @JSONField(name = "reusable")
    private Boolean reusable;

    @JSONField(name = "ephemeral")
    private Boolean ephemeral;

    @JSONField(name = "used")
    private Boolean used;

    @JSONField(name = "expiration")
    private String expiration;

    @JSONField(name = "created_at")
    private String createdAt;

    @JSONField(name = "updated_at")
    private String updatedAt;

    @JSONField(name = "acl_tags")
    private List<String> aclTags;
}
