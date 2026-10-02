# Day13_JVM-Performance — Phân tích hiệu năng JVM: endpoint `/process`

## Đề bài

```java
@GetMapping("/process")
    public String processData() {
        long startTime = System.currentTimeMillis();
String result = "";
        for (int i = 0; i < 150_000; i++) {
            result += " " + i;
}
        long endTime = System.currentTimeMillis();
return "Processing finished in " + (endTime - startTime) + "ms. Result length: " + result. Length();
}
```

> 1. Vấn đề bạn tìm thấy là gì? (Memory Leak, High GC Churn, ...?)
> 2. Bằng chứng (screenshot từ VisualVM, đoạn log GC, ...).
> 3. Nguyên nhân gốc rễ trong code.
> 4. Đề xuất cách sửa lỗi và các cờ JVM để tối ưu hóa.
>
> Đề xuất viết vào file markdown.md

---

## 1. Vấn đề: High GC Churn, không phải Memory Leak

**High GC Churn** nghĩa là chương trình tạo ra và vứt đi rất nhiều object ngắn hạn. Bộ thu gom
rác (**GC** — garbage collector) vì thế phải chạy liên tục.

Mỗi request `/process`:

- **cấp phát 62,5 GiB** bộ nhớ;
- khiến G1 chạy khoảng **130 lần young GC**;
- mất khoảng **7,8 giây**, dù chỉ để tạo ra một chuỗi dài 938 890 ký tự (chưa tới 1 MB).

Đây **không phải Memory Leak** (rò rỉ bộ nhớ: object không còn dùng nhưng vẫn bị giữ tham chiếu
nên GC không thu được). Dấu hiệu của leak là heap sau GC tăng dần, và ở đây không có dấu hiệu đó:

- sau mỗi lần GC, heap đều tụt về khoảng **15 MB**;
- sau khi ép full GC, heap chỉ còn **13,5 MB**.

Chuỗi `result` là biến cục bộ, nên request kết thúc là nó hết được tham chiếu.

Điểm quan trọng nhất là **bản thân GC không phải thứ làm chậm**. Tổng thời gian dừng vì GC chỉ
chiếm khoảng 1,7%. Gần hết 7,8 giây là CPU **chép bộ nhớ**, và GC dày đặc chỉ là triệu chứng của
việc chép đó (xem mục 3).

---

## 2. Bằng chứng

### 2.1 GC log: GC chạy khoảng 60 ms một lần, mỗi lần dọn sạch gần hết heap

Chạy app với `-Xlog:gc*` rồi gọi `/process` ba lần:

```
Processing finished in 7973ms. Result length: 938890
Processing finished in 7847ms. Result length: 938890
Processing finished in 7759ms. Result length: 938890
```

Đoạn log trích trong lúc request đang chạy:

```
[5.216s][info][gc] GC(51) Pause Young (Normal) (G1 Evacuation Pause) 551M->15M(900M) 0.693ms
[5.277s][info][gc] GC(52) Pause Young (Normal) (G1 Evacuation Pause) 551M->15M(900M) 1.021ms
[5.337s][info][gc] GC(53) Pause Young (Normal) (G1 Evacuation Pause) 551M->15M(900M) 0.804ms
[5.399s][info][gc] GC(54) Pause Young (Normal) (G1 Evacuation Pause) 551M->15M(900M) 0.709ms
```

Chi tiết của một lần GC:

```
[20.201s][info][gc,heap     ] GC(300) Eden regions: 134->0(134)
[20.201s][info][gc,heap     ] GC(300) Survivor regions: 1->1(17)
[20.201s][info][gc,heap     ] GC(300) Old regions: 4->4
[20.201s][info][gc,heap     ] GC(300) Humongous regions: 0->0
[20.201s][info][gc          ] GC(300) Pause Young (Normal) (G1 Evacuation Pause) 551M->16M(900M) 0.758ms
```

Cách đọc đoạn log:

| Dòng log | Ý nghĩa |
|---|---|
| Khoảng cách giữa các GC chừng **60 ms** | Eden (vùng cấp phát object mới) đầy lại sau mỗi 60 ms |
| `551M->15M` | Mỗi lần GC dọn khoảng **536 MB**, tức gần như toàn bộ là rác. 536 MB / 60 ms ≈ **9 GB/s** cấp phát |
| `Eden regions: 134->0` | Toàn bộ eden bị dọn sạch, không object nào sống sót để được giữ lại |
| `Old regions: 4->4` | Old generation (vùng chứa object sống lâu) **không tăng**, nên không có leak |
| `0.758ms` | Mỗi lần dừng rất ngắn, vì gần như không có gì phải chép sang vùng khác |

Tổng hợp từ log của cả ba request:

