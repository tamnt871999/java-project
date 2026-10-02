# Day1_Clean-Architect — Demo Clean Architecture với Spring Boot

Bản demo tối giản để hiểu **Clean Architecture**: 14 file source, 2 file test.
API đặt hàng với 2 endpoint, không có gì thừa.

**Stack:** Java 21 · Spring Boot 3.4 · Spring MVC · Spring Data JPA · H2

```bash
.\mvnw spring-boot:run      # http://localhost:8080
.\mvnw test                 # 9 test
```

---

## API

### `POST /api/orders`

```json
{
  "customerId": "CUS-1",
  "items": [
    { "productId": "SKU-A", "quantity": 3, "unitPrice": 129.00 },
    { "productId": "SKU-B", "quantity": 2, "unitPrice": 12.00 }
  ]
}
```

→ `201 Created`

```json
{ "orderId": 1, "total": 411.00 }
```

### `GET /api/orders/{orderId}`

→ `200 OK`

```json
{
  "orderId": 1,
  "customerId": "CUS-1",
  "placedAt": "2026-09-26T09:43:57.927128Z",
  "items": [
    { "productId": "SKU-A", "quantity": 3, "unitPrice": 129.00, "lineTotal": 387.00 },
    { "productId": "SKU-B", "quantity": 2, "unitPrice": 12.00, "lineTotal": 24.00 }
  ],
  "total": 411.00
}
```

### Lỗi

| HTTP | `code` | Khi nào |
|---|---|---|
| `400` | `BAD_REQUEST` | Sai **cú pháp** — thiếu trường, JSON hỏng |
| `422` | `BUSINESS_RULE_VIOLATED` | Đúng cú pháp nhưng sai **nghiệp vụ** — `quantity: 0`, sản phẩm trùng |
| `404` | `ORDER_NOT_FOUND` | Không có đơn hàng với mã này |

`400` là *"tôi không hiểu bạn nói gì"*. `422` là *"tôi hiểu, nhưng không làm được"*.

```bash
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"customerId\":\"CUS-1\",\"items\":[{\"productId\":\"SKU-A\",\"quantity\":3,\"unitPrice\":129.00}]}"
```

---

## Clean Architecture — ý tưởng cốt lõi

Ba package, và **một luật duy nhất**:

```
   adapter/       ← biết Spring, biết JPA, biết HTTP
       ↓ phụ thuộc vào
   application/   ← chỉ biết domain
       ↓ phụ thuộc vào
   domain/        ← không biết gì cả
```

> **The Dependency Rule:** mã ở tầng trong không được biết gì về tầng ngoài.

Mở bất kỳ file nào trong `domain/` — sẽ không thấy một dòng `import org.springframework`
hay `import jakarta.*` nào. Đó là toàn bộ vấn đề.

### 14 file

```
src/main/java/com/example/ordering/
├── OrderingApplication.java              Khởi động Spring Boot
│
├── domain/                               Nghiệp vụ thuần — 3 file
│   ├── Order.java                          Đơn hàng + quy tắc hợp lệ
│   ├── OrderItem.java                      Dòng hàng + tính thành tiền
│   └── DomainException.java                Vi phạm quy tắc nghiệp vụ
│
├── application/                          Điều phối — 5 file
│   ├── port/in/PlaceOrderUseCase.java      Cổng VÀO: "tạo đơn hàng"
│   ├── port/in/GetOrderUseCase.java        Cổng VÀO: "xem đơn hàng"
│   ├── port/out/OrderRepository.java       Cổng RA: "lưu / đọc ở đâu đó"
│   ├── usecase/PlaceOrderService.java      Làm việc của cổng vào thứ nhất
│   └── usecase/GetOrderService.java        Làm việc của cổng vào thứ hai
│
└── adapter/                              Nối với thế giới thật — 5 file
    ├── in/web/OrderController.java         HTTP  → use case
    ├── in/web/ApiExceptionHandler.java     Exception → mã HTTP
    ├── out/persistence/OrderJpaEntity.java     Bảng trong database
    ├── out/persistence/OrderJpaRepository.java Spring Data (1 dòng)
    └── out/persistence/OrderRepositoryAdapter.java  use case → JPA
```

---

## Điểm mấu chốt: `OrderRepository`

Đây là thứ đáng hiểu nhất trong cả dự án. Nếu chỉ nhớ một điều, hãy nhớ điều này.

`OrderRepository` là interface nằm ở **`application/port/out/`** — tức là tầng
`application` **sở hữu** nó. Còn class hiện thực nó (`OrderRepositoryAdapter`) lại nằm ở
tầng `adapter` **bên ngoài**:

```
application/port/out/OrderRepository          ← interface, tầng trong sở hữu
          ▲
          │ implements
adapter/out/persistence/OrderRepositoryAdapter ← tầng ngoài, import NGƯỢC VÀO TRONG
```

Kết quả:

