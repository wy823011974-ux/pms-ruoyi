import { post, get, put } from './request'

export const login = (data) => post('/auth/login', data)
export const register = (data) => post('/auth/register', data)
export const getUserInfo = () => get('/auth/me')
export const changePassword = (data) => put('/auth/change-password', data)
