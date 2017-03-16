package com.ccnode.codegenerator.dialog;

import com.ccnode.codegenerator.database.DatabaseComponenent;
import com.ccnode.codegenerator.dialog.dto.mybatis.ClassMapperMethod;
import com.ccnode.codegenerator.dialog.dto.mybatis.ColumnAndField;
import com.ccnode.codegenerator.dialog.dto.mybatis.MapperMethodEnum;
import com.ccnode.codegenerator.enums.MethodName;
import com.ccnode.codegenerator.freemarker.TemplateConstants;
import com.ccnode.codegenerator.freemarker.TemplateUtil;
import com.ccnode.codegenerator.view.GenerateMethodXmlAction;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bruce.ge on 2016/12/29.
 */
public class MapperUtil {

    public static final String SELECT = "select";
    public static final String FROM = "from";

    @Nullable
    static String generateSql(List<GenCodeProp> newAddedProps, List<ColumnAndField> deleteFields, String sqlText, List<ColumnAndField> existingFields) {
        sqlText = deleteEndEmptyChar(sqlText);
        String beforeWhere = sqlText;
        //text before where make it on it.
        int start = 0;
        int end = sqlText.length();
        String lowerSqlText = sqlText.toLowerCase();
        int where = findMatchFor(lowerSqlText, FROM);
        if (where != -1) {
            end = where;
            beforeWhere = sqlText.substring(0, where);
        }
        int select = findMatchFor(lowerSqlText, SELECT);
        if (select != -1) {
            start = select + SELECT.length();
            beforeWhere = beforeWhere.substring(select + SELECT.length());
        }

        //not support for with select function ect.
        if (beforeWhere.contains("(")) {
            return null;
        }
        String[] split = beforeWhere.split(",");

        List<String> beforeFormatted = new ArrayList<>();
        for (String uu : split) {
            String term = trimUseLess(uu);
            boolean isDeleted = false;
            for (ColumnAndField deleteField : deleteFields) {
                if (term.toLowerCase().equals(deleteField.getColumn().toLowerCase())) {
                    isDeleted = true;
                    break;
                }
            }
            if (isDeleted) {
                continue;
            }
            beforeFormatted.add(uu);
        }
        String beforeInsert = "";
        for (int i = 0; i < beforeFormatted.size(); i++) {
            beforeInsert += beforeFormatted.get(i);
            if (i != beforeFormatted.size() - 1) {
                beforeInsert += ",";
            }
        }
        String newAddInsert = "";
        for (int i = 0; i < newAddedProps.size(); i++) {
            newAddInsert += ",\n" + DatabaseComponenent.formatColumn(newAddedProps.get(i).getColumnName());
        }
        String newValueText = sqlText.substring(0, start) + beforeInsert + newAddInsert + sqlText.substring(end) + "\n";
        return newValueText;
    }

    private static int findMatchFor(String lowerSqlText, String where) {
        Pattern matcher = Pattern.compile("\\b" + where + "\\b");
        Matcher matcher1 = matcher.matcher(lowerSqlText);
        if (matcher1.find()) {
            return matcher1.start();
        } else {
            return -1;
        }
    }

    private static String trimUseLess(String uu) {
        int len = uu.length();
        int start = 0;
        int end = uu.length();
        char c;
        c = uu.charAt(start++);
        while (start != len && (c == '\n') || (c == ' ') || c == '\t' || c == '`') {
            c = uu.charAt(start++);
        }
        if (start == len) {
            return "";
        }
        c = uu.charAt(--end);
        while (end >= start && (c == '\n') || (c == ' ') || c == '\t' || c == '`') {
            c = uu.charAt(--end);
        }
        return uu.substring(start - 1, end + 1);
    }

    public static String generateMapperMethod(List<ColumnAndField> finalFields, String tableName, MapperMethodEnum type, ClassMapperMethod classMapperMethod) {
        if (tableName == null) {
            tableName = "";
        }
        String methodName = classMapperMethod.getMethodName();
        if (methodName.equals(MethodName.insert.name())) {
            return genInsert(finalFields, tableName);

        } else if (methodName.equals(MethodName.insertList.name())) {
            return genInsertList(finalFields, tableName);
        } else if (methodName.equals(MethodName.insertSelective.name())) {
            return genInsertSelective(finalFields, tableName);
        } else {
            if (methodName.equals(MethodName.update.name())) {
                return genUpdateMethod(finalFields, tableName);
            }
        }
        return null;
    }

