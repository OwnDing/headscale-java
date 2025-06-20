package com.ownding.headscale.dal.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * Generic Headscale API Response wrapper
 */
@Data
public class HeadscaleApiResponse<T> {

    @JSONField(name = "user")
    private T user;

    @JSONField(name = "users")
    private List<T> users;

    @JSONField(name = "node")
    private T node;

    @JSONField(name = "nodes")
    private List<T> nodes;

    @JSONField(name = "preAuthKey")
    private T preAuthKey;

    @JSONField(name = "preAuthKeys")
    private List<T> preAuthKeys;

    @JSONField(name = "policy")
    private T policy;

    // Generic getter for single item responses
    public T getItem() {
        if (user != null) return user;
        if (node != null) return node;
        if (preAuthKey != null) return preAuthKey;
        if (policy != null) return policy;
        return null;
    }

    // Generic getter for list responses
    public List<T> getItems() {
        if (users != null) return users;
        if (nodes != null) return nodes;
        if (preAuthKeys != null) return preAuthKeys;
        return null;
    }
}
