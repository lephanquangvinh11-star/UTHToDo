UTH Todo — Ứng dụng Quản lý Công việc & Học tậpỨng dụng quản lý công việc cá nhân và học tập dành cho sinh viên UTH (Trường Đại học Giao thông Vận tải TP.HCM), xây dựng bằng Kotlin, Jetpack Compose (Material 3) và Google Firebase (com.uthtodo.dpgh).   
1. Tính năng chính
- Khởi động & Cấu hình từ xa: Màn hình chào mừng (WelcomeScreen) hỗ trợ đồng bộ cấu hình/slogan qua Firebase Remote Config (remote_config.json), kết hợp luồng giới thiệu (OnboardingScreen) vẽ vector bằng Compose Canvas.
- Xác thực & Quản lý tài khoản: Hỗ trợ đăng nhập Google, chế độ khách, liên kết nâng cấp tài khoản và xóa tài khoản/dữ liệu theo chuẩn chính sách.
- Quản lý & Đồng bộ công việc: Phân nhóm Học tập và Cuộc sống, mức độ ưu tiên, hạn chót, việc nhỏ (Subtasks), tìm kiếm/lọc nhanh và đồng bộ Cloud Firestore (/users/{uid}/tasks/task_{id}) kết hợp lưu cục bộ.
- Nhắc việc & Thông báo: Đặt lịch nhắc trước hạn 15 phút (ReminderScheduler), nhận thông báo qua FCM (FCMService) và trung tâm thông báo (NotificationScreen).
- Cá nhân hóa & Thống kê: Hỗ trợ giao diện Sáng/Tối/Tự động, tùy chọn màu chủ đạo (AppPreferences.kt) và thống kê hoạt động trên ProfileScreen.
