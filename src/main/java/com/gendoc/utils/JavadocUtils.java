package com.gendoc.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocToken;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavadocUtils {

    /**
     * 提取 Javadoc 的摘要部分（第一个段落）
     *
     * @param docComment Javadoc 对象
     * @return 摘要文本
     */
    public static String getExtractSummary(PsiDocComment docComment) {
        if (docComment == null) {
            log.info("方法没有javaDoc注释");
            return null;
        }

        StringBuilder summary = new StringBuilder();
        boolean isFirstParagraph = true;

        for (PsiElement element : docComment.getChildren()) {
            if (element instanceof PsiDocTag) {
                log.info("遇到标签，结束段落");
                break;
            }

            if (element instanceof PsiDocToken) {
                PsiDocToken token = (PsiDocToken) element;
                if (token.getText().contains("*")) {
                    continue;
                }
                String text = token.getText().trim();
                if (isFirstParagraph && !text.isEmpty()) {
                    summary.append(text);
                    isFirstParagraph = false;
                } else if (!isFirstParagraph) {
                    // 合并多行段落
                    summary.append(" ").append(text);
                }
            }
        }

        return summary.toString().trim();
    }
}
