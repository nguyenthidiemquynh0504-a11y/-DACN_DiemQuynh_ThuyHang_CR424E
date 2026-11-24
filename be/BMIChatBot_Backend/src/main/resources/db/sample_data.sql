-- Dữ liệu giả lập cho BMI ChatBot. Chọn database đã được Spring Boot tạo bảng.
-- Đăng ký demo.bmi@example.com qua app trước nếu muốn có hồ sơ và lịch sử cá nhân.
-- Để dùng email demo khác, chạy SET @bmi_demo_email = 'email-demo@example.com';
-- trong cùng phiên SQL trước khi chạy file. Mỗi tài khoản demo nhận một bộ lịch sử.
-- Danh mục được thêm theo tên và đơn vị. Dữ liệu có sẵn được giữ lại.
-- Web admin: mở /admin/login và dùng tài khoản riêng đã được cấp role ADMIN.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @bmi_demo_email = COALESCE(NULLIF(TRIM(@bmi_demo_email), ''), 'demo.bmi@example.com');
SET @bmi_demo_today = CURRENT_DATE();
SET @bmi_demo_lock = CONCAT('bmi-demo-', LEFT(SHA2(DATABASE(), 256), 32));
SET @bmi_demo_locked = GET_LOCK(@bmi_demo_lock, 15);

START TRANSACTION;

