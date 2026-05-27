package com.pms.modules.dashboard;

/**
 * @author ROY
 * @date 2026/05/27
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.Result;
import com.pms.modules.project.PmsProject;
import com.pms.modules.project.PmsProjectMapper;
import com.pms.modules.file.PmsFileRecord;
import com.pms.modules.file.PmsFileRecordMapper;
import com.pms.modules.survey.PmsSurveyData;
import com.pms.modules.survey.PmsSurveyDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PmsProjectMapper projectMapper;
    private final PmsFileRecordMapper fileMapper;
    private final PmsSurveyDataMapper surveyDataMapper;

    @GetMapping("/stats")
    public Result<Map<String,Object>> stats() {
        Map<String,Object> m = new HashMap<>();
        m.put("totalProjects", projectMapper.selectCount(new LambdaQueryWrapper<PmsProject>().eq(PmsProject::getIsDeleted, 0)));
        m.put("activeProjects", projectMapper.selectCount(new LambdaQueryWrapper<PmsProject>().eq(PmsProject::getIsDeleted, 0).eq(PmsProject::getStatus, "IN_PROGRESS")));
        m.put("totalFiles", fileMapper.selectCount(null));
        m.put("totalDataRows", surveyDataMapper.selectCount(null));
        return Result.ok(m);
    }
}
