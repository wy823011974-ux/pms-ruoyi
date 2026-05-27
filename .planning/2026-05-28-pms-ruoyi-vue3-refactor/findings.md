# Findings & Decisions — PMS RuoYi-Vue3 Refactor

## Architecture
- **Backend**: 39 Java files, 7 modules (auth/user/project/projectType/fileType/file/survey/audit/dashboard)
- **Frontend**: 8 Vue pages + Pinia store + Axios interceptors
- **Database**: 7 MySQL tables, Redis 7 cache
- **Build**: Maven 3.9 + Vite 5.2

## Key Discoveries

### EasyExcel 3.x invokeHead Trap
EasyExcel 3.x separates header (`invokeHead()`) from data (`invoke()`). Our code only overrode `invoke()`, causing data row to become headers. Fix: `.headRowNumber(0)`.

### Anonymizer Key Mismatch
Original anonymizer matched pinyin keys (`xing_ming`) but Excel columns are Chinese (`姓名`). Fix: use field definition `extra_attrs.anonymize_rule` with Chinese `field_label`.

### 5-Angle Review Findings (2026-05-28)
- RateLimitFilter read-modify-write race → atomic `compute()`
- Cascade delete missing @Transactional
- 401 interceptor fallthrough after redirect
- Login brute-force lock never implemented
- skipRows snake_case/camelCase param mismatch
- RuntimeException messages leaked to clients
- UserController phone null before regex

## Verified Safe
- No SQL injection (MyBatis-Plus parameterized)
- No SimpleDateFormat (java.time.*)
- No System.out (@Slf4j)
- No raw types, no BigDecimal(double)
