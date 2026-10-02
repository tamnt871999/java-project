# Day15_Advanced-Data-Access — Quản lý kho với Pessimistic Lock, Optimistic Lock và Envers

## Đề bài

> Xây dựng một hệ thống quản lý kho đơn giản cho một sản phẩm.
>
> Yêu cầu:
> 1. Tạo một Spring Boot project.
> 2. Tạo Entity Inventory với các trường: productId, productName, quantity, và version (cho optimistic lock).
> 3. Tạo API POST /inventories/{productId}/stock-in để nhập hàng (tăng quantity).
>    - Yêu cầu: Sử dụng Pessimistic Locking để xử lý việc nhiều người cùng nhập hàng cho một sản phẩm.
> 4. Tạo API PUT /inventories/{productId} để cập nhật thông tin sản phẩm (ví dụ: productName).
>    Yêu cầu: Sử dụng Optimistic Locking để xử lý việc nhiều người cùng sửa thông tin sản phẩm.
> 5. Tích hợp Hibernate Envers để theo dõi toàn bộ lịch sử thay đổi của Inventory.
> 6. Tạo API GET /inventories/{productId}/history để xem lại lịch sử thay đổi số lượng tồn kho.

## Chạy

```bash
cd Day15_Advanced-Data-Access
.\mvnw test              # 14 test
.\mvnw spring-boot:run   # cong 8085, H2 in-memory
```

Đề không có API tạo sản phẩm, nên lúc khởi động
[`InventorySeeder`](src/main/java/com/example/inventory/config/InventorySeeder.java) tạo sẵn một
sản phẩm: `productId = 1`, `"Ban phim co"`, `quantity = 100`. Sản phẩm được lưu qua JPA nên
Envers ghi luôn revision đầu tiên.

```bash
curl -X POST http://localhost:8085/inventories/1/stock-in -H 'Content-Type: application/json' -d '{"quantity": 5}'
curl -X PUT  http://localhost:8085/inventories/1 -H 'Content-Type: application/json' -d '{"productName": "Ban phim co Pro", "version": 1}'
curl http://localhost:8085/inventories/1/history
```

## Mỗi yêu cầu nằm ở đâu

| # | Yêu cầu | Cài đặt |
|---|---|---|
| 1 | Spring Boot project | Spring Boot 3.4 · Spring Data JPA · H2 |
| 2 | Entity `Inventory` | [`Inventory.java`](src/main/java/com/example/inventory/entity/Inventory.java): `productId` là `@Id`, `version` là `@Version` |
| 3 | Stock-in, Pessimistic Locking | [`InventoryRepository.findByIdForUpdate`](src/main/java/com/example/inventory/repository/InventoryRepository.java) có `@Lock(PESSIMISTIC_WRITE)`, gọi trong `InventoryService.stockIn` (`@Transactional`) |
| 4 | Cập nhật, Optimistic Locking | `InventoryService.update`: client gửi kèm `version` đã đọc; lệch thì `409` |
| 5 | Hibernate Envers | `@Audited` trên `Inventory` → bảng `inventory_aud` + `revinfo` |
| 6 | Lịch sử số lượng | `InventoryService.quantityHistory`: truy vấn Envers, chỉ lấy revision có `quantity` thay đổi |

## Pessimistic Locking — nhập hàng

Nhập hàng là thao tác **đọc – cộng – ghi**. Hai người cùng đọc `quantity = 100` rồi cùng ghi
`105` thì mất một lượt nhập.

`PESSIMISTIC_WRITE` khoá dòng ngay lúc đọc. SQL Hibernate sinh ra (lấy từ log khi chạy app):

```sql
select ... from inventory i1_0 where i1_0.product_id=? for update
```

Người thứ hai phải **chờ** ở câu `select` này tới khi người thứ nhất commit, rồi mới đọc được số
lượng mới. Nhờ vậy mọi lượt nhập đều thành công, không ai bị từ chối. Đây là lý do chọn khoá bi
quan cho nhập hàng: tranh chấp trên một sản phẩm là chuyện thường xuyên, và lượt nhập nào cũng
phải được ghi nhận.

Hai test kiểm điều này. Tạm thay `findByIdForUpdate` bằng `findById` thì cả hai đều đỏ:

| Test | Có khoá | Bỏ khoá |
|---|---|---|
| 20 người nhập 1 cùng lúc → `quantity = 120` | xanh | đỏ: `ObjectOptimisticLockingFailureException` |
| Một người đang giữ khoá và nhập 10 → người thứ hai phải chờ, xong đọc được `110` rồi cộng 5 thành `115` | xanh | đỏ |

## Optimistic Locking — sửa thông tin sản phẩm

`@Version` làm Hibernate sinh câu `update` có điều kiện version:

```sql
update inventory set product_name=?,quantity=?,version=? where product_id=? and version=?
```

Sửa tên thì hiếm khi trùng nhau, nên không khoá. Ai ghi sau trên bản đã cũ thì bị từ chối, thay vì
âm thầm đè lên thay đổi của người trước.

Hai người sửa tên thường cách nhau vài giây, tức là nằm ở **hai request khác nhau**. Vì vậy client
phải gửi kèm `version` mà mình đã đọc:

```json
{ "productName": "Ban phim co Pro", "version": 1 }
```

| Tình huống | Ai chặn | Kết quả |
|---|---|---|
| Người khác đã sửa trước, `version` gửi lên đã cũ | `InventoryService.update` so `version` | `409 VERSION_CONFLICT`, tên không đổi |
| Hai giao dịch cùng đọc một version, cùng ghi trong tích tắc | `@Version` (`where version=?` khớp 0 dòng) | `409 VERSION_CONFLICT` |

`version` là của cả dòng, nên mỗi lần nhập hàng cũng làm `version` tăng. Client lấy `version` mới
nhất từ response của `stock-in` hoặc `PUT`.

## Envers và lịch sử

`@Audited` ghi **mọi** thay đổi của `Inventory` vào `inventory_aud`, kể cả lần chỉ đổi tên.
Trường `quantity` có thêm `withModifiedFlag = true`, nên Envers ghi thêm cột `quantity_mod` cho biết
revision đó có đổi số lượng hay không. API lịch sử lọc theo cột này:

```json
[
  {"revision": 1, "changedAt": "2026-10-02T04:19:14.995Z", "quantity": 100},
  {"revision": 2, "changedAt": "2026-10-02T04:19:28.747Z", "quantity": 105},
  {"revision": 4, "changedAt": "2026-10-02T04:19:28.923Z", "quantity": 108}
]
```

Kết quả trên lấy từ app thật sau chuỗi thao tác: khởi tạo 100 → nhập 5 → đổi tên → nhập 3.
Revision 3 là lần đổi tên: vẫn nằm trong `inventory_aud`, nhưng không hiện ở đây vì số lượng không đổi.

## Mã lỗi

| Tình huống | HTTP | `code` |
|---|---|---|
| `quantity` không dương, `productName` rỗng, thiếu `version`, JSON hỏng, `productId` không phải số | `400` | `BAD_REQUEST` |
| `productId` không tồn tại | `404` | `INVENTORY_NOT_FOUND` |
| `version` gửi lên đã cũ | `409` | `VERSION_CONFLICT` |
