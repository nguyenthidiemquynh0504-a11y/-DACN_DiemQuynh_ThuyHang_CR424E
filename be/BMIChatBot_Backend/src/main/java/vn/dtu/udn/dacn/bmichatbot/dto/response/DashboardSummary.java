package vn.dtu.udn.dacn.bmichatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private Double currentWeight;
    private Double bmi;
    private Integer totalCaloriesToday;
}