-- Món Việt, món phổ biến, đồ uống, bữa nhẹ và lựa chọn chay.
CREATE TEMPORARY TABLE bmi_seed_foods (
    food_name VARCHAR(255) NOT NULL PRIMARY KEY,
    serving_unit VARCHAR(255) NOT NULL,
    calories_per_unit INT NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO bmi_seed_foods (food_name, serving_unit, calories_per_unit) VALUES
-- Món chính
('Phở bò', 'tô', 350),
('Phở gà', 'tô', 300),
('Bún bò Huế', 'tô', 400),
('Bún chả', 'suất', 450),
('Cơm tấm', 'đĩa', 500),
('Cơm gà', 'đĩa', 450),
('Bánh mì thịt', 'ổ', 350),
('Bánh mì pate', 'ổ', 300),
('Mì xào hải sản', 'đĩa', 550),
('Hủ tiếu Nam Vang', 'tô', 380),

-- Cơm/bún
('Cơm trắng', 'chén', 200),
('Bún', 'chén', 150),
('Mì trứng', 'suất', 220),
('Phở khô', 'đĩa', 250),

-- Thịt/cá
('Thịt lợn nướng', '100g', 250),
('Thịt bò xào', '100g', 200),
('Cá kho', '100g', 180),
('Gà luộc', '100g', 165),
('Trứng gà chiên', 'quả', 90),
('Tôm luộc', '100g', 99),

-- Rau củ
('Rau xào', 'đĩa', 50),
('Canh rau', 'tô', 30),
('Salad trộn', 'đĩa', 80),
('Dưa chuột', '100g', 16),

-- Đồ uống
('Cà phê sữa', 'ly', 150),
('Trà đá', 'ly', 0),
('Nước cam', 'ly', 110),
('Sữa tươi', 'hộp 250ml', 120),
('Nước suối', 'chai', 0),

-- Snack/Tráng miệng
('Bánh ngọt', 'cái', 250),
('Yaourt', 'hộp', 100),
('Chuối', 'quả', 105),
('Táo', 'quả', 95),
('Cam', 'quả', 62),

-- Fast food
('Gà rán KFC', 'miếng', 290),
('Burger bò', 'cái', 540),
('Pizza phô mai', 'miếng', 285),
('Khoai tây chiên', 'suất', 365),
('Bánh cuốn chả lụa', 'đĩa', 360),
('Bún riêu cua', 'tô', 390),
('Cháo yến mạch', 'tô', 230),
('Xôi gà', 'gói', 460),
('Cơm gạo lứt', 'chén', 215),
('Cá hồi áp chảo', '100g', 208),
('Ức gà áp chảo', '100g', 170),
('Đậu hũ sốt cà', 'đĩa', 165),
('Gỏi cuốn tôm', 'cuốn', 105),
('Bông cải hấp', '100g', 35),
('Khoai lang luộc', '100g', 86),
('Sữa chua không đường', 'hộp', 70),
('Thanh long', '100g', 55),
('Hạnh nhân', '30g', 175),
('Trà chanh ít đường', 'ly', 65);

INSERT INTO foods (food_name, serving_unit, calories_per_unit)
SELECT seed.food_name, seed.serving_unit, seed.calories_per_unit
FROM bmi_seed_foods seed
WHERE @bmi_demo_locked = 1 AND NOT EXISTS (
    SELECT 1 FROM foods existing
    WHERE CONVERT(existing.food_name USING utf8mb4) COLLATE utf8mb4_unicode_ci = seed.food_name
      AND CONVERT(existing.serving_unit USING utf8mb4) COLLATE utf8mb4_unicode_ci = seed.serving_unit
);

CREATE TEMPORARY TABLE bmi_seed_food_ids AS
SELECT seed.food_name, MIN(food.food_id) AS food_id
FROM bmi_seed_foods seed
JOIN foods food
  ON CONVERT(food.food_name USING utf8mb4) COLLATE utf8mb4_unicode_ci = seed.food_name
 AND CONVERT(food.serving_unit USING utf8mb4) COLLATE utf8mb4_unicode_ci = seed.serving_unit
GROUP BY seed.food_name;

-- Cardio, sức mạnh, vận động nhẹ và thể thao.
CREATE TEMPORARY TABLE bmi_seed_exercises (
    exercise_name VARCHAR(255) NOT NULL PRIMARY KEY,
    calories_burned_per_hour INT NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO bmi_seed_exercises (exercise_name, calories_burned_per_hour) VALUES
-- Cardio
('Chạy bộ', 600),
('Đi bộ nhanh', 300),
('Đạp xe', 500),
('Bơi lội', 450),
('Nhảy dây', 700),
('Leo cầu thang', 550),

-- Gym
('Chạy máy treadmill', 600),
('Đạp xe đạp tĩnh', 400),
('Tập tạ', 350),
('Squat', 400),
('Plank', 300),
('Push-up', 350),
('Sit-up', 300),

-- Thể thao
('Đá bóng', 500),
('Bóng chuyền', 360),
('Cầu lông', 450),
('Tennis', 420),
('Bóng rổ', 480),

-- Yoga/Nhẹ
('Yoga', 180),
('Thái cực quyền', 240),
('Aerobic', 400),
('Zumba', 450),

-- Thể dục nhịp điệu
('Jumping Jack', 500),
('Burpee', 600),
('Mountain Climber', 550),
('Đi bộ thư giãn', 190),
('Đi bộ đường dài', 390),
('Giãn cơ toàn thân', 150),
('Pilates', 220),
('Tập dây kháng lực', 295),
('Rowing trong nhà', 510),
('Bóng bàn', 280),
('Pickleball', 330);

INSERT INTO exercises (exercise_name, calories_burned_per_hour)
SELECT seed.exercise_name, seed.calories_burned_per_hour
FROM bmi_seed_exercises seed
WHERE @bmi_demo_locked = 1 AND NOT EXISTS (
    SELECT 1 FROM exercises existing
    WHERE CONVERT(existing.exercise_name USING utf8mb4) COLLATE utf8mb4_unicode_ci = seed.exercise_name
);

CREATE TEMPORARY TABLE bmi_seed_exercise_ids AS
SELECT seed.exercise_name, MIN(exercise.exercise_id) AS exercise_id
FROM bmi_seed_exercises seed
JOIN exercises exercise
  ON CONVERT(exercise.exercise_name USING utf8mb4) COLLATE utf8mb4_unicode_ci = seed.exercise_name
GROUP BY seed.exercise_name;

-- Bộ FAQ chạy trong backend với intent và từ khóa, độc lập với Gemini.
CREATE TEMPORARY TABLE bmi_seed_rules (
    intent VARCHAR(255) NOT NULL PRIMARY KEY,
    keywords TEXT NOT NULL,
    response_template TEXT NOT NULL,
    priority INT NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO bmi_seed_rules (intent, keywords, response_template, priority) VALUES
('greeting', 'chào,hello,xin chào', 'Chào bạn! Mình có thể hướng dẫn xem BMI, ghi bữa ăn, ghi bài tập và tìm lại lịch sử trong ứng dụng.', 10),
('bmi_info', 'bmi là gì,chỉ số khối cơ thể,công thức bmi', 'BMI được tính bằng cân nặng (kg) chia cho bình phương chiều cao (m). Bạn có thể cập nhật số đo trong Hồ sơ và xem kết quả ở Tổng quan.', 12),
('food_suggestion', 'gợi ý món,thực đơn,nên ăn gì', 'Bạn có thể tìm phở gà, gỏi cuốn tôm, cơm gạo lứt hoặc đậu hũ sốt cà trong danh mục. Mở Nhật ký, chọn Món ăn và nhập khẩu phần để ghi lại.', 8),
('vegetarian_food', 'món chay,ăn chay,thực đơn chay', 'Bạn có thể tìm cơm gạo lứt, đậu hũ sốt cà, bông cải hấp hoặc khoai lang luộc trong danh mục. Khi chọn món ngoài hàng, hỏi thêm về nước sốt và gia vị nếu bạn ăn chay.', 23),
('exercise_suggestion', 'gợi ý vận động,bài tập nào,tập gì,bài tập nhẹ', 'Danh mục có đi bộ nhanh, yoga, đạp xe, tập dây kháng lực và cầu lông. Chọn bài phù hợp với khả năng của bạn rồi ghi số phút trong Nhật ký.', 8),
('calories_info', 'calories,bao nhiêu calo,năng lượng,kcal', 'Trong Nhật ký, năng lượng món ăn được tính theo khẩu phần. Màn hình Tổng quan tổng hợp năng lượng ăn vào và vận động theo ngày.', 7),
('log_food_help', 'lưu bữa trưa,ghi bữa ăn,ghi món ăn,ghi bữa sáng,cách lưu món', 'Mở Nhật ký → Món ăn, tìm tên món, chọn loại bữa và điền số khẩu phần. Bấm lưu rồi mở Lịch sử để kiểm tra.', 20),
('log_exercise_help', 'lưu buổi tập,ghi bài tập,ghi vận động,nhập phút tập', 'Mở Nhật ký → Bài tập, chọn hoạt động, nhập số phút và lưu. Các buổi đã lưu xuất hiện trong Lịch sử.', 20),
('log_history_help', 'lịch sử nhật ký,bữa ăn đã lưu,xem buổi tập cũ,xem nhật ký cũ', 'Mở Nhật ký và chọn Lịch sử. Chuyển giữa hai tab Món ăn và Bài tập để xem các mục đã lưu.', 20),
('chat_history_help', 'mở lại cuộc trò chuyện,lịch sử hội thoại,xem tin nhắn cũ,lịch sử chat', 'Mở Chat và chạm biểu tượng lịch sử. Chọn một cuộc trò chuyện để đọc lại câu hỏi và câu trả lời.', 22),
('measurements_help', 'cập nhật số đo,đổi chiều cao,sửa cân nặng,ghi cân nặng', 'Trong Hồ sơ, mở phần cập nhật số đo và lưu chiều cao, cân nặng. Tổng quan sẽ hiển thị BMI và xu hướng từ các lần ghi.', 20),
('favorites_help', 'món yêu thích,bài tập yêu thích,lưu mục yêu thích', 'Trong màn hình ghi nhật ký, lưu món ăn hoặc bài tập thường dùng vào mục yêu thích để lần sau chọn nhanh hơn.', 18),
('dashboard_help', 'đọc biểu đồ,biểu đồ nằm ở đâu,tổng quan ở đâu', 'Mở Tổng quan để xem số đo, xu hướng cân nặng và năng lượng theo ngày. Dùng các khoảng thời gian có sẵn để xem chi tiết.', 18),
('goals_help', 'đổi mục tiêu,mục tiêu cân nặng,mục tiêu năng lượng', 'Mở Hồ sơ để xem mục tiêu cân nặng và năng lượng. Các mục tiêu trong dữ liệu demo chỉ để minh họa giao diện.', 17),
('streak_help', 'chuỗi ngày,điểm danh,thành tích,huy hiệu', 'Vào Hồ sơ để xem chuỗi ngày và các thành tích. Ghi nhật ký đều đặn để theo dõi tiến độ của bạn.', 17),
('weight_loss', 'theo dõi giảm cân,muốn giảm cân,giảm mỡ,xuống cân', 'Bạn có thể lưu cân nặng trong Hồ sơ và theo dõi xu hướng ở Tổng quan. Nhật ký bữa ăn và vận động giúp bạn nhìn lại thói quen của mình.', 8),
('weight_gain', 'theo dõi tăng cân,muốn tăng cân,cách tăng cân', 'Đặt mục tiêu cân nặng ở Hồ sơ, cập nhật số đo định kỳ và xem xu hướng ở Tổng quan. Bạn cũng có thể ghi bữa ăn để dễ nhìn lại khẩu phần.', 8),
('motivation', 'mất động lực,khó duy trì,muốn bỏ cuộc', 'Bạn có thể bắt đầu lại bằng một việc nhỏ, như ghi bữa ăn gần nhất hoặc một buổi đi bộ. Xem phần thành tích để nhìn lại những mốc đã làm được.', 7);

INSERT INTO chatbot_rules (intent, keywords, response_template, priority, created_by_user_id)
SELECT seed.intent, seed.keywords, seed.response_template, seed.priority, NULL
FROM bmi_seed_rules seed
WHERE @bmi_demo_locked = 1 AND NOT EXISTS (
    SELECT 1 FROM chatbot_rules existing
    WHERE CONVERT(existing.intent USING utf8mb4) COLLATE utf8mb4_unicode_ci = seed.intent
);

-- Hồ sơ và 30 ngày số đo cho tài khoản USER demo đã đăng ký.
SET @bmi_demo_user_id = (
    SELECT user_id FROM users
    WHERE CONVERT(email USING utf8mb4) COLLATE utf8mb4_unicode_ci = @bmi_demo_email
      AND role = 'USER'
);
SET @bmi_demo_history_user_id = (
    SELECT user_id FROM users
    WHERE user_id = @bmi_demo_user_id AND @bmi_demo_locked = 1 AND NOT EXISTS (
        SELECT 1 FROM chat_conversations
        WHERE user_id = @bmi_demo_user_id AND channel = 'DEMO_ANDROID_BMI'
    )
);

INSERT INTO user_profiles (user_id, date_of_birth, gender, goal_type, goal_weight_kg, daily_calorie_goal)
SELECT @bmi_demo_history_user_id, '1999-09-15', 'Nữ', 'Giảm cân', 66.50, 1850
WHERE @bmi_demo_history_user_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM user_profiles WHERE user_id = @bmi_demo_history_user_id
);

CREATE TEMPORARY TABLE bmi_seed_days AS
SELECT ones.n + tens.n * 10 AS days_ago,
       DATE_SUB(@bmi_demo_today, INTERVAL (ones.n + tens.n * 10) DAY) AS log_date
FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
      UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
      UNION ALL SELECT 8 UNION ALL SELECT 9) ones
CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2) tens;

