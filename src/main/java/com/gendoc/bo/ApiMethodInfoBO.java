package com.gendoc.bo;

import lombok.Data;

import java.util.List;

@Data
public class ApiMethodInfoBO {

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 方法注释描述（Javadoc 摘要）
     */
    private String methodDesc;

    /**
     * 返回值
     */
    private ParameterInfoBO returnParam;

    /**
     * 方法参数列表，每个参数包含参数名、类型、注释及内部字段信息
     */
    private List<ParameterInfoBO> parameters;
}
