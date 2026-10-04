# Kiến thức cơ bản — Day9_PACELC-Theorem

Bài xoay quanh một câu hỏi: trong hệ thống Flash Sale, mỗi luồng (Listing stock, Add-to-cart, Checkout) nên ưu tiên dữ liệu **đúng** hay trả lời **nhanh / luôn trả lời được**, và làm sao để ba luồng có ba lựa chọn khác nhau trên **cùng một cụm PostgreSQL**.

## Thứ tự nên học

1. Network partition và định lý CAP: nền móng của chữ "PAC".
2. PACELC: thêm câu hỏi "lúc mạng bình thường thì chọn nhanh hay chọn đúng".
3. Replication đồng bộ và bất đồng bộ: công cụ để "vặn" PACELC trong bài.
4. Replication lag và read-your-writes: cái giá phải trả khi đọc từ replica.
5. Bất biến "không oversell": `UPDATE` có điều kiện và ràng buộc `CHECK`.
6. Thundering herd và retry storm: rủi ro khi 50k người dồn vào cùng lúc.
7. Idempotency: thử lại mà không trừ kho, không thu tiền hai lần.

## 1. Network partition và CAP

**Là gì:** **Network partition** (phân vùng mạng: các máy trong cụm vẫn chạy nhưng mất liên lạc với nhau). Định lý **CAP** nói: khi có partition, hệ phân tán chỉ giữ được một trong hai thứ là **Consistency** (C, tính nhất quán: ai đọc cũng thấy dữ liệu mới nhất) hoặc **Availability** (A, tính sẵn sàng: request nào cũng nhận được câu trả lời không lỗi).

**Vì sao cần:** partition chắc chắn sẽ có lúc xảy ra. Nếu không chọn trước, lúc mạng đứt hệ thống sẽ hành xử theo kiểu "may rủi".

**Trong bài này:** Checkout chọn **C**: phía bị cô lập từ chối ghi và trả `503`. Listing chọn **A**: replica vẫn trả con số cuối cùng nó có. Xem bảng ở mục 1 của [README.md](README.md).

**Ví dụ đời thường:** hai quầy bán vé mất liên lạc với nhau. Hoặc tạm ngừng bán (chọn C), hoặc cứ bán tiếp và chấp nhận có thể bán trùng ghế (chọn A).

## 2. PACELC

**Là gì:** PACELC mở rộng CAP thành hai câu hỏi. **If P** (khi có partition): chọn **A** hay **C**? **Else** (lúc bình thường): chọn **L** — **latency** (độ trễ: thời gian chờ phản hồi) — hay **C**? Kết quả viết gọn thành một "quadrant" (ô phân loại) như `PA/EL` hay `PC/EC`.

**Vì sao cần:** CAP chỉ nói về lúc mạng đứt, mà lúc đó hiếm. Phần lớn thời gian mạng bình thường nhưng vẫn phải chọn: chờ các bản sao xác nhận (đúng hơn, chậm hơn) hay trả lời ngay (nhanh hơn, có thể là dữ liệu cũ).

**Trong bài này:** phân loại **theo từng luồng**, không phải theo cả hệ thống ([README.md](README.md), mục 1):

- Listing stock: `PA/EL`, vì đề cho phép tồn kho hiển thị trễ tối đa 2s.
- Add-to-cart: `PA/EL`, vì giỏ hàng là dữ liệu riêng từng người, không trừ kho.
- Checkout: `PC/EC`, vì đề đòi "không oversell" và "thanh toán phải chính xác".

## 3. Replication đồng bộ và bất đồng bộ

**Là gì:** **Replication** (nhân bản: sao chép dữ liệu sang nhiều máy). Máy **primary** (máy chính, nhận mọi lệnh ghi) ghi thay đổi vào **WAL** (write-ahead log: nhật ký ghi trước, mọi thay đổi được ghi vào đây trước khi coi là xong) rồi gửi WAL cho **standby** / **replica** (máy bản sao). **Synchronous** (đồng bộ): commit chờ bản sao xác nhận. **Asynchronous** (bất đồng bộ): commit xong ngay, bản sao nhận sau.

