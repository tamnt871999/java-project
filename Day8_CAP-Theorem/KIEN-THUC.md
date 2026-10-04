# Kiến thức cơ bản — Day8_CAP-Theorem

Bài này xoay quanh một câu hỏi: khi mạng giữa các máy chủ bị đứt, hệ thống giữ chỗ tồn kho nên **từ chối phục vụ** hay **phục vụ tiếp và chấp nhận dữ liệu có thể sai**? Đó chính là lựa chọn mà định lý CAP buộc ta phải đưa ra.

## Thứ tự nên học

1. **Distributed system** và **replica** — vì sao dữ liệu nằm trên nhiều máy.
2. **Network partition** — sự cố mạng mà hệ phân tán không tránh được.
3. **CAP theorem** — ba tính chất C, A, P và vì sao không có đủ cả ba.
4. **CP và AP**, cùng khái niệm **quorum** — hai cách phản ứng khi mạng đứt.
5. **Invariant** và **conflict resolution** — vì sao hoà giải dữ liệu không cứu được bất biến.
6. **Strategy pattern** — cách bài code biến "CAP strategy" thành thứ đổi được lúc chạy.

## 1. Distributed system và replica

**Là gì:** **Distributed system** (hệ phân tán: nhiều máy chạy cùng nhau như một hệ thống) lưu dữ liệu trên nhiều **node** (một máy / một tiến trình trong cụm). Mỗi node giữ một **replica** (bản sao dữ liệu).

**Vì sao cần:** Một máy duy nhất mà chết thì cả hệ thống chết. Có nhiều bản sao thì một node hỏng, các node khác vẫn còn dữ liệu.

**Trong bài này:** Cụm mô phỏng có 3 node `node-1`, `node-2`, `node-3`, mỗi node giữ một `Map` tồn kho riêng ([ClusterNodes.java](src/main/java/com/example/inventory/cluster/ClusterNodes.java)). Phần thiết kế thật dùng cụm PostgreSQL 3 node trải trên 3 **AZ** (availability zone: vùng hạ tầng tách biệt trong cùng một region) — xem [README.md](README.md), mục 5 và 6.

```java
private static final List<String> MAJORITY_SIDE = List.of("node-1", "node-2");
private static final List<String> MINORITY_SIDE = List.of("node-3");
private final Map<String, Map<String, Integer>> availableByNode = new LinkedHashMap<>();
```

## 2. Network partition

**Là gì:** **Network partition** (mạng bị chia cắt: các node vẫn sống nhưng không liên lạc được với nhau) chia cụm thành các nhóm tách rời.

**Vì sao cần:** Đây không phải thứ ta "chọn" mà là thứ sẽ xảy ra. Hệ phân tán phải định sẵn mình sẽ làm gì khi nó xảy ra.

**Trong bài này:** Endpoint `POST /api/cluster/partition` cắt mạng, để `node-3` một mình một phía. Khi đó `reachableFrom("node-3")` chỉ trả về chính nó, còn `node-1` thấy được `node-1` và `node-2`. Kịch bản đầy đủ ở [README.md](README.md), mục 7.

**Ví dụ đời thường:** Hai chi nhánh của một cửa hàng bị mất điện thoại liên lạc; cả hai vẫn mở cửa nhưng không biết bên kia vừa bán gì.

## 3. CAP theorem

**Là gì:** **CAP theorem** (định lý CAP) nói rằng khi có partition, hệ phân tán chỉ giữ được một trong hai: **Consistency** hoặc **Availability**.

- **C — Consistency** (nhất quán: mọi lần đọc đều thấy lần ghi mới nhất, như thể chỉ có một bản dữ liệu).
- **A — Availability** (sẵn sàng: mọi request tới node còn sống đều nhận được câu trả lời không lỗi).
- **P — Partition tolerance** (chịu được partition: hệ thống vẫn chạy khi mạng giữa các node bị đứt).

**Vì sao cần:** Nó cho ta một câu hỏi rõ ràng để thiết kế: khi mạng đứt, nghiệp vụ này sợ **sai dữ liệu** hơn hay sợ **ngừng phục vụ** hơn?

**Trong bài này:** Đề yêu cầu "Quyết định CAP strategy". Bài chọn **CP** cho dịch vụ giữ chỗ tồn kho vì bán quá số hàng (**oversell**: bán nhiều hơn số đang có) gây mất tiền, còn lỗi "hệ thống bận, thử lại" thì phục hồi được — xem [README.md](README.md), mục 5.1.

## 4. CP, AP và quorum

**Là gì:**

- **CP**: khi mạng đứt, phía không chắc chắn về dữ liệu sẽ **từ chối** (ở đây trả `503`).
- **AP**: khi mạng đứt, mọi node vẫn **phục vụ** bằng bản sao tại chỗ, hoà giải sau.
- **Quorum** (đa số tối thiểu: số node cần đồng ý để một thao tác được chấp nhận). Với 3 node, quorum = 2.

**Vì sao cần:** Quorum giúp CP biết phía nào "được quyền" phục vụ. Hai phía của một partition không thể cùng có đa số, nên không có chuyện cả hai cùng ghi.

**Trong bài này:** [CpStrategy.java](src/main/java/com/example/inventory/cluster/CpStrategy.java) kiểm tra quorum trước cả đọc lẫn ghi; [ApStrategy.java](src/main/java/com/example/inventory/cluster/ApStrategy.java) thì không kiểm tra gì, luôn phục vụ.

```java
private Set<String> requireQuorum(String nodeId) {
    Set<String> reachable = nodes.reachableFrom(nodeId);
    if (reachable.size() < nodes.quorum()) {
        throw new ClusterUnavailableException(...);
    }
    return reachable;
}
```