| Chỉ số | Giá trị |
|---|---|
| Số lần `Pause Young (Normal)` | **403**, tức khoảng 134 lần mỗi request |
| Tổng thời gian dừng vì GC | **390,6 ms** trên khoảng 23,6 s chạy, ≈ **1,7%** |
| Thời gian dừng trung bình / dài nhất | 0,97 ms / 10,45 ms |

Ép full GC xong (`jcmd <pid> GC.run`) rồi xem heap (`jcmd <pid> GC.heap_info`):

```
 garbage-first heap   total 57344K, used 13779K
```

Heap chỉ còn **13,5 MB** đang dùng. Không có gì bị giữ lại sau ba request.

### 2.2 JFR: chỉ đúng dòng code gây cấp phát

Cấp phát theo vị trí (`jfr view allocation-by-site`):

```
Method                                                              Allocation Pressure
jdk.internal.misc.Unsafe.allocateUninitializedArray(Class, int)                  99.85%
java.lang.StringConcatHelper.newString(byte[], long)                              0.06%
```

**99,85%** áp lực cấp phát đến từ một chỗ duy nhất. Stack trace của các mẫu cấp phát
(`jfr print --events jdk.ObjectAllocationSample`) cho biết chỗ đó nằm đâu trong code:

```
objectClass = byte[]
eventThread = "http-nio-8085-exec-1"
stackTrace = [
  jdk.internal.misc.Unsafe.allocateUninitializedArray(Class, int) line: 1380
  java.lang.StringConcatHelper.newArray(long) line: 511
  com.example.jvmperf.controller.ProcessController.processData() line: 14
```

`ProcessController` là controller dùng để đo, chứa đúng đoạn code của đề. Dòng 14 của nó là
`result += " " + i;`. Tổng cộng có 6 685 mẫu cấp phát trỏ về dòng này.

Tổng bộ nhớ mà mỗi luồng request đã cấp phát (`jdk.ThreadAllocationStatistics`):

```
allocated = 62.5 GB   thread = "http-nio-8085-exec-1"
allocated = 62.5 GB   thread = "http-nio-8085-exec-2"
allocated = 62.5 GB   thread = "http-nio-8085-exec-3"
```

JFR dùng đơn vị nhị phân, nên "62.5 GB" ở đây là **62,5 GiB**. Mỗi request được một luồng riêng
xử lý, và mỗi luồng cấp phát 62,5 GiB.

---

## 3. Nguyên nhân gốc rễ: `String +=` trong vòng lặp có độ phức tạp O(n²)

```java
String result = "";
for (int i = 0; i < 150_000; i++) {
    result += " " + i;
}
```

`String` trong Java là **immutable** (không đổi được sau khi tạo). Vì vậy `result += x` không nối
thêm vào chuỗi cũ mà làm ba việc:

1. cấp phát một mảng `byte[]` mới, đủ chứa `result` cũ cộng `x`;
2. **chép toàn bộ** `result` cũ sang mảng mới, rồi chép thêm `x`;
3. trỏ `result` sang chuỗi mới. Chuỗi cũ thành rác.

Vòng lặp thứ *k* phải chép lại cả *k* đoạn trước đó. Tổng số byte phải chép vì thế tăng theo
**bình phương** số vòng lặp. Chuỗi chỉ chứa chữ số và dấu cách, nên với `CompactStrings` (bật
mặc định) mỗi ký tự chiếm đúng 1 byte. Có thể tính chính xác:

| Đại lượng | Giá trị |
|---|---|
| Độ dài chuỗi cuối cùng | 938 890 byte (khớp `Result length` mà endpoint trả về) |
| Tổng byte đã cấp phát = tổng độ dài chuỗi qua 150 000 vòng | 67 134 474 495 byte = **62,52 GiB** |
| JFR đo được | **62,5 GiB** mỗi request |

Lý thuyết và đo đạc khớp nhau, nên đây đúng là nguyên nhân, không phải phỏng đoán.

Chuỗi nhân quả đầy đủ:

1. `String` immutable làm mỗi lần `+=` phải **chép lại cả chuỗi**.
2. Chép lại cả chuỗi trong vòng lặp tạo ra tổng **62,5 GiB rác**, trong khi kết quả cần chưa tới 1 MB.
3. Rác sinh ra khoảng 9 GB/s nên eden đầy sau mỗi 60 ms, dẫn tới **High GC Churn**.
4. Thời gian request thì bị CPU **chép bộ nhớ** ăn hết. GC chỉ chiếm khoảng 1,7%.

Kết luận thứ tư quyết định cách sửa ở mục 4: chỉnh GC chỉ giảm triệu chứng, còn muốn chữa thì
phải sửa code.

---

## 4. Đề xuất

### 4.1 Sửa code: dùng `StringBuilder`

