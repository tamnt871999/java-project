# BÀI TẬP: TRIỂN KHAI "PLACE ORDER" THEO CLEAN ARCHITECTURE

> **Đề bài:** Triển khai code "Place Order" cho Clean Architecture theo Sequence sample.

Toàn bộ bài bám đúng sequence diagram mẫu: tên class khớp từng lifeline, số lời gọi
khớp từng message, và thứ tự lời gọi được canh bằng một bài test riêng.

Chạy được chỉ với **JDK 21**, không cần Maven, không có thư viện ngoài.

| Lệnh | Tác dụng |
|---|---|
| `.\run.ps1 serve` | Chạy REST API tại `http://localhost:8080` (mặc định) |
| `.\run.ps1 serve 9090` | Chạy ở cổng khác |
| `.\run.ps1 test` | Chạy 38 bài kiểm thử (`SelfCheck` + `ArchitectureFitness`) |
| `.\run.ps1 build` / `clean` | Chỉ biên dịch / dọn thư mục build |

### API

| Method | Đường dẫn | Trả về |
|---|---|---|
| `POST` | `/orders` | `201 Created` — `{orderId, total}` |
| `GET` | `/orders/{id}` | `200 OK` — chi tiết đơn, hoặc `404` nếu không có |

Đặt hàng:

```bash
curl -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d "{\"customerId\":\"CUS-1\",\"items\":[{\"productId\":\"SKU-A\",\"quantity\":3,\"unitPrice\":129.00}]}"
```

Đọc lại đơn vừa tạo:

```bash
curl http://localhost:8080/orders/ORD-1001
```

---

## 1. Đối chiếu với sequence diagram

### 9 lifeline → 9 thành phần trong code

| Lifeline trong hình | Vai trò | File |
|---|---|---|
| `Client` | — | người gọi / `curl` |
| `OrderController` | Inbound Adapter / Interface Adapters | [`adapter/in/web/OrderController.java`](src/main/java/com/example/ordering/adapter/in/web/OrderController.java) |
| `PlaceOrderUseCase` | Inbound Port / Application | [`application/port/in/PlaceOrderUseCase.java`](src/main/java/com/example/ordering/application/port/in/PlaceOrderUseCase.java) |
| `PlaceOrderService` | Interactor / Use Case | [`application/usecase/PlaceOrderService.java`](src/main/java/com/example/ordering/application/usecase/PlaceOrderService.java) |
| `OrderPricingService` | Domain Service / Entities | [`domain/OrderPricingService.java`](src/main/java/com/example/ordering/domain/OrderPricingService.java) |
| `OrderRepository` | Outbound Port / Application | [`application/port/out/OrderRepository.java`](src/main/java/com/example/ordering/application/port/out/OrderRepository.java) |
| `JpaOrderRepositoryAdapter` | Outbound Adapter / Interface Adapters | [`adapter/out/persistence/JpaOrderRepositoryAdapter.java`](src/main/java/com/example/ordering/adapter/out/persistence/JpaOrderRepositoryAdapter.java) |
| `Spring Data JPA` | Frameworks & Drivers | [`infrastructure/jpa/`](src/main/java/com/example/ordering/infrastructure/jpa) (giả lập) |
| `H2/Postgres (DB)` | Frameworks & Drivers | [`infrastructure/db/Database.java`](src/main/java/com/example/ordering/infrastructure/db/Database.java) (giả lập) |

### 14 message → 14 bước trong code

| # | Message trong hình | Nơi thực hiện |
|---|---|---|
| 1 | `POST /orders {customerId, items}` | `HttpServerRunner` → `OrderController.placeOrder(body)` |
| 2 | `placeOrder(Command)` | `OrderController` → `PlaceOrderUseCase` |
| 3 | `placeOrder(Command)` *(impl dispatch)* | interface → `PlaceOrderService` |
| 4 | `calculateTotal(items)` | `PlaceOrderService` → `OrderPricingService` |
| 5 | `total` *(return)* | `PriceBreakdown` trả về |
| 6 | `save(Order)` | `PlaceOrderService` → `OrderRepository` |
| 7 | `save(Order)` *(impl dispatch)* | interface → `JpaOrderRepositoryAdapter` |
| 8 | `save(OrderEntity)` | adapter → `OrderJpaRepository` |
| 9 | `INSERT orders + items` | `SimpleJpaRepository` → `Database` |
| 10 | `ok` *(return)* | `Database` |
| 11 | `saved entity` *(return)* | `OrderEntity` |
| 12 | `saved Order (domain)` *(return)* | `OrderEntityMapper.toDomain` |
| 13 | `Result(orderId, total)` *(return)* | `PlaceOrderResult` |
| 14 | `201 Created {orderId, total}` | `ApiResponse.created` |

