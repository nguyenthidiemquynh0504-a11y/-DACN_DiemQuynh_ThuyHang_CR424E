package vn.dtu.udn.dacn.bmichatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.dtu.udn.dacn.bmichatbot.entity.ChatRequestLog;

public interface ChatRequestLogRepository extends JpaRepository<ChatRequestLog, Long> {
}


