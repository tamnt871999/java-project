# Day8_CAP-Theorem — Thiết kế distributed system & quyết định CAP

> **Loại bài:** B (thiết kế) + bonus code. Theo `CLAUDE.md`, tài liệu này là bài nộp chính;
> phần API bên dưới chỉ để minh hoạ quyết định thiết kế.

## 1. Đề bài (nguyên văn)

```
Task: Thiết kế một distributed system

Requirements:
  - Chọn DB (SQL hoặc NoSQL)
  - Quyết định CAP strategy
  - Justify choice
  - Cung cấp architecture diagram
  - Bonus: implement small API
```

## 2. Hệ thống được chọn để thiết kế

Đề chỉ nói "một distributed system" nên tôi tự chọn bài toán. Chọn phần **dễ sai nhất** của
một sàn thương mại điện tử:

> **Inventory Reservation Service** — dịch vụ giữ chỗ tồn kho. Khi khách bấm "Đặt hàng",
> hệ thống phải trừ tồn kho của SKU đó trước khi cho thanh toán.

Lý do chọn: đây là nghiệp vụ mà **CAP không còn là lý thuyết**. Một bất biến duy nhất —
*không bán nhiều hơn số hàng đang có* — buộc phải chọn dứt khoát giữa C và A, và chọn sai
thì hậu quả đo được bằng tiền.

## 3. Giả định (do người làm bài tự đặt ra, không có trong đề)

Đề thiết kế luôn thiếu thông tin. Không có các giả định này thì mọi lựa chọn bên dưới đều
vô căn cứ. **Tất cả đều là giả định, không phải số đo thật.**

| Giả định | Giá trị | Ảnh hưởng tới quyết định |
|---|---|---|
| Quy mô | ~2 triệu tài khoản, cao điểm flash sale ~5.000 req/s vào dịch vụ giữ chỗ | Một primary vẫn gánh được → chưa cần sharding |
| Tỉ lệ đọc/ghi | ~20 đọc : 1 ghi | Đọc nhiều nhưng ghi mới là chỗ tranh chấp |
| Hàng hoá | Vật lý, tồn kho **hữu hạn**, có SKU nóng chiếm phần lớn traffic | Có bất biến `available >= 0` không thể bỏ |
| Triển khai | 3 AZ trong **cùng một region** | Round-trip nội region đủ rẻ để chờ đồng bộ |
| Mất mát chấp nhận được | **0 đơn đã xác nhận** bị mất; nhưng chấp nhận **từ chối phục vụ vài chục giây** khi mất một AZ | Ưu tiên C hơn A |
| Chi phí của oversell | Phải huỷ đơn đã thu tiền, hoàn tiền, mất uy tín | Không hoà giải tự động được |

## 4. Các phương án đã cân nhắc

| | **PostgreSQL 16 + Patroni/etcd** | **MongoDB replica set** | **Cassandra (RF=3)** |
|---|---|---|---|
| Mô hình | Quan hệ, ACID | Document | Wide-column, không leader |
| CAP khi partition | **CP** | **CP** (mặc định) | **AP** |
| Núm vặn đổi CAP strategy | `synchronous_standby_names`, `synchronous_commit` | `writeConcern`, `readPreference` | `consistencyLevel` mỗi query |
| Chặn oversell ở tầng storage | `CHECK (available >= 0)` + `SELECT … FOR UPDATE` | Transaction + update có điều kiện; **không có** CHECK constraint tương đương | **Không có**; phải dùng LWT (Paxos) |
| Ghi khi mất 1 node | Còn ghi được (quorum 2/3) | Còn ghi được | Còn ghi được |
| Điểm yếu chính | Mọi ghi qua 1 primary | Cũng 1 primary, thêm một hệ vận hành mới | Không giữ nổi bất biến hữu hạn |

## 5. Quyết định

> **Chọn PostgreSQL 16, cụm 3 node quản lý bởi Patroni + etcd — CAP strategy: CP.**

### 5.1. Vì sao CP, gắn với nghiệp vụ

