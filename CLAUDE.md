# CLAUDE.md — quy ước repo `java-project`

Bài tập môn kiến trúc phần mềm, mục tiêu **junior → middle**. Trọng tâm là hiểu design pattern
và kiến trúc, không phải xây hệ thống production. Chỉ Backend.

- **Không tự ý `git commit` / `push`.** Được yêu cầu thì commit và push **thẳng lên `main`**,
  không tạo branch riêng cho từng bài.
- **Mỗi bài một thư mục `Day<N>_<Chủ-đề>`** (vd. `Day11-12_Redis`), độc lập hoàn toàn: package
  gốc riêng `com.example.<domain>`, `artifactId` viết thường (`day11-12-redis`), không import chéo.
  Thêm bài mới thì cập nhật bảng trong `README.md` ở root.

---

## 1. Luật phạm vi — thắng mọi mục khác

**Đề bài là nguồn DUY NHẤT quyết định bài nộp gồm những gì.**

Trước khi viết: chép từng gạch đầu dòng của đề thành danh sách kiểm. Mỗi thứ sắp tạo — file,
endpoint, trường JSON, mã lỗi, mục tài liệu — phải chỉ ra được nó phục vụ gạch đầu dòng nào.
**Không chỉ ra được thì không làm.**

| | Là gì | Ai quyết định |
|---|---|---|
| **Hình thức** | *Viết thế nào*: đặt tên, chia package, cách viết test, cách trình bày | File này |
| **Nội dung** | *Bài có những gì*: code hay không, endpoint, trường, mã lỗi, mục tài liệu, hạ tầng | **Chỉ đề bài** |

Mọi danh sách trong file này là quy ước **hình thức**, không phải danh mục phải có.

Những lần out scope đã phải gỡ:

| Đã tự thêm | Đề đòi gì |
|---|---|
| PACELC trong `Day8_CAP-Theorem` | Chỉ CAP |
| Lớp "van Redis" trong `Day9_PACELC-Theorem` | Không nhắc tới cache |
| Cây quyết định, giả định, bảng tổng hợp, sơ đồ trong `Day10_Consistency-Models` | 3 câu cho mỗi tính năng |
| Trường `rank`, mã lỗi `503` trong `Day11-12_Redis` | 2 endpoint |
| Cả ứng dụng Spring Boot + test trong `Day13_JVM-Performance` | Chỉ một file `markdown.md` |
| Mục giả định, lệnh tái hiện, nguồn trong `Day13_JVM-Performance/markdown.md` | Chỉ trả lời 4 câu hỏi |

Một trường thừa, một mục thừa cũng là out scope. *"Nó liên quan và mình biết rõ"* không phải lý do.

- **Thấy thứ hay nhưng ngoài đề:** không làm. Nói **một dòng** cuối câu trả lời rồi chờ người
  dùng quyết. Làm trước rồi báo sau vẫn là vi phạm.
- **Không sửa bài khác** khi đang làm một bài; thấy vấn đề thì báo.
- **Tự kiểm trước khi báo xong:** đối chiếu từng thứ đã tạo với từng gạch đầu dòng của đề; thứ
  nào không map được thì gỡ.

---

## 2. Phân loại đề — quyết định có code hay không

| Loại | Dấu hiệu | Bài nộp |
|---|---|---|
| **Code** | "triển khai", "implement", "viết API", "tái cấu trúc" | Mã nguồn chạy được + test + `README.md` |
| **Tài liệu** | "thiết kế", "chọn", "so sánh", "phân tích", đề chỉ định file nộp | **Chỉ** file tài liệu, **không có code** |
| **Lai** | Tài liệu kèm *"Bonus: implement small API"* | Tài liệu trước, code tối giản sau |

- Đề chỉ định tên file (vd. `markdown.md`) thì dùng đúng tên đó; không thì `README.md`.
- Tài liệu nộp = **đề bài chép nguyên văn + trả lời đúng từng câu hỏi**. Không thêm mục giả
  định, nguồn, cách tái hiện… trừ khi đề đòi.
- Cần chạy thử để lấy số đo / bằng chứng thì chạy trong scratchpad, **không** đưa code vào repo.
- Không chắc thuộc loại nào → **hỏi**.

---

## 3. Không được bịa

| Loại thông tin | Xử lý |
|---|---|
| Khái niệm nền tảng ổn định | Được khẳng định |
| Giá trị mặc định của config, tên tham số / API, hành vi theo phiên bản | **Phải kiểm chứng** |
| Con số hiệu năng / benchmark | **Không đưa ra** nếu không tự đo hoặc không có nguồn |

Thứ tự kiểm chứng: **chạy thử** → **tài liệu chính thức** → **hỏi người dùng**. Không làm được
thì nói thẳng là không chắc.

Dùng đúng thuật ngữ của từng hệ thống (vd. Redis là **master / replica**, không phải primary).

---

## 4. Bài code

JDK 21, Maven Wrapper, Spring Boot 3.4 MVC + annotation. Không Gradle, không Lombok.

