package com.binfo.monitor.util;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import tgtools.exceptions.APPErrorException;
import tgtools.util.FileUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author alex
 */
public class ExcelUtil {

    public static final String EXCEL_TYPE_XLS = "xls";
    public static final String EXCEL_TYPE_XLSX = "xlsx";

    /***
     * 读取单元格的值
     * @param cell
     * @return
     */
    public static String getCellValue(Cell cell) {
        Object result = "";
        if (cell != null) {
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_STRING:
                    result = cell.getStringCellValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    result = cell.getNumericCellValue();
                    break;
                case Cell.CELL_TYPE_BOOLEAN:
                    result = cell.getBooleanCellValue();
                    break;
                case Cell.CELL_TYPE_FORMULA:
                    result = cell.getCellFormula();
                    break;
                case Cell.CELL_TYPE_ERROR:
                    result = cell.getErrorCellValue();
                    break;
                case Cell.CELL_TYPE_BLANK:
                    break;
                default:
                    break;
            }
        }
        return result.toString();
    }

    public static boolean isNum(String str) {
        return str.matches("^[0-9]+(.[0-9]+)?$");
    }

    public static Workbook createWorkbook(MultipartFile file) throws APPErrorException {
        String type = FileUtil.getExtensionName(file.getOriginalFilename());
        try {
            if (EXCEL_TYPE_XLS.equals(type.toLowerCase())) {
                return new HSSFWorkbook(file.getInputStream());
            } else if (EXCEL_TYPE_XLSX.equals(type.toLowerCase())) {
                return new XSSFWorkbook(file.getInputStream());
            }
        } catch (Exception e) {
            throw new APPErrorException("加载文件失败；文件路径：" + file.getContentType() + ";原因：" + e.getMessage(), e);
        }

        return null;
    }


    /***
     * 根据list创建对应的HSSFWorkbook
     * @param list  数据源
     * @param map  传入LinkedHashMap  固定的ket:sheet value:sheet名称  key:list实体中的字段，value：对应名称
     * @param c     list value类型
     * @return
     * @throws Exception
     */
    public static HSSFWorkbook createWorkBookByList(List list, Map<String,String> map, Class c) throws Exception {
        HSSFWorkbook book = new HSSFWorkbook();
        HSSFSheet sheet = book.createSheet(map.get("sheet")==null||"".equals(map.get("sheet"))?"数据表格":map.get("sheet"));
        map.remove("sheet");
        List<String> title = new ArrayList<>();
        HSSFRow headerRow = sheet.createRow(0);
        int index = 0;
        for (String item : map.keySet()){
            HSSFCell cell = headerRow.createCell(index);
            title.add(item);
            cell.setCellValue(map.get(item));
            ++index;
        }
        for (int i=1;i<list.size();i++){
            HSSFRow row = sheet.createRow(i);
            Method method = null;
            for (int j=0;j<title.size();j++){
                HSSFCell cell = row.createCell(j);
                method = c.getMethod("get"+title.get(j).substring(0,1).toUpperCase()+title.get(j).substring(1,title.get(j).length()));
                cell.setCellValue(method.invoke(list.get(i)).toString());
            }

        }
        return book;
    }


}
