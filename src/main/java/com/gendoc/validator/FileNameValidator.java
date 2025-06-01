package com.gendoc.validator;

import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.ui.InputValidatorEx;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

/**
 * 文件名校验
 */
@Slf4j
public class FileNameValidator implements InputValidatorEx {

    @Override
    public boolean checkInput(String inputString) {
        if (StrUtil.isBlankIfStr(inputString)) {
            log.warn("文件名不能为空");
            return false;
        }

        try {
            // 尝试作为路径的一部分进行验证
            Paths.get("temp", inputString + ".xlsx");
            return true;
        } catch (InvalidPathException e) {
            log.warn("文件名无效：" + inputString);
            return false;
        }
    }

    @Override
    public boolean canClose(String inputString) {
        return checkInput(inputString);
    }

    @Override
    public String getErrorText(String inputString) {
        if (inputString == null || inputString.trim().isEmpty()) {
            return "文件名不能为空";
        }

        try {
            Paths.get("temp", inputString + ".xlsx");
            return null; // no error
        } catch (InvalidPathException e) {
            return "文件名包含非法字符";
        }
    }
}