```bash
.\mvnw test                 # PHAI xanh truoc khi bao xong
.\mvnw spring-boot:run
```

**Cấu trúc:** mặc định Spring Boot thường (`controller/ service/ repository/ entity/ exception/`).
Chỉ dùng Clean Architecture khi đề nói "Clean Architecture", "hexagonal", "tách tầng", "tái cấu
trúc anemic". Thêm tầng mà đề không đòi là làm sai đề.

**Quy ước:**

- Không comment trong mã nguồn; thật sự cần thì tiếng Việt **không dấu**. File `.md` viết đủ dấu.
- Tên class / method / biến tiếng Anh. Tên method test và `@DisplayName` tiếng Việt không dấu.
- Constructor injection, field `final`. DTO là `record` lồng trong class sở hữu nó.
- Repository trả `Optional`. Khoá chính do database cấp (`@GeneratedValue`).
- Ghi tên tường minh: `@PathVariable("id")`, `@RequestParam("q")` — thiếu thì lỗi chỉ nổ lúc chạy.
- Tiền dùng `BigDecimal`, không `double`. Không trả thẳng `@Entity` ra API. Nghiệp vụ ở `service/`.
- `@Transactional`, `@Cacheable`, `@CacheEvict` chỉ có tác dụng qua proxy Spring: gọi từ method
  cùng class hoặc qua `new` thì annotation im lặng không chạy.
- Database mặc định H2 in-memory.

**Dịch lỗi** trong `@RestControllerAdvice`, body `{ "code": ..., "message": ... }`. Chỉ map lỗi
mà endpoint trong đề thật sự sinh ra:

| Exception | HTTP | `code` |
|---|---|---|
| `MethodArgumentNotValidException`, `HttpMessageNotReadableException` | `400` | `BAD_REQUEST` |
| Exception nghiệp vụ tự định nghĩa | `422` | `BUSINESS_RULE_VIOLATED` |
| `*NotFoundException` | `404` | `<X>_NOT_FOUND` |

**Test:** luôn có end-to-end `@SpringBootTest` + `MockMvc`, gồm các nhánh lỗi mà endpoint sinh ra.

### Khi đề đòi Clean Architecture

```
adapter/       in/web, out/persistence, config — noi DUY NHAT biet Spring
application/   port/in, port/out, usecase
domain/        nghiep vu thuan
```

- `import` chỉ hướng vào trong. Tầng trong định nghĩa interface (`<X>UseCase`, `<X>Repository`),
  tầng ngoài implements.
- `domain/` không import `org.springframework`, `jakarta.*`, `javax.*`, `com.fasterxml`,
  `java.sql`; không `@Entity` (tách `<X>JpaEntity` ở `adapter/out/persistence`); không setter
  công khai.
- `application/` không import `adapter/`; `adapter/in` không import `adapter/out`. Use case không
  biết HTTP.
- **Bắt buộc fitness function**: test đọc thẳng mã nguồn, fail build khi vi phạm các luật trên.

---

## 5. Bài tài liệu

- Bài thiết kế (chọn phương án): justify phải gắn với yêu cầu cụ thể của đề, nêu phương án bị loại
  và lý do, nêu cái giá phải trả. Diagram (nếu đề đòi) viết bằng Mermaid nhúng trong file `.md`.
- **Đề đòi PDF / slide:** dùng đúng template của `Day9_PACELC-Theorem` / `Day10_Consistency-Models`.
  - Deck 16:9, khổ 960 × 540 pt, mỗi heading `#` là một slide. Bìa là `# <tiêu đề>` + đề bài.
  - Chuỗi công cụ: markdown → HTML (`marked` + `mermaid` từ CDN) → `chrome --headless
    --print-to-pdf --no-pdf-header-footer`.
  - Số slide do đề quyết định. Script tự co giãn nội dung cho vừa slide; tỉ lệ cao nội dung /
    cao slide vượt ~1.6 thì tách slide.
  - **Render ra ảnh và nhìn tận mắt trước khi báo xong** (đã gặp: bìa tách hai trang, trang trắng
    cuối, ảnh tràn khung).

---

## 6. Quy trình và bẫy đã mắc

1. Chạy test trước khi báo xong; thay đổi lớn thì chạy thật app rồi `curl`, gồm cả nhánh lỗi.
2. Báo cáo trung thực: test đỏ thì nói kèm output; bỏ bước nào thì nói.
3. Giải thích cả *tại sao*; có hai cách thì nói rõ đánh đổi.

- **Mặc định của một tham số ≠ tính năng đã bật.** Kiểm tham số nào thật sự kích hoạt hành vi.
- **Xoá hay đổi tên thứ gì thì sửa luôn mọi tài liệu trỏ tới nó.**
- **Kiểm thời điểm của surefire report** — lệnh `mvnw` chưa chạy thì report cũ vẫn nằm đó.
- **Nội dung dài thì ghi bằng công cụ ghi file**, không dùng heredoc nhiều khối.
