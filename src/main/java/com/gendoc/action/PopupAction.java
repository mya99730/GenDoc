package com.gendoc.action;

import cn.hutool.core.util.StrUtil;
import com.gendoc.bo.ApiMethodInfoBO;
import com.gendoc.service.ExcelExporter;
import com.gendoc.utils.PsiUtils;
import com.gendoc.validator.FileNameValidator;
import com.google.common.base.Throwables;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 选择方法后，右键弹出列表动作
 */
@Slf4j
public class PopupAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        log.info("触发生成接口文档操作。");

        // 当前编辑的 PSI 文件对象，用于解析代码结构
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        // 当前编辑器实例，用于获取用户正在编辑的内容及光标位置
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        // 当前项目对象，用于操作项目级资源和路径
        Project project = event.getProject();

        if (psiFile == null || editor == null || project == null) {
            log.warn("缺少必要数据：psiFile、editor 或 project 为 null。");
            return;
        }

        // 获取当前光标所在方法
        PsiMethod method = getPsiMethod(editor, psiFile);
        if (null == method) {
            log.warn("当前光标不在方法中，无法获取当前方法");
            return;
        }

        log.info("选中方法：" + method.getName());
        // 选择输出目录
        VirtualFile selectedDir = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                project,
                ProjectUtil.guessProjectDir(project));
        if (null == selectedDir) {
            log.warn("未选择输出目录");
            return;
        }

        // 弹出文件名输入对话框
        String defaultFileName = method.getName();
        String fileName = Messages.showInputDialog(
                project,
                "请输入导出的文件名（不带扩展名）：",
                "输入文件名",
                Messages.getQuestionIcon(),
                defaultFileName,
                new FileNameValidator());
        if (StrUtil.isBlankIfStr(fileName)) {
            log.warn("未输入文件名");
            return;
        }

        String outputPath = selectedDir.getPath() + File.separator + fileName + ".xlsx";
        log.info("导出路径已选择：" + outputPath);

        try {
            ApiMethodInfoBO methodInfoBO = PsiUtils.parseMethodInfo(method);
            ExcelExporter.exportToExcel(methodInfoBO, outputPath);
            Messages.showInfoMessage("接口文档已生成到：" + outputPath, "成功");
            log.info("接口文档已成功导出至：" + outputPath);
        } catch (Exception e) {
            log.error("解析或导出过程中发生错误", Throwables.getStackTraceAsString(e));
            Messages.showErrorDialog("导出失败：" + e.getMessage(), "错误");
        }

    }

    private PsiMethod getPsiMethod(Editor editor, PsiFile psiFile) {
        //  获取光标位置
        int offset = editor.getCaretModel().getOffset();
        //  获取光标下的 PsiElement
        PsiElement elementAtCursor = psiFile.findElementAt(offset);
        // 向上查找，获取包含该元素的最靠近的 PsiMethod 对象，即当前选中或光标所在的方法
        return PsiTreeUtil.getParentOfType(elementAtCursor, PsiMethod.class);
    }
}