    private static String genUpdateMethod(List<ColumnAndField> finalFields, String tableName) {
        Map<String, Object> root = Maps.newHashMap();
        root.put("finalFields", finalFields);
        root.put("tableName", tableName);
        // TODO: 2017/1/12 could know the primary key from  the old update string
        return TemplateUtil.processToString(TemplateConstants.updateTemplateName, root);
    }

    private static String genInsertList(List<ColumnAndField> finalFields, String tableName) {
        Map<String, Object> root = Maps.newHashMap();
        root.put("finalFields", finalFields);
        root.put("tableName", tableName);
        root.put(TemplateConstants.CURRENTDATABASE,DatabaseComponenent.currentDatabase());
        return TemplateUtil.processToString(TemplateConstants.insertListTemplateName, root);
    }

    private static String genInsert(List<ColumnAndField> finalFields, String tableName) {
        Map<String, Object> root = Maps.newHashMap();
        root.put("finalFields", finalFields);
        root.put("tableName", tableName);
        String s = null;
        boolean useTest = false;
        if (useTest) {
            s = TemplateUtil.processToString(TemplateConstants.insertTemplateName, root);
        } else {
            s = TemplateUtil.processToString(TemplateConstants.insertWithOutTestTemplateName, root);
        }
        return s;
    }


    private static String genInsertSelective(List<ColumnAndField> finalFields, String tableName) {
        Map<String, Object> root = Maps.newHashMap();
        root.put("finalFields", finalFields);
        root.put("tableName", tableName);
        String s = null;
        boolean useTest = true;
        if (useTest) {
            s = TemplateUtil.processToString(TemplateConstants.insertTemplateName, root);
        } else {
            s = TemplateUtil.processToString(TemplateConstants.insertWithOutTestTemplateName, root);
        }
        return s;
    }

    public static String extractTable(String insertText) {
        if (insertText.length() == 0) {
            return null;
        }
        String formattedInsert = formatBlank(insertText).toLowerCase();
        int i = formattedInsert.indexOf(GenerateMethodXmlAction.INSERT_INTO);
        if (i == -1) {
            return null;
        }
        int s = i + GenerateMethodXmlAction.INSERT_INTO.length() + 1;
        StringBuilder resBuilder = new StringBuilder();
        for (int j = s; j < formattedInsert.length(); j++) {
            char c = formattedInsert.charAt(j);
            if (!isBlankChar(c)) {
                resBuilder.append(c);
            } else {
                break;
            }
        }
        if (resBuilder.length() > 0) {
            return resBuilder.toString();
        } else {
            return null;
        }
    }

    private static String formatBlank(String insertText) {
        StringBuilder result = new StringBuilder();
        char firstChar = insertText.charAt(0);
        result.append(firstChar);
        boolean before = isBlankChar(firstChar);
        for (int i = 1; i < insertText.length(); i++) {
            char curChar = insertText.charAt(i);
            boolean cur = isBlankChar(curChar);
            if (cur && before) {
                continue;
            } else {
                result.append(curChar);
                before = cur;
            }
        }
        return result.toString();
    }

    private static boolean isBlankChar(char c) {
        if (c == ' ' || c == '\t' || c == '\n' || c == '(' || c == '<' || c == ')' || c == '>') {
            return true;
        }
        return false;
    }

    @NotNull
    public static String extractClassShortName(String fullName) {
        int i = fullName.lastIndexOf(".");
        return fullName.substring(i + 1);
    }


    @Nullable
    public static String extractPackage(String fullName) {
        int i = fullName.lastIndexOf(".");
        if(i==-1){
            return null;
        } else {
            return fullName.substring(0,i);
        }
    }

    public static String deleteEndEmptyChar(String text) {
        int end = text.length();
        int useEnd = end - 1;
        for (int j = end - 1; j >= 0; j--) {
            char c = text.charAt(j);
            if (c != '\n' && c != '\t' && c != ' ') {
                useEnd = j;
                break;
            }
        }
        return text.substring(0, useEnd + 1);
    }
}