`quorum()` trong `ClusterNodes` tính bằng `size / 2 + 1`, nên `node-3` (chỉ thấy 1 node) bị từ chối, còn `node-1` (thấy 2 node) vẫn chạy. `ClusterUnavailableException` được [ApiExceptionHandler.java](src/main/java/com/example/inventory/exception/ApiExceptionHandler.java) dịch thành `503 CLUSTER_UNAVAILABLE`.

## 5. Invariant và conflict resolution

**Là gì:** **Invariant** (bất biến: điều luôn phải đúng, ở đây là `available >= 0` — không bán quá số hàng). **Conflict resolution** (hoà giải xung đột: gộp dữ liệu của các phía khi mạng nối lại).

**Vì sao cần:** Với AP, hai phía ghi độc lập trong lúc mạng đứt, nên khi nối lại phải gộp. Câu hỏi là gộp xong thì bất biến còn đúng không.

**Trong bài này:** Tồn kho 5; phía đa số giữ 3, phía thiểu số cũng giữ 3. Với AP, `mergeOnHeal` cộng đủ thao tác của cả hai phía:

```
5 - 3 - 3 = -1   ->  da ban 6/5, oversold = 1
```

Phép gộp không sai, nhưng kết quả vi phạm bất biến. Với CP, phía thiểu số đã bị từ chối nên `oversold = 0`. Xem bảng kết quả ở [README.md](README.md), phần "CAP vận hành ra sao — kết quả thật đã chạy", và code gộp ở [ClusterService.java](src/main/java/com/example/inventory/service/ClusterService.java).

## 6. Strategy pattern

**Là gì:** **Strategy pattern** (mẫu thiết kế đóng gói từng thuật toán vào một class riêng cùng chung một interface, để đổi qua lại mà không sửa code gọi nó).

**Vì sao cần:** CP và AP là hai cách xử lý cùng một việc (đọc, ghi, hoà giải). Tách chúng ra thì service nghiệp vụ không phải chứa `if (cp) ... else ...` ở khắp nơi.

**Trong bài này:** Interface [CapStrategy.java](src/main/java/com/example/inventory/cluster/CapStrategy.java) có hai cài đặt `CpStrategy` và `ApStrategy`. [StockService.java](src/main/java/com/example/inventory/service/StockService.java) chỉ gọi `strategy.read(...)` rồi `strategy.write(...)`, không biết mình đang chạy CP hay AP. [CapStrategies.java](src/main/java/com/example/inventory/cluster/CapStrategies.java) chọn strategy theo header `X-Strategy: cp|ap`, mặc định lấy từ `app.cap.strategy` (đang là `cp`).

```java
public interface CapStrategy {
    int  read(String nodeId, String sku);
    void write(String nodeId, String sku, int available);
    int  mergeOnHeal(String sku, int initial);
}
```

## Hay nhầm

- **"CAP là chọn 2 trong 3, có thể chọn CA."** → Partition không phải thứ được chọn; mạng tự đứt (bước 2 trong demo). Thứ duy nhất được chọn là phản ứng: C hay A.
- **"CP chỉ cần chặn ghi."** → Bài chặn cả đọc ở phía thiểu số, vì `node-3` trả về `5` trong khi thực tế còn `2` cũng là vi phạm consistency (bước 6).
- **"Hoà giải đúng thì dữ liệu đúng."** → Phép gộp AP cộng đủ mọi thao tác mà vẫn ra `-1`: hoà giải được dữ liệu, không hoà giải được bất biến.
- **"`503` và `422` đều là hết hàng."** → `422` là *biết chắc* không đủ hàng; `503` là *không biết* còn hay không nên từ chối đoán.

## Tự kiểm tra

1. Cụm có 3 node. Khi partition tách `node-3` ra một mình, phía nào còn đủ quorum?

<details><summary>Đáp án</summary>

Phía <code>node-1</code> + <code>node-2</code>, vì quorum = 3 / 2 + 1 = 2 và chỉ phía này có 2 node.

</details>

2. Ở chế độ CP, request giữ hàng gửi tới `node-3` lúc mạng đứt nhận được gì?

<details><summary>Đáp án</summary>

HTTP <code>503</code> với <code>code</code> là <code>CLUSTER_UNAVAILABLE</code>, do <code>CpStrategy</code> ném <code>ClusterUnavailableException</code> khi thiếu quorum.

</details>

3. Vì sao AP cho ra `oversold = 1` dù không mất đơn nào?

<details><summary>Đáp án</summary>

Mỗi phía tự kiểm tra bản sao của riêng mình và đều thấy đủ 5 cái, nên cả hai cùng bán 3. Gộp lại: 5 - 3 - 3 = -1, tức đã bán 6 trong khi chỉ có 5.

</details>

4. Muốn đổi từ CP sang AP, có phải sửa `StockService` không?

<details><summary>Đáp án</summary>

Không. <code>StockService</code> chỉ làm việc với interface <code>CapStrategy</code>; chỉ cần gửi header <code>X-Strategy: ap</code> (hoặc đổi <code>app.cap.strategy</code>).

</details>

5. Theo bài, khi nào nên chọn AP thay vì CP?

<details><summary>Đáp án</summary>

Khi không có bất biến hữu hạn cần giữ, ví dụ hàng hoá vô hạn như ebook, hoặc dữ liệu như lượt xem, trang chi tiết sản phẩm — những thứ gộp lại được mà không ai thiệt (README mục 5.1 và 9).

</details>