Giữ chỗ tồn kho là một phép **đọc — so sánh — ghi** trên đúng một dòng. Nếu hai phía của một
partition cùng cho phép thao tác này, cả hai đều tin là còn hàng, và kết quả là **bán cái áo
cuối cùng cho hai người**.

Điểm mấu chốt: **oversell không có cách hoà giải tự động.** Với giỏ hàng hay lượt xem, khi
mạng nối lại ta có thể hợp nhất hai phiên bản và không ai thiệt. Với tồn kho thì không —
máy không thể "bán ngược lại" một cái áo không tồn tại. Cách duy nhất là **huỷ một đơn đã
thu tiền**, tức là đẩy chi phí của lỗi kỹ thuật sang cho khách hàng.

Ngược lại, cái giá của C rất rẻ trong bài toán này: khách gặp lỗi *"hệ thống bận, thử lại"*
trong vài chục giây. Khó chịu, nhưng phục hồi được và không mất tiền.

> Đây cũng là lý do dịch vụ **giữ chỗ tồn kho** khác với **trang chi tiết sản phẩm** hay
> **lượt xem**. Những thứ đó nên để AP. Bài này chỉ thiết kế cho dịch vụ giữ chỗ.

### 5.2. Vì sao loại Cassandra

Cassandra là lựa chọn AP tự nhiên và sẽ thắng về throughput. Bị loại vì:

- Ghi mặc định hoà giải theo **last-write-wins** ở mức cột. Hai phía cùng trừ tồn kho thì
  một trong hai lần trừ **biến mất** — không phải chậm, mà là mất hẳn.
- Có thể dùng **LWT (lightweight transaction, Paxos)** để có compare-and-set. Nhưng LWT cần
  đồng thuận cho từng thao tác — tức là **tự tay bỏ đi đúng cái ưu thế AP** mà ta chọn
  Cassandra để có. Chọn một công cụ rồi tắt tính chất của nó là chọn sai công cụ.
- Thêm nữa, SKU nóng trong flash sale tạo **hot partition** — đúng kiểu tải mà mô hình
  partition key của Cassandra ghét nhất.

### 5.3. Vì sao loại MongoDB

MongoDB replica set **hoàn toàn đạt CP** được: write concern mặc định là `w: "majority"`
(có ngoại lệ với cụm có arbiter — xem mục Nguồn), cộng transaction là đủ cho bài này. Bị loại
vì một lý do cụ thể chứ không phải "PostgreSQL tốt hơn":

**PostgreSQL cho phép đặt `CHECK (available >= 0)` ngay trong bảng.** Đó là hàng rào cuối
cùng nằm *trong* storage engine: kể cả khi tầng application có bug, khi ai đó chạy nhầm một
câu UPDATE tay, hay khi một service mới quên mất luật nghiệp vụ — database vẫn từ chối.

Với bất biến mà vi phạm nó đồng nghĩa mất tiền, một hàng rào không thể bị code đi vòng qua
đáng giá hơn mọi lợi thế khác. MongoDB không có ràng buộc tương đương ở mức tương tự.

### 5.4. Cấu hình cụ thể tạo ra tính chất CP

| Mục | Cấu hình | Tác dụng |
|---|---|---|
| Đồng bộ replica | `synchronous_standby_names = 'ANY 1 (az_b, az_c)'` | Commit phải được ít nhất 1 standby flush |
| Độ bền commit | `synchronous_commit = on` | Chờ standby ghi xuống đĩa bền |
| Bầu leader | Patroni + etcd 3 node (mỗi AZ 1) | Phía mất quorum etcd tự **demote**, không có split-brain |
| Định tuyến | HAProxy/PgBouncer luôn trỏ tới leader hiện tại | Client không tự ý ghi vào standby |
| Chặn oversell | `CHECK (available >= 0)`, cập nhật bằng `SELECT … FOR UPDATE` | Hàng rào trong storage engine |

