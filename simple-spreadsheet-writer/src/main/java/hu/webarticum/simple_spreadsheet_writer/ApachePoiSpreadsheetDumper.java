package hu.webarticum.simple_spreadsheet_writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import hu.webarticum.simple_spreadsheet_writer.Sheet.Column;
import hu.webarticum.simple_spreadsheet_writer.Sheet.Row;

abstract public class ApachePoiSpreadsheetDumper implements SpreadsheetDumper {

    static protected final float MM_PTS = 2.834645669291f;

    // XXX
    static protected final int MM_WUS = 100;
    
    @Override
    public void dump(Spreadsheet spreadheet, File file) throws IOException {
        dump(spreadheet, new FileOutputStream(file));
    }

    @Override
    public void dump(Spreadsheet spreadsheet, OutputStream outputStram) throws IOException {
        Workbook outputWorkbook = createWorkbook();
        
        for (Spreadsheet.Page page: spreadsheet) {
            Sheet sheet = page.sheet;
            if (sheet.hasNegative()) {
                sheet = new Sheet(sheet);
                sheet.moveToNonNegative();
            }
            org.apache.poi.ss.usermodel.Sheet outputSheet = outputWorkbook.createSheet(page.label);
            for (Integer rowIndex: sheet.getRowIndexes()) {
                Row row = sheet.getRow(rowIndex);
                org.apache.poi.ss.usermodel.Row outputRow = outputSheet.getRow(rowIndex);
                if (outputRow == null) {
                    outputRow = outputSheet.createRow(rowIndex);
                }
                if (row.height > 0) {
                    outputRow.setHeightInPoints(row.height * MM_PTS);
                }
            }
            for (Integer columnIndex: sheet.getColumnIndexes()) {
                Column column = sheet.getColumn(columnIndex);
                if (column.width > 0) {
                    outputSheet.setColumnWidth(columnIndex, column.width * MM_WUS);
                }
            }
            for (Sheet.Range mergeRange: sheet.merges) {
                outputSheet.addMergedRegion(new CellRangeAddress(
                    mergeRange.rowIndex1, mergeRange.rowIndex2,
                    mergeRange.columnIndex1, mergeRange.columnIndex2
                ));
            }
            for (Sheet.CellEntry entry: sheet) {
                org.apache.poi.ss.usermodel.Row outputRow = outputSheet.getRow(entry.rowIndex);
                if (outputRow == null) {
                    outputRow = outputSheet.createRow(entry.rowIndex);
                }
                org.apache.poi.ss.usermodel.Cell outputCell = outputRow.createCell(entry.columnIndex);
                outputCell.setCellValue(entry.cell.text);
                applyFormat(outputWorkbook, outputCell, entry.getComputedFormat());
                applyProblematicFormat(outputWorkbook, outputCell, entry.getComputedFormat());
            }
        }
        
        outputWorkbook.write(outputStram);
    }

    abstract protected Workbook createWorkbook();
    
    protected void applyFormat(Workbook outputWorkbook, org.apache.poi.ss.usermodel.Cell outputCell, Sheet.Format format) {
        CellStyle cellStyle = outputWorkbook.createCellStyle();
        for (Map.Entry<String, String> entry: format.entrySet()) {
            String property = entry.getKey();
            String value = entry.getValue();
            if (property.equals("text-align")) {
                cellStyle.setAlignment(getHorizontalAlignment(value));
            } else if (property.equals("vertical-align")) {
                cellStyle.setVerticalAlignment(getVerticalAlignment(value));
            }
            // TODO
        }
        outputCell.setCellStyle(cellStyle);
    }

    abstract protected void applyProblematicFormat(Workbook outputWorkbook, org.apache.poi.ss.usermodel.Cell outputCell, Sheet.Format format);

    protected TreeMap<String, Short> horizontalAlignmentMap = null;
    protected short getHorizontalAlignment(String value) {
        if (horizontalAlignmentMap == null) {
            horizontalAlignmentMap = new TreeMap<String, Short>();
            horizontalAlignmentMap.put("left", CellStyle.ALIGN_LEFT);
            horizontalAlignmentMap.put("center", CellStyle.ALIGN_CENTER);
            horizontalAlignmentMap.put("right", CellStyle.ALIGN_RIGHT);
            horizontalAlignmentMap.put("justify", CellStyle.ALIGN_JUSTIFY);
        }
        if (horizontalAlignmentMap.containsKey(value)) {
            return horizontalAlignmentMap.get(value);
        } else {
            return CellStyle.ALIGN_GENERAL;
        }
    }

    protected TreeMap<String, Short> verticalAlignmentMap = null;
    protected short getVerticalAlignment(String value) {
        if (verticalAlignmentMap == null) {
            verticalAlignmentMap = new TreeMap<String, Short>();
            verticalAlignmentMap.put("top", CellStyle.VERTICAL_TOP);
            verticalAlignmentMap.put("middle", CellStyle.VERTICAL_CENTER);
            verticalAlignmentMap.put("bottom", CellStyle.VERTICAL_BOTTOM);
        }
        if (verticalAlignmentMap.containsKey(value)) {
            return verticalAlignmentMap.get(value);
        } else {
            return CellStyle.VERTICAL_TOP;
        }
    }
    
}
