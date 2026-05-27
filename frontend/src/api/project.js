import { get, post, put, del } from './request'
export const listProjects = (params) => get('/projects', params)
export const createProject = (data) => post('/projects', data)
export const updateProject = (id, data) => put(`/projects/${id}`, data)
export const deleteProject = (id) => del(`/projects/${id}`)
export const updateStatus = (id, data) => put(`/projects/${id}/status`, data)

export const listProjectTypes = () => get('/project-types')
export const createProjectType = (data) => post('/project-types', data)
export const deleteProjectType = (id) => del(`/project-types/${id}`)

export const listFileTypes = (typeId) => get(`/project-types/${typeId}/file-types`)
export const createFileType = (typeId, data) => post(`/project-types/${typeId}/file-types`, data)
export const deleteFileType = (typeId, cfgId) => del(`/project-types/${typeId}/file-types/${cfgId}`)

export const listFields = (typeId, cfgId) => get(`/project-types/${typeId}/file-types/${cfgId}/fields`)
export const createField = (typeId, cfgId, data) => post(`/project-types/${typeId}/file-types/${cfgId}/fields`, data)
export const updateField = (typeId, cfgId, fId, data) => put(`/project-types/${typeId}/file-types/${cfgId}/fields/${fId}`, data)
export const deleteField = (typeId, cfgId, fId) => del(`/project-types/${typeId}/file-types/${cfgId}/fields/${fId}`)

/** Schema 导入导出 */
export const exportSchema = (typeId) => get(`/project-types/${typeId}/schema/export`)
export const importSchema = (typeId, data) => post(`/project-types/${typeId}/schema/import`, data)