> **Bẫy quan trọng, đã kiểm chứng trong tài liệu chính thức:** `synchronous_commit = on` là
> **giá trị mặc định** của PostgreSQL, nhưng **một mình nó KHÔNG tạo ra synchronous
> replication**. Khi `synchronous_standby_names` rỗng, mọi mức khác `off` đều chỉ có nghĩa là
> *flush WAL xuống đĩa cục bộ*. Nói "chúng tôi dùng mặc định nên đã có replication đồng bộ"
> là sai. Phải đặt `synchronous_standby_names` thì `synchronous_commit` mới điều khiển
> hành vi chờ standby.

## 6. Architecture diagram

```mermaid
graph TB
    C["Client"] --> LB["Load balancer"]
    LB --> APP["Inventory API<br/>(app node o ca 3 AZ)"]
    APP --> PGB["HAProxy / PgBouncer<br/>luon tro toi leader hien tai"]

    subgraph AZA["AZ-A - phia THIEU SO sau khi dut mang (1/3 vote)"]
        PG1[("PostgreSQL<br/>primary cu, bi demote")]
        ET1["etcd-1"]
    end

    subgraph AZBC["AZ-B + AZ-C - phia DA SO (2/3 vote, giu quorum)"]
        PG2[("PostgreSQL<br/>sync standby, thanh primary moi")]
        ET2["etcd-2"]
        PG3[("PostgreSQL<br/>async standby")]
        ET3["etcd-3"]
    end

    PGB --> PG1
    PG1 -->|"WAL DONG BO - cho standby flush"| PG2
    PG1 -.->|"WAL bat dong bo"| PG3
    ET1 ---|"raft"| ET2
    ET2 ---|"raft"| ET3

    CUT{{"DUT MANG o day"}}
    PG1 -.- CUT
    CUT -.- PG2
```

## 7. Kịch bản partition cụ thể

Mạng giữa **AZ-A** và **{AZ-B, AZ-C}** đứt. Cả hai phía đều còn sống, chỉ không thấy nhau.

| Bước | AZ-A (1/3 node — thiểu số) | AZ-B + AZ-C (2/3 node — đa số) |
|---|---|---|
| 1. Phát hiện | `etcd-1` mất quorum | `etcd-2` + `etcd-3` vẫn đủ quorum (2/3) |
| 2. Vai trò | Patroni **tự demote** primary cũ xuống read-only | Patroni promote sync standby ở AZ-B thành **primary mới** |
| 3. Ghi | **Từ chối** → HTTP `503` | Chấp nhận bình thường |
| 4. Đọc | **Từ chối** — nếu cho đọc thì trả về dữ liệu cũ, phá vỡ linearizability | Trả dữ liệu mới nhất |
| 5. Khách hàng thấy gì | *"Hệ thống đang bận, vui lòng thử lại"* | Không thấy gì bất thường |

**Khi mạng nối lại — và đây là phần thưởng thật sự của CP:**

Primary cũ ở AZ-A có thể đã ghi vài WAL record chưa kịp được thừa nhận. Nó **không được**
quay lại làm primary. Patroni chạy **`pg_rewind`** — công cụ chính thức của PostgreSQL để
đồng bộ một cluster đã phân kỳ timeline, cụ thể là để đưa một primary cũ trở lại sau failover
với vai trò standby đi theo primary mới. Các thay đổi chưa được thừa nhận bị **bỏ đi**.

**Không có gì để hoà giải.** Không có merge, không có conflict resolution, không có đơn hàng
mồ côi. Đó chính là cái ta mua được bằng việc hy sinh A.

## 8. Đánh đổi đã chấp nhận

