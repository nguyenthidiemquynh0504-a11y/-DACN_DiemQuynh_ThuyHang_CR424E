package vn.dtu.udn.dacn.bmichatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    /**
     * Câu trả lời gửi về cho người dùng.
     */
    private String reply;

    /**
     * Nguồn của câu trả lời: LOCAL, AI_INTENT, RULE hoặc FALLBACK.
     */
    private String source;

    /**
     * TraceId để truy vết log end-to-end.
     */
    private String traceId;

    /**
     * Thời gian xử lý (ms) tính từ khi nhận request tới khi trả response.
     */
    private long durationMs;

    /**
     * Conversation ID hiện tại.
     * Dùng để FE tiếp tục cuộc trò chuyện ở lần request tiếp theo.
     */
    private Long conversationId;
}
