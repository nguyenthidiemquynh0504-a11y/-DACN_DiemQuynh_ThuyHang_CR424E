package vn.dtu.udn.dacn.bmichatbot.service;

import org.springframework.stereotype.Service;
import vn.dtu.udn.dacn.bmichatbot.dto.response.AchievementResponse;
import vn.dtu.udn.dacn.bmichatbot.entity.User;
import vn.dtu.udn.dacn.bmichatbot.entity.UserAchievement;
import vn.dtu.udn.dacn.bmichatbot.repository.UserAchievementRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AchievementService {

    private final UserAchievementRepository achievementRepository;
    private final UserRepository userRepository;

    public AchievementService(UserAchievementRepository achievementRepository,
                              UserRepository userRepository) {
        this.achievementRepository = achievementRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all achievements for user (unlocked + locked)
     */
    public List<AchievementResponse> getUserAchievements(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        List<UserAchievement> unlockedAchievements = 
                achievementRepository.findByUserUserIdOrderByAchievedAtDesc(user.getUserId());

        List<AchievementResponse> allAchievements = new ArrayList<>();

        // Define all possible achievements
        String[] achievementTypes = {
                "FIRST_LOG", "FIRST_EXERCISE", "7_DAY_STREAK", "30_DAY_STREAK",
                "100_DAY_STREAK", "GOAL_ACHIEVED", "50_LOGS", "100_LOGS",
                "EARLY_BIRD", "NIGHT_OWL"
        };

        for (String type : achievementTypes) {
            UserAchievement unlocked = unlockedAchievements.stream()
                    .filter(a -> a.getAchievementType().equals(type))
                    .findFirst()
                    .orElse(null);

            AchievementResponse response = new AchievementResponse();
            response.setAchievementType(type);
            response.setTitle(getAchievementTitle(type));
            response.setDescription(getAchievementDescription(type));
            response.setIconName(getAchievementIcon(type));

            if (unlocked != null) {
                response.setAchievementId(unlocked.getAchievementId());
                response.setAchievedAt(unlocked.getAchievedAt());
                response.setIsUnlocked(true);
            } else {
                response.setIsUnlocked(false);
            }

            allAchievements.add(response);
        }

        return allAchievements;
    }

    /**
     * Unlock achievement for user
     */
    public void unlockAchievement(Integer userId, String achievementType) {
        // Check if already unlocked
        if (achievementRepository.existsByUserUserIdAndAchievementType(userId, achievementType)) {
            return; // Already unlocked
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserAchievement achievement = new UserAchievement();
        achievement.setUser(user);
        achievement.setAchievementType(achievementType);
        achievement.setAchievedAt(LocalDateTime.now());

        achievementRepository.save(achievement);
    }

    /**
     * Check and unlock achievements after user action
     */
    public void checkAndUnlockAchievements(Integer userId, String actionType) {
        // Logic to check various achievements based on user stats
        // This will be called after food log, exercise log, etc.
        
        // Example: Check FIRST_LOG
        if ("FOOD_LOG".equals(actionType)) {
            unlockAchievement(userId, "FIRST_LOG");
        }
        
        if ("EXERCISE_LOG".equals(actionType)) {
            unlockAchievement(userId, "FIRST_EXERCISE");
        }
        
        // More complex checks can be added (50_LOGS, 100_LOGS, etc.)
    }

    // Helper methods
    private String getAchievementTitle(String type) {
        return switch (type) {
            case "FIRST_LOG" -> "🎉 Bước Đầu Tiên";
            case "FIRST_EXERCISE" -> "💪 Người Mới Bắt Đầu";
            case "7_DAY_STREAK" -> "🔥 Tuần Hoàn Hảo";
            case "30_DAY_STREAK" -> "⭐ Tháng Kiên Trì";
            case "100_DAY_STREAK" -> "👑 Huyền Thoại";
            case "GOAL_ACHIEVED" -> "🎯 Đạt Mục Tiêu";
            case "50_LOGS" -> "📝 Người Ghi Chép";
            case "100_LOGS" -> "📚 Chuyên Gia";
            case "EARLY_BIRD" -> "🌅 Chim Sớm";
            case "NIGHT_OWL" -> "🦉 Cú Đêm";
            default -> type;
        };
    }

    private String getAchievementDescription(String type) {
        return switch (type) {
            case "FIRST_LOG" -> "Log bữa ăn đầu tiên";
            case "FIRST_EXERCISE" -> "Hoàn thành bài tập đầu tiên";
            case "7_DAY_STREAK" -> "Log liên tục 7 ngày";
            case "30_DAY_STREAK" -> "Log liên tục 30 ngày";
            case "100_DAY_STREAK" -> "Log liên tục 100 ngày";
            case "GOAL_ACHIEVED" -> "Đạt được mục tiêu cân nặng";
            case "50_LOGS" -> "Hoàn thành 50 logs";
            case "100_LOGS" -> "Hoàn thành 100 logs";
            case "EARLY_BIRD" -> "Log trước 7 giờ sáng";
            case "NIGHT_OWL" -> "Log sau 10 giờ tối";
            default -> "";
        };
    }

    private String getAchievementIcon(String type) {
        return switch (type) {
            case "FIRST_LOG" -> "ic_achievement_first";
            case "FIRST_EXERCISE" -> "ic_achievement_exercise";
            case "7_DAY_STREAK" -> "ic_achievement_7day";
            case "30_DAY_STREAK" -> "ic_achievement_30day";
            case "100_DAY_STREAK" -> "ic_achievement_100day";
            case "GOAL_ACHIEVED" -> "ic_achievement_goal";
            case "50_LOGS" -> "ic_achievement_50logs";
            case "100_LOGS" -> "ic_achievement_100logs";
            case "EARLY_BIRD" -> "ic_achievement_early";
            case "NIGHT_OWL" -> "ic_achievement_night";
            default -> "ic_achievement_default";
        };
    }
}

