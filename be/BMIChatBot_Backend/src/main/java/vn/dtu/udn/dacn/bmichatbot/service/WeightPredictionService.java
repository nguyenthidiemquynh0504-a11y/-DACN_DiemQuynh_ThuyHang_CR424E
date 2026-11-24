package vn.dtu.udn.dacn.bmichatbot.service;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;
import vn.dtu.udn.dacn.bmichatbot.dto.response.WeightPredictionResponse;
import vn.dtu.udn.dacn.bmichatbot.dto.response.WeightPredictionResponse.ModelMetrics;
import vn.dtu.udn.dacn.bmichatbot.dto.response.WeightPredictionResponse.WeightDataPoint;
import vn.dtu.udn.dacn.bmichatbot.entity.BodyMeasurement;
import vn.dtu.udn.dacn.bmichatbot.entity.User;
import vn.dtu.udn.dacn.bmichatbot.repository.BodyMeasurementRepository;
import vn.dtu.udn.dacn.bmichatbot.repository.UserRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WeightPredictionService {

    private final BodyMeasurementRepository measurementRepository;
    private final UserRepository userRepository;

    public WeightPredictionService(BodyMeasurementRepository measurementRepository,
                                    UserRepository userRepository) {
        this.measurementRepository = measurementRepository;
        this.userRepository = userRepository;
    }

    /**
     * Predict weight for next N days using Linear Regression
     */
    public WeightPredictionResponse predictWeight(String userEmail, int daysAhead) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        // Get historical measurements (last 60 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(60);
        
        List<BodyMeasurement> measurements = measurementRepository
                .findByUserUserIdAndDateRecordedBetween(user.getUserId(), startDate, endDate);

        // Need at least 3 data points for meaningful prediction
        if (measurements.size() < 3) {
            throw new IllegalStateException(
                "Insufficient data for prediction. Need at least 3 weight measurements."
            );
        }

        // Prepare data for Linear Regression
        SimpleRegression regression = new SimpleRegression();
        LocalDate firstDate = measurements.get(0).getDateRecorded();
        
        for (BodyMeasurement measurement : measurements) {
            long daysSinceStart = ChronoUnit.DAYS.between(firstDate, measurement.getDateRecorded());
            double weight = measurement.getWeightKg().doubleValue();
            regression.addData(daysSinceStart, weight);
        }

        // Calculate model metrics
        ModelMetrics metrics = new ModelMetrics();
        metrics.setRSquared(regression.getRSquare());
        metrics.setSlope(regression.getSlope());
        metrics.setIntercept(regression.getIntercept());
        metrics.setDataPoints(measurements.size());
        metrics.setTrend(determineTrend(regression.getSlope()));

        // Historical data points
        List<WeightDataPoint> historicalData = measurements.stream()
                .map(m -> new WeightDataPoint(m.getDateRecorded(), m.getWeightKg().doubleValue()))
                .collect(Collectors.toList());

        // Generate predictions
        List<WeightDataPoint> predictions = new ArrayList<>();
        LocalDate lastDate = measurements.get(measurements.size() - 1).getDateRecorded();
        
        for (int i = 1; i <= daysAhead; i++) {
            LocalDate predictionDate = lastDate.plusDays(i);
            long daysSinceStart = ChronoUnit.DAYS.between(firstDate, predictionDate);
            double predictedWeight = regression.predict(daysSinceStart);
            
            // Ensure weight is realistic (minimum 30kg, maximum 300kg)
            predictedWeight = Math.max(30.0, Math.min(300.0, predictedWeight));
            
            predictions.add(new WeightDataPoint(predictionDate, 
                    Math.round(predictedWeight * 10.0) / 10.0));
        }

        // Generate insights
        String insights = generateInsights(metrics, predictions, user);

        return new WeightPredictionResponse(historicalData, predictions, metrics, insights);
    }

    /**
     * Determine trend from slope
     */
    private String determineTrend(double slope) {
        if (Math.abs(slope) < 0.01) { // Less than 10g per day
            return "STABLE";
        } else if (slope > 0) {
            return "INCREASING";
        } else {
            return "DECREASING";
        }
    }

    /**
     * Generate AI insights based on prediction
     */
    private String generateInsights(ModelMetrics metrics, List<WeightDataPoint> predictions, User user) {
        StringBuilder insights = new StringBuilder();
        
        double slopePerDay = metrics.getSlope();
        double slopePerWeek = slopePerDay * 7;
        
        if ("DECREASING".equals(metrics.getTrend())) {
            insights.append(String.format("📉 Tuyệt vời! Bạn đang giảm cân với tốc độ %.2f kg/tuần. ", Math.abs(slopePerWeek)));
            
            if (Math.abs(slopePerWeek) > 1.0) {
                insights.append("Tốc độ này hơi nhanh, hãy đảm bảo bạn vẫn ăn đủ dinh dưỡng. ");
            } else if (Math.abs(slopePerWeek) >= 0.5) {
                insights.append("Đây là tốc độ giảm cân lý tưởng và bền vững! ");
            } else {
                insights.append("Tốc độ này an toàn và lành mạnh. ");
            }
            
            // Estimate days to goal
            WeightDataPoint lastPrediction = predictions.get(predictions.size() - 1);
            insights.append(String.format("Dự đoán sau %d ngày bạn sẽ đạt %.1f kg.", 
                    predictions.size(), lastPrediction.getWeightKg()));
                    
        } else if ("INCREASING".equals(metrics.getTrend())) {
            insights.append(String.format("📈 Cân nặng đang tăng với tốc độ %.2f kg/tuần. ", slopePerWeek));
            insights.append("Hãy xem lại chế độ ăn và tăng cường vận động để quay lại đúng hướng. ");
            
        } else {
            insights.append("➡️ Cân nặng đang ổn định. ");
            insights.append("Tiếp tục duy trì thói quen tốt của bạn! ");
        }
        
        // Model accuracy note
        if (metrics.getRSquared() < 0.5) {
            insights.append("\n⚠️ Lưu ý: Dữ liệu còn ít nên dự đoán có thể chưa chính xác. ");
            insights.append("Hãy log cân nặng thường xuyên hơn để cải thiện độ chính xác.");
        } else if (metrics.getRSquared() >= 0.8) {
            insights.append("\n✅ Mô hình dự đoán có độ chính xác cao dựa trên dữ liệu của bạn.");
        }
        
        return insights.toString();
    }
}

