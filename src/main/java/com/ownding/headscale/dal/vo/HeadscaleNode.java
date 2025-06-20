package com.ownding.headscale.dal.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class HeadscaleNode {

    @JSONField(name = "id")
    private String id;

    @JSONField(name = "machine_key")
    private String machineKey;

    @JSONField(name = "node_key")
    private String nodeKey;

    @JSONField(name = "disco_key")
    private String discoKey;

    @JSONField(name = "ip_addresses")
    private List<String> ipAddresses;

    @JSONField(name = "name")
    private String name;

    @JSONField(name = "user")
    private HeadscaleUser user;

    @JSONField(name = "last_seen")
    private String lastSeen;

    @JSONField(name = "last_successful_update")
    private String lastSuccessfulUpdate;

    @JSONField(name = "expiry")
    private String expiry;

    @JSONField(name = "preAuthKey")
    private HeadscalePreAuthKey preAuthKey;

    @JSONField(name = "created_at")
    private String createdAt;

    @JSONField(name = "updated_at")
    private String updatedAt;

    @JSONField(name = "online")
    private Boolean online;

    @JSONField(name = "invalid")
    private Boolean invalid;

    @JSONField(name = "given_name")
    private String givenName;

    @JSONField(name = "forced_tags")
    private List<String> forcedTags;

    @JSONField(name = "registerMethod")
    private String registerMethod;

    @JSONField(name = "invalidTags")
    private List<String> invalidTags;

    @JSONField(name = "validTags")
    private List<String> validTags;
}
