# Zapoo — Backend Services

Đây là repository backend cho dự án Zapoo (một mạng xã hội dạng sách / social network). Backend được chia thành nhiều microservices (Spring Boot) nhỏ, mỗi service đảm nhận một phần chức năng (identity, profile, post, chat, file, notification, gateway...).

Mục lục
- Tổng quan
- Kiến trúc và danh sách service
- Yêu cầu trước khi chạy
- Chạy project (nhanh)
- Chạy từng service (chi tiết)
- Cấu hình quan trọng


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
