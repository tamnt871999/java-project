# Day3_Domain-Driven-Design — Ví điện tử: anemic → rich domain model

Chuyển `WalletEntity` — một túi dữ liệu với mọi thuộc tính `public` — thành **Aggregate Root
tự bảo vệ invariant của chính nó**, đặt trong khung Clean Architecture và bọc bằng REST API.

**Stack:** Java 21 · Spring Boot 3.4 · Spring MVC · Spring Data JPA · H2

```bash
.\mvnw spring-boot:run      # http://localhost:8081
.\mvnw test                 # 20 test
```

---

## 1. Vấn đề của mã nguồn ban đầu

```java
public class WalletEntity {
    public UUID id;
    public BigDecimal balance;    // ai cũng sửa được
    public String status;         // ai cũng sửa được
}

public class WalletService {
    public void withdraw(WalletEntity wallet, BigDecimal amount) throws Exception {
        if ("LOCKED".equals(wallet.status)) throw new Exception("Vi dien tu hien dang bi khoa!");
        wallet.balance = wallet.balance.subtract(amount);   // LỖI: thiếu kiểm tra số dư
    }
}
```

`WalletEntity` không biết gì về chính nó — mọi luật nằm bên ngoài ở `WalletService`. Ba hậu
quả, và **cả ba đều được chứng minh bằng test chạy được** trong `WalletTest.DoiChieuBanCu`:

1. **Luật "không rút quá số dư" bị quên.** Rút 5000 từ ví 1000 → số dư còn `-4000.00`, chương
   trình vẫn chạy bình thường.
2. **Sửa `WalletService` cho đúng vẫn chưa an toàn.** Bất kỳ code nào khác vẫn viết được
   `wallet.balance = ...` để đi vòng qua luật. Sửa một chỗ không làm hệ thống an toàn.
3. **`status` là `String`.** Gán `"Locked"` thì `"LOCKED".equals(status)` trả `false` — khoá
   mất tác dụng mà không báo lỗi. Test chứng minh: ví `"Locked"` vẫn rút được tiền.

Đó là **anemic domain model**: dữ liệu một nơi, hành vi một nơi khác.

---

## 2. Đối chiếu với yêu cầu đề bài

| | Yêu cầu | Cách giải |
|---|---|---|
| **a** | `balance`/`status` về `private`, xoá mọi setter công khai | `Wallet` chỉ đổi trạng thái qua method nghiệp vụ. Có test dùng reflection canh không method nào bắt đầu bằng `set` |
| **b** | Method giàu hành vi `withdrawMoney(BigDecimal)`, `lockWallet()` | Đúng chữ ký đề bài, đặt tên theo nghiệp vụ chứ không theo kỹ thuật |
| **c** | Invariant + `DomainException` tự định nghĩa | Kiểm **ngay trong aggregate**, không phải ở service |

```java
public void withdrawMoney(BigDecimal amount) {
    Money requested = new Money(amount);           // Money chặn số âm
    if (status.isLocked()) {
        throw new DomainException("Vi dien tu hien dang bi khoa, khong the rut tien");
    }
    if (!balance.isAtLeast(requested)) {
        throw new DomainException("So du khong du: can " + requested + " nhung chi con " + balance);
    }
    this.balance = balance.minus(requested);
}
```

Vì luật nằm **trong** aggregate nên không còn đường nào lách được. Dù viết thêm bao nhiêu use
case sau này, ví cũng không thể âm.

### Ba thứ nhỏ nhưng đổi hẳn độ an toàn

| Trước | Sau | Được gì |
|---|---|---|
| `public BigDecimal balance` | `private Money balance` | Luật "tiền không âm" chỉ viết **một lần** trong `Money` |
| `public String status` | `private WalletStatus status` (enum) | Compiler canh tập giá trị hợp lệ, không phụ thuộc gõ đúng chuỗi |
| `public UUID id` | `WalletId` | Port ghi `findById(WalletId)` — đọc ra là biết đang tìm ví |

---

## 3. API

Cổng `8081` (để chạy song song với `Day1_Clean-Architect` ở `8080`).

| Method | Đường dẫn | Tác dụng |
|---|---|---|
| `POST` | `/api/wallets` | Mở ví mới — `{"initialBalance": 1000.00}` → `201` |
| `GET` | `/api/wallets/{id}` | Xem ví |
| `POST` | `/api/wallets/{id}/withdraw` | Rút tiền — `{"amount": 300.00}` |
| `POST` | `/api/wallets/{id}/lock` | Khoá ví |

Cả bốn đều trả về cùng một `WalletSnapshot`:

```json
{ "walletId": "d5afbb1f-c2ec-4496-a15e-b73a8f824691", "balance": 700.00, "status": "ACTIVE" }
```

