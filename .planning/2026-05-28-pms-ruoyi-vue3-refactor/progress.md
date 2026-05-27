# Progress Log — PMS RuoYi-Vue3 Refactor

## Session 2026-05-28

### Current Phase
Phase 8 complete, Phase 11 (收尾验证) pending

### Backend Fixes Today
- Login brute-force lock: failCount increment + lock_until
- RateLimitFilter: atomic Caffeine counter
- GlobalExceptionHandler: no more internal error leaks
- skipRows: `@RequestParam(name="skip_rows")` param fix
- creatorId: from SecurityContextHolder
- isDeleted: added to VO for proper UI

### Frontend Fixes Today
- request.js: 401 handler returns Promise.reject (no fallthrough)
- router: meta.roles guard enforced
- user store: fetchUserInfo no longer logs out on transient errors
- login: catch blocks given proper error handling
- el-link: underline boolean → "never" string

### Apple HIG Design (All 8 Pages)
- Inputs: 24px elliptical, consistent wrapper+inner
- Buttons: 24px capsule
- Sidebar: frosted glass
- Colors: #007AFF + #F2F2F7
- Fonts: SF Pro system stack

### Commits
`2bce201` → `c23f8db` (10 commits today)

### Services Running
- MySQL 8.0 :3307 (Docker ✓)
- Redis 7 :6379 (Docker ✓)
- Backend :8080 (Spring Boot ✓)
- Frontend :5173 (Vite dev ✓)

### Test Results
- Backend: 23 tests, 0 failures
- Frontend: vite build ✓
