# Kiến thức cơ bản — Day1_Clean-Architect

Bài này xoay quanh một câu hỏi: làm sao chia một API đặt hàng nhỏ thành các tầng sao cho phần
nghiệp vụ (quy tắc của đơn hàng) **không phụ thuộc** vào Spring, HTTP hay database.

## Thứ tự nên học

1. Clean Architecture và Dependency Rule — ba tầng và luật duy nhất giữa chúng.
2. Domain — nơi chứa quy tắc nghiệp vụ thuần.
3. Use case và input port — tầng `application` điều phối công việc.
4. Output port và Dependency Inversion — vì sao `OrderRepository` nằm ở tầng trong.
5. Adapter — nối tầng trong với HTTP và database, đổi dạng dữ liệu ở ranh giới.
6. Fitness function — biến luật kiến trúc thành test chạy được.

## 1. Clean Architecture và Dependency Rule

**Là gì:** **Clean Architecture** (kiến trúc chia code thành các vòng, nghiệp vụ ở giữa, công
nghệ ở ngoài) là cách tổ chức code theo tầng. **Dependency Rule** (luật phụ thuộc) nói: code ở
tầng trong không được biết gì về tầng ngoài — tức là không `import` tầng ngoài.

**Vì sao cần:** Nếu nghiệp vụ `import` thẳng Spring hay JPA thì đổi công nghệ là phải sửa cả
nghiệp vụ. Giữ phụ thuộc chỉ đi vào trong thì phần quan trọng nhất ít bị ảnh hưởng nhất.

**Trong bài này:** Ba package `adapter/` → `application/` → `domain/`, mũi tên là chiều được
phép `import`. Xem mục "Clean Architecture — ý tưởng cốt lõi" trong [README.md](README.md).

## 2. Domain

**Là gì:** **Domain** (miền nghiệp vụ) là các class mô tả đúng bài toán: đơn hàng, dòng hàng, và
quy tắc của chúng. Chỉ dùng Java thuần, không annotation của framework.

**Vì sao cần:** Quy tắc nghiệp vụ là thứ phải đúng dù dữ liệu đến từ HTTP, từ test hay từ chỗ
khác. Đặt nó ở một chỗ duy nhất thì không ai "đi vòng" qua được.

**Trong bài này:** [Order.java](src/main/java/com/example/ordering/domain/Order.java)
chặn sản phẩm trùng; [OrderItem.java](src/main/java/com/example/ordering/domain/OrderItem.java)
chặn số lượng < 1. Vi phạm thì ném
[DomainException.java](src/main/java/com/example/ordering/domain/DomainException.java),
được dịch thành `422`.

```java
public record OrderItem(String productId, int quantity, BigDecimal unitPrice) {
    public OrderItem {
        if (quantity < 1) {
            throw new DomainException("So luong phai lon hon 0, nhan duoc " + quantity);
        }
    }
}
```

## 3. Use case và input port

**Là gì:** **Use case** (một việc người dùng muốn hệ thống làm, vd. "tạo đơn hàng") là class điều
phối: nhận dữ liệu, gọi domain, gọi lưu trữ. **Input port** (cổng vào: interface mô tả use case mà
tầng ngoài được gọi) là hợp đồng giữa controller và use case.

**Vì sao cần:** Controller chỉ biết interface, không biết class cụ thể làm việc. Use case nhận
`PlaceOrderCommand` thay vì JSON nên không biết gì về HTTP.

**Trong bài này:** Interface [PlaceOrderUseCase.java](src/main/java/com/example/ordering/application/port/in/PlaceOrderUseCase.java)
và [GetOrderUseCase.java](src/main/java/com/example/ordering/application/port/in/GetOrderUseCase.java);
class làm việc là [PlaceOrderService.java](src/main/java/com/example/ordering/application/usecase/PlaceOrderService.java)
và [GetOrderService.java](src/main/java/com/example/ordering/application/usecase/GetOrderService.java).

## 4. Output port và Dependency Inversion

**Là gì:** **Output port** (cổng ra: interface do tầng trong định nghĩa để gọi ra ngoài, vd. lưu
đơn hàng). **Dependency Inversion** (đảo ngược phụ thuộc) nghĩa là tầng trong sở hữu interface,
tầng ngoài `implements` nó — nên chiều `import` ngược với chiều lời gọi.

**Vì sao cần:** Use case cần lưu vào database (tầng ngoài) mà vẫn không được `import` tầng ngoài.
Cách giải: use case chỉ gọi interface của chính nó; adapter bên ngoài lo phần hiện thực.

**Trong bài này:** [OrderRepository.java](src/main/java/com/example/ordering/application/port/out/OrderRepository.java)
nằm ở `application/port/out/` và không `extends JpaRepository`. Lúc chạy, use case gọi ra
database; lúc biên dịch, adapter `import` vào trong. Xem mục "Điểm mấu chốt: `OrderRepository`"
trong [README.md](README.md).

## 5. Adapter

