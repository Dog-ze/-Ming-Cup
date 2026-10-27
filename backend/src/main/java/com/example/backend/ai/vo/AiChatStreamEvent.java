package com.example.backend.ai.vo;

/** SSE 事件数据：前端收到 delta 时将 content 追加到当前 AI 消息。 */
public record AiChatStreamEvent(String content) {
}