**Vì sao cần:** primary chết thì còn bản sao để thay thế, và việc đọc có thể chia ra nhiều máy. Chọn đồng bộ hay bất đồng bộ chính là chọn **C** hay **L** ở vế "Else" của PACELC.

**Trong bài này** ([README.md](README.md), mục 2 và 3):

- Topology (cách bố trí các máy): 1 primary + 1 sync standby + 2 async replica, ba AZ trong một region.
- Knob (núm vặn cấu hình) `synchronous_commit`: checkout dùng `SET LOCAL synchronous_commit = on` (chờ standby flush → **EC**); giỏ hàng dùng `SET LOCAL synchronous_commit = local` (không chờ standby → **EL**).
- Knob đó chỉ có tác dụng với standby khi đã đặt `synchronous_standby_names = 'ANY 1 (sb_sync)'`.
- **Failover** (chuyển sang máy dự phòng khi primary hỏng) do **Patroni + etcd** đảm nhiệm. PostgreSQL thuần không tự failover, nên thiếu Patroni thì "PC" chỉ là trên giấy.

## 4. Replication lag và read-your-writes

**Là gì:** **Replication lag** (độ trễ sao chép: replica tụt lại sau primary bao lâu). **Read-your-writes** (đọc thấy chính cái mình vừa ghi): người vừa ghi xong, đọc lại phải thấy dữ liệu mới của mình.

**Vì sao cần:** async replica nhanh nhưng có thể hiển thị số cũ. Phải giới hạn lag, và không để người vừa đặt hàng mở trang ra lại không thấy đơn của mình.

**Trong bài này** ([README.md](README.md), mục 3 và 5):

- 2s trong đề là **trần lag**. Đo bằng `now() - pg_last_xact_replay_timestamp()`; vượt 2s thì rút replica khỏi pool đọc của listing, cảnh báo sớm ở 1s.
- Trang "đơn của tôi" dùng **sticky to primary** (đọc từ primary trong N giây sau khi ghi) để bảo đảm read-your-writes.

**Ví dụ đời thường:** bảng điện tử ở sân bay cập nhật chậm vài giây thì chấp nhận được; nhưng vé bạn vừa mua mà tra lại không thấy thì không chấp nhận được.

## 5. Bất biến "không oversell"

**Là gì:** **Invariant** (bất biến: điều kiện lúc nào cũng phải đúng), ở đây là `sold <= total`. **Oversell** (bán vượt số hàng đang có). Thao tác **atomic** (nguyên tử: kiểm tra và trừ diễn ra như một bước, không ai chen vào giữa được).

**Vì sao cần:** nếu tách thành "đọc còn bao nhiêu" rồi mới "trừ", hai người có thể cùng đọc thấy "còn 1" và cùng trừ, kết quả bán ra 2 cái.

**Trong bài này** ([README.md](README.md), mục 1 và 3):

- Trừ kho bằng **một câu** `UPDATE stock SET sold = sold + :qty WHERE sku = :sku AND sold + :qty <= total`. Nếu `rowsAffected = 0` nghĩa là hết hàng.
- `CHECK (sold <= total AND sold >= 0)` là hàng rào cuối cùng nằm ngay trong database.
- Đây cũng là lý do loại Cassandra: ghi hoà giải theo last-write-wins (lần ghi sau đè lần ghi trước) nên một lần trừ kho có thể biến mất.

## 6. Thundering herd và retry storm

**Là gì:** **Thundering herd** (bầy đàn ập tới: rất nhiều request cùng dồn vào một chỗ tại một thời điểm). **Retry storm** (bão thử lại: hệ chậm khiến client, gateway, app cùng thử lại, tải bị nhân lên đúng lúc hệ yếu nhất).

**Vì sao cần:** cả hai làm vỡ SLO và có thể đánh sập primary, kéo theo luồng checkout.