| Mất gì | Mức độ | Vì sao chấp nhận |
|---|---|---|
| Phía thiểu số ngừng phục vụ **cả đọc lẫn ghi** | Toàn bộ thời gian partition | Đọc tồn kho cũ dẫn tới khách đặt hàng rồi bị huỷ — tệ hơn là báo lỗi ngay |
| Cửa sổ không ghi được lúc failover | Vài chục giây (phụ thuộc `ttl`/`loop_wait` của Patroni — **cần đo khi triển khai, không khẳng định con số**) | Hiếm, và phục hồi tự động |
| Latency ghi cao hơn | Thêm 1 round-trip nội region mỗi commit | Đổi lấy không mất đơn hàng |
| Ghi không scale ngang | Mọi ghi qua 1 primary | Với quy mô đã giả định thì chưa chạm trần |
| Vận hành phức tạp hơn | Phải chạy thêm etcd + Patroni | PostgreSQL thuần **không** có failover tự động; thiếu Patroni thì "CP" chỉ là trên giấy |

## 9. Khi nào quyết định này sai

Quyết định trên chỉ đúng trong khung giả định ở mục 3. Phải chọn lại nếu:

1. **Hàng hoá trở thành vô hạn** (ebook, khoá học, vé điện tử không giới hạn). Không còn bất
   biến hữu hạn → không còn lý do hy sinh A → chuyển sang AP.
2. **Nghiệp vụ chấp nhận oversell và bù bằng chính sách.** Nhiều sàn thật cho oversell khi
   tồn kho dư dả, rồi huỷ đơn kèm voucher. Nếu chi phí voucher rẻ hơn chi phí mất đơn lúc
   partition thì AP thắng về kinh tế. **Đây là quyết định kinh doanh, không phải kỹ thuật.**
3. **Mở rộng đa region xuyên lục địa.** Chờ quorum vòng quanh trái đất quá đắt. Lời giải
   không phải đổi sang AP, mà là **chia tồn kho theo region** — mỗi region sở hữu một phần
   tồn kho riêng và vẫn CP cục bộ trong region đó.
4. **Lưu lượng ghi vượt sức một primary.** Chuyển sang **sharding theo SKU**; mỗi shard vẫn
   là một cụm CP. Bất biến tồn kho nằm gọn trong một SKU nên nó shard được sạch sẽ.
