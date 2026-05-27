package com.pms.services;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.pms.modules.fileType.PmsFieldDefinition;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Anonymizer {

    private static final Map<String, Function<String, String>> RULES = new HashMap<>();
    private static final List<String> COMPOUND_SURNAMES = Arrays.asList(
        "欧阳","司马","上官","诸葛","东方","独孤","南宫","夏侯","尉迟","皇甫","慕容","令狐","宇文","长孙","司徒","司空","端木","申屠","轩辕","公孙");

    static {
        RULES.put("name", Anonymizer::maskName);
        RULES.put("id_card", Anonymizer::maskIdCard);
        RULES.put("phone", Anonymizer::maskPhone);
        RULES.put("address", Anonymizer::maskAddress);
    }

    /**
     * 根据字段定义脱敏 —— 通过 field_label（中文列名）匹配 + extra_attrs 中的 anonymize_rule 确定脱敏规则
     */
    public static Map<String, String> anonymize(Map<String, String> row, List<PmsFieldDefinition> fields) {
        Map<String, String> result = new HashMap<>(row);
        for (PmsFieldDefinition fd : fields) {
            if (fd.getExtraAttrs() == null) continue;
            try {
                Map<String, Object> attrs = JSONUtil.parseObj(fd.getExtraAttrs());
                if (Boolean.TRUE.equals(attrs.get("sensitive"))) {
                    String rule = (String) attrs.getOrDefault("anonymize_rule", "name");
                    // 用中文列名匹配
                    String key = fd.getFieldLabel();
                    String val = result.get(key);
                    if (val == null) val = result.get(fd.getFieldKey());
                    if (StrUtil.isNotBlank(val) && RULES.containsKey(rule)) {
                        result.put(key, RULES.get(rule).apply(val));
                    }
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    public static Map<String, String> anonymize(Map<String, String> row) {
        Map<String, String> result = new HashMap<>(row);
        for (String key : row.keySet()) {
            String val = row.get(key);
            if (StrUtil.isBlank(val)) continue;
            // Detect type from key name
            if (key.contains("xing_ming") || key.contains("name")) result.put(key, maskName(val));
            else if (key.contains("shen_fen") || key.contains("id_card")) result.put(key, maskIdCard(val));
            else if (key.contains("shou_ji") || key.contains("phone") || key.contains("dian_hua") || key.contains("tel")) result.put(key, maskPhone(val));
            else if (key.contains("zhu_zhi") || key.contains("address") || key.contains("di_zhi")) result.put(key, maskAddress(val));
        }
        return result;
    }

    public static String maskName(String v) {
        v = v.trim(); if (v.isEmpty()) return v;
        for (String cs : COMPOUND_SURNAMES) { if (v.startsWith(cs)) return cs + "*".repeat(Math.max(0, v.length() - cs.length())); }
        if (v.contains("·")) { int idx = v.indexOf("·"); return v.substring(0, idx + 1) + "*".repeat(Math.max(0, v.length() - idx - 1)); }
        return v.charAt(0) + "*".repeat(Math.max(0, v.length() - 1));
    }

    /**
     * 身份证脱敏：保留前6位+后2位，中间替换为 ****。
     * 仅显示4个星号而非填充等长，防止结合出生日期列反推完整身份证号。
     */
    public static String maskIdCard(String v) {
        v = v.trim();
        if (v.length() >= 15) return v.substring(0, 6) + "****" + v.substring(v.length() - 2);
        if (v.length() >= 6) return v.substring(0, 3) + "****" + v.substring(v.length() - 1);
        return "****";
    }

    public static String maskPhone(String v) {
        v = v.trim();
        if (v.length() == 11 && v.matches("\\d+")) return v.substring(0, 3) + "****" + v.substring(7);
        if (v.length() >= 6) return v.substring(0, 3) + "*".repeat(Math.max(0, v.length() - 6)) + v.substring(v.length() - 3);
        return "*".repeat(v.length());
    }

    public static String maskAddress(String v) {
        v = v.trim(); if (v.isEmpty()) return v;
        Pattern p = Pattern.compile("^([^省]+省)?([^市]+市)?([^区县市]+[区县市])?");
        Matcher m = p.matcher(v);
        if (m.find() && m.end() > 0) return v.substring(0, m.end()) + "*".repeat(Math.max(0, v.length() - m.end()));
        if (v.length() > 4) return v.substring(0, 4) + "*".repeat(Math.max(0, v.length() - 4));
        return v.charAt(0) + "*".repeat(Math.max(0, v.length() - 1));
    }
}
