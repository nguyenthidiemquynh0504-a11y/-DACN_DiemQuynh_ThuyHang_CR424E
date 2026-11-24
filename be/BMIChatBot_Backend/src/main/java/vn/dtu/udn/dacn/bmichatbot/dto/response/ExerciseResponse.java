package vn.dtu.udn.dacn.bmichatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseResponse {
    private Integer exerciseId;
    private String exerciseName;
    private Integer caloriesBurnedPerHour;
}