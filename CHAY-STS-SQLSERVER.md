# Chạy trên Spring Tool Suite với SQL Server / SSMS

Máy đã được kiểm tra có instance `SQLEXPRESS`, TCP bật và đang lắng nghe cổng 1433. Profile `sqlserver` kết nối trực tiếp `localhost:1433`, không cần SQL Server Browser.

1. Giải nén bản mã nguồn mới. Trong STS: **File → Import → Maven → Existing Maven Projects**, chọn thư mục có `pom.xml`. Nếu đã import trước đó, cập nhật các file mới rồi nhấp phải dự án → **Maven → Update Project**.
2. Trong SSMS, đăng nhập instance của bạn bằng SQL Server Authentication, user `sa`, mật khẩu của bạn. Chạy `sql/create-sqlserver.sql` để tạo database `jwt_nimbus` nếu chưa có.
3. Trong STS: **Run → Run Configurations → Spring Boot App**, chọn cấu hình của dự án (hoặc tạo mới, main class `vn.iotstar.JwtNimbusApplication`).
4. Trong tab **Environment**, thêm:

| Name | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `sqlserver` |
| `DB_USERNAME` | `sa` |
| `DB_PASSWORD` | Nhập mật khẩu SQL Server bạn đã cung cấp |

5. **Apply → Run**. Console phải báo profile `sqlserver` đang hoạt động, rồi `Started JwtNimbusApplication`.
6. Mở **http://localhost:8005/login**, đăng ký tài khoản ứng dụng rồi đăng nhập.

Tài khoản `sa` dùng để ứng dụng kết nối database; tài khoản đăng nhập website là email/mật khẩu bạn tự đăng ký trên giao diện. Hai tài khoản này khác nhau.

Nếu không bật profile `sqlserver`, ứng dụng vẫn chạy H2. Tài khoản đã tạo trong H2 không tự chuyển sang SQL Server. Mật khẩu DB không được lưu trong source/ZIP; STS lưu biến môi trường trong cấu hình chạy cục bộ, không chia sẻ cấu hình đó cùng mật khẩu.

SQL Server dùng dữ liệu Unicode qua cấu hình Hibernate để giữ đúng họ tên tiếng Việt. Hibernate tự tạo bảng `users` khi khởi động. Cấu hình này dành cho SQL Server cục bộ: `encrypt=true;trustServerCertificate=true` mã hóa kết nối nhưng chấp nhận chứng chỉ của server mà không xác minh. Khi triển khai lên server thật cần dùng chứng chỉ tin cậy và bỏ tùy chọn tin cậy tự động này. [Tham khảo Microsoft JDBC](https://learn.microsoft.com/en-us/sql/connect/jdbc/connecting-with-ssl-encryption).

Lỗi thường gặp:

- `Login failed for user 'sa'`: kiểm tra mật khẩu, tài khoản được bật và SQL Server Authentication.
- `Cannot open database jwt_nimbus`: chạy script tạo database trên đúng instance.
- `Connection refused` hoặc timeout: kiểm tra SQL Server đang chạy và cổng TCP; nếu cổng khác, thêm `DB_URL` tương ứng.
- Cổng 8005 đã dùng: dừng lần chạy cũ trong STS hoặc thêm `SERVER_PORT=8006`, rồi mở cổng tương ứng.