**Là gì:** **Adapter** (bộ chuyển đổi: class nối tầng trong với một công nghệ cụ thể). **Adapter
in** nhận yêu cầu từ ngoài vào (HTTP); **adapter out** thực hiện việc tầng trong cần ra ngoài
(database).

**Vì sao cần:** Mọi chi tiết công nghệ — `@RestController`, `@Entity`, Spring Data — dồn hết vào
đây, để tầng trong sạch. Adapter cũng đổi dạng dữ liệu ở mỗi ranh giới.

**Trong bài này:**
- In: [OrderController.java](src/main/java/com/example/ordering/adapter/in/web/OrderController.java)
  đổi `PlaceOrderRequest` thành `PlaceOrderCommand`;
  [ApiExceptionHandler.java](src/main/java/com/example/ordering/adapter/in/web/ApiExceptionHandler.java)
  đổi exception thành mã HTTP.
- Out: [OrderRepositoryAdapter.java](src/main/java/com/example/ordering/adapter/out/persistence/OrderRepositoryAdapter.java)
  `implements OrderRepository`, đổi `Order` ↔ [OrderJpaEntity.java](src/main/java/com/example/ordering/adapter/out/persistence/OrderJpaEntity.java).

Chuỗi đổi dạng: `JSON → PlaceOrderRequest → PlaceOrderCommand → Order → OrderJpaEntity → SQL`.
Nhờ vậy đổi tên cột trong database không làm đổi tên trường trên JSON.

## 6. Fitness function

**Là gì:** **Fitness function** (hàm kiểm tra sức khỏe kiến trúc) là test tự động kiểm tra code có
tuân theo luật kiến trúc không, và làm build fail khi vi phạm.

**Vì sao cần:** Luật chỉ viết trong tài liệu thì dễ bị quên. Một dòng `import jakarta.persistence`
lỡ tay trong `Order.java` vẫn biên dịch được; chỉ có test mới bắt được.

**Trong bài này:** [ArchitectureFitnessTest.java](src/test/java/com/example/ordering/architecture/ArchitectureFitnessTest.java)
đọc từng dòng `import` trong source và kiểm 4 luật: domain không biết application/adapter; domain
không dính Spring, `jakarta.*`, `javax.*`, `com.fasterxml`, `java.sql`; application không biết
adapter; adapter web không biết adapter persistence.

## Hay nhầm

- **"`@Valid` là đủ để kiểm tra đầu vào."** → `@Valid` chỉ kiểm cú pháp (thiếu trường → `400`);
  `quantity: 0` vẫn lọt qua và bị `OrderItem` trong domain chặn (→ `422`).
- **"`OrderJpaEntity` chính là domain."** → Nó là class của adapter persistence; domain là
  `Order`, và adapter chuyển đổi qua lại giữa hai class này.
- **"Dependency Inversion nghĩa là lời gọi đi ngược."** → Lời gọi vẫn đi từ use case ra database;
  chỉ chiều `import` bị đảo (adapter `import` interface của tầng trong).
- **"Clean Architecture là cấm hẳn Spring ở tầng application."** → Bài này vẫn để `@Service` trên
  use case cho đơn giản (README ghi là đã lược bỏ); fitness test chỉ cấm Spring ở `domain/`.

## Tự kiểm tra

1. Vì sao `OrderRepository` nằm ở `application/port/out/` mà không ở `adapter/`?

<details><summary>Đáp án</summary>

Để tầng application sở hữu interface; adapter <code>implements</code> nó nên phụ thuộc chỉ đi vào trong.
Đổi database chỉ cần viết adapter khác, không sửa <code>domain/</code> và <code>application/</code>.

</details>

2. Gửi `quantity: 0` thì nhận mã gì, và class nào từ chối?

<details><summary>Đáp án</summary>

<code>422 BUSINESS_RULE_VIOLATED</code>. <code>OrderItem</code> trong domain ném <code>DomainException</code>,
<code>ApiExceptionHandler</code> dịch thành 422.

</details>

3. Controller gọi trực tiếp `PlaceOrderService` hay gọi gì?

<details><summary>Đáp án</summary>

Gọi interface <code>PlaceOrderUseCase</code> (input port); Spring tiêm class hiện thực vào qua constructor.

</details>

4. Nếu ai đó thêm `import jakarta.persistence.Entity;` vào `Order.java` thì chuyện gì xảy ra?

<details><summary>Đáp án</summary>

Code vẫn biên dịch được, nhưng <code>ArchitectureFitnessTest</code> (luật "domain khong dinh cong nghe ha tang")
fail nên build đỏ.

</details>

5. Vì sao không dùng thẳng `PlaceOrderRequest` làm tham số cho use case?

<details><summary>Đáp án</summary>

<code>PlaceOrderRequest</code> thuộc tầng web (có annotation validation, gắn với JSON). Use case nhận
<code>PlaceOrderCommand</code> của tầng application để không phụ thuộc vào HTTP.

</details>
