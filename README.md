# java-project

Bài tập môn kiến trúc phần mềm. Mỗi thư mục là một bài **độc lập hoàn toàn**: khác đề bài,
khác package gốc, không chia sẻ một dòng code nào.

Mỗi bài gồm:

- `README.md` — bài nộp (riêng `Day13_JVM-Performance` dùng `markdown.md` theo đề);
- `KIEN-THUC.md` — kiến thức cơ bản để hiểu các khái niệm của bài;
- source code (`pom.xml`, `mvnw`, `src/`) nằm ngay trong thư mục bài, chạy như bình thường;
- thư mục con **cùng tên với bài** — bản sao chỉ gồm `src/main`, dùng để nộp bài. Bài chỉ có tài
  liệu thì không có hai mục này.

| Bài | Đề bài | Kiến trúc | Stack | Tài liệu |
|---|---|---|---|---|
| [`Day1_Clean-Architect/`](Day1_Clean-Architect) | REST API đặt hàng | Clean Architecture — 3 package | Spring Boot 3.4 · Spring MVC · JPA · H2 | [README.md](Day1_Clean-Architect/README.md) · [Kiến thức](Day1_Clean-Architect/KIEN-THUC.md) |
| [`Day3_Domain-Driven-Design/`](Day3_Domain-Driven-Design) | REST API ví điện tử — tái cấu trúc anemic sang rich domain model | Clean Architecture — 3 package | Spring Boot 3.4 · Spring MVC · JPA · H2 | [README.md](Day3_Domain-Driven-Design/README.md) · [Kiến thức](Day3_Domain-Driven-Design/KIEN-THUC.md) |
| [`Day8_CAP-Theorem/`](Day8_CAP-Theorem) | Thiết kế distributed system & quyết định CAP — **bài thiết kế**, code chỉ là bonus | Spring Boot thường + Strategy pattern (CP / AP) | Spring Boot 3.4 · Spring MVC · cụm mô phỏng trong bộ nhớ | [README.md](Day8_CAP-Theorem/README.md) · [Kiến thức](Day8_CAP-Theorem/KIEN-THUC.md) |
| [`Day9_PACELC-Theorem/`](Day9_PACELC-Theorem) | PACELC cho hệ thống Flash Sale — **chỉ có tài liệu** | Một cụm PostgreSQL, mỗi luồng một cặp knob replication | Tài liệu · Mermaid · slide 16:9 | [README.md](Day9_PACELC-Theorem/README.md) · [PDF](Day9_PACELC-Theorem/Day9_PACELC-Theorem.pdf) · [Kiến thức](Day9_PACELC-Theorem/KIEN-THUC.md) |
| [`Day10_Consistency-Models/`](Day10_Consistency-Models) | Consistency model cho từng tính năng mạng xã hội — **chỉ có tài liệu** | Mỗi tính năng một mô hình riêng, chọn theo bất biến của chính nó | Tài liệu · slide 16:9 | [README.md](Day10_Consistency-Models/README.md) · [PDF](Day10_Consistency-Models/Day10_Consistency-Models.pdf) · [Kiến thức](Day10_Consistency-Models/KIEN-THUC.md) |
| [`Day11-12_Redis/`](Day11-12_Redis) | Leaderboard trên Redis Sorted Set (`ZADD` / `ZREVRANGE`, hai container master–replica) · `DELETE /products/{id}` qua cache hai tầng L1/L2 + Redis Pub/Sub | Spring Boot thường | Spring Boot 3.4 · Spring MVC · Spring Data Redis · Spring Cache · JPA · H2 · Docker Compose | [README.md](Day11-12_Redis/README.md) · [Kiến thức](Day11-12_Redis/KIEN-THUC.md) |
| [`Day13_JVM-Performance/`](Day13_JVM-Performance) | Phân tích hiệu năng JVM cho endpoint `/process` — **chỉ có tài liệu** | — | Tài liệu · GC log · JFR | [markdown.md](Day13_JVM-Performance/markdown.md) · [Kiến thức](Day13_JVM-Performance/KIEN-THUC.md) |
| [`Day15_Advanced-Data-Access/`](Day15_Advanced-Data-Access) | Quản lý kho: nhập hàng với Pessimistic Locking, sửa thông tin với Optimistic Locking, lịch sử bằng Hibernate Envers | Spring Boot thường | Spring Boot 3.4 · Spring MVC · JPA · Hibernate Envers · H2 | [README.md](Day15_Advanced-Data-Access/README.md) · [Kiến thức](Day15_Advanced-Data-Access/KIEN-THUC.md) |

