# CLAUDE.md — quy ước repo `java-project`

Bài tập môn kiến trúc phần mềm, mục tiêu **junior → middle**. Trọng tâm là hiểu design pattern
và kiến trúc, không phải xây hệ thống production.

- **Chỉ Backend.** Repo không biết gì về Frontend — giả định đã nhận được API phù hợp.
- **Giữ đơn giản.** Không tự thêm security, cache, queue, phân trang, hay tầng kiến trúc nếu
  đề không yêu cầu.
- **Không tự ý `git commit` / `push`.** Mặc định ở nhánh `main`; được yêu cầu commit thì tạo
  branch trước.

---

## 1. Luật phạm vi — đọc trước mọi thứ khác

**Mỗi `assN/` chỉ giải quyết đúng đề của nó. Không hơn. Luật này bắt buộc, không ngoại lệ.**

- Đề không nhắc tới chủ đề nào thì **không đưa chủ đề đó vào**, kể cả khi nó liên quan chặt và
  mình biết rõ. *Đã mắc: đề `ass3` chỉ hỏi CAP, tự thêm PACELC vào — phải gỡ ra.*
- **Khuôn ở Phần A / Phần B là gợi ý về hình thức, không phải giấy phép thêm nội dung.** Mục nào
  đề không đòi (diagram, bảng so sánh, bonus API…) thì **hỏi trước khi làm**, đừng tự đưa vào
  rồi mới báo.
- Đang làm bài này thì **không sửa bài khác**. Thấy bài cũ có vấn đề thì **báo**, để người dùng
  quyết.
- **Không chia sẻ code giữa các bài.** Copy `pom.xml` / `mvnw` thì được; import chéo package
  thì không.
- Thấy thứ hay nhưng ngoài đề → **nói một dòng ở cuối câu trả lời**, đừng tự nhét vào bài.

> **Tự kiểm trước khi báo xong:** mở lại đề, đối chiếu **từng gạch đầu dòng** với mục trong
> `README.md` của bài. Mục nào không map được vào yêu cầu nào → đó là out scope, gỡ ra.

---

## 2. Phân loại đề

| Loại | Dấu hiệu trong đề | Bài nộp chính |
|---|---|---|
| **A — Code** | "triển khai", "implement", "viết API", "tái cấu trúc" | Mã nguồn chạy được + test |
| **B — Thiết kế** | "thiết kế", "chọn", "so sánh", "justify", "diagram" | `README.md` của bài |

**Bài lai** (B kèm *"Bonus: implement small API"*): tài liệu là bài nộp chính → **làm tài liệu
trước, code sau**, code tối giản.

Không chắc thuộc loại nào → **hỏi**, đừng đoán.

---

## 3. Quy ước chung

- **Không comment trong mã nguồn.** Thật sự cần thì tiếng Việt **không dấu**. File `.md` viết
  đủ dấu.
- Tên class / method / biến: tiếng Anh chuẩn Java. `@DisplayName` và tên method test: tiếng
  Việt không dấu, mô tả hành vi.
- Mỗi bài có `README.md` riêng, ghi rõ **phần nào là giả định của người làm bài**.
- Thêm bài mới thì cập nhật bảng trong `README.md` ở root.

---

## 4. Không được bịa

| Loại thông tin | Xử lý |
|---|---|
| Khái niệm nền tảng ổn định (định nghĩa CAP, Redis là in-memory store) | Được khẳng định |
| **Giá trị mặc định của config**, **tên tham số / API**, **hành vi theo phiên bản** | **PHẢI kiểm chứng** |
| **Con số hiệu năng / benchmark / giới hạn** | **KHÔNG đưa ra** nếu không có nguồn |

Kiểm chứng theo thứ tự ưu tiên: **chạy thử** → **tài liệu chính thức** (`WebFetch` /
`WebSearch`, ghi link vào README của bài) → **hỏi người dùng**.

Không làm được cả ba thì **nói thẳng là không chắc**, đánh dấu *"giả định, chưa kiểm chứng"*
trong tài liệu, rồi đi tiếp.

---

# PHẦN A — bài code

## Lệnh