### Thứ tự lời gọi được canh bằng test

Bài test `luongChayDungThuTuSequenceDiagram()` ghi lại mọi lời gọi từ
`PlaceOrderService` đi ra và chốt đúng **hai** message mà hình vẽ cho lifeline này:

```java
List<String> mongDoi = List.of(
        "OrderPricingService.calculateTotal",
        "OrderRepository.save");
```

Thêm bất kỳ lời gọi nào khác — dù code vẫn chạy đúng — là bài test này đỏ.

---

## 2. Phạm vi: luồng ghi bám sát hình, luồng đọc là phần mở rộng

Sequence diagram chỉ vẽ **một** luồng: đặt hàng. Luồng ghi được giữ đúng từng chi
tiết của hình:

| | |
|---|---|
| `PlaceOrderUseCase` | Đúng **một** method: `placeOrder` |
| Message từ `PlaceOrderService` | Đúng **hai**: `calculateTotal` rồi `save` — có test canh |
| `PlaceOrderResult` | Đúng **hai** trường `{orderId, total}` như hình ghi. Có một test đếm số thành phần của record để không ai âm thầm thêm trường vào hợp đồng API. |
| Bảng dữ liệu | Hai bảng `orders` + `order_items`, khớp `INSERT orders + items` |

`GET /orders/{id}` **không có trong hình** — nó được thêm vào có chủ đích để bài
tập có đủ cả hai chiều đọc/ghi. Phần này được đánh dấu rõ ở đây để người chấm phân
biệt được đâu là yêu cầu của đề, đâu là phần tự thêm:

| Thành phần thêm cho luồng đọc | Vòng |
|---|---|
| `GetOrderUseCase`, `OrderView`, `OrderNotFoundException` | 2 — Use Cases |
| `GetOrderService` | 2 — Use Cases |
| `OrderRepository.findById` | 2 — Use Cases (outbound port) |
| `JpaOrderRepositoryAdapter.findById`, `OrderPersistenceMapping.fromRows` | 3 — Interface Adapters |
| `OrderController.getOrder`, `OrderJsonMapper.toJson(OrderView)` | 3 — Interface Adapters |
| `Database.selectById/selectWhere`, `JpaRepository.findById` | 4 — Frameworks & Drivers |

Luồng ghi không bị đụng tới một dòng nào: bài test đối chiếu sequence diagram vẫn
xanh, và `PlaceOrderResult` vẫn đúng hai trường.

### Vì sao DTO đọc tách khỏi DTO ghi

`PlaceOrderResult` trả `{orderId, total}`; `OrderView` trả đầy đủ chi tiết đơn.
Gộp làm một sẽ khiến mỗi lần màn hình chi tiết cần thêm trường là response của API
tạo đơn phình theo. Tách ra thì hai bên tiến hoá độc lập — đây là ý tưởng nền tảng
của **CQRS** (tách mô hình đọc khỏi mô hình ghi).

Mã đơn hàng được cấp **lúc lưu**, đúng như hình: hình không có message hỏi mã đơn
trước khi save, mã đơn đi ngược ra qua chuỗi `saved entity` → `saved Order (domain)`.
Đây chính là ngữ nghĩa `@GeneratedValue` của JPA.

---

## 3. Giả định của người làm bài

Sequence diagram là spec về **cấu trúc** (class nào, gọi gì, theo thứ tự nào). Nó
vẽ `calculateTotal(items) → total` nhưng **không quy định công thức tính giá**.
Phần dưới đây là **giả định tự đặt ra để `calculateTotal` có việc mà làm**, không
phải lấy từ đề bài:

| Quy tắc | Giá trị |
|---|---|
| Tạm tính | Σ (đơn giá × số lượng) |
| Giảm giá | 10% khi tạm tính ≥ 500.00 |
| Phí ship | Miễn phí khi tạm tính ≥ 100.00, ngược lại 9.99 |
| Tổng cộng | tạm tính − giảm giá + phí ship |

Ba con số ngưỡng nằm gọn trong `OrderPricingService` dưới dạng hằng số có tên, nên
đổi chính sách chỉ sửa một file. Nếu đề bài thật có biểu giá khác, thay các hằng số
đó là xong — không tầng nào khác phải đụng tới.

