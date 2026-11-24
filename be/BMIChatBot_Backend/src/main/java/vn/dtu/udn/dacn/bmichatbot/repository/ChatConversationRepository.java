package vn.dtu.udn.dacn.bmichatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.dtu.udn.dacn.bmichatbot.entity.ChatConversation;
import vn.dtu.udn.dacn.bmichatbot.entity.User;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    // Lấy tất cả conversations của một user, sort theo startedAt DESC (mới nhất trước)
    List<ChatConversation> findByUserOrderByStartedAtDesc(User user);

    // Tìm conversation theo ID và user (để check ownership)
    Optional<ChatConversation> findByConversationIdAndUser(Long conversationId, User user);
}


