# java-project

Bài tập môn kiến trúc phần mềm. Mỗi thư mục là một bài **độc lập hoàn toàn**: khác đề bài,
khác package gốc, không chia sẻ một dòng code nào.

| Bài | Đề bài | Kiến trúc | Stack | Tài liệu |
|---|---|---|---|---|
| [`ass1/`](ass1) | REST API đặt hàng | Clean Architecture — 3 package | Spring Boot 3.4 · Spring MVC · JPA · H2 | [ass1/README.md](ass1/README.md) |
| [`ass2/`](ass2) | REST API ví điện tử — tái cấu trúc anemic sang rich domain model | Clean Architecture — 3 package | Spring Boot 3.4 · Spring MVC · JPA · H2 | [ass2/README.md](ass2/README.md) |
| [`ass3/`](ass3) | Thiết kế distributed system & quyết định CAP — **bài thiết kế**, code chỉ là bonus | Clean Architecture — hai adapter CP/AP sau một port | Spring Boot 3.4 · Spring MVC · cụm mô phỏng trong bộ nhớ | [ass3/README.md](ass3/README.md) |
| [`ass4/`](ass4) | PACELC cho hệ thống Flash Sale — **bài thiết kế thuần, không có mã nguồn** | Hai lớp: van Redis + sổ cái PostgreSQL | Tài liệu · Mermaid | [ass4/README.md](ass4/README.md) |

Mỗi thư mục tự chứa mã nguồn, bộ test và tài liệu riêng. README của từng bài ghi rõ phạm vi
của nó, và phần nào là giả định của người làm bài chứ không lấy từ đề.

Chỉ cần **JDK 21**. Mọi bài dùng Maven Wrapper nên không phải cài Maven — `mvnw` tự tải về
lần đầu chạy. `ass1` chạy ở cổng 8080, `ass2` ở 8081, `ass3` ở 8082 nên bật song song được.
`ass4` là bài thiết kế thuần — chỉ có tài liệu, không có gì để chạy.

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
.\mvnw test              # 20 test, gồm 4 fitness function canh kiến trúc
```

## Quy ước chung

Xem [CLAUDE.md](CLAUDE.md) để biết đầy đủ. Ba điểm hay dùng nhất:

- **Không comment trong mã nguồn.** Nếu thật sự cần, viết tiếng Việt **không dấu** để tránh
  lỗi encoding khi mở bằng editor cấu hình khác. Tài liệu `.md` thì viết đầy đủ dấu.
- **Tầng `domain/` không được dính annotation của framework** — không Spring, không JPA,
  không Jackson. Các tầng ngoài thì dùng thoải mái.
- **Mỗi bài có fitness function** đọc thẳng mã nguồn và fail build nếu có file vượt ranh
  giới tầng. Luật kiến trúc phải chạy được, không chỉ nằm trong tài liệu.
