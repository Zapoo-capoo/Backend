# Zapoo — Backend Services

Đây là repository backend cho dự án Zapoo (một mạng xã hội dạng sách / social network). Backend được chia thành nhiều microservices (Spring Boot) nhỏ, mỗi service đảm nhận một phần chức năng (identity, profile, post, chat, file, notification, gateway...).

Mục lục
- Tổng quan
- Kiến trúc và danh sách service
- Yêu cầu trước khi chạy
- Chạy project (nhanh)
- Chạy từng service (chi tiết)
- Cấu hình quan trọng
- Lưu ý về API và các tính năng (hiện tại & TODO)
- Hệ thống file & upload
- Góp ý phát triển

Tổng quan
--------
Project gồm nhiều microservice viết bằng Spring Boot (Java 21). Mỗi service có 1 context-path và port mặc định (xem phần "Kiến trúc và danh sách service" bên dưới). Một số service dùng MongoDB, Neo4j, MySQL và Kafka.

Kiến trúc và danh sách service
------------------------------
Các dịch vụ chính (đã tìm thấy cấu hình mặc định trong `src/main/resources/application.yaml` của từng service):

- api-gateway — Spring Cloud Gateway (một gateway, không cấu hình port mặc định trong file pom nhưng thường chạy cùng dev khi cần)
- identity-service — port: 8080, context-path: /identity (MySQL, JWT)
- profile-service — port: 8081, context-path: /profile (Neo4j)
- post-service — port: 8083, context-path: /post (MongoDB)
- file-service — port: 8084, context-path: /file (MongoDB + lưu file trên disk theo cấu hình)
- notification-service — port: 8082, context-path: /notification (MongoDB + Kafka)
- chat-service — port: 8085, context-path: /chat (MongoDB)

Ngoài ra repository chứa các tài nguyên hỗ trợ:
- docker-compose.yml — chứa cấu hình Kafka (dev)
- scripts/ — các script trợ giúp (ví dụ troubleshoot-kafka.ps1)

