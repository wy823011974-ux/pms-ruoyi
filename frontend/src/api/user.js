import { get, post, put, del } from './request'
export const listUsers = (params) => get('/users', params)
export const createUser = (data) => post('/users', data)
export const updateUser = (id, data) => put(`/users/${id}`, data)
export const deleteUser = (id) => del(`/users/${id}`)
export const resetPassword = (id, data) => put(`/users/${id}/reset-password`, data)
