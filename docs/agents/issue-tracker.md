# Issue Tracker: GitHub

Issues and PRDs for this repo live as GitHub issues. Use the `gh` CLI for all operations.

## Setup

本仓库尚未关联 GitHub 远程仓库。首次使用时需要：

```bash
gh repo create pms-ruoyi --private --source=. --remote=origin --push
```

这会创建私有仓库、设 `origin`、推送代码。

已有远程后，`gh` 会自动识别 `git remote -v` 的仓库。

## 常用命令

| 操作 | 命令 |
|------|------|
| 列出 Issue | `gh issue list --state open` |
| 创建 Issue | `gh issue create --title "标题" --body "描述"` |
| 查看 Issue | `gh issue view <编号> --comments` |
| 关闭 Issue | `gh issue close <编号> --comment "原因"` |
| 添加标签 | `gh issue edit <编号> --add-label "标签名"` |

## 首次创建标签

```bash
gh label create "待评估" --color "FFA500" --description "等待审查和分类"
gh label create "待补充" --color "1E90FF" --description "信息不足，等待补充"
gh label create "AI可处理" --color "32CD32" --description "AI Agent 可以接手"
gh label create "需人工" --color "FF6347" --description "需要人工实现"
gh label create "不处理" --color "808080" --description "决定不处理"
```

## 技能接入规则

- **创建 Issue**：`gh issue create --title "..." --body "..."`，多行内容用 heredoc
- **读取 Issue**：`gh issue view <number> --comments`，可用 `--json` 提取结构化数据
- **发布需求文档**：创建 GitHub Issue
- **获取相关 Issue**：`gh issue view <number> --comments`
