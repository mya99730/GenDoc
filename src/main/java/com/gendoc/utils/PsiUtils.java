package com.gendoc.utils;


import cn.hutool.core.util.StrUtil;
import com.gendoc.bo.ApiMethodInfoBO;
import com.gendoc.bo.ParameterInfoBO;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PsiUtils {

    public static ApiMethodInfoBO parseMethodInfo(PsiMethod method) {
        String methodName = method.getName();
        log.info("开始解析方法：" + methodName);


        // 获取方法注释
        PsiDocComment methodDocComment = method.getDocComment();

        //  获取方法参数
        List<ParameterInfoBO> parameters = new ArrayList<>();
        // 📦 解析每个参数及其字段（支持嵌套对象）
        for (PsiParameter param : method.getParameterList().getParameters()) {
            // 形参列表
            Map<String, String> formalParamComments = getParamComments(methodDocComment, "param");
            String paramName = param.getName();
            String paramType = param.getType().getCanonicalText();
            String paramDesc = formalParamComments.getOrDefault(paramName, "");
            ParameterInfoBO parameterInfoBO = buildParameterInfoBO(method.getProject(), paramName, paramType, paramDesc);
            parameters.add(parameterInfoBO);
        }
        // 方法返回参数信息
        ParameterInfoBO returnParameterInfo = getReturnParameterInfo(method);
        ApiMethodInfoBO apiMethodInfoBO = new ApiMethodInfoBO();
        apiMethodInfoBO.setMethodName(methodName);
        apiMethodInfoBO.setMethodDesc(JavadocUtils.getExtractSummary(methodDocComment));
        apiMethodInfoBO.setReturnParam(returnParameterInfo);
        apiMethodInfoBO.setParameters(parameters);
        return apiMethodInfoBO;
    }

    private static ParameterInfoBO getReturnParameterInfo(PsiMethod method) {
        // 方法返回值为void或Void直接return
        if (null == method.getReturnType() || StrUtil.equals(method.getReturnType().getCanonicalText(), "void")) {
            log.info("方法返回值为void或Void，无需处理");
            return null;
        }

        Map<String, String> returnParamComment = getParamComments(method.getDocComment(), "return");
        String returnType = method.getReturnType().getCanonicalText();
        String returnParamDesc = returnParamComment.get("return");
        return buildParameterInfoBO(method.getProject(), "", returnType, returnParamDesc);
    }

    private static Map<String, String> getParamComments(PsiDocComment methodDocComment, String tagName) {
        Map<String, String> paramComments = new HashMap<>();
        if (methodDocComment != null) {
            // 提取 @param 注释
            for (PsiDocTag tag : methodDocComment.findTagsByName(tagName)) {
                if (StrUtil.equals("param", tagName) && tag.getDataElements().length >= 2) {
                    String paramName = tag.getDataElements()[0].getText();
                    String paramComment = tag.getDataElements()[1].getText();
                    paramComments.put(paramName, paramComment);
                }

                if (StrUtil.equals("return", tagName) && tag.getDataElements().length >= 1) {
                    String paramComment = tag.getDataElements()[0].getText();
                    paramComments.put("return", paramComment);
                }
            }
        }
        return paramComments;
    }

    /**
     * 判断是否是基本类型
     *
     * @param type
     * @return
     */
    private static boolean isPrimitiveType(String type) {
        Set<String> primitives = new HashSet<>(Arrays.asList(
                // 基本类型
                "int", "long", "double", "float", "boolean", "char", "byte", "short",
                // 包装类型（简单类名）
                "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte", "Short", "String",
                // 全限定类名版本
                "int", "java.lang.Long", "java.lang.Double", "java.lang.Float",
                "java.lang.Boolean", "java.lang.Character", "java.lang.Byte", "java.lang.Short",
                "java.lang.String"
        ));
        return primitives.contains(type);
    }

    private static List<ParameterInfoBO> parseClassFields(PsiClass psiClass) {
        List<ParameterInfoBO> fields = new ArrayList<>();
        for (PsiField field : psiClass.getFields()) {
            String fieldName = field.getName();
            String fieldType = field.getType().getCanonicalText();
            String fieldComment = getFieldComment(field);
            ParameterInfoBO fieldInfo = buildParameterInfoBO(psiClass.getProject(), fieldName, fieldType, fieldComment);
            fields.add(fieldInfo);
        }
        return fields;
    }

    private static @Nullable ParameterInfoBO buildParameterInfoBO(Project project, String fieldName, String fieldType,
                                                                  String fieldComment) {
        ParameterInfoBO fieldInfo = new ParameterInfoBO(fieldName, fieldType, fieldComment);
        if (isPrimitiveType(fieldType)) {
            log.info("参数是基本类型，参数名：{}", fieldName);
            return fieldInfo;
        }
        PsiClass fieldClass = JavaPsiFacade.getInstance(project).findClass(fieldType, GlobalSearchScope.allScope(project));
        if (null == fieldClass) {
            log.info("未找到参数类：{}", fieldType);
            return fieldInfo;
        }
        fieldInfo.getChildParamList().addAll(parseClassFields(fieldClass));
        return fieldInfo;
    }

    private static String getFieldComment(PsiField field) {
        PsiDocComment docComment = field.getDocComment();
        if (docComment == null) {
            return null;
        }
        String fieldComment = "";
        PsiElement[] elements = docComment.getDescriptionElements();

        if (elements.length > 0) {
            fieldComment = Arrays.stream(elements).map(element -> element.getText().trim()).collect(Collectors.joining(" "));
        }
        return fieldComment.trim();
    }
}
