# Bài tập JWT - thay JJWT bằng Nimbus JOSE + JWT

**Dùng SQL Server/SSMS với Spring Tool Suite:** xem [CHAY-STS-SQLSERVER.md](CHAY-STS-SQLSERVER.md). Bật profile `sqlserver` và đặt `DB_PASSWORD` trong Run Configurations → Environment.

Bài làm theo phần demo trang 15-34 của `04_JWT.pdf`: Spring Boot 3, Spring Security 6, JPA, đăng ký/đăng nhập, JWT filter, API người dùng, xử lý lỗi và giao diện AJAX.

**JWT là tiêu chuẩn token; Nimbus thay thư viện JJWT (`io.jsonwebtoken`), không thay tiêu chuẩn JWT.** Dự án chỉ dùng `com.nimbusds:nimbus-jose-jwt` để ký và xác thực JWT.

## 1. Chạy nhanh

Yêu cầu: JDK 17 trở lên và Maven 3.6.3 trở lên. Đã kiểm thử với JDK 21 và Maven 3.9.11. Mở terminal trong thư mục có `pom.xml`:

```powershell
mvn test
mvn spring-boot:run
```

Mở **http://localhost:8005/login**, tạo tài khoản rồi đăng nhập. Trang hồ sơ gọi `/users/me`; nút “Xem danh sách người dùng” gọi `/users/`. Có thể dùng email `student@example.com`, mật khẩu `Demo123!`, họ tên `Nguyễn Văn A` để tự đăng ký; dự án không cài sẵn tài khoản.

Mặc định dùng H2 dạng file, dữ liệu được lưu trong `data/` của thư mục chạy. Không cần cài database để thử. Cổng mặc định là `8005` như bài giảng; nếu trùng cổng:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8006"
```

Nếu không đặt `JWT_SECRET_BASE64`, ứng dụng tự sinh khóa 256 bit mỗi lần khởi động. Tài khoản vẫn còn nhưng token cũ sẽ mất hiệu lực sau khi khởi động lại. Muốn giữ khóa ổn định, tạo khóa trong PowerShell rồi tự lưu ở nơi riêng:

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtKeyBytes)
$jwtRng.Dispose()
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($jwtKeyBytes)
mvn spring-boot:run
```

Không đưa khóa thật vào mã nguồn. HS256 cần khóa ít nhất 32 byte sau khi giải mã Base64.

## 2. Chạy MySQL theo bài giảng

Tạo database bằng MySQL client:

