# Kiến thức cơ bản — Day13_JVM-Performance

Bài này hỏi: vì sao endpoint `/process` (nối chuỗi 150 000 lần trong vòng lặp) chậm gần 8 giây và làm GC
chạy liên tục, và phải sửa ở code hay ở cấu hình JVM. Bài phân tích nằm ở [markdown.md](markdown.md).

## Thứ tự nên học

1. Heap và các vùng young / old generation — object được cấp phát ở đâu.
2. Garbage collector và G1 — ai dọn object không còn dùng, dọn thế nào.
3. GC churn và memory leak — hai vấn đề hay bị nhầm với nhau.
4. String immutable và vì sao `+=` trong vòng lặp là O(n²) — nguyên nhân gốc của bài.
5. `StringBuilder` — cách sửa.
6. Đọc GC log và JFR — cách lấy bằng chứng.
7. Humongous object và region của G1 — hiện tượng gặp khi heap nhỏ.
8. Các cờ JVM trong bài — cờ nào giúp, cờ nào không.

## 1. Heap, young generation, old generation, eden

**Là gì:** **heap** (vùng nhớ chứa object do JVM quản lý) là nơi mọi `new` trong Java cấp phát object.
Heap được chia thành **young generation** (vùng chứa object mới tạo) và **old generation** (vùng chứa object
sống lâu). Trong young generation, object mới sinh ra ở **eden** (vùng cấp phát object mới); object còn sống
sau một lần dọn được chuyển sang **survivor** (vùng giữ tạm object vừa sống sót).

**Vì sao cần:** phần lớn object chết rất nhanh. Tách riêng vùng "object mới" giúp GC dọn nhanh một vùng nhỏ
chứa toàn rác, thay vì quét cả heap.

**Trong bài này:** log trong [markdown.md](markdown.md) (mục 2.1) cho thấy `Eden regions: 134->0` — eden bị dọn
sạch — và `Old regions: 4->4` — old generation không tăng.

## 2. Garbage collector và G1

**Là gì:** **garbage collector** (GC — bộ thu gom rác) tự tìm object không còn ai tham chiếu và giải phóng
bộ nhớ của chúng. **G1** (Garbage-First, một loại GC có sẵn trong JVM) chia heap thành nhiều ô bằng nhau gọi là
**region** (ô nhớ có kích thước cố định), mỗi region đóng vai eden, survivor, old hoặc humongous.

**Vì sao cần:** lập trình viên Java không tự `free` bộ nhớ; GC làm việc đó. Cái giá là mỗi lần GC có thể phải
**pause** (tạm dừng các luồng của ứng dụng) trong chốc lát.

**Trong bài này:** app chạy với G1. Mỗi lần eden đầy, G1 chạy một **young GC** (lần dọn chỉ young generation),
trong log ghi là `Pause Young (Normal) (G1 Evacuation Pause)`. Xem [markdown.md](markdown.md) mục 2.1.

## 3. GC churn và memory leak

**Là gì:** **GC churn** (tình trạng tạo và vứt object quá nhiều, khiến GC chạy dồn dập) khác với **memory leak**
(rò rỉ bộ nhớ: object không còn dùng nhưng vẫn bị giữ tham chiếu nên GC không thu được).

**Vì sao cần:** hai bệnh có triệu chứng ban đầu giống nhau (GC chạy nhiều) nhưng cách chữa khác hẳn. Phân biệt
bằng cách nhìn heap **sau** GC: leak thì con số này tăng dần; churn thì nó luôn tụt về mức thấp.

**Trong bài này:** heap sau mỗi GC luôn về khoảng 15 MB, sau full GC chỉ còn 13,5 MB → **High GC Churn**, không phải leak. Xem [markdown.md](markdown.md) mục 1.

## 4. String immutable và vì sao `+=` trong vòng lặp là O(n²)

**Là gì:** `String` trong Java là **immutable** (không đổi được sau khi tạo). `result += x` không nối vào chuỗi
cũ, mà tạo một mảng mới, chép toàn bộ chuỗi cũ sang, chép thêm `x`, rồi bỏ chuỗi cũ thành rác.

**Vì sao cần:** hiểu điều này mới thấy vì sao code trông vô hại lại tốn kém. Vòng thứ *k* phải chép lại cả
*k* đoạn trước đó, nên tổng số byte phải chép tỉ lệ với 1 + 2 + … + n, tức tăng theo **bình phương** số vòng — gọi là
**O(n²)** (chi phí tăng theo bình phương kích thước đầu vào).

**Trong bài này:** dòng gây lỗi:

```java
for (int i = 0; i < 150_000; i++) {
    result += " " + i;
}
```

Chuỗi cuối chỉ dài 938 890 byte nhưng mỗi request cấp phát tổng cộng 62,5 GiB, khớp với phép tính lý thuyết.
Xem [markdown.md](markdown.md) mục 3.

**Ví dụ đời thường:** mỗi lần muốn thêm một dòng vào vở, bạn chép lại cả cuốn vở sang cuốn mới rồi mới viết thêm.

## 5. StringBuilder

**Là gì:** `StringBuilder` giữ một **buffer** (bộ đệm) đổi được; `append` ghi tiếp vào cuối, chỉ khi đầy mới cấp phát mảng lớn hơn và chép một lần.

**Vì sao cần:** chi phí nối chuỗi trở thành tuyến tính (O(n)) thay vì O(n²).

**Trong bài này:** sau khi đổi sang `result.append(' ').append(i)`, thời gian giảm từ khoảng 7 800 ms xuống
1 – 6 ms, bộ nhớ cấp phát từ 62,5 GiB xuống 2,3 MB, số lần GC từ 403 xuống 0. Xem [markdown.md](markdown.md) mục 4.1.