Yêu cầu trước khi chạy
----------------------
- Java 21
- Maven (hoặc dùng wrapper `mvnw` / `mvnw.cmd` có sẵn trong từng service)
- Docker & docker-compose (nếu muốn chạy Kafka container nhanh)
- MongoDB (mặc định services trỏ tới `mongodb://root:root@localhost:27017/<db>?authSource=admin`)
- Neo4j (profile-service mặc định kết nối bolt://localhost:7687)
- MySQL cho identity-service (mặc định `jdbc:mysql://localhost:3306/identity`)

Lưu ý: mật khẩu/uri mặc định được đặt trong `application.yaml` mẫu cho môi trường dev; nhớ kiểm tra file cấu hình từng service trước khi chạy.

Chạy project (nhanh)
--------------------
1. Khởi Kafka (nếu cần) bằng docker-compose (file `docker-compose.yml` ở root chỉ gồm kafka):
   (Windows PowerShell)

   cd D:\Project\Zapoo\Backend; docker-compose up -d

2. Khởi các service từng cái một bằng `mvnw` (ở Windows dùng `mvnw.cmd` hoặc `./mvnw` trong Git Bash). Ví dụ chạy `post-service`:

   cd post-service; .\mvnw.cmd spring-boot:run

   Tương tự cho các service khác: `profile-service`, `identity-service`, `file-service`, `notification-service`, `chat-service`, `api-gateway`.

Chạy từng service (chi tiết)
----------------------------
Mỗi service có `mvnw`/`mvnw.cmd` và có thể chạy bằng:
- Windows PowerShell:
  cd <service-folder>; .\mvnw.cmd spring-boot:run

Hoặc build jar và chạy:
- .\mvnw.cmd clean package
- java -jar target\<artifact>.jar

Cấu hình quan trọng
-------------------
- `post-service/src/main/resources/application.yaml` — port 8083, MongoDB URI và địa chỉ các service khác (profile, identity)
- `profile-service/src/main/resources/application.yaml` — port 8081, Neo4j config, multipart limits
- `file-service/src/main/resources/application.yaml` — port 8084, `app.file.storage-dir` (đường dẫn lưu file trên disk) và `download-prefix`
- `identity-service/src/main/resources/application.yaml` — port 8080, MySQL và các cấu hình JWT

API: những điểm cần biết & ví dụ
--------------------------------
- Base URL cho mỗi service theo port + context-path (ví dụ post-service: http://localhost:8083/post)
- Post service:
  - Tạo bài đăng (có thể gửi media multipart) — có logic xử lý upload media trong `PostService.createPostWithMedia` và gọi `file-service` để upload. (Xem code `post-service/src/main/java/com/capoo/post/service/PostService.java`)
  - Lấy bài viết của user (my posts) — service enrich dữ liệu username & avatar từ `profile-service` khi có thể.
  - Lấy bài viết của bạn bè (friends' posts) — service đang gọi `profileClient.getMyFriends()` để lấy danh sách bạn bè và truy vấn các post từ những userId đó.

- File service:
  - Chịu trách nhiệm upload media và trả về URL; post-service dùng `FileClient` để upload.

- Profile service:
  - Quản lý thông tin người dùng, friend relations, avatar. Các service khác tương tác qua HTTP client (OpenFeign).

Lưu ý: README không liệt kê toàn bộ endpoints chi tiết vì controllers có thể nằm ở nhiều file; xem bước tiếp theo để liệt kê endpoints (tự động) nếu cần.

Các yêu cầu / TODOs hiện tại (từ trao đổi gần đây)
-------------------------------------------------
Đây là những thay đổi/những điểm cần làm mà bạn đã nêu — tôi đưa vào README để theo dõi trong development backlog:

1. Truy vấn search user (profile-service): chỉ trả về `UserProfile` những người đã là `friend` thôi (lọc theo quan hệ friend).
2. Khi update profile/avatar thì phải cập nhật luôn thông tin participant (ví dụ username/avatar) trong data chat (để messages/participant hiển thị đúng thông tin mới).
3. Post service: cung cấp API để xem bài viết của bạn bè (đã có logic trong `PostService.getFriendsPosts` — đảm bảo controller expose endpoint phù hợp).
4. Post response phải trả về luôn `avatar` trong `UserProfileResponse` (đảm bảo dữ liệu profile được enrich lên post response).
5. Flow `POST /create-password` (hoặc endpoint tạo password trong identity flow): khi tạo mật khẩu và cập nhật username, cần đồng bộ cập nhật `username` trong profile-service luôn.
6. Cho phép đăng ảnh trong Post (multipart upload) — post-service đã có method `createPostWithMedia` và gọi `file-service` để upload; cần expose endpoint multipart/form-data ở controller và test upload/download.

Nếu bạn muốn, tôi có thể:
- Tạo checklist issue / TODO files hoặc PR cho từng mục trên (kèm các thay đổi code cần thiết).
- Thêm các unit/integration tests cho `post-service` upload media và `getFriendsPosts`.

Hệ thống file & upload
----------------------
- `file-service` lưu file vào folder được cấu hình `app.file.storage-dir` (mặc định trong repo là `D:/Project/Zapoo/file`). Thay đổi config nếu muốn dùng nơi khác.
- `file-service` cũng có `download-prefix` để xây dựng URL trả về từ service.

Gợi ý phát triển
-----------------
- Khi thay đổi cấu trúc dữ liệu user (username, avatar), cân nhắc gửi event qua Kafka để các service khác (chat, post, notification) có thể subscribe và cập nhật participant cache/copy của họ. Hiện notification & identity cấu hình Kafka.
- Viết integration test nhỏ giữa `post-service` và `profile-service` (mock profile client bằng WireMock hoặc sử dụng testcontainers cho Neo4j/MongoDB) để đảm bảo response enrich đúng avatar/username.

Chạy test & lint
-----------------
- Mỗi module có dependency `spring-boot-starter-test`; dùng:
  cd <service>; .\mvnw.cmd test

Ghi chú bảo mật & secrets
-------------------------
- Một số mật khẩu/khóa (MySQL, Neo4j, jwt signer, brevo api key) đang có trong `application.yaml` — chỉ để phát triển. Không commit secrets thật lên remote.

Tôi có thể mở rộng README này nữa (thêm swagger/examples cho mỗi service, list endpoint chi tiết, hoặc script start-all để dev). Muốn mình thêm phần nào tiếp theo không?
