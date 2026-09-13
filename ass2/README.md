# BÀI TẬP 2 — Thiết kế điểm mở rộng Ports & Adapters cho Logistics

Hệ thống tính phí giao hàng cho sàn TMĐT, tích hợp **GiaoHàngNhanh (GHN)** và
**GiaoHàngTiếtKiệm (GHTK)**, tự động chuyển đổi nhà vận chuyển theo tỉnh thành —
mà **không** có một dòng `if-else` chọn đối tác nào nằm trong Use Case.

Chạy được chỉ với **JDK 21**, không cần Maven, không có thư viện ngoài.

```bash
cd D:\Java\ass2
```

| Lệnh | Tác dụng |
|---|---|
| `.\run.ps1 demo` | Chạy bộ kịch bản mẫu (mặc định) |
| `.\run.ps1 quote "Ha Noi" 1200` | Báo giá một đơn (thành phố + số gram) |
| `.\run.ps1 test` | Chạy 26 bài kiểm thử `SelfCheck` |
| `.\run.ps1 build` / `clean` | Chỉ biên dịch / dọn thư mục build |

Kết quả hiện tại: **31/31 test pass**, trong đó có 3 fitness function canh giữ kiến trúc.

---

## 1. Đối chiếu với yêu cầu đề bài

| Yêu cầu | Thành phần | File |
|---|---|---|
| **a.** Outbound Port `ShippingCarrierPort` | Interface tầng Core, tính phí theo **trọng lượng + thành phố** | `core/port/out/ShippingCarrierPort.java` |
| **b.** Use Case `CalculateShippingFeeUseCase` nhận Port qua **Constructor Injection** | Điều phối luồng, độc lập công nghệ | `core/usecase/CalculateShippingFeeUseCase.java` |
| **c.** 2 Adapters: `GhnCarrierAdapter`, `GhtkCarrierAdapter` | Tầng Infrastructure, giả lập gọi API thật | `infrastructure/carrier/ghn/`, `infrastructure/carrier/ghtk/` |
| **d.** `ShippingCarrierFactory` ở tầng Core | Chọn **strategy** Adapter theo tên thành phố, không lộ chi tiết hạ tầng | `core/factory/ShippingCarrierFactory.java` |

---

## 2. Sơ đồ kiến trúc

```
  INFRASTRUCTURE                 ┌────────────────────────────────────────┐
  (driving side)                 │          APPLICATION CORE              │
                                 │                                        │
  ShippingFeeCliAdapter ────────►│  CalculateShippingFeePort (inbound)    │
      (in/cli)                   │             ▲                          │
                                 │             │ implements               │
                                 │  CalculateShippingFeeUseCase           │
                                 │             │ dùng                     │
                                 │  ShippingCarrierProvider   (interface) │
                                 │             ▲                          │
                                 │             │ implements               │
                                 │  ShippingCarrierFactory ──► CarrierRoutingPolicy
                                 │             │ trả về                   │
                                 │  ShippingCarrierPort      (outbound)   │
                                 │             ▲                          │
                                 └─────────────┼──────────────────────────┘
                                               │ implements
  INFRASTRUCTURE                 ┌─────────────┴──────────────────────────┐
  (driven side)                  │  GhnCarrierAdapter ──► GhnShippingSdk  │
                                 │  GhtkCarrierAdapter ─► GhtkRestClient  │
                                 └────────────────────────────────────────┘

         BOOTSTRAP — Main.java chỉ LẮP RÁP, không chứa nghiệp vụ lẫn trình bày.

       Mũi tên phụ thuộc LUÔN hướng từ ngoài vào trong (Dependency Inversion).
```

**Hai loại adapter, dễ lẫn nhau:**

| | Vai trò | Cắm vào | File |
|---|---|---|---|
| **Driving** (inbound) | người dùng gọi **vào** lõi | `CalculateShippingFeePort` | `infrastructure/cli/ShippingFeeCliAdapter` |
| **Driven** (outbound) | lõi gọi **ra** thế giới ngoài | `ShippingCarrierPort` | `infrastructure/carrier/ghn`, `.../ghtk` |

