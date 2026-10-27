/**
 * AI 偶尔仍会返回 Markdown；App 不解析 Markdown，因此在显示前只保留用户需要的纯文本内容。
 */
export function formatBrewGuideText(content) {
  return String(content || '')
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map(line => line
      .replace(/^\s{0,3}#{1,6}\s+/, '')
      .replace(/^\s*[-+*]\s+/, '')
      .replaceAll('*', '')
      .replaceAll('`', '')
      .trimEnd()
    )
    .join('\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}