INSERT INTO body_measurements (user_id, weight_kg, height_cm, date_recorded)
SELECT @bmi_demo_history_user_id,
       ROUND(68.10 + day.days_ago * 0.095 + (MOD(day.days_ago * 3, 7) - 3) * 0.08, 2),
       165.00, day.log_date
FROM bmi_seed_days day
WHERE @bmi_demo_history_user_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM body_measurements existing
    WHERE existing.user_id = @bmi_demo_history_user_id AND existing.date_recorded = day.log_date
)
ORDER BY day.log_date;

-- Năm thực đơn luân phiên, đủ các khung giờ sáng, trưa, tối và bữa nhẹ.
CREATE TEMPORARY TABLE bmi_seed_meals (
    menu_number INT NOT NULL,
    meal_time TIME NOT NULL,
    meal_type VARCHAR(16) NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    quantity DECIMAL(6,2) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO bmi_seed_meals (menu_number, meal_time, meal_type, food_name, quantity) VALUES
(0, '07:10:00', 'BREAKFAST', 'Phở bò', 1),
(0, '07:20:00', 'BREAKFAST', 'Cà phê sữa', 1),
(0, '11:45:00', 'LUNCH', 'Cơm trắng', 1.5),
(0, '11:45:00', 'LUNCH', 'Ức gà áp chảo', 1.5),
(0, '11:45:00', 'LUNCH', 'Canh rau', 1),
(0, '15:30:00', 'SNACK', 'Sữa chua không đường', 1),
(0, '18:40:00', 'DINNER', 'Cơm gà', 1),
(0, '19:05:00', 'DINNER', 'Chuối', 1),
(1, '07:00:00', 'BREAKFAST', 'Cháo yến mạch', 1),
(1, '07:10:00', 'BREAKFAST', 'Sữa tươi', 1),
(1, '11:55:00', 'LUNCH', 'Bún chả', 1),
(1, '15:25:00', 'SNACK', 'Táo', 1),
(1, '15:25:00', 'SNACK', 'Hạnh nhân', 1),
(1, '18:50:00', 'DINNER', 'Cơm gạo lứt', 1.5),
(1, '18:50:00', 'DINNER', 'Cá hồi áp chảo', 1.4),
(1, '18:50:00', 'DINNER', 'Bông cải hấp', 1),
(2, '07:25:00', 'BREAKFAST', 'Bánh cuốn chả lụa', 1),
(2, '07:35:00', 'BREAKFAST', 'Nước cam', 1),
(2, '12:10:00', 'LUNCH', 'Cơm tấm', 1),
(2, '12:10:00', 'LUNCH', 'Canh rau', 1),
(2, '16:00:00', 'SNACK', 'Thanh long', 1.5),
(2, '18:30:00', 'DINNER', 'Bún riêu cua', 1),
(2, '18:35:00', 'DINNER', 'Gỏi cuốn tôm', 2),
(2, '19:00:00', 'DINNER', 'Trà chanh ít đường', 1),
(3, '07:30:00', 'BREAKFAST', 'Bánh mì thịt', 1),
(3, '07:40:00', 'BREAKFAST', 'Sữa tươi', 1),
(3, '11:50:00', 'LUNCH', 'Cơm gạo lứt', 1.75),
(3, '11:50:00', 'LUNCH', 'Đậu hũ sốt cà', 1.5),
(3, '11:50:00', 'LUNCH', 'Bông cải hấp', 1.5),
(3, '15:10:00', 'SNACK', 'Khoai lang luộc', 1.5),
(3, '18:25:00', 'DINNER', 'Mì xào hải sản', 1),
(3, '19:15:00', 'DINNER', 'Cam', 1),
(4, '07:15:00', 'BREAKFAST', 'Xôi gà', 1),
(4, '07:25:00', 'BREAKFAST', 'Trà đá', 1),
(4, '12:00:00', 'LUNCH', 'Hủ tiếu Nam Vang', 1),
(4, '12:00:00', 'LUNCH', 'Gỏi cuốn tôm', 2),
(4, '15:40:00', 'SNACK', 'Yaourt', 1),
(4, '18:45:00', 'DINNER', 'Cơm trắng', 1.5),
(4, '18:45:00', 'DINNER', 'Cá kho', 1.2),
(4, '18:45:00', 'DINNER', 'Rau xào', 1);

INSERT INTO user_food_logs (user_id, food_id, date_eaten, meal_type, quantity)
SELECT @bmi_demo_history_user_id, food.food_id, TIMESTAMP(day.log_date, meal.meal_time),
       meal.meal_type, meal.quantity
FROM bmi_seed_days day
JOIN bmi_seed_meals meal ON meal.menu_number = MOD(day.days_ago, 5)
JOIN bmi_seed_food_ids food ON food.food_name = meal.food_name
WHERE @bmi_demo_history_user_id IS NOT NULL
  AND TIMESTAMP(day.log_date, meal.meal_time) <= CURRENT_TIMESTAMP()
  AND NOT EXISTS (
      SELECT 1 FROM user_food_logs existing
      WHERE existing.user_id = @bmi_demo_history_user_id AND existing.food_id = food.food_id
        AND existing.date_eaten = TIMESTAMP(day.log_date, meal.meal_time)
  )
ORDER BY day.log_date, meal.meal_time, food.food_id;

CREATE TEMPORARY TABLE bmi_seed_workouts (
    day_number INT NOT NULL,
    workout_time TIME NOT NULL,
    exercise_name VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO bmi_seed_workouts (day_number, workout_time, exercise_name, duration_minutes) VALUES
(0, '06:30:00', 'Đi bộ nhanh', 35),
(1, '17:30:00', 'Đạp xe', 42),
(2, '06:45:00', 'Yoga', 30),
(3, '18:00:00', 'Tập dây kháng lực', 32),
(3, '18:40:00', 'Giãn cơ toàn thân', 12),
(5, '17:40:00', 'Cầu lông', 55),
(6, '07:15:00', 'Chạy bộ', 27);

INSERT INTO user_exercise_logs (user_id, exercise_id, date_exercised, duration_minutes)
SELECT @bmi_demo_history_user_id, exercise.exercise_id,
       TIMESTAMP(day.log_date, workout.workout_time),
       workout.duration_minutes + MOD(day.days_ago, 4)
FROM bmi_seed_days day
JOIN bmi_seed_workouts workout ON workout.day_number = MOD(day.days_ago, 7)
JOIN bmi_seed_exercise_ids exercise ON exercise.exercise_name = workout.exercise_name
WHERE @bmi_demo_history_user_id IS NOT NULL
  AND TIMESTAMP(day.log_date, workout.workout_time) <= CURRENT_TIMESTAMP()
  AND NOT EXISTS (
      SELECT 1 FROM user_exercise_logs existing
      WHERE existing.user_id = @bmi_demo_history_user_id AND existing.exercise_id = exercise.exercise_id
        AND existing.date_exercised = TIMESTAMP(day.log_date, workout.workout_time)
  )
ORDER BY day.log_date, workout.workout_time;

INSERT INTO user_favorite_foods (user_id, food_id, added_at)
SELECT @bmi_demo_history_user_id, seed.food_id, CURRENT_TIMESTAMP()
FROM bmi_seed_food_ids seed
WHERE @bmi_demo_history_user_id IS NOT NULL
  AND seed.food_name IN ('Phở bò', 'Cơm gạo lứt', 'Gỏi cuốn tôm', 'Sữa chua không đường')
  AND NOT EXISTS (
      SELECT 1 FROM user_favorite_foods existing
      WHERE existing.user_id = @bmi_demo_history_user_id AND existing.food_id = seed.food_id
  );

INSERT INTO user_favorite_exercises (user_id, exercise_id, added_at)
SELECT @bmi_demo_history_user_id, seed.exercise_id, CURRENT_TIMESTAMP()
FROM bmi_seed_exercise_ids seed
WHERE @bmi_demo_history_user_id IS NOT NULL
  AND seed.exercise_name IN ('Đi bộ nhanh', 'Yoga', 'Đạp xe')
  AND NOT EXISTS (
      SELECT 1 FROM user_favorite_exercises existing
      WHERE existing.user_id = @bmi_demo_history_user_id AND existing.exercise_id = seed.exercise_id
  );

SET @bmi_demo_log_days = (
    SELECT COUNT(DISTINCT DATE(date_eaten)) FROM user_food_logs
    WHERE user_id = @bmi_demo_history_user_id
      AND date_eaten >= DATE_SUB(@bmi_demo_today, INTERVAL 29 DAY)
      AND date_eaten < DATE_ADD(@bmi_demo_today, INTERVAL 1 DAY)
);
SET @bmi_demo_last_log = (
    SELECT MAX(DATE(date_eaten)) FROM user_food_logs WHERE user_id = @bmi_demo_history_user_id
);

INSERT INTO user_streaks (user_id, current_streak, longest_streak, last_log_date, updated_at)
SELECT @bmi_demo_history_user_id, @bmi_demo_log_days, @bmi_demo_log_days,
       @bmi_demo_last_log, @bmi_demo_today
WHERE @bmi_demo_history_user_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM user_streaks WHERE user_id = @bmi_demo_history_user_id
);

INSERT INTO user_achievements (user_id, achievement_type, achieved_at, metadata)
SELECT @bmi_demo_history_user_id, seed.achievement_type,
       DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL seed.days_ago DAY), NULL
