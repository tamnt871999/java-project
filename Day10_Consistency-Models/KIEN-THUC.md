# Kiến thức cơ bản — Day10_Consistency-Models

Bài này trả lời một câu hỏi: khi dữ liệu được chép ra nhiều bản sao, mỗi tính năng của mạng xã hội
(đăng ký / đăng nhập, profile, post, comment, like count) cần các bản sao "giống nhau" đến mức nào,
và chấp nhận trả giá gì để có mức đó.

## Thứ tự nên học

1. **Replication** và **replica lag** — vì sao dữ liệu có thể "cũ" ngay từ đầu.
2. **Strong consistency (linearizability)** — mức chặt nhất, dùng cho đăng ký / đăng nhập.
3. **Eventual consistency** — mức lỏng nhất, dùng cho like count và cho "người khác" xem profile, post.
4. **Read-your-writes** — chính mình phải thấy thứ mình vừa ghi (profile, post).
5. **Monotonic reads** — không được thấy dữ liệu "lùi về quá khứ" (profile).
6. **Causal consistency** — giữ đúng thứ tự "cái này xảy ra vì cái kia" (comment).

## 1. Replication và replica lag

**Là gì.** **Replication** (nhân bản: chép cùng một dữ liệu ra nhiều máy) thường có một
**primary** (máy chính, nhận lệnh ghi) và nhiều **replica** (bản sao, chủ yếu phục vụ đọc). Với
**async replica** (bản sao bất đồng bộ: primary không chờ replica chép xong mới trả lời), replica
luôn có thể chậm hơn primary một chút — khoảng chậm đó gọi là **replica lag** (độ trễ bản sao).

**Vì sao cần.** Một máy không chịu nổi toàn bộ lượng đọc, nên phải đọc từ replica. Cái giá là
có lúc đọc phải dữ liệu cũ. Mọi **consistency model** (mô hình nhất quán: lời hứa của hệ thống về
việc bạn sẽ đọc được dữ liệu "mới" đến đâu) đều sinh ra để trả lời: dữ liệu cũ được phép cũ đến
mức nào?

**Trong bài này.** README đề xuất PostgreSQL "1 primary + N async replica" cho profile và post —
xem [README.md](README.md), mục "2. Cập nhật Profile" và "3. Đăng bài viết". Ví dụ ảnh đại diện
còn cũ sau khi F5 chính là do request "rơi vào replica chưa kịp nhận".

**Ví dụ đời thường.** Thông báo dán ở văn phòng chính được photo gửi về các chi nhánh; chi
nhánh nào nhận bản photo muộn thì nhân viên ở đó vẫn đọc bản cũ.

## 2. Strong consistency (linearizability)

**Là gì.** **Strong consistency** (nhất quán mạnh) — ở dạng chặt nhất là **linearizability**
(tuyến tính hoá: hệ thống cư xử như thể chỉ có một bản dữ liệu duy nhất) — hứa rằng mọi lần đọc
đều thấy lần ghi mới nhất đã hoàn tất, dù đọc ở đâu.

**Vì sao cần.** Có những luật mà hệ thống phải giữ đúng ở mọi thời điểm, README gọi là
**bất biến toàn cục** (global invariant: điều luôn đúng trên toàn bộ dữ liệu), ví dụ "username
là duy nhất". Nếu hai replica cùng tự quyết, cả hai có thể cùng thấy "chưa ai dùng tên này" và
cùng chấp nhận → hai tài khoản trùng tên.

**Trong bài này.** Chỉ tính năng đăng ký / đăng nhập dùng strong — xem [README.md](README.md),
mục "1. Đăng ký / Đăng nhập". Đăng ký cần nó để giữ username duy nhất; đăng nhập cần nó vì đọc
mật khẩu cũ sau khi đã đổi là **lỗ hổng bảo mật**. Cái giá: ghi và đọc đều dồn về primary, nên
lúc **failover** (chuyển sang máy dự phòng khi primary hỏng) thì không đăng nhập được.

## 3. Eventual consistency

**Là gì.** **Eventual consistency** (nhất quán cuối cùng: các bản sao sẽ giống nhau sau một
khoảng thời gian) chỉ hứa: nếu ngừng ghi, rồi sẽ đến lúc mọi replica có cùng giá trị. Trước lúc
đó, đọc ở đâu có thể thấy giá trị khác nhau.

**Vì sao cần.** Không phải chờ các bản sao thống nhất với nhau, nên ghi và đọc nhanh, đọc từ
bất kỳ replica nào cũng được. Đáng dùng khi đọc cũ một chút **không gây hại gì**.

**Trong bài này.** Like count dùng eventual: không ai phân biệt được 10.412 với 10.415 — xem
[README.md](README.md), mục "5. Bộ đếm Thích". Profile và post cũng là eventual **đối với người
khác**: người theo dõi thấy bài chậm vài giây là chấp nhận được.

## 4. Read-your-writes

**Là gì.** **Read-your-writes** (đọc được thứ mình vừa ghi) hứa rằng sau khi **bạn** ghi, các
lần đọc tiếp theo **của chính bạn** luôn thấy thay đổi đó. Nó là một trong các **session
guarantees** (bảo đảm theo phiên: lời hứa chỉ áp dụng cho một người dùng / một phiên làm việc).

