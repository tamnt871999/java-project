# Kiến thức cơ bản — Day3_Domain-Driven-Design

Câu hỏi của bài: làm sao để ví điện tử **không bao giờ** sai (số dư âm, ví khoá vẫn rút được),
bằng cách chuyển luật nghiệp vụ từ service vào chính đối tượng `Wallet`.

## Thứ tự nên học

1. Anemic domain model và rich domain model — vấn đề xuất phát của bài.
2. Invariant và encapsulation — thứ mà rich model phải bảo vệ, và cách bảo vệ.
3. Value Object — `Money`, `WalletId`, `WalletStatus`.
4. Entity và Aggregate Root — vai trò của `Wallet`.
5. Clean Architecture và Dependency Rule — vì sao `domain/` không biết Spring hay database.

## 1. Anemic domain model và rich domain model

**Là gì.** **Anemic domain model** (mô hình "thiếu máu": class chỉ chứa dữ liệu, không có hành
vi) là khi dữ liệu một nơi, luật ở nơi khác (thường là service). **Rich domain model** (mô hình
"giàu hành vi") là khi đối tượng vừa giữ dữ liệu vừa tự thực thi luật nghiệp vụ của chính nó.

**Vì sao cần.** Khi luật nằm ngoài đối tượng, chỉ cần một chỗ quên kiểm tra là dữ liệu sai. Bản
cũ quên luật "không rút quá số dư", nên rút 5000 từ ví 1000 ra `-4000.00` mà không báo lỗi.

**Trong bài này.** Bản cũ (giữ làm tang vật trong
[`LegacyWalletCode.java`](src/test/java/com/example/wallet/legacy/LegacyWalletCode.java)):
`WalletEntity` ba field `public`, luật nằm ở `WalletService`. Bản mới
[`Wallet.java`](src/main/java/com/example/wallet/domain/Wallet.java)
có method nghiệp vụ `withdrawMoney(BigDecimal)`, `lockWallet()`. Xem thêm mục 1 của
[`README.md`](README.md).

## 2. Invariant và encapsulation

**Là gì.** **Invariant** (bất biến: điều luôn phải đúng với đối tượng, ở mọi thời điểm) là luật
kiểu "số dư không âm". **Encapsulation** (đóng gói: giấu dữ liệu bên trong, chỉ cho thay đổi qua
method do chính class cung cấp) là cách giữ cho invariant không bị phá.

**Vì sao cần.** Nếu `balance` là `public`, code khác vẫn viết được `wallet.balance = ...` để đi
vòng qua luật. Field `private` + không setter thì method nghiệp vụ là **cửa duy nhất**.

**Trong bài này.** `Wallet` có field `private`, không có setter. Vi phạm luật thì ném
[`DomainException`](src/main/java/com/example/wallet/domain/DomainException.java)
(exception nghiệp vụ tự định nghĩa), được dịch thành HTTP `422 BUSINESS_RULE_VIOLATED`:

```java
public void withdrawMoney(BigDecimal amount) {
    Money requested = new Money(amount);
    if (status.isLocked()) {
        throw new DomainException("Vi dien tu hien dang bi khoa, khong the rut tien");
    }
    if (!balance.isAtLeast(requested)) {
        throw new DomainException("So du khong du: can " + requested + " nhung chi con " + balance);
    }
    this.balance = balance.minus(requested);
}
```

[`WalletTest.java`](src/test/java/com/example/wallet/WalletTest.java)
dùng reflection (đọc cấu trúc class lúc chạy) kiểm không có method `set...`, mọi field `private`.

## 3. Value Object

**Là gì.** **Value Object** (đối tượng giá trị) là đối tượng được nhận diện bằng **giá trị** của
nó chứ không bằng id: hai tờ 100.00 là như nhau. Nó thường **immutable** (bất biến về dữ liệu:
tạo xong không sửa được; muốn đổi thì tạo object mới) và tự kiểm tra tính hợp lệ ngay khi tạo.

**Vì sao cần.** Kiểu nguyên thuỷ như `BigDecimal` hay `String` không mang luật nào. `BigDecimal`
chấp nhận số âm; `String` chấp nhận `"Locked"` lẫn `"LOCKED"`. Bản cũ gán `"Locked"` thì
`"LOCKED".equals(status)` trả `false`, khoá mất tác dụng mà không báo lỗi.

**Trong bài này.**

- [`Money`](src/main/java/com/example/wallet/domain/Money.java): chặn
  số âm trong constructor, field `final`, `minus()` trả `Money` mới. Luật "tiền không âm" chỉ
  viết **một lần** ở đây.
- [`WalletId`](src/main/java/com/example/wallet/domain/WalletId.java):
  `record` bọc `UUID`, để `findById(WalletId)` đọc ra là biết đang tìm ví.
- [`WalletStatus`](src/main/java/com/example/wallet/domain/WalletStatus.java):
  `enum` `ACTIVE` / `LOCKED`, để compiler canh tập giá trị hợp lệ thay cho chuỗi tự do.

## 4. Entity và Aggregate Root

