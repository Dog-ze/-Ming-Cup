import request from '@/utils/request'

// 操作日志是审计数据，只允许查询，不提供修改或删除接口。
export const getOperationLogs = (params) => request.get('/admin/operation-logs', { params })
