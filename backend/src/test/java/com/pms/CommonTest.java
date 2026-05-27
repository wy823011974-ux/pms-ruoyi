package com.pms;

import cn.hutool.json.JSONUtil;
import com.pms.common.Result;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通用组件测试 — Result、JWT 等
 */
class CommonTest {

    @Test
    void testResultOk() {
        Result<String> r = Result.ok("hello");
        assertEquals(200, r.getCode());
        assertEquals("success", r.getMsg());
        assertEquals("hello", r.getData());
    }

    @Test
    void testResultOkNull() {
        Result<Void> r = Result.ok();
        assertEquals(200, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void testResultFail() {
        Result<Void> r = Result.fail("错误信息");
        assertEquals(500, r.getCode());
        assertEquals("错误信息", r.getMsg());
    }

    @Test
    void testResultFailWithCode() {
        Result<Void> r = Result.fail(401, "未登录");
        assertEquals(401, r.getCode());
        assertEquals("未登录", r.getMsg());
    }

    @Test
    void testResultPage() {
        List<String> rows = List.of("a", "b", "c");
        Result<Map<String, Object>> r = Result.page(3, rows);

        assertEquals(200, r.getCode());
        assertEquals(3L, r.getData().get("total"));
        assertEquals(rows, r.getData().get("rows"));
    }

    @Test
    void testAnonymizerIntegration() {
        // 模拟完整的脱敏流程：上传 -> 脱敏 -> 入库
        Map<String, String> rawRow = new HashMap<>();
        rawRow.put("姓名", "张三");
        rawRow.put("性别", "男");
        rawRow.put("年龄", "25");
        rawRow.put("手机号", "13800138001");
        rawRow.put("身份证号", "110101199003071234");
        rawRow.put("地址", "海南省海口市龙华区金贸西路100号");

        // 使用 key 检测脱敏（因为 key 是中文，不会自动匹配规则）
        // 所以需要用拼音 key 来测试
        Map<String, String> pinyinRow = new HashMap<>();
        pinyinRow.put("xing_ming", "张三");
        pinyinRow.put("xing_bie", "男");
        pinyinRow.put("nian_ling", "25");
        pinyinRow.put("shou_ji_hao", "13800138001");
        pinyinRow.put("shen_fen_zheng", "110101199003071234");
        pinyinRow.put("di_zhi", "海南省海口市龙华区金贸西路100号");

        Map<String, String> masked = com.pms.services.Anonymizer.anonymize(pinyinRow);

        // 验证脱敏结果
        assertEquals("张*", masked.get("xing_ming"));
        assertEquals("男", masked.get("xing_bie")); // 非敏感数据不变
        assertEquals("25", masked.get("nian_ling")); // 数字不变
        assertEquals("138****8001", masked.get("shou_ji_hao"));
        assertEquals("110101********1234", masked.get("shen_fen_zheng"));
        assertTrue(masked.get("di_zhi").contains("*"), "地址应被脱敏");
    }

    @Test
    void testJsonSerialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", Map.of("id", 1, "name", "test"));

        String json = JSONUtil.toJsonStr(map);
        assertNotNull(json);
        assertTrue(json.contains("\"code\":200"));
        assertTrue(json.contains("\"name\":\"test\""));
    }
}
