# BÀI TẬP: TÁI CẤU TRÚC VÍ ĐIỆN TỬ TỪ ANEMIC SANG RICH MODEL

Chuyển `WalletEntity` — một túi dữ liệu với mọi thuộc tính `public` — thành
**Rich Domain Aggregate Root** tự bảo vệ luật bất biến của chính nó, đặt trong
khung **DDD meets Clean Architecture**.

Chạy được chỉ với **JDK 21**, không cần Maven, không có thư viện ngoài.

| Lệnh | Tác dụng |
|---|---|
| `.\run.ps1 demo` | Chạy 5 kịch bản cho thấy invariant hoạt động (mặc định) |
| `.\run.ps1 test` | Chạy 30 bài kiểm thử (`SelfCheck` + `ArchitectureFitness`) |
| `.\run.ps1 build` / `clean` | Chỉ biên dịch / dọn thư mục build |

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
        if ("LOCKED".equals(wallet.status)) throw new Exception("Ví điện tử hiện đang bị khóa!");
        // LỖI CHÍ MẠNG: thiếu kiểm tra số dư
        wallet.balance = wallet.balance.subtract(amount);
    }
}
```

`WalletEntity` không biết gì về chính nó — mọi luật nằm bên ngoài ở `WalletService`.
Hậu quả không phải lý thuyết:

1. **Luật "không được rút quá số dư" bị quên, và không có gì nhắc.** Ví tụt xuống
   âm mà chương trình vẫn chạy bình thường.
2. **Dù có sửa `WalletService` cho đúng thì vẫn chưa an toàn.** Bất kỳ đoạn code
   nào khác trong hệ thống vẫn viết được `wallet.balance = ...` để đi vòng qua
   luật đó. Sửa một chỗ không làm hệ thống an toàn.
3. `status` là `String` với ghi chú `// "ACTIVE", "LOCKED"`. Ghi chú không phải
   ràng buộc: gán `"Locked"` thì `"LOCKED".equals(status)` trả `false` — **khóa
   mất tác dụng mà không báo lỗi**.

Đó chính là **anemic domain model**: dữ liệu một nơi, hành vi một nơi khác.

---

## 2. Đối chiếu với yêu cầu đề bài

| Yêu cầu | Cách giải | File |
|---|---|---|
| **a.** Aggregate Root, `balance`/`status` về `private`, xoá mọi setter công khai | `Wallet` chỉ đổi trạng thái qua method nghiệp vụ; không còn một setter nào | [`domain/Wallet.java`](src/main/java/com/example/wallet/domain/Wallet.java) |
| **b.** Phương thức giàu hành vi `withdrawMoney(BigDecimal)` và `lockWallet()` | Đúng chữ ký đề bài, đặt tên theo nghiệp vụ chứ không theo kỹ thuật | [`domain/Wallet.java`](src/main/java/com/example/wallet/domain/Wallet.java) |
| **c.** Invariant + `DomainException` tự định nghĩa | Đúng 2 luật đề bài liệt kê: ví khóa không rút được, không rút quá số dư | [`domain/DomainException.java`](src/main/java/com/example/wallet/domain/DomainException.java) |

### Yêu cầu a — đóng gói

```java
public class Wallet {
    private final WalletId id;
    private Money balance;        // private
    private WalletStatus status;  // private
    // KHÔNG có setBalance, KHÔNG có setStatus
}
```

Được canh bằng test dùng **reflection** chứ không đọc bằng mắt — một setter được
thêm lại sau này sẽ làm build đỏ ngay:

```
[OK]   Wallet khong co setter cong khai
[OK]   truong 'balance' la private
[OK]   truong 'status' la private
```

### Yêu cầu b — phương thức giàu hành vi

So sánh `wallet.setBalance(x)` với `wallet.withdrawMoney(x)`: tên thứ hai nói rõ
điều gì đang xảy ra, **và vì thế mới có chỗ để gắn luật vào**. Một setter thì
không có chỗ nào hợp lý để đặt câu hỏi "có được phép không".

### Yêu cầu c — invariant

```java
public void withdrawMoney(BigDecimal amount) {
    Money requested = new Money(amount);                  // chặn null / số âm
    if (status.isLocked())     throw new DomainException("Vi dien tu hien dang bi khoa...");
    if (!balance.isAtLeast(requested))
        throw new DomainException("So du khong du: can ... nhung chi con ...");
    this.balance = balance.minus(requested);
}
```

**Đúng 2 luật đề bài liệt kê, không thêm luật nào.** Việc chặn số **âm** không
phải luật thêm vào: nếu bỏ, `subtract(-100)` sẽ **cộng** tiền vào ví — đúng loại
bug mà bài này đang đi sửa. Ràng buộc đó nằm sẵn trong Value Object `Money`.

Vì luật nằm *trong* aggregate, không còn đường nào lách được. Dù có bao nhiêu use
case mới viết sau này, ví cũng không thể âm.

---

## 3. Bằng chứng: chạy cùng một kịch bản qua bản cũ và bản mới