**Vì sao cần.** Eventual consistency cho phép bạn vừa đăng xong lại không thấy bài của mình.
Người dùng sẽ tưởng lỗi và làm lại.

**Trong bài này.** Profile và post dùng read-your-writes cho chính chủ — xem
[README.md](README.md), mục "2" và "3". Không thấy ảnh mới → upload lại; không thấy bài vừa đăng
→ đăng lại → **bài trùng**. README nêu cách làm rẻ: sau khi ghi, chuyển request của người đó về
primary trong N giây — chỉ là định tuyến, không cần các máy đồng thuận.

## 5. Monotonic reads

**Là gì.** **Monotonic reads** (đọc đơn điệu: đã thấy bản mới thì không bao giờ thấy lại bản cũ
hơn) cũng là một session guarantee: dữ liệu bạn thấy chỉ có thể đứng yên hoặc tiến lên.

**Vì sao cần.** Nếu **load balancer** (bộ chia tải: rải request sang nhiều máy) gửi mỗi lần F5
sang một replica có độ trễ khác nhau, bạn có thể thấy ảnh mới, F5 lại thấy ảnh cũ, rồi lại mới.
Eventual consistency vẫn cho phép điều đó, nhưng người dùng sẽ nghĩ hệ thống hỏng.

**Trong bài này.** Dùng cho profile, đi kèm read-your-writes — xem [README.md](README.md),
mục "2. Cập nhật Profile". Cách làm: **sticky** (gắn cố định) một người dùng với một replica
suốt phiên.

## 6. Causal consistency

**Là gì.** **Causal consistency** (nhất quán nhân quả) hứa rằng nếu thao tác B xảy ra **vì**
đã thấy thao tác A (B phụ thuộc A), thì mọi người đều thấy A trước B. Những thao tác không liên
quan nhau thì không bị ép thứ tự.

**Vì sao cần.** Với eventual consistency, một replica có thể nhận câu trả lời trước câu hỏi.
Khi đó người đọc không chỉ thấy dữ liệu cũ mà **hiểu sai** cuộc hội thoại.

**Trong bài này.** Comment dùng causal, phạm vi **một bài viết** — xem [README.md](README.md),
mục "4. Bình luận". Hai lỗi cần tránh: **comment mồ côi** (comment hiện ra trước cả bài viết nó
thuộc về) và **hội thoại đọc ngược** ("tôi đi" hiện trước "ai đi không?"). Không chọn strong vì
không cần thứ tự giữa các bài khác nhau, chỉ cần đúng thứ tự bên trong một bài.

**Ví dụ đời thường.** Trong nhóm chat, câu trả lời không bao giờ được hiện lên trước câu hỏi;
còn hai cuộc trò chuyện ở hai nhóm khác nhau thì cái nào hiện trước cũng được.

## Hay nhầm

- **"Eventual nghĩa là có thể mất dữ liệu."** → Sai. Eventual nói về *khi nào người khác thấy*;
  việc ghi có bền hay không là một trục khác (README mục 3: bài viết đọc eventual nhưng ghi vẫn
  phải bền).
- **"Dữ liệu quan trọng thì cứ dùng strong cho chắc."** → Strong đắt khi lưu lượng lớn; profile
  có mặt trên mọi màn hình, ép đọc primary là làm quá tải primary (README mục 1 và 2).
- **"Read-your-writes nghĩa là ai cũng thấy ngay."** → Chỉ người vừa ghi thấy ngay; người khác
  vẫn là eventual.
- **"Like của mình phải đỏ ngay nên like count cần strong."** → Đó là yêu cầu giao diện, giải
  quyết bằng **optimistic update** (client tự hiển thị kết quả trước khi server xác nhận) — README
  mục 5.

## Tự kiểm tra

1. Vì sao đăng ký username không dùng eventual consistency được?
<details><summary>Đáp án</summary>

"Username là duy nhất" là bất biến toàn cục. Hai replica có thể cùng thấy tên chưa tồn tại và
cùng chấp nhận, tạo ra hai tài khoản trùng tên.

</details>

2. Người dùng đổi ảnh đại diện, F5 thấy ảnh cũ. Thiếu bảo đảm nào? Còn nếu ảnh nhảy qua nhảy lại
giữa cũ và mới?
<details><summary>Đáp án</summary>

Trường hợp đầu thiếu read-your-writes. Trường hợp sau thiếu monotonic reads.

</details>

3. Comment "tôi đi" hiện trước "ai đi không?". Mô hình nào ngăn được, và vì sao không cần strong?
<details><summary>Đáp án</summary>

Causal consistency. Chỉ cần đúng thứ tự giữa các thao tác có quan hệ nhân quả trong cùng một bài,
không cần một thứ tự toàn cục cho mọi comment nên không phải trả chi phí của strong.

</details>

4. Vì sao like count chọn eventual mà không gây hại?
<details><summary>Đáp án</summary>

Đọc số cũ không phá bất biến nào và không ai phân biệt được chênh lệch vài like; đổi lại luồng
ghi lớn nhất của hệ không phải chờ các bản sao đồng thuận.

</details>