### Luật kiến trúc được THỰC THI, không chỉ được ghi chép

`.\run.ps1 test` chạy 3 **fitness function** (bài test canh giữ kiến trúc) đọc thẳng
mã nguồn trong `src/main/java` và fail build nếu có file vượt ranh giới:

```
[OK]   core khong phu thuoc infrastructure / bootstrap
[OK]   core/domain khong phu thuoc port / usecase / factory
[OK]   core khong dinh cong nghe ha tang (net / sql / io / spring / json)
```

Đã kiểm chứng là bẫy thật: cố tình thêm `import ...infrastructure.ghn.GhnCarrierAdapter`
vào Use Case → build fail, báo đúng `CalculateShippingFeeUseCase.java:12`.

Kiểm chứng thủ công tương đương:

```bash
grep -r "import com.example.logistics.infrastructure" src/main/java/com/example/logistics/core
```

Kết quả: rỗng. Mạnh hơn nữa — compile riêng 13 file của `core/` mà **không** kèm
một file infrastructure nào: `javac` exit code 0, lõi tự đứng được.

---

## 3. Điểm thiết kế đáng chú ý

### a. Port định nghĩa hợp đồng bằng ngôn ngữ của lõi

`ShippingCarrierPort` chỉ dùng `ShipmentRequest` / `ShippingQuote` — không có
`HttpClient`, không có JSON, không có token, không có kiểu dữ liệu nào của SDK
đối tác. Interface nằm ở **Core**, còn bản hiện thực nằm ở **Infrastructure**:
đây chính là **Dependency Inversion** (đảo ngược phụ thuộc).

### b. Use Case sạch khỏi if-else

Nhìn phần `import` của `CalculateShippingFeeUseCase`: không có `GhnCarrierAdapter`,
không có `GhtkCarrierAdapter`. Nếu viết trực tiếp:

```java
// VI PHẠM OCP — mỗi lần ký hợp đồng với đối tác mới lại phải mở Use Case ra sửa
if (city.equals("Ha Noi")) { new GhnSdk().fee(...); }
else                       { new GhtkClient().fee(...); }
```

thì mỗi lần thêm đối tác là một lần có nguy cơ làm hỏng luồng đặt hàng đang chạy
ổn định. Ở đây: việc **chọn** đối tác đẩy ra `ShippingCarrierFactory`, việc **gọi**
đối tác đẩy ra Adapter.

### c. Adapter là nơi duy nhất chứa phần "bẩn"

Hai đối tác có phong cách hoàn toàn khác nhau, và toàn bộ chênh lệch đó bị chặn
lại ở tầng Infrastructure:

| | GHN | GHTK |
|---|---|---|
| Giao tiếp | SDK Java (object) | HTTP REST (chuỗi JSON thô) |
| Trọng lượng | gram (`int`) | kilogram (`double`) |
| Tiền | VND (`int`) | **nghìn đồng** (`int`) |
| Thời gian | số ngày | số **giờ** |
| Báo lỗi | trường `code` trong body | ném exception |

Adapter dịch hết về `ShippingQuote`, nên Use Case chỉ phải hiểu **một** dạng kết
quả duy nhất. Ngoại lệ riêng của hạ tầng (`GhnApiException`, `GhtkApiException`)
cũng được dịch sang `CarrierUnavailableException` của Core — nếu không, chi tiết
công nghệ sẽ rò rỉ vào lõi qua đường `throws`.

### d. Factory ở Core mà vẫn không biết Infrastructure là ai

Đây là chỗ dễ làm sai nhất của đề bài. Nếu Factory tự `new GhnCarrierAdapter()`
thì package `core` buộc phải `import` package `infrastructure`, và toàn bộ kiến
trúc Hexagonal sụp đổ ngay tại dòng import đó.

Cách giải quyết ở đây:

