package vn.dtu.udn.dacn.bmichatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.dtu.udn.dacn.bmichatbot.entity.UserAchievement;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Integer> {

    List<UserAchievement> findByUserUserIdOrderByAchievedAtDesc(Integer userId);

    Optional<UserAchievement> findByUserUserIdAndAchievementType(Integer userId, String achievementType);

    boolean existsByUserUserIdAndAchievementType(Integer userId, String achievementType);
}

