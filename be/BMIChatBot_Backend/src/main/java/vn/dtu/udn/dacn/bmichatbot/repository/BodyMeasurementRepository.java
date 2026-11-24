package vn.dtu.udn.dacn.bmichatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.dtu.udn.dacn.bmichatbot.entity.BodyMeasurement;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BodyMeasurementRepository extends JpaRepository<BodyMeasurement, Integer> {
    List<BodyMeasurement> findByUserUserId(Integer userId);
    List<BodyMeasurement> findByUserUserIdOrderByDateRecordedDesc(Integer userId);
    List<BodyMeasurement> findByUserUserIdAndDateRecordedBetween(Integer userId, LocalDate startDate, LocalDate endDate);
}