```sql
CREATE DATABASE jwt_springboot3 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Đặt thông tin kết nối trong terminal của bạn:

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'MAT_KHAU_MYSQL_CUA_BAN'
$env:DB_URL = 'jdbc:mysql://localhost:3306/jwt_springboot3?serverTimezone=UTC'
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Profile `mysql` dùng MySQL connector; Hibernate tạo/cập nhật bảng `users`. `DB_PASSWORD` phải được đặt khi bật profile này. Không sử dụng mật khẩu minh họa trong slide. Nếu cấu hình MySQL cục bộ yêu cầu thêm tham số TLS/xác thực, điều chỉnh `DB_URL` theo máy của bạn. Kiểm thử tự động của bài làm sử dụng H2; MySQL cần kiểm tra với máy chủ và tài khoản của bạn.

## 3. Đối chiếu 10 bước trong bài giảng

| Bước | Phần thực hiện |
|---|---|
| 1 - Dependency | `pom.xml`: bỏ 3 dependency JJWT, thêm Nimbus JOSE + JWT 10.10 |
| 2 - Entity | `entity/User.java`: bảng users, implements UserDetails, ảnh mặc định, thời gian tạo/cập nhật |
| 3 - Models | `RegisterUserModel`, `LoginUserModel`, `LoginResponse`, `UserResponse` |
| 4 - Repository, services | `UserRepository`, `AuthenticationService`, `UserService`, **`JwtService` dùng Nimbus** |
| 5 - ApplicationConfiguration | UserDetailsService, BCryptPasswordEncoder, DaoAuthenticationProvider, AuthenticationManager |
| 6 - Filter | `JwtAuthenticationFilter`: đọc Bearer token, kiểm tra, tải user, thiết lập SecurityContext |
| 7 - SecurityConfig | Stateless, chỉ mở trang giao diện và API đăng ký/đăng nhập, bảo vệ API users |
| 8 - REST Controller | `AuthenticationController`, `UserController` |
| 9 - Kiểm thử | Postman collection và `JwtApplicationTest` |
| 10 - Exception, AJAX | `GlobalExceptionHandler`, `SecurityErrorWriter`, `login.html`, `profile.html`, `mainjs.js`, `AuthController` |

Mã Java nằm dưới `src/main/java/vn/iotstar/`; giao diện và cấu hình nằm dưới `src/main/resources/`.

## 4. Phần thay JJWT bằng Nimbus

Dependency sử dụng:

```xml
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>10.10</version>
</dependency>
```

| JJWT trong bài giảng | Nimbus trong bài làm |
|---|---|
| `Jwts.builder()` | `new JWTClaimsSet.Builder()` và `new SignedJWT(...)` |
| `Claims` | `JWTClaimsSet` |
| `.signWith(..., Jwts.SIG.HS256)` | `jwt.sign(new MACSigner(secret))` |
| `.compact()` | `jwt.serialize()` |
| `Jwts.parser().verifyWith(...).parseSignedClaims(...)` | `SignedJWT.parse(token)` rồi `jwt.verify(new MACVerifier(secret))` |
| `ExpiredJwtException`, `SignatureException` | Tự kiểm tra claims, chuyển lỗi token thành `BadCredentialsException` và HTTP 401 |

Trình tự trong `JwtService.generateToken()`:

1. Tạo claims `sub` (email), `iss`, `aud`, `iat`, `nbf`, `exp`, `jti`.
2. Tạo header với `alg=HS256`, `typ=JWT`.
3. Ký bằng `MACSigner`, trả chuỗi `header.payload.signature` qua `serialize()`.

Trình tự trong `JwtService.validateToken()`:

1. Parse bằng `SignedJWT.parse()`.
2. Chỉ chấp nhận HS256 và kiểu JWT; xác minh chữ ký bằng `MACVerifier`.
3. Kiểm tra subject, issuer, audience, bắt buộc hạn dùng và thời điểm phát hành, từ chối token chưa có hiệu lực hoặc đã hết hạn.
4. Chỉ trả claims khi tất cả kiểm tra thành công. Filter tải user từ database để kiểm tra tài khoản còn tồn tại và chưa khóa/vô hiệu hóa.

**`SignedJWT.parse()` không chứng minh token hợp lệ; `verify()` chỉ kiểm tra chữ ký, không tự kiểm tra hết hạn.** Đây là điểm cần chú ý khi chuyển từ JJWT sang Nimbus. Phần HMAC dựa trên [tài liệu chính thức Nimbus](https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-hmac).

Token có hạn dùng `3600000` ms (1 giờ). Cả `exp` lẫn `expiresIn` được tính từ cùng cấu hình `security.jwt.expiration-time`, sửa sự lệch giữa cấu hình 1 giờ và đoạn code 30 giờ trong slide. NumericDate trong token có đơn vị giây theo JWT; Nimbus thực hiện chuyển đổi từ `Date`. `expiresIn` trong JSON phản hồi giữ đơn vị **mili giây** như bài giảng.

## 5. API và kết quả mong đợi

| Phương thức | URL | Nội dung/quyền | Thành công |
|---|---|---|---|
| POST | `/auth/signup` | JSON email, password, fullName; công khai | 200, thông tin user |
| POST | `/auth/login` | JSON email, password; công khai | 200, token và expiresIn |
| GET | `/users/me` | Bearer token | 200, user hiện tại |
| GET | `/users` hoặc `/users/` | Bearer token | 200, danh sách user |
| GET | `/login` | Trang giao diện công khai | 200 |
| GET | `/user/profile` | Trang khung công khai; dữ liệu lấy từ API có xác thực | 200 |

Body đăng ký:

```json
{"email":"student@example.com","password":"Demo123!","fullName":"Nguyễn Văn A"}
```

Body đăng nhập:

```json
{"email":"student@example.com","password":"Demo123!"}
```

Sau đăng nhập, lấy trường `token` và gửi:

```http
GET /users/me HTTP/1.1
Host: localhost:8005
Authorization: Bearer <token-vua-nhan>
```

Mật khẩu được băm BCrypt; API chỉ trả `UserResponse` nên không có mật khẩu hoặc hash. Email được chuẩn hóa về chữ thường và có ràng buộc unique.

| Tình huống | HTTP |
|---|---|
| Sai email hoặc mật khẩu | 401 |
| Không có token, token sai định dạng, sai chữ ký, hết hạn | 401 |
| Sai issuer/audience, alg không được chấp nhận, token chưa có hiệu lực | 401 |
| Tài khoản bị khóa/vô hiệu hóa hoặc truy cập đường dẫn bị cấm sau đăng nhập | 403 |
| Email đăng ký bị trùng | 409 |
| Dữ liệu đầu vào không hợp lệ | 400 |

## 6. Thử bằng Postman và PowerShell

Import `JWT-Nimbus.postman_collection.json`, chạy lần lượt **Đăng ký → Đăng nhập → Hồ sơ → Danh sách**. Request đăng nhập tự lưu token vào biến collection. Nếu chạy lại đăng ký với email cũ sẽ nhận 409; dùng tiếp bước đăng nhập hoặc đổi email trong biến collection.

Thử trực tiếp bằng PowerShell (khi server đang chạy):

```powershell
$base = 'http://localhost:8005'
$newUser = @{email='student@example.com';password='Demo123!';fullName='Nguyễn Văn A'} | ConvertTo-Json
Invoke-RestMethod "$base/auth/signup" -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($newUser))
$credentials = @{email='student@example.com';password='Demo123!'} | ConvertTo-Json
$login = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType 'application/json' -Body $credentials
$authHeader = @{Authorization="Bearer $($login.token)"}
Invoke-RestMethod "$base/users/me" -Headers $authHeader
Invoke-RestMethod "$base/users/" -Headers $authHeader
```

## 7. Giao diện AJAX và phạm vi bài tập

`mainjs.js` dùng Fetch API thực hiện AJAX cùng luồng bài giảng; không cần tải jQuery/Bootstrap từ mạng. Giao diện dùng `textContent` để hiển thị dữ liệu người dùng. Token lưu trong `sessionStorage` của tab; nút đăng xuất chỉ xóa token của bài tập rồi chuyển về login. Token đã sao chép ra ngoài vẫn hợp lệ tới khi hết hạn. Bản này chưa triển khai refresh token hoặc danh sách thu hồi vì phần demo trong PDF không yêu cầu.

`sessionStorage` vẫn có thể bị JavaScript đọc khi có XSS; đây là lựa chọn minh họa luồng Bearer token của bài tập. Triển khai thực tế cần HTTPS, quản lý khóa bền vững, chính sách lưu token và thu hồi phù hợp. CSRF tắt trong mô hình API stateless chỉ nhận Bearer header; nếu đổi sang cookie xác thực thì phải thiết kế lại bảo vệ CSRF.

Giữ `/users/` cho mọi người dùng đã đăng nhập theo bài giảng. Bài giảng chưa có mô hình role; dự án không tự thêm ADMIN/USER. Với ứng dụng thật cần giới hạn quyền xem danh sách người dùng theo yêu cầu nghiệp vụ.

## 8. Đóng gói

```powershell
mvn package
java -jar target/jwt-nimbus-1.0.0.jar
```

Tham khảo: bài giảng `04_JWT.pdf` trang 15-34; [Nimbus HMAC JWT](https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-hmac); [Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/reference/index.html).