> `POST /api/wallets` và `GET /api/wallets/{id}` **không có trong đề bài** — chúng được thêm
> để API dùng được (phải có cách tạo ví và đọc lại). Đề bài chỉ yêu cầu `withdrawMoney` và
> `lockWallet`.

### Lỗi

| HTTP | `code` | Khi nào |
|---|---|---|
| `400` | `BAD_REQUEST` | Thiếu trường, JSON hỏng, UUID sai định dạng |
| `422` | `BUSINESS_RULE_VIOLATED` | Ví bị khoá, số dư không đủ, số tiền âm |
| `404` | `WALLET_NOT_FOUND` | Không có ví với id này |

Cả ba tình huống `422` đều do **chính aggregate `Wallet` phán xử**, không phải do use case
kiểm tra hộ.

```bash
curl -X POST http://localhost:8081/api/wallets -H "Content-Type: application/json" -d "{\"initialBalance\":1000.00}"
```

---

## 4. Cấu trúc

```
src/main/java/com/example/wallet/            22 file
├── WalletApplication.java
│
├── domain/                                  Nghiệp vụ thuần — không phụ thuộc gì
│   ├── Wallet.java                            Aggregate Root — TRỌNG TÂM BÀI TẬP
│   ├── Money.java                             Value Object: tiền không âm, scale 2
│   ├── WalletId.java                          Value Object bọc UUID
│   ├── WalletStatus.java                      enum ACTIVE / LOCKED
│   └── DomainException.java
│
├── application/                             Điều phối
│   ├── port/in/   OpenWalletUseCase, WithdrawMoneyUseCase,
│   │              LockWalletUseCase, GetWalletUseCase,
│   │              WalletSnapshot, WalletNotFoundException
│   ├── port/out/  WalletRepository            Interface do tầng này sở hữu
│   └── usecase/   4 service tương ứng, mỗi cái mang @Transactional
│
└── adapter/                                 Nơi duy nhất biết Spring
    ├── in/web/    WalletController, ApiExceptionHandler
    └── out/persistence/  WalletJpaEntity, WalletJpaRepository, WalletRepositoryAdapter
```

**Dependency Rule:** `domain/` không import Spring, JPA hay Jackson. Có fitness function canh.

Điểm mấu chốt giống mọi bài Clean Architecture: `WalletRepository` là interface nằm ở
`application/port/out/` — tầng `application` **sở hữu** nó, còn `WalletRepositoryAdapter` ở
tầng ngoài **implements ngược vào trong**. Đổi H2 sang Postgres chỉ cần viết adapter khác.

### Vì sao use case có `@Transactional`

Rút tiền là **đọc → sửa → ghi**. Không có transaction thì hai request đồng thời cùng đọc số
dư 1000, cùng rút 600, và cả hai cùng thành công — ví âm mà không dòng code nào sai. Ranh
giới transaction thuộc về use case, không phải repository.

---

## 5. Test

```bash
.\mvnw test
```

| File | Số test | Kiểm gì |
|---|---|---|
| `WalletTest` | 10 | Invariant của aggregate + **đối chiếu với bản anemic cũ** |
| `WalletApiTest` | 6 | Gọi thật qua cả stack: mở ví, rút, khoá, `422`, `404`, `400` |
| `ArchitectureFitnessTest` | 4 | Fail build nếu có file vượt ranh giới tầng |

Mã nguồn cũ được giữ nguyên văn trong
[`src/test/java/.../legacy/LegacyWalletCode.java`](src/test/java/com/example/wallet/legacy/LegacyWalletCode.java)
— để ở thư mục test vì nó là **tang vật đối chiếu**, không phải mã đang chạy.

Hai bài test trong `DoiChieuBanCu` **khẳng định bản cũ có bug**. Nếu ai đó "sửa" mã legacy cho
đúng thì hai test này đỏ, nhắc rằng tang vật đã bị động vào.

`WalletTest` chạy hoàn toàn bằng `new`, không cần Spring — đó là phần thưởng của việc giữ
`domain/` sạch.

---

## 6. Những gì đã lược bỏ cho đơn giản

| Bỏ gì | Thật ra nên |
|---|---|
| Không có nghiệp vụ nạp tiền (`Money` chỉ có `minus`) | Thêm `plus()` khi có use case nạp tiền — không viết sẵn hàm chưa ai dùng |
| Không có lịch sử giao dịch | Aggregate riêng `Transaction`, hoặc domain event |
| Không có optimistic locking | `@Version` trên entity khi nhiều người cùng thao tác một ví |
| `ddl-auto: create-drop`, H2 in-memory | Database thật + Flyway quản lý schema |
| Khoá ví idempotent, không có `unlockWallet()` | Đề bài không yêu cầu — aggregate không tự nghĩ thêm luật |
