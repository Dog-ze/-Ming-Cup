/**
 * 将网络分段中的 SSE 文本还原为事件。
 * 一次网络回调不一定刚好包含一个完整事件，因此保留未完成部分等待下一段数据。
 */
export function createSseParser(onEvent) {
  let buffer = ''

  function consumeBlock(block) {
    const lines = block.split('\n')
    const event = lines.find((line) => line.startsWith('event:'))?.slice(6).trim() || 'message'
    const dataText = lines
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trim())
      .join('\n')

    if (!dataText) return
    try {
      onEvent({ event, data: JSON.parse(dataText) })
    } catch {
      // 格式异常的单条事件直接忽略，保留后续正常事件继续显示。
    }
  }

  return {
    push(chunk) {
      buffer += String(chunk || '').replace(/\r\n/g, '\n')
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        consumeBlock(buffer.slice(0, boundary))
        buffer = buffer.slice(boundary + 2)
        boundary = buffer.indexOf('\n\n')
      }
    },
    finish() {
      if (buffer.trim()) consumeBlock(buffer)
      buffer = ''
    }
  }
}
