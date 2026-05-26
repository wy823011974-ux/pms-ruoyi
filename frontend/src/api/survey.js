import request, { get, del } from './request'
export const uploadData = (pid, fileEl, ftcId, skipRows, overwrite) => {
  const fd = new FormData(); fd.append('file', fileEl.files[0]); fd.append('file_type_config_id', ftcId);
  fd.append('skip_rows', skipRows || 0); fd.append('overwrite', overwrite ? 'true' : 'false');
  return request.post(`/projects/${pid}/upload-data`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const listSurveyData = (pid, page, pageSize, ftcId) => get(`/projects/${pid}/survey-data`, { page, pageSize, fileTypeConfigId: ftcId })
export const clearData = (pid, ftcId) => del(`/projects/${pid}/survey-data/clear`, { fileTypeConfigId: ftcId })
export const history = (pid, ftcId) => get(`/projects/${pid}/upload-history`, { fileTypeConfigId: ftcId })
