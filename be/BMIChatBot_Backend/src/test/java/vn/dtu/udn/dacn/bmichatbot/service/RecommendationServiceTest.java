package vn.dtu.udn.dacn.bmichatbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.dtu.udn.dacn.bmichatbot.entity.BodyMeasurement;
import vn.dtu.udn.dacn.bmichatbot.entity.Exercise;
import vn.dtu.udn.dacn.bmichatbot.entity.Food;
import vn.dtu.udn.dacn.bmichatbot.entity.User;
import vn.dtu.udn.dacn.bmichatbot.entity.UserExerciseLog;
import vn.dtu.udn.dacn.bmichatbot.entity.UserFoodLog;
import vn.dtu.udn.dacn.bmichatbot.entity.UserProfile;
import vn.dtu.udn.dacn.bmichatbot.repository.BodyMeasurementRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.ExerciseRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.FoodRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.UserExerciseLogRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.UserFoodLogRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.UserProfileRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
    private static final String DEMO_EMAIL = "demo@example.com";

    @Mock private UserRepository users;
    @Mock private UserProfileRepository profiles;
    @Mock private BodyMeasurementRepository measurements;
    @Mock private UserFoodLogRepository foodLogs;
    @Mock private UserExerciseLogRepository exerciseLogs;
    @Mock private FoodRepository foods;
    @Mock private ExerciseRepository exercises;
    @InjectMocks private RecommendationService service;

    @BeforeEach
    void setUpSimilarUsers() {
        User demo = new User();
        demo.setUserId(1);
        User peer = new User();
        peer.setUserId(2);
        UserProfile profile = new UserProfile();
        profile.setGoalType("Giữ cân");
        BodyMeasurement measurement = new BodyMeasurement();
        measurement.setWeightKg(BigDecimal.valueOf(68));

        when(users.findByEmail(DEMO_EMAIL)).thenReturn(Optional.of(demo));
        when(users.findAll()).thenReturn(List.of(demo, peer));
        when(profiles.findByUserUserId(anyInt())).thenReturn(Optional.of(profile));
        when(measurements.findByUserUserIdOrderByDateRecordedDesc(anyInt())).thenReturn(List.of(measurement));
    }

    @Test
    void recommendsAnUnloggedCatalogFoodWhenPeerFoodsWereAlreadyLogged() {
        Food recent = new Food();
        recent.setFoodId(11);
        Food alternative = new Food();
        alternative.setFoodId(12);
        alternative.setFoodName("Đậu hũ sốt cà");
        alternative.setServingUnit("đĩa");
        alternative.setCaloriesPerUnit(165);
        UserFoodLog sharedLog = new UserFoodLog();
        sharedLog.setFood(recent);
        when(foodLogs.findByUserUserIdAndDateEatenAfter(anyInt(), any())).thenReturn(List.of(sharedLog));
        when(foods.findAll()).thenReturn(List.of(recent, alternative));

        var response = service.recommendFoods(DEMO_EMAIL, 1);

        assertEquals(1, response.getItems().size());
        assertEquals(12, response.getItems().get(0).getId());
        assertEquals(0, response.getTotalSimilarUsers());
    }

    @Test
    void recommendsAnUnloggedCatalogExerciseWhenPeerExercisesWereAlreadyLogged() {
        Exercise recent = new Exercise();
        recent.setExerciseId(21);
        Exercise alternative = new Exercise();
        alternative.setExerciseId(22);
        alternative.setExerciseName("Đi bộ thư giãn");
        alternative.setCaloriesBurnedPerHour(190);
        UserExerciseLog sharedLog = new UserExerciseLog();
        sharedLog.setExercise(recent);
        when(exerciseLogs.findByUserUserIdAndDateExercisedAfter(anyInt(), any())).thenReturn(List.of(sharedLog));
        when(exercises.findAll()).thenReturn(List.of(recent, alternative));

        var response = service.recommendExercises(DEMO_EMAIL, 1);

        assertEquals(1, response.getItems().size());
        assertEquals(22, response.getItems().get(0).getId());
        assertEquals(0, response.getTotalSimilarUsers());
    }
}
