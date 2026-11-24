package vn.dtu.udn.dacn.bmichatbot.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.dtu.udn.dacn.bmichatbot.dto.response.ChatResponse;
import vn.dtu.udn.dacn.bmichatbot.dto.response.DailySummaryItem;
import vn.dtu.udn.dacn.bmichatbot.dto.response.DashboardSummary;
import vn.dtu.udn.dacn.bmichatbot.dto.response.RecommendationResponse;
import vn.dtu.udn.dacn.bmichatbot.entity.ChatbotRule;
import vn.dtu.udn.dacn.bmichatbot.entity.Exercise;
import vn.dtu.udn.dacn.bmichatbot.entity.Food;
import vn.dtu.udn.dacn.bmichatbot.ml.IntentClassifierService;
import vn.dtu.udn.dacn.bmichatbot.ml.IntentType;
import vn.dtu.udn.dacn.bmichatbot.repository.ChatbotRuleRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.ExerciseRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.FoodRepository;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Pattern ENERGY = Pattern.compile("\\b(?:calo|calories|kcal|nang luong)\\b");
    private static final Pattern FOOD_SUGGESTION = Pattern.compile("\\b(?:goi y mon an|nen an gi)\\b");
    private static final Pattern EXERCISE_SUGGESTION = Pattern.compile("\\b(?:goi y bai tap|goi y van dong|nen tap gi)\\b");
    private static final String MEASUREMENT_NUMBER = "(?<![\\p{L}\\p{N}.,])(-?\\d+(?:[.,]\\d+)?)\\s*";
    private static final Pattern WEIGHT = Pattern.compile(MEASUREMENT_NUMBER + "kg\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT = Pattern.compile(MEASUREMENT_NUMBER + "(cm|m)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final String HELP = "Mình có thể tra kcal của món ăn hoặc bài tập, xem số liệu của bạn "
            + "và gợi ý món ăn, bài tập từ danh mục. Bạn có thể hỏi: "
            + "“Bánh mì pate bao nhiêu kcal?”, “Hôm nay tôi đã nạp bao nhiêu kcal?” "
            + "hoặc “Gợi ý bài tập cho tôi”.";

    private final ChatbotRuleRepository chatbotRuleRepository;
    private final IntentClassifierService intentClassifier;
    private final FoodRepository foodRepository;
    private final ExerciseRepository exerciseRepository;
    private final DashboardService dashboardService;
    private final RecommendationService recommendationService;

    public ChatbotService(ChatbotRuleRepository chatbotRuleRepository,
                          IntentClassifierService intentClassifier,
                          FoodRepository foodRepository,
                          ExerciseRepository exerciseRepository,
                          DashboardService dashboardService,
                          RecommendationService recommendationService) {
        this.chatbotRuleRepository = chatbotRuleRepository;
        this.intentClassifier = intentClassifier;
        this.foodRepository = foodRepository;
        this.exerciseRepository = exerciseRepository;
        this.dashboardService = dashboardService;
        this.recommendationService = recommendationService;
    }

    public ChatResponse handleMessage(String userMessage, String traceId, String userEmail) {
        long start = System.currentTimeMillis();
        if (!StringUtils.hasText(userMessage)) {
            return buildReply("Bạn hãy nhập một câu hỏi để mình hỗ trợ.", "FALLBACK", traceId, start);
        }

        String message = normalize(userMessage);
        String directAnswer = answerSpecificQuestion(userMessage, message, userEmail);
        if (directAnswer != null) {
            return buildReply(directAnswer, "LOCAL", traceId, start);
        }

        IntentClassifierService.IntentResult result = intentClassifier.classifyWithConfidence(userMessage);
        if (result.getConfidence() > 0.6) {
            String intentAnswer = answerIntent(result.getIntent(), userEmail);
            if (intentAnswer != null) {
                return buildReply(intentAnswer, "AI_INTENT", traceId, start);
            }
        }

        return chatbotRuleRepository.findAll().stream()
                .filter(rule -> matchesRule(rule, message))
                .max(Comparator.comparing(ChatbotRule::getPriority))
                .map(rule -> buildReply(rule.getResponseTemplate(), "RULE", traceId, start))
                .orElseGet(() -> buildReply("Mình chưa hiểu câu hỏi này. " + HELP, "FALLBACK", traceId, start));
    }

    private String answerSpecificQuestion(String originalMessage, String message, String userEmail) {
        if (containsPhrase(message, "bmi")) {
            String calculatedBmi = calculateBmi(originalMessage);
            if (calculatedBmi != null) {
                return calculatedBmi;
            }
            if (containsPhrase(message, "bmi la gi") || containsPhrase(message, "cong thuc bmi")) {
                return "BMI là chỉ số khối cơ thể. Công thức: cân nặng (kg) ÷ bình phương chiều cao (m). "
                        + "Bạn có thể cập nhật số đo và xem BMI ở Tổng quan.";
            }
            if (containsPhrase(message, "bmi cua toi") || containsPhrase(message, "bmi cua minh")
                    || containsPhrase(message, "bmi hien tai")) {
                return answerIntent(IntentType.VIEW_BMI, userEmail);
            }
        }

        if (ENERGY.matcher(message).find()) {
            String energyAnswer = answerEnergyQuestion(message, userEmail);
            if (energyAnswer != null) {
                return energyAnswer;
            }
        }

        if (FOOD_SUGGESTION.matcher(message).find() && !containsPhrase(message, "chay")) {
            return answerIntent(IntentType.GET_FOOD_RECOMMENDATION, userEmail);
        }
        if (EXERCISE_SUGGESTION.matcher(message).find()) {
            return answerIntent(IntentType.GET_EXERCISE_RECOMMENDATION, userEmail);
        }
        if (containsPhrase(message, "can nang hien tai") || containsPhrase(message, "can nang cua toi")
                || containsPhrase(message, "can nang cua minh")) {
            return answerIntent(IntentType.VIEW_WEIGHT, userEmail);
        }
        return null;
    }

    private String answerEnergyQuestion(String message, String userEmail) {
        Food food = bestCatalogMatch(foodRepository.findAll(), Food::getFoodName, message);
        if (food != null) {
            return String.format(VIETNAMESE, "Theo danh mục, %s có %d kcal / %s.",
                    food.getFoodName(), food.getCaloriesPerUnit(), foodUnit(food.getServingUnit()));
        }
        Exercise exercise = bestCatalogMatch(exerciseRepository.findAll(), Exercise::getExerciseName, message);
        if (exercise != null) {
            return String.format(VIETNAMESE, "Theo danh mục, %s tiêu hao khoảng %d kcal / giờ.",
                    exercise.getExerciseName(), exercise.getCaloriesBurnedPerHour());
        }
        if (containsPhrase(message, "hom nay")) {
            if (containsPhrase(message, "dot") || containsPhrase(message, "tieu hao")) {
                return exerciseEnergyToday(userEmail);
            }
            if (containsPhrase(message, "nap") || containsPhrase(message, "da an")
                    || containsPhrase(message, "kcal hom nay") || containsPhrase(message, "calo hom nay")
                    || containsPhrase(message, "calories hom nay")) {
                return dashboardReply(userEmail, IntentType.VIEW_CALORIES_TODAY);
            }
        }
        if (containsPhrase(message, "bao nhieu") && !containsPhrase(message, "hom nay")) {
            return "Bạn cho mình tên món ăn hoặc bài tập trong Nhật ký để mình tra kcal nhé.";
        }
        return null;
    }

    private String exerciseEnergyToday(String userEmail) {
        if (!StringUtils.hasText(userEmail)) {
            return "Bạn hãy đăng nhập để mình đọc số liệu trong Nhật ký.";
        }
        LocalDate today = LocalDate.now();
        int caloriesBurned = dashboardService.getWeeklySummary(userEmail).getDailySummaries().stream()
                .filter(day -> today.equals(day.getDate()))
                .findFirst()
                .map(DailySummaryItem::getCaloriesBurned)
                .orElse(0);
        return String.format(VIETNAMESE, "Theo Nhật ký, hôm nay bạn đã ghi nhận tiêu hao khoảng %,d kcal từ bài tập.",
                caloriesBurned);
    }

    private String answerIntent(IntentType intent, String userEmail) {
        return switch (intent) {
            case LOG_FOOD -> "Vào Nhật ký → Món ăn, tìm món, nhập số lượng theo gram và chọn bữa. "
                    + "Bấm Ghi lại bữa ăn rồi mở Lịch sử để kiểm tra.";
            case LOG_EXERCISE -> "Vào Nhật ký → Bài tập, chọn hoạt động, nhập số phút và bấm Ghi lại bài tập.";
            case GET_FOOD_RECOMMENDATION -> recommendationReply(userEmail, true);
            case GET_EXERCISE_RECOMMENDATION -> recommendationReply(userEmail, false);
            case VIEW_WEIGHT, VIEW_BMI, VIEW_CALORIES_TODAY -> dashboardReply(userEmail, intent);
            case UPDATE_WEIGHT -> "Vào Tổng quan, bấm Cập nhật chỉ số hiện tại rồi nhập cân nặng và chiều cao.";
            case VIEW_DASHBOARD -> "Ở Tổng quan, bạn sẽ thấy số đo gần nhất, năng lượng đã ghi hôm nay "
                    + "và biểu đồ cân nặng.";
            case PREDICT_WEIGHT -> "Vào Tổng quan và cuộn đến phần dự đoán xu hướng cân nặng. "
                    + "Biểu đồ dùng các lần đo bạn đã lưu.";
            case VIEW_PROGRESS, VIEW_PROFILE, UPDATE_GOAL, VIEW_ACHIEVEMENTS ->
                    "Vào Hồ sơ để xem mục tiêu, tiến độ, chuỗi ngày và thành tích.";
            case GREETING -> "Chào bạn! " + HELP;
            case HELP -> HELP;
            default -> null;
        };
    }

    private String dashboardReply(String userEmail, IntentType intent) {
        if (!StringUtils.hasText(userEmail)) {
            return "Bạn hãy đăng nhập để mình đọc số liệu trong Nhật ký và Tổng quan.";
        }
        DashboardSummary summary = dashboardService.getSummary(userEmail);
        return switch (intent) {
            case VIEW_BMI -> summary.getBmi() == null
                    ? "Chưa có đủ số đo để tính BMI. Bạn có thể cập nhật cân nặng và chiều cao ở Tổng quan."
                    : String.format(VIETNAMESE, "BMI từ số đo gần nhất của bạn là %.1f.", summary.getBmi());
            case VIEW_WEIGHT -> summary.getCurrentWeight() == null
                    ? "Bạn chưa lưu cân nặng. Bạn có thể cập nhật số đo ở Tổng quan."
                    : String.format(VIETNAMESE, "Cân nặng gần nhất của bạn là %.1f kg.", summary.getCurrentWeight());
            case VIEW_CALORIES_TODAY -> String.format(VIETNAMESE,
                    "Theo Nhật ký, hôm nay bạn đã ghi nhận %,d kcal từ các bữa ăn.", summary.getTotalCaloriesToday());
            default -> throw new IllegalArgumentException("Unsupported dashboard intent: " + intent);
        };
    }

    private String recommendationReply(String userEmail, boolean food) {
        if (!StringUtils.hasText(userEmail)) {
            return "Bạn hãy đăng nhập để xem gợi ý từ danh mục Nhật ký.";
        }
        RecommendationResponse recommendations = food
                ? recommendationService.recommendFoods(userEmail, 3)
                : recommendationService.recommendExercises(userEmail, 3);
        if (recommendations.getItems().isEmpty()) {
            return food ? "Danh mục hiện chưa có món ăn để gợi ý." : "Danh mục hiện chưa có bài tập để gợi ý.";
        }
        String choices = recommendations.getItems().stream()
                .map(item -> String.format(VIETNAMESE, "• %s · %d kcal / %s",
                        item.getName(), item.getCalories(), food ? foodUnit(item.getUnit()) : "giờ"))
                .collect(Collectors.joining("\n"));
        String header = food ? "Một vài món trong danh mục Nhật ký:" : "Một vài bài tập trong danh mục Nhật ký:";
        return header + "\n" + choices;
    }

    private static String calculateBmi(String message) {
        Matcher weight = WEIGHT.matcher(message);
        Matcher height = HEIGHT.matcher(message);
        if (!weight.find() || !height.find()) {
            return null;
        }
        double kg = Double.parseDouble(weight.group(1).replace(',', '.'));
        double meters = Double.parseDouble(height.group(1).replace(',', '.'));
        if (height.group(2).equalsIgnoreCase("cm")) {
            meters /= 100;
        }
        double bmi = kg / (meters * meters);
        if (!Double.isFinite(bmi) || kg <= 0 || meters <= 0 || bmi <= 0) {
            return "Bạn nhập cân nặng và chiều cao lớn hơn 0, ví dụ: 68 kg và 1,65 m.";
        }
        return String.format(VIETNAMESE, "BMI theo số đo bạn vừa nhập là %.1f.", bmi);
    }

    private static <T> T bestCatalogMatch(List<T> items, Function<T, String> name, String message) {
        return items.stream()
                .filter(item -> containsPhrase(message, normalize(name.apply(item))))
                .max(Comparator.comparingInt(item -> name.apply(item).length()))
                .orElse(null);
    }

    private static String foodUnit(String unit) {
        String normalized = normalize(unit);
        return switch (normalized) {
            case "g", "gram", "100 g", "100g", "100 gram" -> "100 g";
            default -> unit;
        };
    }

    private static boolean matchesRule(ChatbotRule rule, String message) {
        return StringUtils.hasText(rule.getKeywords())
                && StringUtils.hasText(rule.getResponseTemplate())
                && Arrays.stream(rule.getKeywords().split(","))
                        .map(ChatbotService::normalize)
                        .anyMatch(keyword -> containsPhrase(message, keyword));
    }

    private static String normalize(String text) {
        String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(decomposed).replaceAll("").replace('đ', 'd');
        return SEPARATORS.matcher(withoutDiacritics).replaceAll(" ").strip();
    }

    private static boolean containsPhrase(String message, String phrase) {
        return !phrase.isBlank() && (" " + message + " ").contains(" " + phrase + " ");
    }

    private static ChatResponse buildReply(String reply, String source, String traceId, long start) {
        return ChatResponse.builder()
                .reply(reply)
                .source(source)
                .traceId(traceId)
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
