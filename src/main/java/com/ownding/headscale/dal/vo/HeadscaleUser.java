package com.ownding.headscale.dal.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class HeadscaleUser {

    @JSONField(name = "id")
    private String id;

    @JSONField(name = "name")
    private String name;

    @JSONField(name = "created_at")
    private String createdAt;

    @JSONField(name = "updated_at")
    private String updatedAt;

    @JSONField(name = "displayName")
    private String displayName;

    @JSONField(name = "email")
    private String email;

    @JSONField(name = "providerId")
    private String providerId;

    @JSONField(name = "provider")
    private String provider;

    @JSONField(name = "profilePicUrl")
    private String profilePicUrl;
}