JDK 21. Luôn `cd` vào thư mục bài trước.

```bash
.\mvnw test                 # PHAI xanh truoc khi bao xong
.\mvnw spring-boot:run
```

Spring Boot MVC + annotation. Đừng thêm Gradle, đừng thêm Lombok, đừng đổi build tool.

## Chọn cấu trúc — KHÔNG mặc định Clean Architecture

| Đề nói gì | Dùng gì |
|---|---|
| Không nhắc tới kiến trúc | **Spring Boot thường** |
| "Clean Architecture", "hexagonal", "ports & adapters", "tách tầng", "tái cấu trúc anemic" | [Phụ lục A1](#phụ-lục-a1--khi-đề-đòi-clean-architecture) |
| Design pattern (Strategy, Factory, Observer…) | Spring Boot thường + đúng pattern đề hỏi |
| Transaction, validation, test, hiệu năng, bảo mật | Spring Boot thường |

```
com.example.<domain>/
    controller/  service/  repository/  entity/  exception/
```

> **Thêm tầng mà đề không đòi là làm sai đề, không phải làm kỹ.**

## Cấm

| # | Cấm | Vì sao |
|---|---|---|
| 1 | `double` / `float` cho tiền | `0.1 + 0.2 = 0.30000000000000004`. Dùng `BigDecimal` |
| 2 | Trả thẳng `@Entity` ra API | Đổi cột là đổi response; lazy loading nổ lúc serialize |
| 3 | Nghiệp vụ nằm trong controller | Luật ở `service/` |
| 4 | Chép một luật nghiệp vụ ra hai chỗ | Ngày đổi sẽ quên một chỗ |
| 5 | `ddl-auto: create-drop` ngoài dev | Xoá sạch database mỗi lần khởi động |
| 6 | Xoá file vì grep không thấy ai gọi | `@RestControllerAdvice`, `@Entity`, repo Spring Data, `*Test` đều do **framework** gọi |

## Nên làm

- Constructor injection với field `final`. Không `@Autowired` trên field.
- Repository trả `Optional`, không trả `null`.
- DTO dùng `record`, lồng vào class sở hữu nó.
- Database cấp khoá chính (`@GeneratedValue`), ứng dụng không tự đặt ID.
- **Ghi tên tường minh: `@PathVariable("id")`, `@RequestParam("q")`, `@RequestHeader("X-Node")`.**
  Bỏ tên thì Spring phải đoán qua cờ compiler `-parameters`; cờ đó có thể vắng mặt và lỗi chỉ
  nổ **lúc chạy** (`Name for argument … not specified`), không phải lúc biên dịch.
- **`@Transactional` chỉ có tác dụng qua proxy Spring.** Gọi object bằng `new`, hoặc gọi method
  `@Transactional` từ method khác **cùng class** → transaction im lặng không tồn tại.

## Dịch lỗi

Đặt trong `@RestControllerAdvice` — **nơi duy nhất** biết con số HTTP.
Body lỗi luôn là `{ "code": ..., "message": ... }`.

| Exception | HTTP | `code` |
|---|---|---|
| `MethodArgumentNotValidException`, `HttpMessageNotReadableException` | `400` | `BAD_REQUEST` |
| Exception nghiệp vụ tự định nghĩa | `422` | `BUSINESS_RULE_VIOLATED` |
| `*NotFoundException` | `404` | `<X>_NOT_FOUND` |

`400` = *"tôi không hiểu bạn nói gì"* (sai cú pháp, do `@NotNull` / `@NotBlank` bắt).
`422` = *"tôi hiểu, nhưng không làm được"* (cú pháp đúng, nghiệp vụ từ chối).

## Test

**Luôn phải có:** end-to-end `@SpringBootTest` + `MockMvc`, gồm cả nhánh `400` / `404` / `422`.
Thêm khi bài đáng: test tầng nghiệp vụ chạy bằng `new`, `@DataJpaTest` cho repository.

**Fitness function chỉ bắt buộc với bài Clean Architecture.**

## Database

Mặc định **H2 in-memory**, trừ khi đề yêu cầu khác.

---

## Phụ lục A1 — khi đề đòi Clean Architecture

**Chỉ đọc mục này khi đề thật sự đòi.** Luật dưới đây **thêm vào** luật chung ở trên.

```
adapter/       in/web, out/persistence, config — nơi DUY NHẤT biết Spring
application/   port/in, port/out, usecase
domain/        nghiệp vụ thuần
```

**Dependency Rule:** `import` luôn chỉ vào trong. Hai chỗ cắt bắt buộc — `<X>UseCase` (cổng
vào) và `<X>Repository` (cổng ra): **tầng trong định nghĩa interface, tầng ngoài implements**.

| # | Cấm thêm |
|---|---|
| CA-1 | `domain/` import `org.springframework`, `jakarta.*`, `javax.*`, `com.fasterxml`, `java.sql` |
| CA-2 | `application/` import `adapter/` |
| CA-3 | `@Entity` trên aggregate của `domain/` — tách `<X>JpaEntity` ở `adapter/out/persistence` |
| CA-4 | `adapter/in/**` import `adapter/out/**` |
| CA-5 | Use case trả `ResponseEntity` hoặc biết `404` / `400` |
| CA-6 | Controller gọi thẳng `JpaRepository` |

Annotation: `domain/` **cấm tuyệt đối**; `application/` được `@Service` / `@Transactional`;
`adapter/` thoải mái.

**Nên làm thêm:** port ra do `application` **sở hữu**, chữ ký hàm chỉ nói ngôn ngữ domain
(không `Entity`, không SQL, không `Jpa`) · luật nghiệp vụ nằm trong constructor / factory
method của domain · **không setter công khai** trên object domain.

**Fitness function bắt buộc** — biến CA-1, CA-2, CA-4 và "domain không dính hạ tầng" thành
test đọc thẳng mã nguồn. Test của `domain/` mà cần Spring context → kiến trúc sai.

---

# PHẦN B — bài thiết kế

`assN/README.md` **chính là bài nộp**. Cấu trúc:

1. **Đề bài** — chép nguyên văn, để người chấm đối chiếu.
2. **Giả định** — quy mô, tỉ lệ đọc/ghi, độ trễ, mức chấp nhận mất dữ liệu. Không có giả định
   thì mọi lựa chọn đều vô căn cứ.
3. **Các phương án đã cân nhắc** — ít nhất 3, kèm bảng so sánh.
4. **Quyết định + justify.**
5. **Architecture diagram.**
6. **Đánh đổi đã chấp nhận** — chọn phương án này thì mất gì.
7. **Khi nào quyết định này sai** — điều kiện nào đổi thì phải chọn lại.

Mục 6–7 là thứ phân biệt bài middle với bài junior. Junior viết *"tôi chọn X vì X tốt"*.
Middle viết *"tôi chọn X, chấp nhận mất Y, và nếu Z đổi thì phải xem lại"*.

**Justify chỉ hợp lệ khi đủ ba phần:** gắn với yêu cầu nghiệp vụ cụ thể (không phải *"X phổ
biến và dễ scale"*) · nêu phương án bị loại **và lý do loại** · nêu cái giá phải trả.

**Diagram:** Mermaid nhúng thẳng trong `README.md` — GitHub render được, diff được như code.
Phải thể hiện: các node, hướng replication, client đi vào đâu, chỗ nào sự cố có thể xảy ra.
Viết xong nên **render thử** để chắc nó không vỡ.

## Khi đề đòi nộp file (PDF / slide)

**Dùng đúng template của `ass4` / `ass5`: deck 16:9, mỗi heading `#` trong `README.md` là một
slide.** Không dựng khuôn mới cho từng bài.

- `README.md` viết luôn theo dạng slide: bìa là `# <tiêu đề>` + khối đề bài nguyên văn, sau đó
  mỗi `# ` là **một slide nói một ý**. `##` trở xuống nằm trong slide.
- Chuỗi công cụ: markdown → HTML (`marked` + `mermaid` từ CDN) → `chrome --headless
  --print-to-pdf --no-pdf-header-footer`. Khổ trang **960 × 540 pt**. Số trang do template chèn.
- **Số slide do đề quyết định, không có con số chuẩn.** Trả lời hết các tình huống đề hỏi,
  không thêm slide nào cho thứ đề không hỏi, cũng không gộp ép để cho đủ một con số đẹp.
- **Đo rồi mới chia slide, đừng đoán.** Script báo tỉ lệ *cao nội dung / cao slide* của từng
  slide và **tự co giãn** cho vừa khung (thu nhỏ tới 0.55×, phóng to tới 1.30×). Chỉ phải tự tay
  tách slide khi tỉ lệ vượt **~1.6** — lúc đó thu nhỏ sẽ hết đọc được.
- **Render ra ảnh rồi nhìn tận mắt trước khi báo xong.** Đúng số trang không chứng minh được
  trang không vỡ. *Đã mắc: bìa bị tách hai trang, trang trắng ở cuối, ảnh tràn khỏi khung.*

**Nếu có "Bonus: implement small API":** làm **sau** khi tài liệu xong, theo Phần A, **tối
giản nhất có thể**. Với bài dạy một khái niệm, cân nhắc một endpoint chạy trọn kịch bản rồi
**tường thuật từng bước** — dễ hiểu hơn nhiều so với bắt người đọc tự gõ 7 lệnh `curl`.

---

## Bẫy đã mắc

Mục này **chỉ ghi bẫy về cách làm việc**, không chứa kiến thức chủ đề — kiến thức nằm ở
`README.md` của từng bài (xem [Luật phạm vi](#1-luật-phạm-vi--đọc-trước-mọi-thứ-khác)).

- **Mặc định của một tham số ≠ tính năng đã bật.** `synchronous_commit` của PostgreSQL mặc
  định là `on`, nhưng khi `synchronous_standby_names` rỗng thì nó chỉ flush WAL **cục bộ**.
  Trước khi viết "mặc định đã là X", kiểm tra tham số nào thật sự kích hoạt hành vi đó.
- **Đừng suy kiến thức chủ đề này sang chủ đề khác.** Chưa kiểm chứng thì nói là chưa chắc.
- **Xoá code thì xoá cả tài liệu trỏ tới nó.**
- **Heredoc nhiều khối trong một lệnh bash hay vỡ.** Nội dung dài thì ghi bằng công cụ ghi file.
- **Sửa file hàng loạt bằng script thì cẩn thận string literal và text block.** Regex ngây thơ
  cắt nhầm `"http://..."` thành comment.

Kiến thức đã kiểm chứng, kèm link nguồn, nằm ở:

| Chủ đề | Bài |
|---|---|
| CAP, quorum, PostgreSQL + Patroni, `pg_rewind` | `ass3/README.md` |
| PACELC, knob replication, thundering herd, idempotency | `ass4/README.md` |
| Consistency model, session guarantees, bộ đếm không idempotent | `ass5/README.md` |

---

## Quy trình

1. **Chạy test trước khi báo xong.** Không suy đoán kết quả.
2. **Thay đổi lớn thì chạy thật app rồi `curl`**, gồm cả nhánh lỗi.
3. **Refactor lớn chia bước, compile sau mỗi bước** — để compiler bắt thay vì đọc mắt.
4. **Báo cáo trung thực** — test đỏ thì nói kèm output; bỏ bước nào thì nói.
5. **Giải thích cả *tại sao*.** Có hai cách thì nói rõ đánh đổi.

## Khi tạo bài mới

1. **Phân loại đề** (A hay B).
2. **Liệt kê đúng những gì đề đòi, không hơn** — xem
   [Luật phạm vi](#1-luật-phạm-vi--đọc-trước-mọi-thứ-khác).
3. Chủ đề chưa từng kiểm chứng → áp mục *Không được bịa* trước khi viết dòng nào.
4. Thư mục riêng `assN/`, package gốc riêng `com.example.<domain>`.
5. Bài code: copy `pom.xml`, `mvnw` / `.mvn/` từ bài gần nhất. **Chọn cấu trúc trước khi tạo
   package nào** — mặc định Spring Boot thường. **Bắt đầu tối giản.**
6. `README.md` riêng cho bài + cập nhật bảng trong `README.md` ở root.
