# java-project

Bài tập môn kiến trúc phần mềm. Mỗi thư mục là một bài độc lập, chạy được chỉ
với **JDK 21** — không cần Maven, không có thư viện ngoài.

| Bài | Đề bài | Kiến trúc | Tài liệu |
|---|---|---|---|
| [`ass1/`](ass1) | Triển khai "Place Order" theo sequence diagram mẫu | Clean Architecture — 3 package, 4 vòng | [ass1/README.md](ass1/README.md) |
| [`ass2/`](ass2) | Tái cấu trúc ví điện tử từ anemic sang rich model | DDD meets Clean Architecture — 3 vòng | [ass2/README.md](ass2/README.md) |

Hai bài **độc lập hoàn toàn**: khác đề bài, khác package gốc
(`com.example.ordering` và `com.example.wallet`), không chia sẻ một dòng code nào.
Mỗi thư mục tự chứa mã nguồn, bộ test và tài liệu riêng — README của từng bài ghi
rõ phạm vi của nó, và phần nào là giả định của người làm bài chứ không lấy từ đề.

## Chạy nhanh

```bash
cd ass1
.\run.ps1 serve          # REST API: POST /orders, GET /orders/{id}
.\run.ps1 test           # 38 test, gom 5 fitness function canh kien truc
```

```bash
cd ass2
.\run.ps1 demo           # 5 kich ban cho thay invariant hoat dong
.\run.ps1 test           # 30 test, gom 5 fitness function canh kien truc
```

Quy ước chung: comment trong mã nguồn viết tiếng Việt **không dấu** để tránh lỗi
encoding khi mở bằng editor cấu hình khác nhau; tài liệu `.md` thì viết đầy đủ dấu.