1. Các Adapter được **tiêm vào** Factory qua constructor từ Composition Root (`Main`).
2. Factory chỉ giữ một bảng tra `CarrierCode → ShippingCarrierPort` — cả hai kiểu
   này đều thuộc về Core.
3. Luật "thành phố nào thì đối tác nào" tách hẳn sang `CarrierRoutingPolicy`,
   vì đó là **quyết định kinh doanh**, dễ thay đổi nhất.

Ranh giới cần phân biệt: `"GHN"` là tên **đối tác kinh doanh** — lõi được phép
biết mình đang làm việc với ai. Cái lõi **không** được biết là đối tác đó gọi
bằng SDK, REST hay SOAP.

Thêm một tầng nữa: Use Case **không** phụ thuộc thẳng class `ShippingCarrierFactory`
mà phụ thuộc interface `ShippingCarrierProvider`. Factory chỉ là **một** chiến lược
chọn hãng (tra bảng theo tỉnh/thành). Khi kinh doanh yêu cầu chiến lược khác, ta
viết implementation mới chứ không mở file đang chạy ổn định ra sửa.

Test `doiChienLuocChonHangMaKhongSuaUseCase()` chứng minh bằng tình huống thật:
GHTK từ chối kiện trên 20kg, nên đơn 25kg đi Sơn La báo giá thất bại. Thêm
`HeavyParcelCarrierProvider` (kiện nặng → đẩy sang GHN, còn lại ủy quyền cho
Factory) là xử lý được, **không sửa** Use Case, Factory, Port hay 2 Adapter.

### e. `CarrierCode` là value object, không phải `enum`

`enum` là danh sách đóng: thêm đối tác thứ ba buộc phải **sửa** file enum — đúng
vào chỗ mà OCP cấm sửa. Với value object, thêm ViettelPost = thêm **một** file
adapter mới + **một** dòng đăng ký ở `Main`, không động vào bất kỳ file cũ nào.

Bài test `themDoiTacMoiMaKhongSuaCodeCu()` trong `SelfCheck.java` chứng minh điều
này: định tuyến Huế sang VTP mà Factory, Use Case, Port đều giữ nguyên.

### f. Quy ước đặt tên accessor (đồng bộ với ass1)

| Loại | Kiểu | Accessor |
|---|---|---|
| Value object | `Money`, `Weight`, `City`, `CarrierCode` | ngắn — `amount()`, `grams()`, `key()`, `value()` |
| Model / DTO | `ShipmentRequest`, `ShippingQuote` | JavaBean — `getDestination()`, `getFee()` |
| Interface dịch vụ | `ShippingCarrierPort` | động từ / truy vấn — `carrier()`, `calculateFee()` |

Toàn bộ dùng class `private final` + validate trong constructor, **không** dùng
`record`, giống hệt `Money` / `OrderLine` / `PlaceOrderRequest` của ass1.

### g. City được chuẩn hóa trước khi định tuyến

`"TP. Hồ Chí Minh"`, `"ho chi minh"`, `"HCM"`, `"Sài Gòn"` đều ra cùng một khóa
`"ho chi minh"`. Nếu so sánh chuỗi thô bằng `equals()`, luật định tuyến sẽ sai
ngay ở ký tự hoa/thường đầu tiên.

---

## 4. Luật định tuyến mặc định

| Tỉnh / thành | Nhà vận chuyển | Lý do |
|---|---|---|
| Hà Nội, Hồ Chí Minh, Đà Nẵng, Hải Phòng, Cần Thơ | **GHN** | Có hub trung chuyển, giao trong ngày |
| Còn lại (fallback) | **GHTK** | Tối ưu chi phí cho tỉnh lẻ |

Bảng luật được **truyền vào** chứ không hard-code trong thân hàm, nên đổi chính
sách chỉ là sửa dữ liệu ở Composition Root (hoặc nạp từ file cấu hình), không
phải sửa logic:

