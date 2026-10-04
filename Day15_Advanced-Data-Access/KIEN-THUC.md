# Kiến thức cơ bản — Day15_Advanced-Data-Access

Bài này xoay quanh một câu hỏi: khi nhiều người cùng thay đổi **một** dòng `Inventory` cùng lúc,
làm sao để không ai đè mất thay đổi của ai, và làm sao xem lại số lượng tồn kho đã thay đổi thế nào.

## Thứ tự nên học

1. **Transaction** — một nhóm thao tác với database thành công hoặc huỷ cùng nhau.
2. **Lost update** — lỗi mất cập nhật khi hai người cùng "đọc – sửa – ghi" một dòng.
3. **Pessimistic locking** — khoá dòng ngay lúc đọc, người khác phải chờ.
4. **Optimistic locking với `@Version`** — không khoá, lúc ghi thì kiểm tra version.
5. **Optimistic locking qua nhiều HTTP request** — vì sao client phải gửi kèm `version`.
6. **Auditing với Hibernate Envers** — lưu lại lịch sử thay đổi của entity.

## 1. Transaction

**Là gì:** **Transaction** (giao dịch: một nhóm thao tác database được xem như một khối) hoặc thành
công toàn bộ khi **commit** (xác nhận ghi), hoặc bị huỷ toàn bộ khi **rollback** (hoàn tác). Trong
Spring, method có `@Transactional` chạy trọn trong một transaction.

**Vì sao cần:** Khoá trong database gắn với transaction: được giữ từ lúc lấy tới khi transaction kết
thúc. Vì vậy cần một transaction bao trọn cả bước đọc lẫn bước ghi.

**Trong bài này:** `stockIn`, `update`, `quantityHistory` trong
[`InventoryService.java`](src/main/java/com/example/inventory/service/InventoryService.java)
đều có `@Transactional`. [`README.md`](README.md) (mục "Pessimistic Locking — nhập hàng") ghi rõ
người thứ hai phải chờ tới khi người thứ nhất commit.

## 2. Lost update và thao tác đọc – sửa – ghi

**Là gì:** **Read-modify-write** (đọc – sửa – ghi) là đọc giá trị cũ, tính giá trị mới trong code, rồi
ghi lại. **Lost update** (mất cập nhật: lần ghi sau đè mất lần ghi trước) xảy ra khi hai người cùng
đọc một giá trị cũ rồi lần lượt ghi đè lên nhau.

**Vì sao cần hiểu:** Đây chính là lỗi mà cả hai loại khoá trong bài sinh ra để chặn.

**Trong bài này:** nhập hàng là đọc – cộng – ghi (`quantity += amount` trong
[`Inventory.java`](src/main/java/com/example/inventory/entity/Inventory.java)).
Hai người cùng đọc `quantity = 100`, mỗi người nhập 5, cùng ghi `105` → đúng ra phải là `110`.

## 3. Pessimistic locking

**Là gì:** **Pessimistic locking** (khoá bi quan: giả định xung đột sẽ xảy ra nên khoá trước) khoá
dòng ngay lúc đọc. Ai muốn đọc để sửa cùng dòng đó phải **chờ** tới khi người giữ khoá commit.

**Vì sao cần:** Người sau chỉ đọc được khi người trước đã ghi xong, nên luôn thấy số mới nhất.
Không mất lượt nhập nào và cũng không ai bị từ chối.