Mã nguồn cũ được giữ nguyên văn trong [`src/test/java/.../legacy/`](src/test/java/com/example/wallet/legacy/LegacyWalletCode.java)
— để ở thư mục test chứ không phải `src/main/java`, vì nó là tang vật đối chiếu,
không phải một phần của hệ thống.

Test `luatBatBienChanDuocLoiCuaBanCu()` rút **5000** từ ví có **1000**:

```
[OK]   ban cu cho so du tut xuong am        <- legacy.balance == -4000.00, không lỗi
[OK]   ban moi chan dung kich ban do        <- DomainException
[OK]   ban moi giu so du nguyen ven         <- balance vẫn 1000.00
```

Dòng đầu là một test **khẳng định bản cũ có bug** — nếu ai đó "sửa" mã legacy thì
test này đỏ, nhắc rằng tang vật đã bị động vào.

---

## 4. DDD meets Clean Architecture

```
   ┌──────────────────────────────────────────────────────────────┐
   │ Vòng 3 — ADAPTER            InMemoryWalletRepository         │
   │   ┌──────────────────────────────────────────────────────┐   │
   │   │ Vòng 2 — APPLICATION                                 │   │
   │   │   port/in   WithdrawMoneyUseCase, LockWalletUseCase   │  │
   │   │   usecase   WithdrawMoneyService, LockWalletService   │  │
   │   │   port/out  WalletRepository ◄── adapter hiện thực    │  │
   │   │   ┌──────────────────────────────────────────────┐   │   │
   │   │   │ Vòng 1 — DOMAIN                              │   │   │
   │   │   │   Wallet (Aggregate Root)                    │   │   │
   │   │   │   Money, WalletId (Value Object)             │   │   │
   │   │   │   WalletStatus, DomainException              │   │   │
   │   │   └──────────────────────────────────────────────┘   │   │
   │   └──────────────────────────────────────────────────────┘   │
   └──────────────────────────────────────────────────────────────┘

        BOOTSTRAP — Main.java chỉ LẮP RÁP, không chứa nghiệp vụ.

    THE DEPENDENCY RULE: vòng trong không biết gì về vòng ngoài.
```

Bài này **không có vòng `infrastructure` riêng** vì không dùng framework nào —
thêm một package rỗng chỉ để cho đủ bốn vòng là hình thức.

**Kiểm chứng bằng compiler**, mạnh hơn mọi lời cam kết trong tài liệu:

| Compile riêng | Số file | Kết quả |
|---|---|---|
| `domain/` một mình | 5 | `javac` exit 0 |
| `domain/` + `application/` | 14 | `javac` exit 0 |

Vòng trong dịch được mà không cần một dòng nào của vòng ngoài.

### Hai khái niệm DDD được dùng ở đây

**Aggregate Root** — `Wallet` là cửa duy nhất vào cụm dữ liệu của nó. Không ai
chạm được `balance` từ bên ngoài, nên aggregate luôn ở trạng thái hợp lệ.

**Value Object** — `Money` và `WalletId`. Chỉ bọc những thứ **mang theo luật**:
`Money` giữ luật "tiền không được âm" ở đúng một chỗ. `balance` vì thế không thể
là một `BigDecimal` âm ngay từ kiểu dữ liệu.

> `withdrawMoney` vẫn nhận `BigDecimal` đúng như đề bài quy định — việc bọc thành
> `Money` là chuyện riêng bên trong, tầng ngoài không phải biết Value Object của
> domain mới gọi được.

### Use case sau khi nghiệp vụ dọn về aggregate

```java
// CŨ — service vừa điều phối vừa phân xử nghiệp vụ
if ("LOCKED".equals(wallet.status)) throw new Exception("...");
wallet.balance = wallet.balance.subtract(amount);

// MỚI — service chỉ điều phối
wallet.withdrawMoney(command.amount());
```

`WithdrawMoneyService` chỉ còn làm ba việc không mang tính nghiệp vụ: **tìm** ví,
**bảo** ví tự rút tiền, **lưu** lại. Không còn lấy một câu `if` nào về nghiệp vụ
— đó chính là dấu hiệu của một rich domain model đúng nghĩa.

### Hai loại lỗi được tách bạch

| Tình huống | Ngoại lệ | Vì sao |
|---|---|---|
| Ví bị khóa, số dư không đủ, số tiền ≤ 0 | `DomainException` | Luật nghiệp vụ bị vi phạm |
| Không tìm thấy ví | `WalletNotFoundException` | Không luật nào bị vi phạm — đầu vào trỏ tới thứ không tồn tại |

Gộp hai loại lại thì tầng ngoài mất khả năng phân biệt "dữ liệu sai" (thường là
404) với "thao tác bị nghiệp vụ từ chối" (thường là 409 / 422).

`DomainException` là **unchecked**, khác bản cũ dùng `throws Exception`: checked
exception buộc mọi hàm gọi phải `throws` theo, kéo chuỗi ngoại lệ lan từ domain
ra tận controller — đúng thứ Clean Architecture muốn chặn.

---

## 5. Fitness function canh giữ kiến trúc

