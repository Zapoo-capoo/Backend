# Triển khai Zapoo Backend lên EC2 (CD)

Mỗi lần push vào `main`, GitHub Actions (`.github/workflows/deploy.yml`) làm 3 việc:

1. **build** ảnh Docker cho 7 service rồi đẩy lên GHCR (`ghcr.io/zapoo-capoo/backend/<service>:sha-xxxxxxx`);
2. chờ bạn **duyệt** (môi trường `production`, nếu bật Required reviewers);
3. SSH vào EC2, chép file cấu hình, chạy `deploy.sh`: kéo ảnh mới, khởi động, **chờ mọi container healthy**, nếu lỗi thì
   **tự rollback** về bản trước.

Các service: api-gateway, config-service, discovery-service, identity-service, profile-service, chat-service,
storage-service. Hạ tầng chạy chung trong compose: MySQL (identity), PostgreSQL (storage), MongoDB + Redis (chat),
Neo4j (profile), Kafka. `post-service` và `notification-service` chưa triển khai (gateway đã có sẵn route cho chúng).

```
Internet ──8888──▶ api-gateway ──▶ identity / profile / chat / storage      (mạng nội bộ của compose)
Internet ──8099──▶ chat-service (Socket.IO)
```

## 1. Chuẩn bị EC2 (làm một lần)

**Máy chủ**
- Ubuntu 22.04 hoặc 24.04, loại **t3.xlarge (16 GB RAM)** là thoải mái; **t3.large (8 GB)** chạy được nếu thêm swap
  (bước dưới). Ổ đĩa **gp3 40 GB** trở lên (video và dữ liệu CSDL tăng dần).
- Gắn **Elastic IP** (địa chỉ IP cố định). `PUBLIC_BASE_URL` và đường dẫn ảnh/video trả về cho client dùng IP này, IP đổi
  là link cũ hỏng.
- **Security Group** (inbound): `22` chỉ từ IP của bạn; `8888` (API) và `8099` (Socket.IO) từ nơi cần truy cập. Không mở
  cổng nào khác (CSDL, Kafka... chỉ nằm trong mạng nội bộ).

**IAM Role cho S3 (khuyên dùng, khỏi để khóa AWS trên máy chủ)**
1. IAM → Roles → Create role → AWS service → **EC2**, gắn policy cho bucket (đổi tên bucket):
   ```json
   { "Version": "2012-10-17", "Statement": [
     { "Effect": "Allow", "Action": ["s3:ListBucket", "s3:GetBucketLocation"], "Resource": "arn:aws:s3:::zapoo-media" },
     { "Effect": "Allow", "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"], "Resource": "arn:aws:s3:::zapoo-media/*" } ] }
   ```
2. EC2 → chọn máy → Actions → Security → **Modify IAM role** → chọn role vừa tạo.

**Cài Docker và thư mục triển khai** (SSH vào máy)
```bash
# Docker + Compose plugin
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker

# Swap 4 GB (bắt buộc nếu chỉ có 8 GB RAM)
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Thư mục triển khai
sudo mkdir -p /opt/zapoo && sudo chown $USER:$USER /opt/zapoo
```

**Khóa SSH riêng cho GitHub Actions** (trên máy bạn, không dùng lại khóa cá nhân)
```bash
ssh-keygen -t ed25519 -f zapoo-deploy -N "" -C "github-actions-deploy"
# dán nội dung zapoo-deploy.pub vào ~/.ssh/authorized_keys của user trên EC2
```

## 2. Cấu hình GitHub

Repo → **Settings → Environments → New environment → `production`**, bật **Required reviewers** (mỗi lần deploy phải có
người bấm duyệt). Trong environment đó thêm **Secrets**:

| Secret | Giá trị |
|---|---|
| `EC2_HOST` | Elastic IP của máy |
| `EC2_USER` | `ubuntu` (hoặc user bạn dùng) |
| `EC2_SSH_KEY` | **toàn bộ nội dung** file khóa riêng `zapoo-deploy` |
| `PROD_ENV_FILE` | **toàn bộ nội dung** file `.env` (mẫu: `deploy/.env.example`), điền mật khẩu thật |
| `EC2_KNOWN_HOSTS` | (tùy chọn) kết quả `ssh-keyscan -H <IP>`, để ghim khóa máy chủ chống giả mạo |

Tạo mật khẩu mạnh cho `.env` (chỉ chữ và số): `openssl rand -hex 24`. `PUBLIC_BASE_URL` = `http://<Elastic IP>:8888`.

Lần đầu GHCR có thể chặn máy chủ kéo ảnh: vào trang package (github.com/orgs/Zapoo-capoo/packages) → từng package →
**Package settings → Manage Actions access**, thêm repo `Backend` với quyền Read, hoặc đặt package ở chế độ Public.

## 3. Triển khai

- **Tự động**: push vào `main` rồi vào tab **Actions**, duyệt khi workflow dừng ở job `deploy`. Lần đầu mất khoảng
  10 đến 15 phút (build 7 ảnh, tải ảnh hạ tầng, khởi động Kafka và Neo4j).
- **Rollback tay**: Actions → *Deploy to production* → **Run workflow** → điền `image_tag` cũ (ví dụ `sha-1a2b3c4`). Job
  bỏ qua bước build và triển khai đúng bản đó. Rollback tự động xảy ra khi bản mới không healthy.

Kiểm tra sau khi xong:
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://<IP>:8888/swagger-ui.html          # 200
curl -s http://<IP>:8888/api/v1/identity/v3/api-docs | head -c 100                   # JSON OpenAPI
```

## 4. Vận hành trên máy chủ

```bash
cd /opt/zapoo
docker compose --env-file .env -f docker-compose.prod.yml ps                 # trạng thái và health
docker compose --env-file .env -f docker-compose.prod.yml logs -f chat-service
docker compose --env-file .env -f docker-compose.prod.yml restart chat-service
cat .deployed_tag                                                              # bản đang chạy
docker stats --no-stream                                                       # RAM từng container
```

## 5. Cần biết trước khi cho người dùng thật vào

1. **Tài khoản `admin` / mật khẩu `admin` được tự tạo** khi identity-service khởi động lần đầu trên CSDL trống. IP của
   bạn là công khai, hãy **đổi mật khẩu admin ngay sau lần deploy đầu**.
2. **Chưa có HTTPS** (chưa có tên miền): token đăng nhập đi bằng HTTP thuần. Phù hợp để thử nghiệm. Khi có tên miền, đặt
   Caddy hoặc nginx phía trước gateway. Frontend chạy trên `https://` sẽ bị trình duyệt chặn khi gọi `http://` (mixed
   content), nên frontend thử nghiệm cần chạy `http://` hoặc dùng domain + HTTPS.
3. **Sao lưu**: dữ liệu nằm trong các Docker volume (`mysql_data`, `postgres_data`, `mongo_data`, `neo4j_data`...) trên đĩa
   của EC2. Bật **EBS snapshot** định kỳ (AWS Backup hoặc Data Lifecycle Manager), CD không tự sao lưu.
4. `ddl-auto: update` vẫn đang dùng cho MySQL và PostgreSQL (Hibernate tự sửa bảng). Tiện nhưng không kiểm soát được
   thay đổi lược đồ; về lâu dài nên chuyển sang Flyway.
5. Swagger của các service đang **mở công khai** qua gateway.
6. Khi triển khai `post-service` và `notification-service`: thêm service vào `docker-compose.prod.yml`, thêm file cấu hình
   production tương ứng vào `environment-service/prod/`, thêm vào danh sách `matrix` và `JAVA_SERVICES` (`deploy.sh`).
