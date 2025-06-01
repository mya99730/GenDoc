package com.gendoc.service;

import com.alibaba.excel.EasyExcel;
import com.gendoc.bo.ApiMethodInfoBO;
import com.gendoc.bo.ParameterDataBO;
import com.gendoc.bo.ParameterInfoBO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelExporter {

    public static void exportToExcel(ApiMethodInfoBO methodInfo, String filePath) {
        List<ParameterDataBO> dataList = new ArrayList<>();

        // 方法信息部分
        addRow(dataList, "方法名称", methodInfo.getMethodName(), "");
        addRow(dataList, "方法注释", methodInfo.getMethodDesc(), "");
        addRow(dataList, "返回值类型", methodInfo.getReturnParam().getParamType(), "");
        addRow(dataList, "返回值注释", methodInfo.getReturnParam().getParamDesc(), "");

        addRow(dataList, "", "", "");

        // 参数列表
        addRow(dataList, "【请求参数列表】", "类型", "说明");
        for (ParameterInfoBO param : methodInfo.getParameters()) {
            writeParameter(param, dataList, (short) 0);
        }

        addRow(dataList, "", "", "");

        // 返回值字段
        addRow(dataList, "【返回值字段】", "类型", "说明");
        for (ParameterInfoBO field : methodInfo.getReturnParam().getChildParamList()) {
            writeParameter(field, dataList, (short) 0);
        }

        // 写入 Excel 文件
        EasyExcel.write(new File(filePath))
                .head(ParameterDataBO.class)
                .registerWriteHandler(new ColorTagStyleHandler(dataList))
                .sheet(methodInfo.getMethodName())
                .doWrite(dataList);

        // 自动调整列宽
        adjustColumnWidth(filePath, methodInfo.getMethodName());
    }

    private static void adjustColumnWidth(String filePath, String sheetName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);

            // 获取工作表的最大列数
            int maxColumns = sheet.getRow(0).getLastCellNum();

            // 设置每列自动调整宽度（256 * 字符数）
            for (int col = 0; col < maxColumns; col++) {
                int maxLength = 0;
                for (Row row : sheet) {
                    Cell cell = row.getCell(col);
                    if (cell != null) {
                        String value = cell.toString();
                        maxLength = Math.max(maxLength, value.length());
                    }
                }
                sheet.setColumnWidth(col, (maxLength + 20) * 256); // 加 2 个字符空隙
            }

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addRow(List<ParameterDataBO> list, String name, String type, String comment) {
        list.add(new ParameterDataBO(name, type, comment,  (short) 0));
    }

    private static void writeParameter(ParameterInfoBO param, List<ParameterDataBO> list, short colorTag) {
        list.add(new ParameterDataBO(param.getParamName(), param.getParamType(), param.getParamDesc(), colorTag));
        short childColorTag = (short) (colorTag + 1);
        for (ParameterInfoBO child : param.getChildParamList()) {
            writeParameter(child, list, childColorTag);
        }
    }
}