5. **Xuất hiện yêu cầu đọc rất lớn và chịu được dữ liệu cũ** (ví dụ hiển thị "còn vài sản
   phẩm"). Tách riêng đường đọc đó sang replica bất đồng bộ hoặc cache — **đọc AP, ghi CP**.

---

# Bonus — small API

Một Spring Boot app **mô phỏng cụm 3 node**, để chứng minh mục 5 và 8 bằng thứ chạy được thay
vì khẳng định suông.

## Cấu trúc: Spring Boot thường

Theo `CLAUDE.md`, bonus của bài thiết kế **không mặc định Clean Architecture**. Đề này không
đòi tách tầng, nên code là Spring Boot ba tầng bình thường:

```
com.example.inventory/
    controller/   StockController, ClusterController, CapDemoController
    service/      StockService, ClusterService, CapDemoService
    cluster/      ClusterNodes + CapStrategy (CpStrategy / ApStrategy / CapStrategies)
    exception/    ApiExceptionHandler + 3 exception nghiep vu
```

Không port, không adapter, không fitness function. **16 file source, 3 file test.**

### Chỗ duy nhất đáng gọi là "thiết kế": Strategy pattern

Đề hỏi *"quyết định **CAP strategy**"* — và trong code nó đúng nghĩa là một **Strategy pattern**:

```java
public interface CapStrategy {
    int  read(String nodeId, String sku);
    void write(String nodeId, String sku, int available);
    int  mergeOnHeal(String sku, int initial);
}
```

| | `CpStrategy` | `ApStrategy` |
|---|---|---|
| `read` | Cần quorum; thiếu thì ném `ClusterUnavailableException` | Đọc bản sao tại chỗ, luôn thành công |
| `write` | Cần quorum; ghi tới mọi node liên lạc được | Ghi tới các node cùng phía, luôn thành công |
| `mergeOnHeal` | Lấy trạng thái phía đa số — không có gì để gộp | Cộng thao tác của **cả hai** phía → kết quả có thể âm |

`StockService` **không biết** mình đang chạy CP hay AP. Nó chỉ gọi `strategy.read(...)` rồi
`strategy.write(...)`. Đổi chiến lược không sửa một dòng nào trong service — đúng tinh thần
*"CAP strategy là một lựa chọn, không phải một kiến trúc"*.

Vì cả hai strategy đều là bean, **đổi được ngay giữa lúc chạy** bằng header `X-Strategy: cp|ap`,
không cần khởi động lại. Nhờ vậy mới đặt hai bên cạnh nhau mà so sánh được.

## Mô hình cụm

3 node: `node-1`, `node-2` (phía đa số) và `node-3` (phía thiểu số). **Quorum = 2.**
Client chọn node mình kết nối tới bằng header `X-Node` (mặc định `node-1`) — mô phỏng việc
client được định tuyến tới bản sao gần nhất.

## Chạy

```bash
.\mvnw test
.\mvnw spring-boot:run
```

### Xem CAP vận hành — một lệnh duy nhất

```bash
curl -X POST "http://localhost:8082/api/demo/cap?strategy=cp"
```

Endpoint này tự chạy trọn kịch bản partition rồi **tường thuật từng bước**: mỗi bước ghi lại
hành động, kết quả, **node-1 thấy gì**, **node-3 thấy gì**, và vì sao lại ra như vậy. Đổi
`strategy=ap` để chạy lại đúng kịch bản đó với lựa chọn ngược lại.

> Dùng `POST` chứ không phải `GET` vì endpoint này **thay đổi trạng thái cụm** (reset, cắt
> mạng, giữ chỗ). `GET` theo chuẩn HTTP không được có tác dụng phụ.

## CAP vận hành ra sao — kết quả thật đã chạy

Cùng một kịch bản, cùng tồn kho 5, chỉ khác lựa chọn khi mạng đứt:

| Bước | Hành động | **CP** | **AP** |
|---|---|---|---|
| 1 | Tồn kho = 5 | node-1: 5, node-3: 5 | node-1: 5, node-3: 5 |
| 2 | **Mạng đứt**, node-3 bị cô lập | — | — |
| 3 | Phía đa số giữ 3 | CHẤP NHẬN, còn 2 | CHẤP NHẬN, còn 2 |
| 4 | Phía thiểu số giữ 3 | **TỪ CHỐI (503)** | **CHẤP NHẬN, còn 2** |
| 5 | Đọc trên node-1 | trả về 2 | trả về 2 |
| 6 | Đọc trên node-3 | **TỪ CHỐI (503)** | **trả về 2** |
| 7 | Nối lại mạng, hoà giải | đã bán 3/5, **oversold = 0** | đã bán 6/5, **oversold = 1** |

Bốn điều đọc ra được từ bảng này:

**① Bước 2 — chữ P không phải lựa chọn.** Không ai bấm nút "chọn partition". Mạng tự đứt.
Thứ duy nhất được chọn là **phản ứng**: tiếp tục phục vụ (A) hay giữ nhất quán (C).

**② Bước 4 — đây là toàn bộ CAP, gói trong một dòng.** Cùng một request, cùng một node, cùng
một thời điểm. CP trả `503`, AP trả `200`. Không có phương án thứ ba.

**③ Bước 6 — CP hy sinh cả ĐỌC, không chỉ ghi.** Chỗ này hay bị bỏ sót. Nếu node-3 vẫn trả về
`5` thì khách sẽ thấy "còn 5 sản phẩm" trong khi thực tế còn 2 — dữ liệu cũ cũng là vi phạm
consistency. Nên CP phải chặn luôn cả đọc.

**④ Bước 7 — phép hoà giải AP không hề sai, nhưng kết quả vẫn sai.**

```
5 - 3 - 3 = -1
```

Nó cộng đúng mọi thao tác của cả hai phía, không đánh mất một đơn nào — đúng tinh thần CRDT.
Nhưng `-1` nghĩa là **đã bán 6 cái trong khi chỉ có 5**.

> Bài học chính của cả bài: **conflict resolution hoà giải được dữ liệu, nhưng không hoà giải
> được bất biến.** Không có thuật toán nào "bán ngược" được cái áo thứ sáu. Cách duy nhất còn
> lại là bù bằng nghiệp vụ — huỷ một đơn, xin lỗi, tặng voucher.

Và hệ quả thứ hai, nhìn thấy ở bước 3–4: **bất biến chỉ đúng trên bản sao đã kiểm tra nó.**
`StockService` kiểm tra `quantity <= available` hoàn toàn đúng trên từng node. Nhưng node-1
không biết node-3 cũng vừa kiểm tra đúng như vậy cho cùng một cái áo. Viết service chặt chẽ là
điều kiện **cần**, không phải điều kiện **đủ**, trong hệ phân tán.

### Trích nguyên văn bước 4 và 7, chế độ AP

```
BUOC 4. Phia THIEU SO (node-3) giu 3 san pham
   ket qua      : CHAP NHAN, node nay con 2
   node-1 thay  : 2      node-3 thay : 2
   giai thich   : Node node-3 tu kiem tra ban sao CUA RIENG NO va thay du hang.
                  No khong biet phia ben kia vua lam gi.

BUOC 7. Noi lai mang va hoa giai
   ket qua      : da ban 6/5, oversold = 1
   node-1 thay  : 0      node-3 thay : 0
   giai thich   : Gop hai phia: 5 - 3 - 3 = -1. Phep gop nay KHONG SAI - no cong du moi
                  thao tac, khong mat don nao. Nhung ket qua am nghia la da ban 6 cai
                  trong khi chi co 5.
```

Cùng bước 4 nhưng chế độ CP:

```
BUOC 4. Phia THIEU SO (node-3) giu 3 san pham
   ket qua      : TU CHOI - ClusterUnavailableException
   node-1 thay  : 2      node-3 thay : 5
   giai thich   : Node node-3 khong lien lac du quorum nen tu choi thay vi doan.
                  Khach nhan 503 va mat co hoi mua - do chinh la cai gia cua C.
```

## Endpoint

Mọi endpoint nhận hai header tuỳ chọn: **`X-Node`** (mặc định `node-1`) và **`X-Strategy`**
(`cp` / `ap`, mặc định lấy từ `app.cap.strategy`).

| Method | Path | Ý nghĩa |
|---|---|---|
| `POST` | `/api/demo/cap?strategy=cp` | **Chạy trọn kịch bản CAP, trả về tường thuật từng bước** |
| `POST` | `/api/cluster/reset` | Nạp lại tồn kho, nối lại mạng. Body: `{"TSHIRT-M":5}` |
| `POST` | `/api/cluster/partition` | Cắt mạng: `node-3` bị cô lập |
| `POST` | `/api/cluster/heal` | Nối lại mạng và hoà giải, trả về báo cáo |
| `GET` | `/api/cluster` | Trạng thái từng node |
| `POST` | `/api/stock/{sku}/reserve` | Giữ chỗ. Body: `{"quantity":3}` |
| `GET` | `/api/stock/{sku}` | Đọc tồn kho trên node đang kết nối |

Ánh xạ lỗi (mở rộng bảng chuẩn trong `CLAUDE.md` bằng một dòng `503`):

| Tình huống | HTTP | `code` |
|---|---|---|
| Thiếu `quantity`; `X-Node` hoặc `X-Strategy` không tồn tại | `400` | `BAD_REQUEST` |
| SKU không có | `404` | `SKU_NOT_FOUND` |
| Hết hàng, hoặc số lượng ≤ 0 | `422` | `BUSINESS_RULE_VIOLATED` |
| **Không đủ quorum — cái giá của C** | `503` | `CLUSTER_UNAVAILABLE` |

> `422` và `503` khác nhau về bản chất: `422` = *"tôi biết chắc là không còn hàng"*.
> `503` = *"tôi không biết còn hàng hay không, và tôi từ chối đoán"*.

## Những chỗ bản mô phỏng khác với thực tế

Ghi ra để không nhầm bản demo với hệ thật:

1. **Không có PostgreSQL.** Cụm là `Map` trong bộ nhớ. Mục đích là minh hoạ hành vi CP/AP,
   không phải đo hiệu năng. Vì không có JPA nên cũng không có `@Transactional`.
2. **Không có bầu leader thật.** Phía đa số / thiểu số được cố định sẵn; Patroni thật bầu lại
   leader dựa trên lease trong etcd.
3. **`CpStrategy` từ chối cả đọc ở phía thiểu số.** Một replica PostgreSQL thật vẫn sẵn sàng
   trả **dữ liệu cũ** trừ khi ta chủ động chặn. Ở đây chọn mô hình CP chặt để cái giá của C
   hiện ra rõ ràng.
4. **Đọc rồi ghi không nguyên tử.** `StockService` đọc xong mới ghi. Thực tế phải là
   `SELECT … FOR UPDATE` trong một transaction, cộng `CHECK (available >= 0)`. Demo chạy tuần
   tự nên khoảng hở này chưa lộ ra.
5. **Tồn kho sau hoà giải AP bị chặn về 0** để cụm còn ở trạng thái biểu diễn được; số âm thật
   được giữ nguyên trong trường `mergedAvailable` của báo cáo.

## Test

`.\mvnw test` — **19 test, xanh toàn bộ**:

| File | Số test | Chứng minh điều gì |
|---|---|---|
| `StockServiceTest` | 8 | Luật nghiệp vụ, và CP/AP phản ứng khác nhau khi mạng đứt. Chạy bằng `new`, **không cần Spring context** |
| `StockApiTest` | 7 | Đủ nhánh `400` / `404` / `422` / `503`; AP sau heal ra `oversold = 1` |
| `CapDemoApiTest` | 4 | Kịch bản tường thuật: CP ra `oversold = 0`, AP ra `oversold = 1`, và không bước nào thiếu lời giải thích |

Không có fitness function — theo `CLAUDE.md`, bài Spring Boot thường không có ranh giới tầng
nào để canh.

## Nguồn đã kiểm chứng

| Khẳng định | Nguồn |
|---|---|
| `synchronous_commit` mặc định là `on`, và khi `synchronous_standby_names` rỗng thì mọi mức khác `off` chỉ đảm bảo flush WAL **cục bộ** | [PostgreSQL — Write Ahead Log configuration](https://www.postgresql.org/docs/current/runtime-config-wal.html) |
| `pg_rewind` dùng để đồng bộ một cluster đã phân kỳ timeline, đưa primary cũ trở lại làm standby của primary mới | [PostgreSQL — pg_rewind](https://www.postgresql.org/docs/current/app-pgrewind.html) |
| Write concern mặc định của MongoDB là `{ w: "majority" }`, trừ cụm có arbiter thoả công thức riêng thì là `{ w: 1 }` | [MongoDB — Write Concern](https://www.mongodb.com/docs/manual/reference/write-concern/) |
| Các consistency level của Cassandra: `ANY, ONE, TWO, THREE, QUORUM, ALL, LOCAL_QUORUM, LOCAL_ONE, SERIAL, LOCAL_SERIAL` | [Cassandra — cqlsh](https://cassandra.apache.org/doc/4.0/cassandra/tools/cqlsh.html) |

**Chưa kiểm chứng được — nên không khẳng định trong bài:**

- **Consistency level mặc định của `cqlsh`.** Trang `cqlsh` chính thức liệt kê các mức hợp lệ
  nhưng **không nói mức mặc định**. Vì vậy bài này chỉ nói CL của Cassandra là *tuỳ chỉnh theo
  từng query*, không nêu con số mặc định.
- **Phiên bản MongoDB bắt đầu đổi mặc định sang `w:"majority"`.** Trang Write Concern không
  ghi mốc phiên bản, nên bài không nêu.
- **Thời gian failover thực tế của Patroni.** Phụ thuộc `ttl`/`loop_wait`; phải đo khi triển
  khai, không lấy con số từ trí nhớ.
