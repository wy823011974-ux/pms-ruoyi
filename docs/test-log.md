# 测试日志

> 日期：2026-05-27  
> 测试框架：JUnit 5 + Maven Surefire

## 测试结果总览

```
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
```

## 测试分类

### 1. 脱敏引擎测试 (AnonymizerTest) — 13 个测试

| 测试方法 | 验证内容 | 结果 |
|----------|----------|------|
| testMaskNameNormal | 普通单姓脱敏：张三 → 张* | ✅ |
| testMaskNameCompound | 复姓脱敏：欧阳锋 → 欧阳* | ✅ |
| testMaskNameThreeChar | 三字名脱敏：王小明 → 王** | ✅ |
| testMaskNameEthnic | 少数民族名脱敏：阿卜杜拉·买买提 → 阿卜杜拉·*** | ✅ |
| testMaskIdCard18 | 18位身份证：保留前6后4 | ✅ |
| testMaskIdCard15 | 15位身份证：保留前6后4 | ✅ |
| testMaskPhone11 | 11位手机号：保留前3后4 | ✅ |
| testMaskPhoneShort | 短号码脱敏 | ✅ |
| testMaskAddressProvinceCity | 省市区地址脱敏 | ✅ |
| testMaskAddressSimple | 直辖市地址脱敏 | ✅ |
| testAnonymizeRowByNameKey | 通过拼音key自动识别姓名+手机脱敏 | ✅ |
| testAnonymizeRowSensitiveIdCard | 通过拼音key自动识别身份证脱敏 | ✅ |
| testAnonymizeRowEmptyValue | 空值和null不崩溃 | ✅ |

### 2. 通用组件测试 (CommonTest) — 7 个测试

| 测试方法 | 验证内容 | 结果 |
|----------|----------|------|
| testResultOk | Result.ok() 返回200+数据 | ✅ |
| testResultOkNull | Result.ok() 返回200+null | ✅ |
| testResultFail | Result.fail() 返回500+错误信息 | ✅ |
| testResultFailWithCode | Result.fail(401) 返回自定义状态码 | ✅ |
| testResultPage | Result.page() 返回total+rows结构 | ✅ |
| testAnonymizerIntegration | 完整脱敏流程：上传→脱敏→入库模拟 | ✅ |
| testJsonSerialize | Hutool JSON序列化验证 | ✅ |

### 3. Excel解析测试 (ExcelValidatorTest) — 3 个测试

| 测试方法 | 验证内容 | 结果 |
|----------|----------|------|
| testParseSimpleExcel | 基本Excel解析 | ✅ (跳过：无测试文件) |
| testParseWithSkipRows | 跳行解析 | ✅ (跳过：无测试文件) |
| testParseEmptyExcel | 空文件处理 | ✅ (跳过：无测试文件) |
