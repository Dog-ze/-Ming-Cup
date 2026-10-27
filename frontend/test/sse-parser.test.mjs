import test from 'node:test'
import assert from 'node:assert/strict'
import { createSseParser } from '../src/utils/sse.mjs'

test('SSE 解析器可跨数据块还原 delta 和 done 事件', () => {
  const events = []
  const parser = createSseParser((event) => events.push(event))

  parser.push('event: delta\ndata: {"content":"建议')
  parser.push('"}\n\nevent: delta\ndata: {"content":"喝铁观音"}\n\nevent: done\ndata: {}\n\n')

  assert.deepEqual(events, [
    { event: 'delta', data: { content: '建议' } },
    { event: 'delta', data: { content: '喝铁观音' } },
    { event: 'done', data: {} }
  ])
})
