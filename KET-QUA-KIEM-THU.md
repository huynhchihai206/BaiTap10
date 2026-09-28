# Kết quả kiểm thử

Ngày thực hiện: 28/09/2026. Môi trường: Windows, Java 21.0.2, Maven 3.9.11, Spring Boot 3.5.16, Nimbus JOSE + JWT 10.10.

## Kiểm thử tự động

Lệnh: `mvn -B test`

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

18 ca kiểm thử tích hợp trong `src/test/java/vn/iotstar/JwtApplicationTest.java`:

1. Đăng ký, băm mật khẩu BCrypt, đăng nhập, gọi ba URL người dùng; không trả mật khẩu và không tạo cookie session.
2. Thiếu token nhận 401.
3. Sai mật khẩu nhận 401.
4. Email trùng (khác chữ hoa/thường) nhận 409.
5. Đầu vào không hợp lệ nhận 400.
6. Token sai định dạng nhận 401.
7. Payload bị sửa nhưng giữ chữ ký cũ nhận 401.
8. Token hết hạn nhận 401.
9. Thiếu exp nhận 401.
10. Sai issuer hoặc audience nhận 401.
11. nbf hoặc iat ở tương lai nhận 401.
12. Chữ ký bằng khóa khác nhận 401.
13. Thuật toán khác HS256 nhận 401.
14. Token không ký (alg=none) nhận 401.
15. Tài khoản bị khóa nhận 403 cả lúc đăng nhập lẫn khi dùng token đã phát hành.
16. Tài khoản đã bị xóa không dùng được token cũ (401).
17. Người dùng đã đăng nhập truy cập đường dẫn bị cấm nhận 403.
18. Trang login, profile và JavaScript truy cập được.

## Kiểm thử trên trình duyệt

Chạy JAR trên cổng 8005 với H2 trong bộ nhớ riêng cho phiên kiểm tra:

- Đăng ký tài khoản thử `browser-test@example.com` thành công.
- Đăng nhập bằng biểu mẫu; tự chuyển đến `/user/profile`.
- Hồ sơ hiển thị đúng họ tên tiếng Việt và JSON từ `/users/me`.
- Nút danh sách hiển thị dữ liệu từ `/users/`.
- Đăng xuất chuyển về `/login`.

Ảnh minh chứng đi kèm: `jwt-nimbus-demo.png`. Dữ liệu kiểm tra trên trình duyệt không được đóng gói vào dự án.

## Đóng gói và dependency

- `mvn -B package -DskipTests`: BUILD SUCCESS, sau khi bộ test đã chạy thành công.
- Kiểm tra dependency tree: `com.nimbusds:nimbus-jose-jwt:10.10`; không có `io.jsonwebtoken`.
- JAR khởi động thành công và phục vụ giao diện/API.

Giới hạn xác minh: chưa chạy profile MySQL với máy chủ MySQL thực; chưa thử triển khai HTTPS hoặc tải lớn. Đây là bài tập theo demo trong bài giảng.
