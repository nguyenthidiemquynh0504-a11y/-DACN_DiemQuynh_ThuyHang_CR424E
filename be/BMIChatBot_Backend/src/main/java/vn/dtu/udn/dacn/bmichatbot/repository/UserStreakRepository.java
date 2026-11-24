package vn.dtu.udn.dacn.bmichatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.dtu.udn.dacn.bmichatbot.entity.UserStreak;

import java.util.Optional;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, Integer> {

    Optional<UserStreak> findByUserUserId(Integer userId);
}

