package com.pms.services;

import cn.hutool.core.util.StrUtil;
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

    static String maskName(String v) {
        v = v.trim(); if (v.isEmpty()) return v;
        for (String cs : COMPOUND_SURNAMES) { if (v.startsWith(cs)) return cs + "*".repeat(Math.max(0, v.length() - cs.length())); }
        if (v.contains("·")) { int idx = v.indexOf("·"); return v.substring(0, idx + 1) + "*".repeat(Math.max(0, v.length() - idx - 1)); }
        return v.charAt(0) + "*".repeat(Math.max(0, v.length() - 1));
    }

    static String maskIdCard(String v) {
        v = v.trim();
        if (v.length() >= 15) return v.substring(0, 6) + "*".repeat(Math.max(0, v.length() - 10)) + v.substring(v.length() - 4);
        if (v.length() >= 6) return v.substring(0, 3) + "*".repeat(Math.max(0, v.length() - 3));
        return "*".repeat(v.length());
    }

    static String maskPhone(String v) {
        v = v.trim();
        if (v.length() == 11 && v.matches("\\d+")) return v.substring(0, 3) + "****" + v.substring(7);
        if (v.length() >= 6) return v.substring(0, 3) + "*".repeat(Math.max(0, v.length() - 6)) + v.substring(v.length() - 3);
        return "*".repeat(v.length());
    }

    static String maskAddress(String v) {
        v = v.trim(); if (v.isEmpty()) return v;
        Pattern p = Pattern.compile("^([^省]+省)?([^市]+市)?([^区县市]+[区县市])?");
        Matcher m = p.matcher(v);
        if (m.find() && m.end() > 0) return v.substring(0, m.end()) + "*".repeat(Math.max(0, v.length() - m.end()));
        if (v.length() > 4) return v.substring(0, 4) + "*".repeat(Math.max(0, v.length() - 4));
        return v.charAt(0) + "*".repeat(Math.max(0, v.length() - 1));
    }
}