```java
@GetMapping("/process")
public String processData() {
    long startTime = System.currentTimeMillis();
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < 150_000; i++) {
        result.append(' ').append(i);
    }
    long endTime = System.currentTimeMillis();
    return "Processing finished in " + (endTime - startTime) + "ms. Result length: " + result.length();
}
```

`StringBuilder` giữ một bộ đệm đổi được. `append` ghi vào cuối bộ đệm; chỉ khi bộ đệm đầy nó mới
cấp phát mảng lớn hơn và chép một lần. Tổng chi phí vì thế là tuyến tính, không còn bình phương.

Đo bản sửa với đúng cấu hình và đúng cách đo như mục 2:

| Chỉ số mỗi request | Trước | Sau |
|---|---|---|
| Thời gian xử lý | 7 759 – 7 973 ms | **1 – 6 ms** |
| Bộ nhớ cấp phát (JFR, luồng request) | 62,5 GiB | **2,3 MB** |
| Số lần GC trong lúc chạy 3 request | 403 | **0** |
| `Result length` | 938 890 | 938 890 |

### 4.2 Cờ JVM: đo từng cờ trên **bản code lỗi**

Mỗi cấu hình chạy app, gọi `/process` ba lần và đếm GC trong lúc các request chạy:

| Cấu hình | Region G1 | Thời gian 3 request (ms) | Số GC | Humongous GC |
|---|---|---|---|---|
| Mặc định (heap tối đa 8 GB) | 4 MB | 7 149 · 6 976 · 6 997 | 396 | 0 |
| `-Xmx512m` | 1 MB | 7 847 · 7 473 · 7 424 | 1 717 | **1 311** |
| `-Xmx512m -XX:G1HeapRegionSize=2m` | 2 MB | 7 237 · 7 126 · 7 360 | 1 388 | **0** |
| `-Xms4g -Xmx4g` | 2 MB | 8 670 · 7 087 · 6 903 | **95** | 0 |
| `-XX:+UseZGC -XX:+ZGenerational` | — | 9 844 · 9 442 · 7 815 | 34 lần collection, heap lên tới 6 980 MB (86%) | — |

Số GC thay đổi từ 95 tới 1 717 tuỳ cấu hình, nhưng **thời gian request vẫn quanh 7 giây**. Đây là
bằng chứng trực tiếp cho kết luận ở mục 3. Từ bảng này rút ra các đề xuất:

| Cờ | Đề xuất | Lý do |
|---|---|---|
| `-Xms` = `-Xmx` | **Dùng** | Tài liệu G1: *"You can minimize heap resizing work by disabling it; set the options `-Xms` and `-Xmx` to the same value."* Đo được: số GC giảm 396 → 95. Giá trị cụ thể phải chọn theo live set (lượng object còn sống) của ứng dụng thật, đề không cho số này. |
| `-XX:G1HeapRegionSize` | **Dùng khi heap nhỏ** và log có `G1 Humongous Allocation` | G1 coi *"objects larger or equal the size of half a region"* là **humongous** (object khổng lồ) và cấp phát chúng thẳng vào old generation. Với `-Xmx512m`, region chỉ 1 MB, nên mọi chuỗi từ 512 KB trở lên là humongous. Kết quả là 1 311 lần GC kiểu `Concurrent Start`. Tăng region lên 2 MB thì về 0. |
| `-Xmn`, `-XX:NewRatio` | **Không dùng** | Tài liệu G1: *"Setting the young generation size to a single value overrides and practically disables pause-time control."* |
| `-XX:+UseZGC -XX:+ZGenerational` | **Không dùng cho trường hợp này** | ZGC giảm thời gian dừng, nhưng ở đây thời gian dừng vốn đã khoảng 1 ms. Đổi lại request chậm hơn và heap phình tới 86%. |
| `-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=5,filesize=20m` | **Dùng** | Ghi GC log xoay vòng 5 file × 20 MB, để nhìn thấy loại vấn đề này trên môi trường thật. Tốn rất ít chi phí. Cú pháp đã kiểm bằng `java -Xlog:help`. |

Bộ cờ đề xuất:

```bash
java -Xms<N> -Xmx<N> \
     "-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=5,filesize=20m" \
     -jar app.jar
# chi them -XX:G1HeapRegionSize=2m khi heap nho va GC log co "G1 Humongous Allocation"
```

> **Thứ tự đúng là sửa code trước, chỉnh cờ sau.** Cờ JVM giảm được số lần GC nhưng không bớt
> được byte nào phải chép. Bản sửa ở 4.1 giảm thời gian từ khoảng 7 800 ms xuống 1 – 6 ms. Không cờ
> nào trong bảng trên làm thời gian giảm đáng kể.