## 6. Đọc GC log và JFR

**Là gì:** **GC log** (nhật ký JVM ghi lại từng lần GC) được bật bằng cờ `-Xlog:gc*`. **JFR** (Java Flight
Recorder — công cụ ghi sự kiện bên trong JVM) cho biết ai cấp phát bao nhiêu bộ nhớ, ở dòng code nào.

**Vì sao cần:** đây là **bằng chứng**. Không có số đo thì "GC churn" chỉ là phỏng đoán.

**Trong bài này:** đọc một dòng log trong [markdown.md](markdown.md) mục 2.1:

```
GC(51) Pause Young (Normal) (G1 Evacuation Pause) 551M->15M(900M) 0.693ms
```

- `GC(51) Pause Young`: lần GC mang số 51, là một young GC; các luồng ứng dụng tạm dừng.
- `551M->15M`: heap đang dùng 551 MB trước GC, còn 15 MB sau GC — gần như toàn bộ là rác.
- `(900M)`: dung lượng heap JVM đang giữ (đã xin từ hệ điều hành) lúc đó — không phải mức tối đa `-Xmx`.
- `0.693ms`: thời gian dừng của lần GC này.

JFR (`jfr view allocation-by-site`) chỉ ra 99,85% áp lực cấp phát đến từ đúng dòng `result += " " + i;`.

## 7. Humongous object và region của G1

**Là gì:** trong G1, object có kích thước từ **nửa region** trở lên là **humongous object** (object khổng lồ).
G1 cấp phát chúng thẳng vào old generation thay vì eden.

**Vì sao cần:** humongous object làm GC phải làm thêm việc. Kích thước region quyết định chuỗi lớn cỡ nào thì
bị coi là humongous; cờ `-XX:G1HeapRegionSize` đổi được kích thước này.

**Trong bài này:** với `-Xmx512m`, region chỉ 1 MB, nên mọi chuỗi từ 512 KB trở lên là humongous, gây 1 311 lần
GC do humongous (kiểu `Concurrent Start`). Tăng region lên 2 MB thì về 0. Xem [markdown.md](markdown.md) mục 4.2.

## 8. Các cờ JVM trong bài

**Là gì:** cờ JVM là tham số truyền khi chạy `java` để điều chỉnh heap và GC.

**Vì sao cần:** chọn đúng cờ giúp GC làm ít việc thừa và giúp quan sát được vấn đề trên môi trường thật.

**Trong bài này:** (xem [markdown.md](markdown.md) mục 4.2)

- `-Xms` / `-Xmx`: kích thước heap ban đầu / tối đa. Đặt bằng nhau để heap không phải co giãn; đo được số GC giảm 396 → 95.
- `-XX:G1HeapRegionSize`: kích thước region của G1; chỉ dùng khi heap nhỏ và log có `G1 Humongous Allocation`.
- `-Xmn`, `-XX:NewRatio`: ép kích thước young generation — **không dùng**, vì làm G1 mất khả năng tự điều chỉnh thời gian dừng.
- `-XX:+UseZGC -XX:+ZGenerational`: đổi sang **ZGC** (GC khác, tập trung giảm thời gian dừng) — không hợp ở đây vì pause vốn chỉ khoảng 1 ms.
- `-Xlog:gc*:file=gc.log:...`: ghi GC log ra file xoay vòng — **dùng**, để thấy được loại vấn đề này khi chạy thật.

## Hay nhầm

- **"GC chạy nhiều tức là memory leak."** → Sai: leak thì heap sau GC tăng dần; ở đây nó luôn về khoảng 15 MB và `Old regions: 4->4`.
- **"Request chậm vì GC."** → Sai: thời gian dừng vì GC chỉ khoảng 1,7%; gần hết 7,8 giây là CPU chép bộ nhớ.
- **"Tăng heap hoặc đổi GC sẽ làm request nhanh lên."** → Sai: với mọi cấu hình đã đo, số GC thay đổi từ 95 tới 1 717 nhưng thời gian vẫn quanh 7 giây; chỉ sửa code mới chữa được.
- **"`+=` chỉ nối thêm vào cuối chuỗi."** → Sai: `String` immutable nên mỗi lần `+=` tạo chuỗi mới và chép lại toàn bộ chuỗi cũ.

## Tự kiểm tra

1. Nhìn vào chỉ số nào để phân biệt GC churn với memory leak?
<details><summary>Đáp án</summary>

Heap sau GC. Leak thì tăng dần qua các lần GC; churn thì luôn tụt về mức thấp (ở bài này khoảng 15 MB).

</details>

2. Trong dòng log `551M->15M(900M) 0.693ms`, ba con số 551M, 15M và 0.693ms nghĩa là gì?
<details><summary>Đáp án</summary>

Heap đang dùng trước GC là 551 MB, sau GC còn 15 MB, và lần dừng đó kéo dài 0,693 ms.

</details>

3. Vì sao `result += " " + i` trong vòng lặp có chi phí O(n²)?
<details><summary>Đáp án</summary>

Vì `String` immutable: mỗi vòng phải tạo mảng mới và chép lại toàn bộ chuỗi cũ, nên tổng số byte chép tỉ lệ với 1 + 2 + … + n.

</details>

4. Với `-Xmx512m` (region 1 MB), một chuỗi 600 KB có phải humongous object không?
<details><summary>Đáp án</summary>

Có, vì nó lớn hơn hoặc bằng nửa region (512 KB).

</details>

5. Vì sao bài đề xuất sửa code trước, chỉnh cờ JVM sau?
<details><summary>Đáp án</summary>

Cờ JVM chỉ đổi được số lần GC, không bớt được byte nào phải chép; `StringBuilder` mới đưa thời gian từ khoảng 7 800 ms xuống 1 – 6 ms.

</details>
