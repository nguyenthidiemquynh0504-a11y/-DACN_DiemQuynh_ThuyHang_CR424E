package vn.dtu.udn.dacn.bmichatbot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.dtu.udn.dacn.bmichatbot.dto.response.ChatResponse;
import vn.dtu.udn.dacn.bmichatbot.dto.response.DashboardSummary;
import vn.dtu.udn.dacn.bmichatbot.dto.response.RecommendationResponse;
import vn.dtu.udn.dacn.bmichatbot.dto.response.RecommendationResponse.RecommendedItem;
import vn.dtu.udn.dacn.bmichatbot.entity.ChatbotRule;
import vn.dtu.udn.dacn.bmichatbot.entity.Exercise;
import vn.dtu.udn.dacn.bmichatbot.entity.Food;
import vn.dtu.udn.dacn.bmichatbot.ml.IntentClassifierService;
import vn.dtu.udn.dacn.bmichatbot.ml.IntentType;
import vn.dtu.udn.dacn.bmichatbot.repository.ChatbotRuleRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.ExerciseRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.FoodRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    private static final String USER_EMAIL = "test@example.com";
    private static final String TRACE_ID = "test-trace";

    @Mock
    private ChatbotRuleRepository chatbotRuleRepository;
    @Mock
    private IntentClassifierService intentClassifierService;
    @Mock
    private FoodRepository foodRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private RecommendationService recommendationService;
    @InjectMocks
    private ChatbotService chatbotService;

    @Test
    void looksUpTheMostSpecificFoodEvenWithoutAccents() {
        when(foodRepository.findAll()).thenReturn(List.of(
                new Food(1, "Bánh mì", "ổ", 200),
                new Food(2, "Bánh mì pate", "ổ", 300)));

        ChatResponse response = ask("Banh mi pate bao nhieu kcal?");

        assertEquals("Theo danh mục, Bánh mì pate có 300 kcal / ổ.", response.getReply());
        assertEquals("LOCAL", response.getSource());
    }

    @Test
    void looksUpExerciseCaloriesPerHour() {
        when(exerciseRepository.findAll()).thenReturn(List.of(new Exercise(1, "Đi bộ nhanh", 260)));

        assertEquals("Theo danh mục, Đi bộ nhanh tiêu hao khoảng 260 kcal / giờ.",
                ask("Đi bộ nhanh đốt bao nhiêu calo?").getReply());
    }

    @Test
    void calculatesBmiWithVietnameseDecimalSeparator() {
        assertEquals("BMI theo số đo bạn vừa nhập là 25,0.",
                ask("Tôi nặng 68 kg, cao 1,65 m, BMI bao nhiêu?").getReply());
    }

    @Test
    void rejectsNegativeHeightWhenCalculatingBmi() {
        assertTrue(ask("Tính BMI với 68 kg và -1,65 m").getReply().contains("lớn hơn 0"));
    }

    @Test
    void reportsLoggedEnergyForTheAuthenticatedUser() {
        when(dashboardService.getSummary(USER_EMAIL)).thenReturn(new DashboardSummary(67.9, 24.9, 1799));

        assertEquals("Theo Nhật ký, hôm nay bạn đã ghi nhận 1.799 kcal từ các bữa ăn.",
                ask("Hôm nay tôi đã nạp bao nhiêu kcal?").getReply());
    }

    @Test
    void listsRealExerciseRecommendations() {
        when(recommendationService.recommendExercises(USER_EMAIL, 3)).thenReturn(
                new RecommendationResponse(
                        List.of(new RecommendedItem(1, "Aerobic", "Trong danh mục", 1.0, 0, "giờ", 400)),
                        "Từ danh mục", 0));

        assertTrue(ask("Gợi ý bài tập cho tôi").getReply().contains("Aerobic · 400 kcal / giờ"));
    }

    @Test
    void stillUsesAdminRulesForOtherMatchingQuestions() {
        String question = "Mình muốn tăng cân nhanh";
        when(intentClassifierService.classifyWithConfidence(question))
                .thenReturn(new IntentClassifierService.IntentResult(IntentType.UPDATE_GOAL, 0.5));

        ChatbotRule rule = new ChatbotRule();
        rule.setKeywords("tăng cân,weight");
        rule.setResponseTemplate("Đây là câu trả lời rule.");
        rule.setPriority(10);
        when(chatbotRuleRepository.findAll()).thenReturn(List.of(rule));

        ChatResponse response = ask(question);
        assertEquals("Đây là câu trả lời rule.", response.getReply());
        assertEquals("RULE", response.getSource());
    }

    @Test
    void explainsSupportedQuestionsWhenNothingMatches() {
        String question = "một câu hỏi ngoài danh mục";
        when(intentClassifierService.classifyWithConfidence(question))
                .thenReturn(new IntentClassifierService.IntentResult(IntentType.UNKNOWN, 0.5));

        ChatResponse response = ask(question);
        assertEquals("FALLBACK", response.getSource());
        assertTrue(response.getReply().startsWith("Mình chưa hiểu câu hỏi này."));
    }

    private ChatResponse ask(String message) {
        return chatbotService.handleMessage(message, TRACE_ID, USER_EMAIL);
    }
}