- **Lúc chạy:** use case gọi *ra ngoài* → xuống database.
- **Lúc biên dịch:** `adapter` import *vào trong*, không có chiều ngược lại.

Đó là **Dependency Inversion** — mũi tên phụ thuộc ngược chiều với mũi tên lời gọi.

So với cách viết Spring quen thuộc:

| Spring MVC 3 tầng thông thường | Ở đây |
|---|---|
| `interface OrderRepository extends JpaRepository<OrderEntity, Long>` | `interface OrderRepository` không extends gì cả |
| Interface thuộc tầng persistence → service phải import **xuống** | Interface thuộc tầng application → persistence import **lên** |
| Service nhận `OrderEntity` (kiểu của JPA) | Use case nhận `Order` (kiểu của domain) |

**Được gì?** Đổi H2 sang PostgreSQL, hay sang MongoDB, chỉ cần viết một class khác cũng
`implements OrderRepository`. Không một dòng nào trong `domain/` và `application/` phải sửa.

---

## Đường đi của một request

```
POST /api/orders
   │
   ├─ OrderController              nhận JSON, @Valid kiểm tra cú pháp
   │      ↓ đổi Request thành Command
   ├─ PlaceOrderUseCase            (interface — controller chỉ biết tới đây)
   ├─ PlaceOrderService            điều phối
   │      ↓
   ├─ Order.place(...)             domain kiểm tra quy tắc nghiệp vụ
   │      ↓
   ├─ OrderRepository.save()       (interface — use case chỉ biết tới đây)
   ├─ OrderRepositoryAdapter       đổi Order thành OrderJpaEntity
   ├─ OrderJpaRepository           Spring Data
   └─ Hibernate → H2               INSERT orders; INSERT order_items
```

Rồi đi ngược lại đúng đường cũ để trả về `201`.

Để ý dữ liệu **đổi hình dạng 3 lần**, mỗi lần ở một ranh giới:

```
JSON  →  PlaceOrderRequest  →  PlaceOrderCommand  →  Order  →  OrderJpaEntity  →  SQL
         (tầng web)            (tầng application)     (domain)   (tầng adapter)
```

Nghe thừa, nhưng chính nó giữ cho: đổi tên một cột trong database **không** làm đổi tên
một trường trên JSON API.

---

## Hai loại kiểm tra — đừng lẫn lộn

| Loại | Ở đâu | Ví dụ | Trả về |
|---|---|---|---|
| **Cú pháp** — "body có đúng hình dạng không?" | `@NotBlank`, `@NotNull` trong `OrderController.PlaceOrderRequest` | thiếu `customerId` | `400` |
| **Nghiệp vụ** — "giá trị này có hợp lệ không?" | `OrderItem`, `Order.place()` trong `domain/` | `quantity: 0`, sản phẩm trùng | `422` |

`quantity: 0` là một số nguyên hợp lệ nên nó **đi lọt** `@Valid`. Chính `OrderItem` trong
domain mới là chỗ từ chối nó. Đó là lý do quy tắc nghiệp vụ phải nằm trong `domain/`, không
nằm trong Controller.

---

## Test

```bash
.\mvnw test
```

| File | Kiểm gì |
|---|---|
| `OrderApiTest` | Gọi thật qua cả 4 tầng: tạo đơn, đọc lại, `404`, `422`, `400` |
| `ArchitectureFitnessTest` | Đọc source và **fail build** nếu có file vượt ranh giới tầng |

`ArchitectureFitnessTest` canh 4 luật:

```
domain khong phu thuoc application hay adapter
domain khong dinh cong nghe ha tang
application khong phu thuoc adapter
adapter web khong phu thuoc adapter persistence
```

Luật viết trong tài liệu thì không ai bắt buộc phải đọc. Chỉ cần một người gõ
`import jakarta.persistence` vào `Order.java` cho nhanh là kiến trúc âm thầm sụp đổ mà
build vẫn xanh. Bài test này biến luật thành ràng buộc chạy được.

---

## Những gì đã lược bỏ cho đơn giản

Đây là bản demo, không phải bản production. Dự án thật nên có thêm:

| Bỏ gì | Thật ra nên |
|---|---|
| `Instant.now()` gọi thẳng trong use case | Tiêm `Clock` để test đóng băng được thời gian |
| `@Service` trên use case | Cách "sạch" hơn: khai báo `@Bean` trong một `@Configuration` để `application/` không import Spring |
| Không có `@Transactional` | Cần khi một use case ghi nhiều bảng trong cùng một giao dịch |
| `ddl-auto: create-drop`, H2 in-memory | Database thật + Flyway quản lý schema |
| Không có Value Object (`Money`, `Quantity`) | Bọc `BigDecimal`/`int` lại để giá trị sai không tồn tại được |
| Không tính giảm giá / phí ship | Khi logic giá phức tạp lên, tách ra một Domain Service riêng |

Mỗi dòng trên là một bước nâng cấp — làm từng cái một khi bạn đã nắm chắc phần lõi.
