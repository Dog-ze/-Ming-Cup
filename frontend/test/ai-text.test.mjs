import test from 'node:test'
import assert from 'node:assert/strict'
import { formatBrewGuideText } from '../src/utils/ai-text.mjs'

test('泡法文本会去除 Markdown 标题、加粗和无序列表标记，但保留内容与换行', () => {
  const source = '### 泡法建议\n\n- **建议水温**：100℃沸水。\n- **投茶量**：8g。'

  assert.equal(
    formatBrewGuideText(source),
    '泡法建议\n\n建议水温：100℃沸水。\n投茶量：8g。'
  )
})

test('泡法文本能清理未成对的星号，避免装饰符号直接展示', () => {
  assert.equal(formatBrewGuideText('* 注意：睡前少饮。**'), '注意：睡前少饮。')
})
