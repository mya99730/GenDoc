package com.gendoc.utils;


import cn.hutool.core.util.StrUtil;
import com.gendoc.bo.ApiMethodInfoBO;
import com.gendoc.bo.ParameterInfoBO;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
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
            String paramDesc = formalParamComments.getOrDefault(paramName, "");
            ParameterInfoBO parameterInfoBO = buildParameterInfoBO(method.getProject(), paramName, param.getType(), paramDesc);
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
        String returnParamDesc = returnParamComment.get("return");
        return buildParameterInfoBO(method.getProject(), "", method.getReturnType(), returnParamDesc);
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
                "int", "long", "double", "float", "boolean", "char", "byte", "short","String","Date",
                "int[]", "long[]", "double[]", "float[]", "boolean[]", "char[]", "byte[]", "short[]",
                // 包装类型（简单类名）
                "Integer[]", "Long[]", "Double[]", "Float[]", "Boolean[]", "Character[]", "Byte[]", "Short[]", "String[]",
                "Date[]",
                // 全限定类名版本
                "java.lang.Integer", "java.lang.Long", "java.lang.Double", "java.lang.Float",
                "java.lang.Boolean", "java.lang.Character", "java.lang.Byte", "java.lang.Short",
                "java.lang.String", "java.util.Date",
                // 空值
                "void", "java.lang.Void"
        ));
        return primitives.contains(type);
    }

    private static List<ParameterInfoBO> parseClassFields(PsiClass psiClass) {
        List<ParameterInfoBO> fields = new ArrayList<>();
        for (PsiField field : psiClass.getFields()) {
            String fieldName = field.getName();
            String fieldComment = getFieldComment(field);
            ParameterInfoBO fieldInfo = buildParameterInfoBO(psiClass.getProject(), fieldName, field.getType(), fieldComment);
            fields.add(fieldInfo);
        }
        return fields;
    }


    // 新增：提取原始类名的方法
    private static String extractRawClassName(String fullType) {
        int genericStart = fullType.indexOf('<');
        return (genericStart > 0) ?
                fullType.substring(0, genericStart).trim() :
                fullType.trim();
    }

    private static ParameterInfoBO buildParameterInfoBO(
            Project project,
            String fieldName,
            PsiType fieldType,
            String fieldComment) { // 新增上下文参数

            String fieldTypeName = fieldType.getCanonicalText();
        log.info("开始解析字段：{}，类型为：{}", fieldName, fieldTypeName);

        // 处理基本类型
        if (isPrimitiveType(fieldTypeName)) {
            return new ParameterInfoBO(fieldName, fieldTypeName, fieldComment);
        }

        // 解析原始类
        String rawClassName = extractRawClassName(fieldTypeName);
        PsiClass rawClass = JavaPsiFacade.getInstance(project)
                .findClass(rawClassName, GlobalSearchScope.allScope(project));

        if (rawClass == null) {
            log.warn("未找到字段类：{}", rawClassName);
            return new ParameterInfoBO(fieldName, fieldTypeName, fieldComment);
        }

        ParameterInfoBO fieldInfo = new ParameterInfoBO(fieldName, fieldTypeName, fieldComment);
        // 普通类解析字段
        fieldInfo.getChildParamList().addAll(parseClassFields(rawClass));

        // 处理泛型参数
        if (fieldType instanceof PsiClassType) {
            PsiType[] typeParams = ((PsiClassType) fieldType).getParameters();
            for (PsiType paramType : typeParams) {
                ParameterInfoBO nestedParam = buildParameterInfoBO(
                        project,
                        "",
                        paramType,
                        "");
                if (nestedParam != null) {
                    // 添加空行
                    fieldInfo.getChildParamList().add(new ParameterInfoBO());
                    fieldInfo.getChildParamList().add(nestedParam);
                }
            }
        }

        // 集合类处理
        if (isCollectionType(rawClass.getQualifiedName())) {
            return fieldInfo;
        }
        return fieldInfo;
    }

    /**
     * 解析泛型参数的实际类型（不依赖外部工具类）
     */
    private static PsiType resolveGenericType(PsiTypeParameter typeParam, PsiElement context) {
        // 场景1：方法级泛型（如 <T> T parseJson()）
        if (typeParam.getOwner() instanceof PsiMethod) {
            log.info("方法级泛型：{}", typeParam.getName());
            PsiMethod method = (PsiMethod) typeParam.getOwner();
            PsiMethodCallExpression callExpr = PsiTreeUtil.getParentOfType(context, PsiMethodCallExpression.class);
            if (callExpr != null) {
                // ✅ 核心：直接使用 resolveMethodGenerics() 获取类型替换器
                PsiSubstitutor substitutor = callExpr.resolveMethodGenerics().getSubstitutor();
                return substitutor.substitute(typeParam);
            }
        }

        // 场景2：类级泛型（如 Response<T>）
        else if (typeParam.getOwner() instanceof PsiClass) {
            log.info("类级泛型：{}", typeParam.getName());
            PsiClass ownerClass = (PsiClass) typeParam.getOwner();
            PsiClassType[] extendsTypes = ownerClass.getExtendsListTypes();
            for (PsiClassType extendsType : extendsTypes) {
                PsiClass resolvedClass = extendsType.resolve();
                if (resolvedClass != null) {
                    PsiSubstitutor substitutor = extendsType.resolveGenerics().getSubstitutor();
                    return substitutor.substitute(typeParam);
                }
            }
        }

        // 其他场景（如字段泛型）可在此扩展

        return null;
    }

    private static boolean isCollectionType(String className) {
        return className != null && (className.equals("java.util.List")
                || className.equals("java.util.Set")
                || className.equals("java.util.Map")
                || className.equals("java.util.Collection"));
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
