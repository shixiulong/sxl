package com.binfo.monitor.util;

import org.apache.commons.collections.map.HashedMap;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import tgtools.exceptions.APPErrorException;
import tgtools.util.FileUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
    public static HSSFWorkbook createWorkBookByList(List list, Map<String, String> map, Class c) throws Exception {
        HSSFWorkbook book = new HSSFWorkbook();
        HSSFSheet sheet = book.createSheet(map.get("sheet") == null || "".equals(map.get("sheet")) ? "数据表格" : map.get("sheet"));
        map.remove("sheet");
        List<String> title = new ArrayList<>();
        HSSFRow headerRow = sheet.createRow(0);
        int index = 0;
        for (String item : map.keySet()) {
            HSSFCell cell = headerRow.createCell(index);
            title.add(item);
            cell.setCellValue(map.get(item));
            ++index;
        }
        for (int i = 1; i <= list.size(); i++) {
            HSSFRow row = sheet.createRow(i);
            Method method = null;
            for (int j = 0; j < title.size(); j++) {
                HSSFCell cell = row.createCell(j);
                //反射获取get方法
                method = c.getMethod("get" + title.get(j).substring(0, 1).toUpperCase() + title.get(j).substring(1, title.get(j).length()));
                String content = "";
                if (method.invoke(list.get(i - 1)) != null) {
                    content = method.invoke(list.get(i - 1)).toString();
                }
                cell.setCellValue(content);
            }

        }
        return book;
    }


    /***
     * 根据WorkBook创建集合  根据字段及标题印射，列的顺序可以不固定，可多可少，不影响数据
     * @param workbook
     * @param map  固定的ket:sheet value:sheet名称  key:list实体中的字段，value：Excel第一行的标题列的值
     * @param classed  集合实体
     * @return
     */
    public static List createListByWorkBook(Workbook workbook, Map<String, String> map, Class classed) {
        List list = new ArrayList();
        Map<Integer, String> entityRel = new HashedMap();
        Map<String, Class> classType = new HashedMap();
        try {
            Object object = classed.newInstance();
            Class c = object.getClass();
            //            //类成员变量包含父级成员变量
            List<Field> fieldList = new ArrayList<>();
            while (c != null) {
                c = c.getSuperclass();
                if (c != null) {
                    fieldList.addAll(new ArrayList<>(Arrays.asList(c.getDeclaredFields())));
                }

            }
            fieldList.addAll(new ArrayList<>(Arrays.asList(classed.getDeclaredFields())));
            //获取字段类型一便执行获取set方法时，传入
            for (Field item : fieldList) {
                classType.put(item.getName(), item.getType());
            }
            Sheet sheet = workbook.getSheet(map.get("sheet"));
            //标题列大小
            int cellCount = sheet.getRow(0).getPhysicalNumberOfCells();
            map.remove("sheet");
            //entityRel key值对应excel表格索引 value对应实体字段
            for (String item : map.keySet()) {
                String title = map.get(item);
                Row row = sheet.getRow(0);
                for (int i = 0; i < cellCount; i++) {
                    Cell cell = row.getCell(i);
                    if (title.equals(cell.getStringCellValue())) {
                        entityRel.put(i, item);
                    }
                }
            }
            Object entity = null;
            Row row = null;
            Cell cell = null;
            Method method = null;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                row = sheet.getRow(i);
                entity = classed.newInstance();
                for (int j = 0; j < cellCount; j++) {
                    cell = row.getCell(j);
                    String title = entityRel.get(j);
                    if (title!=null){
                        //反射获取set方法
                        method = classed.getMethod("set" + title.substring(0, 1).toUpperCase() +
                                title.substring(1, title.length()), classType.get(title));
                        if (method != null) {
                            method.invoke(entity, getFormactCellValue(cell));
                        }
                    }
                }
                list.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /***
     * 读取单元格的值
     * @param cell
     * @return
     */
    public static Object getFormactCellValue(Cell cell) {
        Object result = "";
        if (cell != null) {
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_STRING:
                    result = cell.getStringCellValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    try {
                        if (HSSFDateUtil.isCellDateFormatted(cell)) {
                            result = cell.getDateCellValue();
                        } else {
                            result = cell.getNumericCellValue();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
        return result;
    }


}