Luật viết trong tài liệu thì không ai bắt buộc phải đọc. 5 bài test trong
[`ArchitectureFitness`](src/test/java/com/example/wallet/ArchitectureFitness.java)
đọc thẳng mã nguồn và **fail build** nếu có file vượt ranh giới:

```
[OK]   tang 'domain' khong phu thuoc ...application, ...adapter, ...bootstrap
[OK]   tang 'application' khong phu thuoc ...adapter, ...bootstrap
[OK]   tang 'domain' khong dinh cong nghe ha tang (spring, jakarta, javax, sql, net, io)
[OK]   tang 'application' khong dinh cong nghe ha tang
[OK]   tang 'adapter' khong phu thuoc ...bootstrap
```

---

## 6. Cấu trúc thư mục

```
ass2/
├── pom.xml
├── run.ps1
└── src/
    ├── main/java/com/example/wallet/
    │   ├── domain/                       ← VÒNG 1 (Entities)
    │   │   ├── Wallet.java                  Aggregate Root — yêu cầu a, b, c
    │   │   ├── Money.java                   Value Object — luật "tiền không âm"
    │   │   ├── WalletId.java                Value Object định danh
    │   │   ├── WalletStatus.java            enum thay cho String
    │   │   └── DomainException.java         yêu cầu c
    │   ├── application/                  ← VÒNG 2 (Use Cases)
    │   │   ├── port/in/                     WithdrawMoneyUseCase, LockWalletUseCase,
    │   │   │                                WithdrawMoneyCommand, WalletSnapshot
    │   │   ├── port/out/WalletRepository.java
    │   │   └── usecase/                     WithdrawMoneyService, LockWalletService,
    │   │                                    WalletNotFoundException, WalletSnapshots
    │   ├── adapter/out/persistence/      ← VÒNG 3 (Interface Adapters)
    │   │   └── InMemoryWalletRepository.java
    │   └── bootstrap/Main.java           ← COMPOSITION ROOT (chỉ lắp ráp)
    └── test/java/com/example/wallet/
        ├── SelfCheck.java                   25 test nghiệp vụ
        ├── ArchitectureFitness.java         5 fitness function
        └── legacy/LegacyWalletCode.java     mã cũ, chỉ để đối chiếu
```

---

## 7. Kết quả chạy `.\run.ps1 demo`

```
=== VI DIEN TU: ANEMIC -> RICH DOMAIN MODEL ===

Vi 625cbc5f-8f3c-46a8-8e02-13a50cbf8a21 mo voi so du 1000.00

  Rut 300.00                   OK      so du = 700.00, trang thai = ACTIVE
  Rut 5000.00 (qua so du)      TU CHOI So du khong du: can 5000.00 nhung chi con 700.00
  Rut -100.00 (so am)          TU CHOI So tien khong duoc am: -100.00
  Khoa vi                      OK      so du = 700.00, trang thai = LOCKED
  Rut 100.00 sau khi khoa      TU CHOI Vi dien tu hien dang bi khoa, khong the rut tien
```

Cả ba lần bị từ chối đều do chính aggregate `Wallet` phân xử, không phải do use
case kiểm tra hộ.

---

## 8. Ranh giới phạm vi: cái gì có trong đề, cái gì không

Bài này bám **đúng** ba yêu cầu a / b / c, không tự thêm luật nghiệp vụ nào.

| Luật | Nguồn |
|---|---|
| Ví bị khóa thì không rút được | Đề bài, yêu cầu **c** |
| Không rút quá số dư hiện có | Đề bài, yêu cầu **c** |
| Không rút số tiền âm | **Không phải luật thêm** — bỏ đi thì `subtract(-100)` sẽ cộng tiền vào ví, tức là một bug mới. Ràng buộc nằm trong `Money`. |

**`lockWallet()` là idempotent** — khóa một ví đã khóa thì không có gì xảy ra và
cũng không báo lỗi. Đề bài không đặt ra luật nào cho trường hợp này, nên aggregate
không tự nghĩ thêm một luật. Test `khoa lai lan nua van LOCKED, khong nem loi`
khẳng định **không** có ngoại lệ nào bắn ra — nó canh sự *vắng mặt* của luật, chứ
không phải thêm một invariant mới.

**Aggregate khả biến (mutable)** — `withdrawMoney` đổi trạng thái tại chỗ và trả
về `void`, đúng chữ ký đề bài yêu cầu. Aggregate khả biến là cách làm kinh điển
của DDD; điểm mấu chốt không phải bất biến hay khả biến, mà là **mọi thay đổi đều
phải đi qua một method có kiểm tra luật**.

### Phần vượt ra ngoài mã domain — và lý do

Ba yêu cầu đề bài đều nằm ở tầng domain. Các tầng `application` / `adapter` /
`bootstrap` có mặt vì đây là bài **DDD meets Clean Architecture** — không có use
case và outbound port thì không có gì để minh họa Dependency Rule. Chúng cố ý mỏng:
`WithdrawMoneyService` chỉ tìm ví → bảo ví tự rút → lưu, không một câu `if` nghiệp vụ.
