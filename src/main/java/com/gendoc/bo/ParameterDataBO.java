package com.gendoc.bo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ParameterDataBO {
    @ExcelProperty("字段名")
    private String name;

    @ExcelProperty("类型")
    private String type;

    @ExcelProperty("说明")
    private String comment;

    /**
     *  参数颜色标签
     */
    // 忽略
    @ExcelIgnore
    private short colorTag;

    public ParameterDataBO() {
    }

    public ParameterDataBO(String name, String type, String comment, short colorTag) {
        this.name = name;
        this.type = type;
        this.comment = comment;
        this.colorTag = colorTag;
    }
}
