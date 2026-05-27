package com.pms;

import com.pms.services.Anonymizer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 脱敏引擎单元测试 — 验证四种脱敏规则正确性
 */
class AnonymizerTest {

    @Test
    void testMaskNameNormal() {
        // 普通单姓：张三 → 张*
        String result = Anonymizer.maskName("张三");
        assertEquals("张*", result);
    }

    @Test
    void testMaskNameCompound() {
        // 复姓：欧阳锋 → 欧阳*
        String result = Anonymizer.maskName("欧阳锋");
        assertEquals("欧阳*", result);
    }

    @Test
    void testMaskNameThreeChar() {
        // 三字名：王小明 → 王**
        String result = Anonymizer.maskName("王小明");
        assertEquals("王**", result);
    }

    @Test
    void testMaskNameEthnic() {
        // 少数民族姓名：阿卜杜拉·买买提 → 阿卜杜拉·***
        String result = Anonymizer.maskName("阿卜杜拉·买买提");
        assertEquals("阿卜杜拉·***", result);
    }

    @Test
    void testMaskIdCard18() {
        // 18位身份证：保留前6后2，中间固定4个*
        String result = Anonymizer.maskIdCard("110101199003071234");
        assertEquals("110101****34", result);
    }

    @Test
    void testMaskIdCard15() {
        // 15位身份证
        String result = Anonymizer.maskIdCard("110101900307123");
        assertEquals("110101****23", result);
    }

    @Test
    void testMaskPhone11() {
        // 11位手机号：保留前3后4
        String result = Anonymizer.maskPhone("13800138001");
        assertEquals("138****8001", result);
    }

    @Test
    void testMaskPhoneShort() {
        // 短号码
        String result = Anonymizer.maskPhone("12345678");
        assertEquals("123**678", result);
    }

    @Test
    void testMaskAddressProvinceCity() {
        // 省市区地址：保留省市区部分，剩余替换为*
        String result = Anonymizer.maskAddress("海南省海口市龙华区金贸西路100号");
        assertTrue(result.startsWith("海南省海口市龙华区"), "应保留省市区前缀");
        assertTrue(result.endsWith("*"), "剩余部分应替换为*");
    }

    @Test
    void testMaskAddressSimple() {
        // 简单地址
        String result = Anonymizer.maskAddress("北京市朝阳区建国路100号");
        assertTrue(result.startsWith("北京市朝阳区"), "应保留市区前缀");
        assertTrue(result.contains("*"), "应有脱敏标记");
    }

    @Test
    void testAnonymizeRowByNameKey() {
        // 通过 key 名自动检测脱敏规则
        Map<String, String> row = new HashMap<>();
        row.put("xing_ming", "张三");
        row.put("shou_ji_hao", "13800138001");

        Map<String, String> result = Anonymizer.anonymize(row);

        assertEquals("张*", result.get("xing_ming"));
        assertEquals("138****8001", result.get("shou_ji_hao"));
    }

    @Test
    void testAnonymizeRowSensitiveIdCard() {
        Map<String, String> row = new HashMap<>();
        row.put("shen_fen_zheng", "110101199003071234");
        row.put("normal_field", "普通数据");

        Map<String, String> result = Anonymizer.anonymize(row);

        assertEquals("110101****34", result.get("shen_fen_zheng"));
        assertEquals("普通数据", result.get("normal_field")); // 非敏感数据不变
    }

    @Test
    void testAnonymizeRowEmptyValue() {
        // 空值不应崩溃
        Map<String, String> row = new HashMap<>();
        row.put("xing_ming", "");
        row.put("shou_ji_hao", null);

        Map<String, String> result = Anonymizer.anonymize(row);

        assertEquals("", result.get("xing_ming"));
        assertNull(result.get("shou_ji_hao"));
    }
}
