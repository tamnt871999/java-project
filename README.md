# java-project

Bài tập môn kiến trúc phần mềm. Mỗi thư mục là một bài độc lập, chạy được chỉ
với **JDK 21** — không cần Maven, không có thư viện ngoài.

| Bài | Chủ đề | Kiến trúc |
|---|---|---|
| [`ass1/`](ass1) | REST API đặt hàng (Place Order) | **Clean Architecture** — 4 vòng: `domain` → `application` → `adapter` → `infrastructure` |
| [`ass2/`](ass2) | Tính phí giao hàng GHN / GHTK | **Hexagonal (Ports & Adapters)** — xem [ass2/README.md](ass2/README.md) |

Hai bài cố tình dùng hai kiểu kiến trúc khác nhau để so sánh:

- **ass1** — Use Case định nghĩa Port vào (`PlaceOrderUseCase`) và Port ra
  (`OrderRepository`); adapter hiện thực Port ra, hạ tầng đóng vai Spring Data
  JPA và H2. Luồng chạy bám đúng sequence diagram mẫu, và thứ tự lời gọi được
  canh bằng một bài test riêng.
- **ass2** — Lõi định nghĩa Port, hạ tầng hiện thực Port. Chiều phụ thuộc bị
  đảo ngược, và luật kiến trúc được canh bằng **fitness function** chạy trong
  bộ test.

## Chạy nhanh

```bash
cd ass1
.\run.ps1 serve          # REST API tai http://localhost:8080
.\run.ps1 test           # 25 test, gom 5 fitness function canh kien truc
```

```bash
cd ass2
.\run.ps1 demo           # bang bao gia theo tung tinh thanh
.\run.ps1 test           # 38 test, gom 3 fitness function canh kien truc
```

Quy ước chung: comment trong mã nguồn viết tiếng Việt **không dấu** để tránh lỗi
encoding khi mở bằng editor cấu hình khác nhau; tài liệu `.md` thì viết đầy đủ dấu.
