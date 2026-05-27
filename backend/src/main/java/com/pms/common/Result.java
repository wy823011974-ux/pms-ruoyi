package com.pms.common;

/**
 * @author ROY
 * @date 2026/05/27
 */

import lombok.Data;
import java.util.Map;

@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) { Result<T> r = new Result<>(); r.code = 200; r.msg = "success"; r.data = data; return r; }
    public static <T> Result<T> ok() { return ok(null); }
    public static <T> Result<T> fail(String msg) { Result<T> r = new Result<>(); r.code = 500; r.msg = msg; return r; }
    public static <T> Result<T> fail(int code, String msg) { Result<T> r = new Result<>(); r.code = code; r.msg = msg; return r; }

    @SuppressWarnings("unchecked")
    public static <T> Result<Map<String, Object>> page(long total, java.util.List<T> rows) {
        Result<Map<String, Object>> r = new Result<>();
        r.code = 200; r.msg = "success";
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("total", total); m.put("rows", rows);
        r.data = m;
        return r;
    }
}
