package vn.dtu.udn.dacn.bmichatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementResponse {

    private Double weightKg;
    private Double heightCm;
    private LocalDate dateRecorded;
}

