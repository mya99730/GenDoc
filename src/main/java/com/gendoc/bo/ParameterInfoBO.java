package com.gendoc.bo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ParameterInfoBO {

    /**
     * 参数名称
     */
    private String paramName;

    /**
     * 参数类型
     */
    private String paramType;

    /**
     * 参数注释
     */
    private String paramDesc;

    /**
     * 子参数
     */
    private List<ParameterInfoBO> childParamList;

    public ParameterInfoBO() {
        this.childParamList = new ArrayList<>();
    }

    public ParameterInfoBO(String paramName, String paramType, String paramDesc) {
        this.paramName = paramName;
        this.paramType = paramType;
        this.paramDesc = paramDesc;
        this.childParamList = new ArrayList<>();
    }
}
