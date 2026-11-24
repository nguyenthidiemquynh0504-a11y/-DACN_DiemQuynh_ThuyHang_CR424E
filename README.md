# BMI ChatBot

Đồ án Spring Boot, MySQL và Android Kotlin. Chatbot tra kcal theo danh mục, đọc số liệu Nhật ký và tính BMI từ số đo.

## Backend

1. Bật MySQL, rồi tạo database `bmi_chatbot_db` trên cổng 3306.
2. Trong PowerShell, đặt `JAVA_HOME` tới JDK 17 hoặc 21. Cấu hình mặc định dùng MySQL `root` với mật khẩu trống. Đặt `DB_USERNAME` và `DB_PASSWORD` trong phiên PowerShell nếu MySQL dùng cấu hình riêng.
3. Chạy các lệnh từ thư mục gốc của repo.

```powershell
Set-Location .\be\BMIChatBot_Backend
if (-not $env:JWT_SECRET) {
    $keyBytes = New-Object byte[] 64
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($keyBytes) } finally { $rng.Dispose() }
    $env:JWT_SECRET = [Convert]::ToBase64String($keyBytes)
}
.\mvnw.cmd spring-boot:run
```

Web quản trị ở `http://localhost:8080/admin/login`. Đăng nhập bằng tài khoản đã được cấp vai trò quản trị.

## Android

Mở `fe/DACN_DiemQuynh_ThuyHang_CR424E` trong Android Studio và cài Android SDK 36. App dùng cổng 8080 của laptop thông qua ADB.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
& $adb -d reverse tcp:8080 tcp:8080
```

Dùng `-d` cho điện thoại USB và `-e` cho emulator. Sau đó chọn cấu hình `app` và bấm **Run** trong Android Studio. Nếu kết nối USB được thiết lập lại, chạy lại lệnh `reverse`.

## Dữ liệu demo

Đăng ký `demo.bmi@example.com` trong app bằng mật khẩu tự đặt. Sau khi backend đã tạo bảng, có thể import `be/BMIChatBot_Backend/src/main/resources/db/sample_data.sql` vào `bmi_chatbot_db` để thêm danh mục và dữ liệu minh họa.