Các con số trung gian (tạm tính, giảm giá, phí ship) vẫn được tính và vẫn ghi đầy
đủ xuống bảng `orders`; chúng chỉ **không** nằm trong hợp đồng trả về, vì hình chỉ
ghi `Result(orderId, total)`.

---

## 4. Bốn vòng của Clean Architecture

```
   Vòng 4  infrastructure   Frameworks & Drivers  (HTTP server, Spring Data JPA, H2)
   Vòng 3  adapter          Interface Adapters    (Controller, Repository Adapter)
   Vòng 2  application      Use Cases             (inbound port, interactor, outbound port)
   Vòng 1  domain           Entities              (aggregate, value object, domain service)

   THE DEPENDENCY RULE: mã nguồn ở vòng trong KHÔNG biết gì về vòng ngoài.
```

**Điểm mấu chốt:** `OrderRepository` là interface do tầng **application** sở hữu,
còn `JpaOrderRepositoryAdapter` hiện thực nó lại nằm ở vòng **adapter** bên ngoài.
Lúc *chạy* thì use case gọi ra ngoài, nhưng lúc *biên dịch* thì mũi tên phụ thuộc
vẫn chỉ vào trong. Đó là **Dependency Inversion**.

### Luật kiến trúc được THỰC THI, không chỉ được ghi chép

`.\run.ps1 test` chạy 5 **fitness function** đọc thẳng mã nguồn và fail build nếu
có file vượt ranh giới:

```
[OK]   tang 'domain' khong phu thuoc application, adapter, infrastructure, bootstrap
[OK]   tang 'application' khong phu thuoc adapter, infrastructure, bootstrap
[OK]   tang 'domain' khong dinh cong nghe ha tang (spring, jakarta, javax, sql, net)
[OK]   tang 'application' khong dinh cong nghe ha tang
[OK]   tang 'adapter' khong phu thuoc bootstrap
```

Vòng 3 **được phép** phụ thuộc vòng 4 — đó đúng là việc của nó: làm nơi duy nhất
chạm vào framework. Cái bị cấm tuyệt đối là domain và application chạm vào hạ tầng.

---

## 5. Cấu trúc thư mục

```
ass1/
├── pom.xml
├── run.ps1
└── src/
    ├── main/java/com/example/ordering/
    │   ├── domain/                       ← VÒNG 1 (Entities)
    │   │   ├── Order.java                   Aggregate Root
    │   │   ├── OrderPricingService.java     Domain Service — lifeline trong hình
    │   │   ├── OrderItem, PriceBreakdown
    │   │   ├── Money, Quantity              Value Object
    │   │   ├── OrderId, CustomerId, ProductId
    │   │   └── OrderStatus, DomainException
    │   ├── application/                  ← VÒNG 2 (Use Cases)
    │   │   ├── port/in/                     PlaceOrderUseCase, PlaceOrderCommand,
    │   │   │                                PlaceOrderResult
    │   │   ├── port/out/OrderRepository.java
    │   │   └── usecase/PlaceOrderService.java
    │   ├── adapter/                      ← VÒNG 3 (Interface Adapters)
    │   │   ├── in/web/                      OrderController, OrderJsonMapper,
    │   │   │                                ApiResponse, Json, JsonSerializer
    │   │   └── out/persistence/             JpaOrderRepositoryAdapter, OrderEntity,
    │   │                                    OrderEntityMapper, OrderJpaRepository,
    │   │                                    GeneratedOrderJpaRepository,
    │   │                                    OrderPersistenceMapping
    │   ├── infrastructure/               ← VÒNG 4 (Frameworks & Drivers)
    │   │   ├── web/HttpServerRunner.java    HTTP server
    │   │   ├── jpa/                         giả lập Spring Data JPA
    │   │   └── db/Database.java             giả lập H2/Postgres
    │   └── bootstrap/Main.java           ← COMPOSITION ROOT (chỉ lắp ráp)
    └── test/java/com/example/ordering/
        ├── SelfCheck.java                   20 test nghiệp vụ
        ├── SequenceRecorder.java            ghi lại thứ tự lời gọi
        └── ArchitectureFitness.java         5 fitness function
```

> Spring Data JPA và H2 được **giả lập** bằng vài class thuần JDK để bài chạy được
> mà không cần tải thư viện. Chữ ký hàm giữ giống bản thật, nên khi chuyển sang
> Spring Boot chỉ việc xoá package `infrastructure/` và đổi import.