**Là gì.** **Entity** (thực thể) là đối tượng có **danh tính** riêng: số dư đổi nhưng vẫn là
cùng một ví. **Aggregate** (cụm đối tượng thay đổi như một khối) có một **Aggregate Root** (gốc
của cụm) — cửa duy nhất bên ngoài được gọi vào, và là nơi canh invariant của cả cụm.

**Vì sao cần.** Nếu bên ngoài sửa thẳng từng phần bên trong, không ai đứng ra đảm bảo luật cho
cả khối. Đi qua một gốc duy nhất thì luật chỉ cần đặt ở một chỗ.

**Trong bài này.** `Wallet` là Aggregate Root, bên trong giữ `Money` và `WalletStatus`.

- `equals` chỉ so `WalletId` — đúng tính chất Entity.
- Constructor `private`; chỉ tạo qua `Wallet.open(...)` (ví mới, luôn `ACTIVE`) hoặc
  `Wallet.rehydrate(...)` (dựng lại ví đã lưu trong database).
- Cả ba lỗi `422` (ví khoá, không đủ số dư, số tiền âm) do **chính `Wallet`** phán xử;
  [`WithdrawMoneyService`](src/main/java/com/example/wallet/application/usecase/WithdrawMoneyService.java)
  chỉ điều phối: tìm ví → gọi `wallet.withdrawMoney(...)` → lưu lại.

## 5. Clean Architecture và Dependency Rule

**Là gì.** **Clean Architecture** chia code thành các vòng: trong cùng là nghiệp vụ, ngoài cùng
là công nghệ (web, database). **Dependency Rule** (luật phụ thuộc) nói `import` chỉ được hướng
vào trong. Tầng trong khai báo **port** (interface mô tả thứ nó cần), tầng ngoài viết **adapter**
(class hiện thực port bằng công nghệ cụ thể).

**Vì sao cần.** Nghiệp vụ không bị buộc vào framework: test `Wallet` chỉ cần `new`, không cần
khởi động Spring; đổi database chỉ cần viết adapter khác.

**Trong bài này.** Ba thư mục trong `src/main/java/com/example/wallet/`:

- `domain/` — `Wallet`, `Money`… không import Spring, JPA hay Jackson.
- `application/` — use case và port. Port ra
  [`WalletRepository`](src/main/java/com/example/wallet/application/port/out/WalletRepository.java)
  là interface do tầng này **sở hữu**.
- `adapter/` — nơi duy nhất biết Spring.
  [`WalletRepositoryAdapter`](src/main/java/com/example/wallet/adapter/out/persistence/WalletRepositoryAdapter.java)
  implements `WalletRepository`, chuyển qua lại giữa `Wallet` và class mang `@Entity`
  [`WalletJpaEntity`](src/main/java/com/example/wallet/adapter/out/persistence/WalletJpaEntity.java).

Luật import được canh bằng **fitness function** (test tự động fail build khi kiến trúc bị vi
phạm):
[`ArchitectureFitnessTest.java`](src/test/java/com/example/wallet/architecture/ArchitectureFitnessTest.java)
đọc từng dòng `import` của mỗi tầng.

## Hay nhầm

- **"Rich model nghĩa là bỏ hết service."** — Không. Use case vẫn còn, nhưng chỉ điều phối (tìm,
  gọi, lưu); luật nằm trong `Wallet`.
- **"Thêm một dòng `if` kiểm số dư vào `WalletService` là đủ."** — Chưa đủ: field còn `public`
  thì code khác vẫn sửa thẳng `balance` được. Phải đóng gói thì luật mới không lách được.
- **"`Wallet` là class `@Entity` của JPA."** — Không. `Wallet` không có annotation nào;
  `@Entity` nằm ở `WalletJpaEntity` trong `adapter/out/persistence/`.

## Tự kiểm tra

1. Vì sao bản cũ gán `status = "Locked"` thì ví vẫn rút được tiền?

<details><summary>Đáp án</summary>

Vì luật so sánh bằng chuỗi `"LOCKED".equals(status)`, khác hoa thường nên trả `false`. Bản mới
dùng `enum WalletStatus` nên không thể gán giá trị ngoài `ACTIVE` / `LOCKED`.

</details>

2. Rút `-100.00` từ ví bị chặn ở đâu, và vì sao không cần viết riêng một `if` trong `Wallet`?

<details><summary>Đáp án</summary>

Bị chặn ở constructor của `Money` (dòng đầu `withdrawMoney` tạo `new Money(amount)`). Luật
"tiền không âm" đã nằm trong Value Object nên không phải lặp lại.

</details>

3. Hai đối tượng `Wallet` được coi là bằng nhau khi nào? Hai `Money` thì sao?

<details><summary>Đáp án</summary>

`Wallet` bằng nhau khi cùng `WalletId` (Entity — so danh tính). `Money` bằng nhau khi cùng giá
trị số tiền (Value Object — so giá trị).

</details>

4. Interface `WalletRepository` nằm ở tầng nào, và ai implements nó?

<details><summary>Đáp án</summary>

Nằm ở `application/port/out/` (tầng trong sở hữu). `WalletRepositoryAdapter` ở
`adapter/out/persistence/` implements nó — phụ thuộc hướng từ ngoài vào trong.

</details>
