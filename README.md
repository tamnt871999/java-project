# java-project

Bài tập môn kiến trúc phần mềm. Mỗi thư mục là một bài **độc lập hoàn toàn**: khác đề bài,
khác package gốc, không chia sẻ một dòng code nào.

| Bài | Đề bài | Kiến trúc | Stack | Tài liệu |
|---|---|---|---|---|
| [`ass1/`](ass1) | REST API đặt hàng | Clean Architecture — 3 package | Spring Boot 3.4 · Spring MVC · JPA · H2 | [ass1/README.md](ass1/README.md) |
| [`ass2/`](ass2) | REST API ví điện tử — tái cấu trúc anemic sang rich domain model | Clean Architecture — 3 package | Spring Boot 3.4 · Spring MVC · JPA · H2 | [ass2/README.md](ass2/README.md) |
| [`ass3/`](ass3) | Thiết kế distributed system & quyết định CAP — **bài thiết kế**, code chỉ là bonus | Spring Boot thường + Strategy pattern (CP / AP) | Spring Boot 3.4 · Spring MVC · cụm mô phỏng trong bộ nhớ | [ass3/README.md](ass3/README.md) |
| [`ass4/`](ass4) | PACELC cho hệ thống Flash Sale — **bài thiết kế thuần, không có mã nguồn** | Một cụm PostgreSQL, mỗi luồng một cặp knob replication | Tài liệu · Mermaid | [ass4/README.md](ass4/README.md) · [bản PDF](ass4/ass4-PACELC-Flash-Sale.pdf) |
| [`ass5/`](ass5) | Consistency model cho từng tính năng mạng xã hội — **bài thiết kế thuần, không có mã nguồn** | Mỗi tính năng một mô hình riêng, chọn theo bất biến của chính nó | Tài liệu · slide 16:9 | [ass5/README.md](ass5/README.md) · [bản PDF](ass5/ass5-Consistency-Models.pdf) |
| [`ass6/`](ass6) | Leaderboard trên Redis Sorted Set — REST API `ZADD` / `ZREVRANGE`, hai container Redis master–replica | Spring Boot thường | Spring Boot 3.4 · Spring MVC · Spring Data Redis · Docker Compose | [ass6/README.md](ass6/README.md) |

Mỗi thư mục tự chứa mã nguồn, bộ test và tài liệu riêng. README của từng bài ghi rõ phạm vi
của nó, và phần nào là giả định của người làm bài chứ không lấy từ đề.

Chỉ cần **JDK 21**. Mọi bài dùng Maven Wrapper nên không phải cài Maven — `mvnw` tự tải về
lần đầu chạy. `ass1` chạy ở cổng 8080, `ass2` ở 8081, `ass3` ở 8082, `ass6` ở 8083 nên bật song song được.
Riêng `ass6` cần **Docker** — hai container Redis là hạ tầng của bài, chạy bằng `docker compose up -d`.
`ass4` và `ass5` là bài thiết kế thuần — chỉ có tài liệu, không có gì để chạy.

## Chạy nhanh

```bash
cd ass1
.\mvnw spring-boot:run   # REST API: POST /api/orders, GET /api/orders/{id}
.\mvnw test              # 9 test, gồm 4 fitness function canh kiến trúc
```

```bash
cd ass2
.\mvnw spring-boot:run   # REST API cong 8081: mo vi, rut tien, khoa vi
.\mvnw test              # 20 test, gồm 4 fitness function canh kiến trúc
```

```bash
cd ass3
.\mvnw spring-boot:run   # REST API cong 8082: giu cho ton kho + cat/noi mang cum
.\mvnw test              # 19 test

# Xem CAP van hanh, tuong thuat tung buoc:
curl -X POST "http://localhost:8082/api/demo/cap?strategy=cp"   # doi sang ap de so sanh
```

```bash
cd ass6
docker compose up -d     # BAT BUOC: redis-master :6379, redis-replica :6380
.\mvnw test              # 13 test, danh vao Redis that
.\mvnw spring-boot:run   # REST API cong 8083

curl -X POST http://localhost:8083/leaderboard/an -H 'Content-Type: application/json' -d '{"score": 1500}'
curl http://localhost:8083/leaderboard/top/3
```

## Quy ước chung

Xem [CLAUDE.md](CLAUDE.md) để biết đầy đủ. Ba điểm hay dùng nhất:

- **Không comment trong mã nguồn.** Nếu thật sự cần, viết tiếng Việt **không dấu** để tránh
  lỗi encoding khi mở bằng editor cấu hình khác. Tài liệu `.md` thì viết đầy đủ dấu.
- **Kiến trúc chọn theo đề, không mặc định.** Mặc định là Spring Boot thường
  (controller / service / repository). Chỉ dùng Clean Architecture khi đề yêu cầu —
  `ass1` và `ass2` thuộc diện đó vì đề nói thẳng về kiến trúc; `ass3` thì không, nên nó là
  Spring Boot thường.
- **Bài nào làm Clean Architecture thì phải có fitness function** đọc thẳng mã nguồn và fail
  build nếu có file vượt ranh giới tầng. Luật kiến trúc phải chạy được, không chỉ nằm trong
  tài liệu. Bài Spring Boot thường không cần.
