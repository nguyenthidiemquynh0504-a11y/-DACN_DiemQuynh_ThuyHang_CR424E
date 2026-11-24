package vn.dtu.udn.dacn.bmichatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.dtu.udn.dacn.bmichatbot.entity.UserExerciseLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserExerciseLogRepository extends JpaRepository<UserExerciseLog, Integer> {

    List<UserExerciseLog> findByUserUserIdAndDateExercisedBetween(Integer userId, LocalDateTime start, LocalDateTime end);
    
    List<UserExerciseLog> findByUserUserIdAndDateExercisedAfter(Integer userId, LocalDateTime date);
}


