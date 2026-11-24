package vn.dtu.udn.dacn.bmichatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.dtu.udn.dacn.bmichatbot.entity.User;
import vn.dtu.udn.dacn.bmichatbot.entity.UserFavoriteExercise;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteExerciseRepository extends JpaRepository<UserFavoriteExercise, Integer> {
    
    List<UserFavoriteExercise> findByUserOrderByAddedAtDesc(User user);
    
    Optional<UserFavoriteExercise> findByUserAndExercise_ExerciseId(User user, Integer exerciseId);
    
    boolean existsByUserAndExercise_ExerciseId(User user, Integer exerciseId);
    
    void deleteByUserAndExercise_ExerciseId(User user, Integer exerciseId);
    
    @Query("SELECT e.exercise FROM UserFavoriteExercise e WHERE e.user = :user ORDER BY e.addedAt DESC")
    List<vn.dtu.udn.dacn.bmichatbot.entity.Exercise> findExercisesByUser(@Param("user") User user);
}