**Trong bài này** ([README.md](README.md), mục 5):

- Chặn từ đầu: **waiting room** (phòng chờ phát lượt vào), **rate limit** (giới hạn số request) theo user, mở bán theo lô.
- Luôn giữ ít nhất 2 replica để rút một cái vẫn còn cái kia gánh.
- Retry có kỷ luật: chỉ retry `5xx` và timeout, không retry `4xx`; dùng **backoff** (chờ lâu dần giữa các lần thử) + **jitter** (cộng thêm thời gian ngẫu nhiên để các client không thử lại cùng lúc); trả `Retry-After`.

## 7. Idempotency

**Là gì:** **Idempotency** (tính lũy đẳng: gửi cùng một request nhiều lần thì kết quả giống như gửi một lần).

**Vì sao cần:** timeout không có nghĩa là thất bại. Đơn có thể **đã** ghi nhưng response không về; client retry thì trừ kho và thu tiền hai lần.

**Trong bài này** ([README.md](README.md), mục 5):

- Client sinh `Idempotency-Key`; server `INSERT … ON CONFLICT DO NOTHING` vào bảng `idempotency_keys` **trong cùng transaction** với việc trừ kho.
- Lưu cả `request_hash`: cùng key mà khác nội dung là lỗi client (`422`).
- Thanh toán không gọi gateway trong transaction mà dùng **outbox pattern** (ghi việc cần làm vào bảng `outbox` trong cùng transaction, một tiến trình khác gửi đi sau), kèm idempotency key của chính gateway và job đối soát.

## Hay nhầm

- **"Một database chỉ có một nhãn PACELC."** → Trong bài, cùng một cụm PostgreSQL nhưng mỗi luồng một quadrant, do đọc từ đâu và commit có chờ standby hay không quyết định.
- **"`synchronous_commit = on` là mặc định nên đã là EC."** → Theo README, khi `synchronous_standby_names` rỗng thì nó chỉ đảm bảo flush WAL cục bộ, không chờ standby nào.
- **"Add-to-cart là AP thì sẽ oversell."** → Add-to-cart không trừ kho; mọi việc chống oversell dồn vào câu `UPDATE` có điều kiện ở checkout.
- **"PC nghĩa là cả hệ thống ngừng khi mạng đứt."** → Chỉ luồng checkout từ chối (`503`); listing và add-to-cart vẫn phục vụ.

## Tự kiểm tra

1. Câu nào trong đề cho phép listing được phân loại `EL`?
<details><summary>Đáp án</summary>

"Chấp nhận hiển thị tồn kho trễ tối đa 2s ở trang listing". 2s là trần lag, nên được đọc từ async replica.

</details>

2. Checkout đạt `EC` nhờ những cấu hình nào?
<details><summary>Đáp án</summary>

`SET LOCAL synchronous_commit = on` cùng với `synchronous_standby_names = 'ANY 1 (sb_sync)'` (commit chờ sync standby flush), và đọc từ primary.

</details>

3. Vì sao không làm `SELECT` tồn kho trước rồi mới `UPDATE`?
<details><summary>Đáp án</summary>

Tách hai câu tạo ra khoảng hở: hai người cùng đọc "còn hàng" rồi cùng trừ. Một câu `UPDATE` có điều kiện là nguyên tử, lại bớt một round-trip.

</details>

4. Một async replica đang trễ 3s. Hệ thống xử lý thế nào?
<details><summary>Đáp án</summary>

Health check theo `now() - pg_last_xact_replay_timestamp()` vượt 2s nên trả unhealthy, replica bị rút khỏi pool đọc của listing.

</details>

5. Checkout bị timeout, client gửi lại. Điều gì ngăn trừ kho hai lần?
<details><summary>Đáp án</summary>

`Idempotency-Key` do client sinh, được `INSERT … ON CONFLICT DO NOTHING` vào `idempotency_keys` trong cùng transaction với việc trừ kho, nên lần gửi lại không trừ thêm.

</details>
