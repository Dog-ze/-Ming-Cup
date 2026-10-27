import test from 'node:test'
import assert from 'node:assert/strict'
import { createOperationId } from '../src/utils/operation-id.mjs'

test('每次操作生成可安全传给后端的不同请求标识', () => {
  const first = createOperationId('chat')
  const second = createOperationId('chat')

  assert.match(first, /^chat_[A-Za-z0-9_-]+$/)
  assert.match(second, /^chat_[A-Za-z0-9_-]+$/)
  assert.notEqual(first, second)
})
