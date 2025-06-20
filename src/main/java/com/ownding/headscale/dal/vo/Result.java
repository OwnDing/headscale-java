package com.ownding.headscale.dal.vo;

import com.ownding.headscale.common.constant.ApiCode;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class Result implements Serializable {
    private static final long serialVersionUID = -3046202914042439812L;

    private int code;
    private String message;
    private Object data;

    public Result(ApiCode apiCode, Object data) {
        this.code = apiCode.code;
        this.message = apiCode.message;
        this.data = data;
    }


    public static Result success() {
        return success(null);
    }

    public static Result success(Object data) {
        return new Result(ApiCode.SUCCESS, data);
    }


    public static Result toResult(ApiCode apiCode) {
        return toResult(apiCode, null);
    }
    public static Result toResult(ApiCode apiCode, Object data) {
        return new Result(apiCode, data);
    }

}
