package com.pms.modules.project;

/**
 * @author ROY
 * @date 2026/05/27
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.modules.projectType.PmsProjectType;
import com.pms.modules.projectType.PmsProjectTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 项目服务 — 编码生成、状态流转规则
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final PmsProjectMapper projectMapper;
    private final PmsProjectTypeMapper ptMapper;

    /**
     * 生成项目编码 — 规则：HBXM-{类型简写}-{年份}-{序号}
     * 序号按年份递增（含已删除），保证永不重复
     */
    public String generateProjectCode(Long projectTypeId, int year) {
        PmsProjectType pt = ptMapper.selectById(projectTypeId);
        String typeCode = (pt != null) ? pt.getCode() : "XX";

        // 不过滤 is_deleted，保证序号永不重复
        long count = projectMapper.selectCount(
                new LambdaQueryWrapper<PmsProject>().eq(PmsProject::getYear, year));
        String seq = String.format("%03d", count + 1);

        return "HBXM-" + typeCode + "-" + year + "-" + seq;
    }

    /**
     * 验证状态流转是否合法
     * IN_PROGRESS → COMPLETED → ARCHIVED（正向流转）
     * COMPLETED → IN_PROGRESS（允许回退）
     */
    public boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) return false;
        return switch (currentStatus) {
            case "IN_PROGRESS" -> "COMPLETED".equals(newStatus) || "ARCHIVED".equals(newStatus);
            case "COMPLETED" -> "ARCHIVED".equals(newStatus) || "IN_PROGRESS".equals(newStatus);
            case "ARCHIVED" -> "IN_PROGRESS".equals(newStatus); // 重新激活
            default -> false;
        };
    }
}
