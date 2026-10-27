import request from '@/utils/request'

export const login = (data) => request.post('/admin/auth/login', data)
export const getCurrentAdmin = () => request.get('/admin/auth/me')
export const logout = () => request.post('/admin/auth/logout')
