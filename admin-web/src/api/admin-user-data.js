import request from '@/utils/request'

// 用户数据模块只有查询接口，管理员在这里不会修改用户的原始记录或问卷。
export const getUserDataPage = (params) => request.get('/admin/user-data', { params })
export const getUserTeaRecords = (userId, params) => request.get(`/admin/user-data/${userId}/tea-records`, { params })
export const getUserQuestionnaire = (userId) => request.get(`/admin/user-data/${userId}/questionnaire`)
export const getQuestionnaireStatistics = (params) => request.get('/admin/user-data/questionnaire-statistics', { params })