**Trong bài này:** [`InventoryRepository.java`](src/main/java/com/example/inventory/repository/InventoryRepository.java):

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select i from Inventory i where i.productId = :productId")
Optional<Inventory> findByIdForUpdate(@Param("productId") Long productId);
```

Hibernate sinh ra SQL có `for update` (lấy từ log, ghi trong README):
`select ... from inventory i1_0 where i1_0.product_id=? for update`.
[`InventoryApiTest.java`](src/test/java/com/example/inventory/InventoryApiTest.java)
chứng minh: 20 người cùng nhập 1 → `quantity = 120`; khi một người đang giữ khoá, người thứ hai phải
chờ rồi mới đọc được `110`.

## 4. Optimistic locking với `@Version`

**Là gì:** **Optimistic locking** (khoá lạc quan: giả định hiếm khi xung đột nên không khoá) không
chặn ai đọc. Mỗi dòng có cột **version** (số phiên bản), tăng 1 sau mỗi lần ghi. Lúc ghi, database
chỉ cập nhật nếu version vẫn đúng là version mình đã đọc.

**Vì sao cần:** Ai ghi sau trên một bản đã cũ sẽ bị từ chối, thay vì âm thầm đè lên người trước.

**Trong bài này:** trường `version` có `@Version` trong
[`Inventory.java`](src/main/java/com/example/inventory/entity/Inventory.java).
Hibernate sinh câu `update` có điều kiện version (theo README):

```sql
update inventory set product_name=?,quantity=?,version=? where product_id=? and version=?
```

Nếu `where ... version=?` khớp 0 dòng, nghĩa là đã có người ghi trước: Spring ném
`ObjectOptimisticLockingFailureException` (test `haiGiaoDichCungSuaThiGiaoDichSauBiTuChoi`), và
[`ApiExceptionHandler.java`](src/main/java/com/example/inventory/exception/ApiExceptionHandler.java)
dịch thành `409 VERSION_CONFLICT`.

## 5. Optimistic locking qua nhiều HTTP request

**Là gì:** Người dùng đọc dữ liệu ở request này, vài giây sau mới gửi sửa ở request khác. Hai request
là hai transaction riêng, nên client phải gửi lên **version mà mình đã đọc**.

**Vì sao cần:** Trong request `PUT`, server đọc lại dòng từ database nên luôn thấy version mới nhất.
Câu `update` của `@Version` dùng chính version vừa đọc đó nên sẽ khớp, không phát hiện được người
khác đã sửa trong lúc client còn đang xem.

**Trong bài này:** body `PUT` là `{ "productName": "Ban phim co Pro", "version": 1 }`.
`InventoryService.update` so version client gửi với version hiện tại, lệch thì `409`:

```java
if (inventory.getVersion() != expectedVersion) {
    throw new InventoryVersionConflictException(productId, expectedVersion, inventory.getVersion());
}
```

Vậy bài có hai lớp chặn: phép so này (xung đột giữa hai request) và `@Version` (hai transaction cùng
ghi trong tích tắc). Xem bảng ở mục "Optimistic Locking — sửa thông tin sản phẩm" của [`README.md`](README.md).

## 6. Auditing với Hibernate Envers

**Là gì:** **Auditing** (lưu vết: giữ lại mọi phiên bản cũ của dữ liệu) giúp xem lại lịch sử.
**Hibernate Envers** làm việc này tự động cho entity có `@Audited`. Mỗi transaction có thay đổi entity
tạo ra một **revision** (lần sửa đổi, đánh số tăng dần).

**Vì sao cần:** Bảng `inventory` chỉ giữ trạng thái **hiện tại**; muốn biết số lượng từng là bao
nhiêu thì phải có nơi lưu các bản cũ.

**Trong bài này:**

- `@Audited` trên `Inventory` → bảng `inventory_aud` (ảnh chụp dòng `Inventory` ở mỗi revision) và
  bảng `revinfo` (số revision và thời điểm xảy ra).
- `quantity` có `@Audited(withModifiedFlag = true)` → thêm cột `quantity_mod` cho biết revision đó có
  đổi `quantity` hay không. Đây là **modified flag** (cờ đánh dấu trường đã đổi).
- `quantityHistory` trong
  [`InventoryService.java`](src/main/java/com/example/inventory/service/InventoryService.java)
  chỉ lấy revision có `quantity` thay đổi: `.add(AuditEntity.property("quantity").hasChanged())`.

Ví dụ trong README: khởi tạo 100 → nhập 5 → đổi tên → nhập 3 tạo 4 revision, nhưng API lịch sử chỉ
trả revision 1, 2, 4 (số lượng 100, 105, 108). Revision 3 là lần đổi tên.

## Hay nhầm

- **"Có `@Version` rồi thì stock-in không cần khoá bi quan."** → Bỏ khoá thì test 20 người đỏ với
  `ObjectOptimisticLockingFailureException`: có người bị từ chối, trong khi lượt nhập nào cũng phải
  được ghi nhận.
- **"`version` chỉ tăng khi sửa tên."** → `version` là của cả dòng; nhập hàng cũng làm nó tăng, nên
  client lấy `version` mới nhất từ response của `stock-in` hoặc `PUT`.
- **"`@Version` tự chặn được xung đột giữa hai HTTP request."** → Không; request sau đọc lại version
  mới nhất, nên phải so với `version` client gửi lên.
- **"API lịch sử đọc từ bảng `inventory`."** → Bảng đó chỉ có trạng thái hiện tại; lịch sử nằm ở
  `inventory_aud` + `revinfo`, đọc qua Envers.

## Tự kiểm tra

1. Hai người cùng đọc `quantity = 100`, mỗi người nhập 5, không có khoá. Kết quả có thể sai thành bao nhiêu? Lỗi này tên gì?

<details><summary>Đáp án</summary>Có thể thành 105 thay vì 110. Đó là lost update: lần ghi sau đè mất lần ghi trước.</details>

2. Vì sao nhập hàng dùng pessimistic locking còn sửa tên dùng optimistic locking?

<details><summary>Đáp án</summary>Nhập hàng hay bị tranh chấp và lượt nào cũng phải thành công, nên khoá để người sau chờ rồi cộng tiếp. Sửa tên hiếm khi trùng; nếu trùng thì từ chối người ghi sau (409) là chấp nhận được.</details>

3. Sản phẩm vừa tạo có `version = 0`. Có một lượt `stock-in`, sau đó client gửi `PUT` với `"version": 0`. Kết quả?

<details><summary>Đáp án</summary>409 VERSION_CONFLICT. Lượt nhập hàng đã làm version thành 1, nên version 0 client gửi lên đã cũ.</details>

4. Câu SQL nào cho thấy pessimistic lock đang hoạt động, câu nào cho thấy `@Version` đang hoạt động?

<details><summary>Đáp án</summary>Pessimistic: select ... for update. @Version: update ... where product_id=? and version=?.</details>

5. Khởi tạo 100 → nhập 5 → đổi tên → nhập 3. Có mấy revision, API `history` trả mấy phần tử, vì sao khác nhau?

<details><summary>Đáp án</summary>4 revision nhưng history trả 3 phần tử. Lần đổi tên không đổi quantity (cờ quantity_mod không bật) nên bị hasChanged() lọc bỏ.</details>
