package com.gendoc.service;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.gendoc.bo.ParameterDataBO;
import org.apache.poi.ss.usermodel.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorTagStyleHandler implements CellWriteHandler {

    private final List<ParameterDataBO> dataList;
    private final Map<String, CellStyle> styleCache = new HashMap<>();
    private Workbook workbook;

    public ColorTagStyleHandler(List<ParameterDataBO> dataList) {
        this.dataList = dataList;
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        if (workbook == null) {
            workbook = writeSheetHolder.getSheet().getWorkbook();
        }

        // 处理表头样式
        if (isHead) {
            cell.setCellStyle(styleCache.get("header"));
            return;
        }

        // 处理数据行样式（通过行索引获取数据对象）
        int rowIndex = cell.getRow().getRowNum() - 1; // 排除表头行
        if (rowIndex >= 0 && rowIndex < dataList.size()) {
            ParameterDataBO data = dataList.get(rowIndex);
            cell.setCellStyle(createColoredStyle(data.getColorTag()));
        }
    }

    private CellStyle createColoredStyle(short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