Chỉ cần **JDK 21**. Mọi bài code dùng Maven Wrapper nên không phải cài Maven — `mvnw` tự tải về
lần đầu chạy. Mỗi bài một cổng nên bật song song được:

| Bài | Cổng | Cần thêm |
|---|---|---|
| `Day1_Clean-Architect` | 8080 | — |
| `Day3_Domain-Driven-Design` | 8081 | — |
| `Day8_CAP-Theorem` | 8082 | — |
| `Day11-12_Redis` | 8083 | **Docker** — hai container Redis, chạy bằng `docker compose up -d` |
| `Day15_Advanced-Data-Access` | 8085 | — |

`Day9_PACELC-Theorem`, `Day10_Consistency-Models` và `Day13_JVM-Performance` chỉ có tài liệu,
không có gì để chạy.

## Chạy nhanh

```bash
cd Day1_Clean-Architect
.\mvnw spring-boot:run   # REST API: POST /api/orders, GET /api/orders/{id}
.\mvnw test              # 9 test, gồm 4 fitness function canh kiến trúc
```

```bash
cd Day3_Domain-Driven-Design
.\mvnw spring-boot:run   # REST API cong 8081: mo vi, rut tien, khoa vi
.\mvnw test              # 20 test, gồm 4 fitness function canh kiến trúc
```

```bash
cd Day8_CAP-Theorem
.\mvnw spring-boot:run   # REST API cong 8082: giu cho ton kho + cat/noi mang cum
.\mvnw test              # 19 test

# Xem CAP van hanh, tuong thuat tung buoc:
curl -X POST "http://localhost:8082/api/demo/cap?strategy=cp"   # doi sang ap de so sanh
```

```bash
cd Day11-12_Redis
docker compose up -d     # BAT BUOC: redis-master :6379, redis-replica :6380
.\mvnw test              # 22 test, danh vao Redis that
.\mvnw spring-boot:run   # REST API cong 8083

curl -X POST http://localhost:8083/leaderboard/an -H 'Content-Type: application/json' -d '{"score": 1500}'
curl http://localhost:8083/leaderboard/top/3
curl -X DELETE http://localhost:8083/products/2
```

```bash
cd Day15_Advanced-Data-Access
.\mvnw test              # 14 test
.\mvnw spring-boot:run   # REST API cong 8085

curl -X POST http://localhost:8085/inventories/1/stock-in -H 'Content-Type: application/json' -d '{"quantity": 5}'
curl http://localhost:8085/inventories/1/history
```

## Quy ước chung

Xem [CLAUDE.md](CLAUDE.md). Ba điểm hay dùng nhất:

- **Không comment trong mã nguồn.** Nếu thật sự cần, viết tiếng Việt **không dấu**. Tài liệu
  `.md` thì viết đầy đủ dấu.
- **Kiến trúc chọn theo đề, không mặc định.** Mặc định là Spring Boot thường. Chỉ
  `Day1_Clean-Architect` và `Day3_Domain-Driven-Design` dùng Clean Architecture, vì đề nói thẳng
  về kiến trúc.
- **Bài Clean Architecture phải có fitness function** — test đọc thẳng mã nguồn và fail build
  nếu có file vượt ranh giới tầng.