```java
CarrierRoutingPolicy policy = CarrierRoutingPolicy.builder()
        .route("Ha Noi", CarrierCode.GHN)
        .route("Hue", CarrierCode.of("VTP", "ViettelPost"))
        .fallback(CarrierCode.GHTK)
        .build();
```

---

## 5. Cách thêm đối tác thứ ba (kiểm chứng OCP)

1. Viết `infrastructure/carrier/vtp/VtpCarrierAdapter implements ShippingCarrierPort`.
2. Thêm một dòng ở `Main`: `new VtpCarrierAdapter(...)` vào danh sách đăng ký.
3. Thêm một dòng `.route(...)` vào `CarrierRoutingPolicy` nếu cần luật riêng.

Không sửa `ShippingCarrierPort`, không sửa `CalculateShippingFeeUseCase`, không
sửa `ShippingCarrierFactory`, không sửa hai adapter cũ.

---

## 6. Cấu trúc thư mục

```
ass2/
├── pom.xml
├── run.ps1
└── src/
    ├── main/java/com/example/logistics/
    │   ├── core/                              ← TẦNG APPLICATION CORE
    │   │   ├── domain/                        (CarrierCode, City, Money, Weight,
    │   │   │                                   ShipmentRequest, ShippingQuote)
    │   │   ├── port/in/CalculateShippingFeePort.java
    │   │   ├── port/out/ShippingCarrierPort.java          ← yêu cầu a
    │   │   ├── port/out/CarrierUnavailableException.java
    │   │   ├── factory/ShippingCarrierProvider.java       (interface chiến lược)
    │   │   ├── factory/CarrierRoutingPolicy.java
    │   │   ├── factory/ShippingCarrierFactory.java        ← yêu cầu d
    │   │   └── usecase/CalculateShippingFeeUseCase.java   ← yêu cầu b
    │   ├── infrastructure/                    ← TẦNG INFRASTRUCTURE
    │   │   ├── carrier/ghn/   GhnShippingSdk, GhnCarrierAdapter   ← yêu cầu c
    │   │   ├── carrier/ghtk/  GhtkRestClient, GhtkCarrierAdapter  ← yêu cầu c
    │   │   └── cli/ShippingFeeCliAdapter.java  (driving adapter)
    │   └── bootstrap/Main.java                ← COMPOSITION ROOT (chỉ lắp ráp)
    └── test/java/com/example/logistics/
        ├── SelfCheck.java                     (28 test nghiệp vụ)
        └── ArchitectureFitness.java           (3 fitness function)
```

---

## 7. Kết quả chạy `.\run.ps1 demo`

```
DIEM DEN                K.LUONG   NHA VAN CHUYEN           CUOC PHI    NGAY   GHI CHU
-------------------------------------------------------------------------------------
Ha Noi                     800g   GiaoHangNhanh              27,000       1   GHN Standard
TP. Ho Chi Minh           1200g   GiaoHangNhanh              32,000       1   GHN Standard
hcm                       1200g   GiaoHangNhanh              32,000       1   GHN Standard
Đà Nẵng                   2500g   GiaoHangNhanh              42,000       1   GHN Standard
Nghe An                    800g   GiaoHangTietKiem           26,000       3   GHTK Tiet Kiem (72h)
Ca Mau                    3000g   GiaoHangTietKiem           42,000       3   GHTK Tiet Kiem (72h)
Binh Duong                5000g   GiaoHangTietKiem           48,000       2   GHTK Tiet Kiem (36h)
Son La                   25000g   GHTK                   TU CHOI - GHTK khong nhan kien hang tren 20kg
```

Cùng một đoạn code gọi Use Case cho mọi dòng ở trên — nhà vận chuyển tự đổi khi
điểm đến đổi.

> Công thức giá của GHN/GHTK trong bài là **giả lập** (deterministic, không gọi
> mạng) để kiểm thử chạy được offline; điểm cần đánh giá là ranh giới kiến trúc,
> không phải biểu giá thật của hai đối tác.
