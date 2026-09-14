# java-project

Bài tập môn kiến trúc phần mềm. Mỗi thư mục là một bài độc lập, chạy được chỉ
với **JDK 21** — không cần Maven, không có thư viện ngoài.

| Bài | Chủ đề | Kiến trúc |
|---|---|---|
| [`ass1/`](ass1) | REST API đặt hàng (Place Order) | **Clean Architecture** — 4 vòng: `domain` → `application` → `adapter` → `infrastructure` |
| [`ass2/`](ass2) | Ví điện tử: anemic → rich model | **DDD meets Clean Architecture** — xem [ass2/README.md](ass2/README.md) |

Hai bài cố tình dùng hai kiểu kiến trúc khác nhau để so sánh:

- **ass1** — Use Case định nghĩa Port vào (`PlaceOrderUseCase`) và Port ra
  (`OrderRepository`); adapter hiện thực Port ra, hạ tầng đóng vai Spring Data
  JPA và H2. Luồng chạy bám đúng sequence diagram mẫu, và thứ tự lời gọi được
  canh bằng một bài test riêng.
- **ass2** — Tái cấu trúc `WalletEntity` (mọi thuộc tính `public`, luật nghiệp
  vụ nằm ngoài ở service) thành Aggregate Root tự bảo vệ invariant của chính nó.
  Trọng tâm là tầng domain; tầng application chỉ điều phối.

## Chạy nhanh

```bash
cd ass1
.\run.ps1 serve          # REST API tai http://localhost:8080
.\run.ps1 test           # 25 test, gom 5 fitness function canh kien truc
```

```bash
cd ass2
.\run.ps1 demo           # 5 kich ban cho thay invariant hoat dong
.\run.ps1 test           # 31 test, gom 5 fitness function canh kien truc
```

Quy ước chung: comment trong mã nguồn viết tiếng Việt **không dấu** để tránh lỗi
encoding khi mở bằng editor cấu hình khác nhau; tài liệu `.md` thì viết đầy đủ dấu.