FROM (
    SELECT 'FIRST_LOG' AS achievement_type, 29 AS days_ago, 1 AS min_days
    UNION ALL SELECT 'FIRST_EXERCISE', 29, 1
    UNION ALL SELECT '7_DAY_STREAK', 23, 7
    UNION ALL SELECT '30_DAY_STREAK', 0, 30
) seed
WHERE @bmi_demo_history_user_id IS NOT NULL
  AND @bmi_demo_log_days >= seed.min_days
  AND NOT EXISTS (
      SELECT 1 FROM user_achievements existing
      WHERE existing.user_id = @bmi_demo_history_user_id AND existing.achievement_type = seed.achievement_type
  );

-- Ba hội thoại dùng để xem lại lịch sử trên app.
CREATE TEMPORARY TABLE bmi_seed_chat AS
SELECT seed.channel, TIMESTAMP(DATE_SUB(@bmi_demo_today, INTERVAL seed.days_ago DAY), '19:20:00') AS started_at
FROM (
    SELECT 'DEMO_ANDROID_BMI' AS channel, 18 AS days_ago
    UNION ALL SELECT 'DEMO_ANDROID_FOOD', 8
    UNION ALL SELECT 'DEMO_ANDROID_WORKOUT', 1
) seed;

INSERT INTO chat_conversations (user_id, channel, started_at, ended_at)
SELECT @bmi_demo_history_user_id, seed.channel, seed.started_at,
       DATE_ADD(seed.started_at, INTERVAL 6 MINUTE)
