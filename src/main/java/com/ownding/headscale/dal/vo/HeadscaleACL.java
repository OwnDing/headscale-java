package com.ownding.headscale.dal.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Headscale ACL DTO
 */
@Data
public class HeadscaleACL {

    @JSONField(name = "groups")
    private Map<String, List<String>> groups;

    @JSONField(name = "hosts")
    private Map<String, String> hosts;

    @JSONField(name = "tagOwners")
    private Map<String, List<String>> tagOwners;

    @JSONField(name = "acls")
    private List<ACLRule> acls;

    @JSONField(name = "tests")
    private List<ACLTest> tests;

    @Data
    public static class ACLRule {
        @JSONField(name = "action")
        private String action;

        @JSONField(name = "src")
        private List<String> src;

        @JSONField(name = "dst")
        private List<String> dst;

        @JSONField(name = "proto")
        private String proto;
    }

    @Data
    public static class ACLTest {
        @JSONField(name = "src")
        private String src;

        @JSONField(name = "accept")
        private List<String> accept;

        @JSONField(name = "deny")
        private List<String> deny;
    }
}
