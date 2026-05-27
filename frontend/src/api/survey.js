import request, { get, del } from './request'

/** 上传调查数据 */
export const uploadData = (pid, fileEl, ftcId, skipRows, overwrite) => {
  const fd = new FormData()
  fd.append('file', fileEl.files[0])
  fd.append('file_type_config_id', ftcId)
  fd.append('skip_rows', skipRows || 0)
  fd.append('overwrite', overwrite ? 'true' : 'false')
  return request.post(`/projects/${pid}/upload-data`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 分页查询调查数据 */
export const listSurveyData = (pid, page, pageSize, ftcId) =>
  get(`/projects/${pid}/survey-data`, { page, pageSize, fileTypeConfigId: ftcId })

/** 删除单条调查数据 */
export const deleteSurveyData = (pid, dataId) =>
  del(`/projects/${pid}/survey-data/${dataId}`)

/** 清空指定类型的所有数据 */
export const clearData = (pid, ftcId) =>
  del(`/projects/${pid}/survey-data/clear`, { fileTypeConfigId: ftcId })

/** 导出调查数据为 Excel（返回 Blob） */
export const exportSurveyData = async (pid, ftcId) => {
  const response = await request.get(`/projects/${pid}/survey-data/export`, {
    params: { fileTypeConfigId: ftcId },
    responseType: 'blob'
  })
  return response.data || response
}

/** 查询上传历史 */
export const history = (pid, ftcId) =>
  get(`/projects/${pid}/upload-history`, { fileTypeConfigId: ftcId })