FROM bmi_seed_chat seed
WHERE @bmi_demo_history_user_id IS NOT NULL
ORDER BY seed.started_at;

CREATE TEMPORARY TABLE bmi_seed_messages (
    channel VARCHAR(50) NOT NULL,
    seconds_after_start INT NOT NULL,
    sender VARCHAR(20) NOT NULL,
    content TEXT NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO bmi_seed_messages (channel, seconds_after_start, sender, content) VALUES
('DEMO_ANDROID_BMI', 0, 'USER', 'Mình xem chỉ số BMI ở đâu?'),
('DEMO_ANDROID_BMI', 15, 'BOT', 'Bạn mở Tổng quan để xem BMI. Kết quả dựa trên cân nặng và chiều cao gần nhất trong Hồ sơ.'),
('DEMO_ANDROID_BMI', 75, 'USER', 'Mình muốn cập nhật số đo mới.'),
('DEMO_ANDROID_BMI', 88, 'BOT', 'Mở Hồ sơ, chọn cập nhật số đo, điền cân nặng và chiều cao rồi lưu.'),
('DEMO_ANDROID_FOOD', 0, 'USER', 'Mình ăn hai cuốn gỏi tôm, lưu bữa trưa thế nào?'),
('DEMO_ANDROID_FOOD', 12, 'BOT', 'Trong Nhật ký → Món ăn, tìm Gỏi cuốn tôm, chọn Bữa trưa, nhập 2 cuốn rồi bấm lưu.'),
('DEMO_ANDROID_FOOD', 83, 'USER', 'Mình xem lại bữa đã lưu ở đâu?'),
('DEMO_ANDROID_FOOD', 98, 'BOT', 'Mở Lịch sử trong Nhật ký, rồi chọn tab Món ăn.'),
('DEMO_ANDROID_WORKOUT', 0, 'USER', 'Chiều nay mình đạp xe 40 phút, nhập vào đâu?'),
('DEMO_ANDROID_WORKOUT', 11, 'BOT', 'Vào Nhật ký → Bài tập, chọn Đạp xe, nhập 40 phút rồi lưu.'),
('DEMO_ANDROID_WORKOUT', 126, 'USER', 'Có tìm lại cuộc trò chuyện này được không?'),
('DEMO_ANDROID_WORKOUT', 140, 'BOT', 'Được. Trong Chat, bấm biểu tượng lịch sử và chọn cuộc trò chuyện bạn muốn mở lại.');

INSERT INTO chat_messages (conversation_id, sender, content, created_at)
SELECT conversation.conversation_id, seed.sender, seed.content,
       DATE_ADD(conversation.started_at, INTERVAL seed.seconds_after_start SECOND)
FROM bmi_seed_messages seed
JOIN bmi_seed_chat planned
  ON planned.channel COLLATE utf8mb4_unicode_ci = seed.channel
JOIN chat_conversations conversation
  ON CONVERT(conversation.channel USING utf8mb4) COLLATE utf8mb4_unicode_ci = seed.channel
 AND conversation.user_id = @bmi_demo_history_user_id AND conversation.started_at = planned.started_at
ORDER BY conversation.started_at, seed.seconds_after_start;

COMMIT;

DROP TEMPORARY TABLE bmi_seed_messages, bmi_seed_chat, bmi_seed_workouts, bmi_seed_meals,
    bmi_seed_days, bmi_seed_rules, bmi_seed_exercise_ids, bmi_seed_exercises,
    bmi_seed_food_ids, bmi_seed_foods;

DO RELEASE_LOCK(@bmi_demo_lock);

SELECT CASE
    WHEN COALESCE(@bmi_demo_locked, 0) <> 1 THEN 'Database đang được import. Hãy chạy lại.'
    WHEN @bmi_demo_user_id IS NULL THEN 'Đã chuẩn bị danh mục. Đăng ký email demo rồi chạy lại để thêm lịch sử.'
    WHEN @bmi_demo_history_user_id IS NULL THEN 'Hồ sơ demo đã có lịch sử. Danh mục đã được kiểm tra.'
    ELSE 'Đã chuẩn bị danh mục, hồ sơ và lịch sử demo.'
END AS ket_qua, @bmi_demo_email AS email_demo;
