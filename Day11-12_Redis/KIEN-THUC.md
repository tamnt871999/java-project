# Kiến thức cơ bản — Day11-12_Redis

Bài này xoay quanh một câu hỏi: dùng **Redis** (kho dữ liệu key–value nằm trong bộ nhớ RAM) thế
nào trong Spring Boot để (1) làm bảng xếp hạng có bản sao dữ liệu, và (2) xoá một sản phẩm mà
không instance nào còn giữ bản cũ trong cache.

## Thứ tự nên học

1. Sorted Set và hai lệnh `ZADD` / `ZREVRANGE` — cấu trúc dữ liệu của bảng xếp hạng.
2. Replication master–replica — hai container Redis, một cái sao chép cái kia.
3. Ghi vào master, đọc từ replica — cách Spring Boot nói chuyện với hai Redis.
4. Cache hai tầng L1 / L2 — đọc sản phẩm qua bộ nhớ riêng, rồi Redis, rồi DB.
5. `@Cacheable` / `@CacheEvict` và proxy của Spring — cách Spring tự đọc/xoá L2.
6. Redis Pub/Sub — báo cho các instance khác xoá L1 của chúng.
7. Thứ tự các bước xoá — vì sao cần `ProductFacade`.

## 1. Sorted Set, `ZADD` và `ZREVRANGE`

**Là gì.** **Sorted Set** (tập hợp có thứ tự) là một kiểu dữ liệu của Redis: mỗi **member**
(phần tử, ở đây là tên người chơi) là duy nhất và đi kèm một **score** (điểm số dùng để sắp xếp).
Redis luôn giữ các member theo thứ tự score.

**Vì sao cần.** Bảng xếp hạng cần đúng hai việc: ghi điểm cho một người, và lấy N người điểm cao
nhất. Sorted Set làm sẵn cả hai, không phải tự sắp xếp trong Java.

**Trong bài này.** [`LeaderboardService`](src/main/java/com/example/leaderboard/service/LeaderboardService.java)
dùng key `leaderboard:global`:

- `POST /leaderboard/{username}` → `ZADD`: thêm người chơi, hoặc **ghi đè** điểm nếu đã có.
- `GET /leaderboard/top/{n}` → `ZREVRANGE leaderboard:global 0 n-1 WITHSCORES`: lấy từ vị trí `0`
  đến `n-1` theo thứ tự **giảm dần** (`REV` = reverse), kèm điểm.

```java
master.opsForZSet().add(key, player, score);
Set<TypedTuple<String>> rows = replica.opsForZSet().reverseRangeWithScores(key, 0, n - 1);
```

README ghi thêm: `ZREVRANGE` bị **deprecated** (không khuyến khích dùng nữa) từ Redis 6.2, bản thay
thế là `ZRANGE key start stop REV`; bài vẫn dùng `ZREVRANGE` vì đề chỉ đích danh.

## 2. Replication master–replica

**Là gì.** **Replication** (sao chép dữ liệu): **master** (node chính, nhận lệnh ghi) gửi thay đổi
sang **replica** (bản sao: node chỉ đọc, nhận dữ liệu từ master).

**Vì sao cần.** Có thêm một bản dữ liệu ở chỗ khác, và có thể chia việc đọc sang replica.

**Trong bài này.** [`docker-compose.yml`](docker-compose.yml) dựng hai **container**
(một tiến trình chạy cô lập trong Docker): `redis-master` ở cổng `6379`, `redis-replica` ở cổng
`6380`. Container thứ hai thành replica nhờ đúng một dòng:

```yaml
command: ["redis-server", "--replicaof", "redis-master", "6379"]
```

`redis-master` là tên service trong compose, được DNS nội bộ của compose network phân giải, nên
không cần biết IP. Hai sự thật README đã kiểm chứng từ tài liệu Redis:
- Replica **read-only theo mặc định** — ghi vào nó sẽ nhận lỗi `READONLY`.
- Replication là **bất đồng bộ** (asynchronous: master không chờ replica chép xong mới trả lời).
  Xem README, mục *Hai container Redis và replication*.

## 3. Ghi vào master, đọc từ replica

**Là gì.** Ứng dụng giữ **hai kết nối** riêng: lệnh ghi (`ZADD`) đi vào master, lệnh đọc
(`ZREVRANGE`) đi vào replica.

**Vì sao cần.** Mọi lệnh đều vào master thì replica chỉ là đồ trang trí. Cái giá: replication bất
đồng bộ nên `GET` ngay sau `POST` **có thể chưa thấy** điểm vừa ghi — bảng xếp hạng chấp nhận được.

**Trong bài này.** [`RedisConfig`](src/main/java/com/example/leaderboard/config/RedisConfig.java)
tạo hai **bean** (đối tượng do Spring tạo và quản lý) `StringRedisTemplate` (công cụ gửi lệnh
Redis của Spring Data Redis): `masterRedisTemplate` (cổng `6379`) và `replicaRedisTemplate` (cổng
`6380`). `LeaderboardService` nhận cả hai qua constructor, chọn đúng cái bằng `@Qualifier`.

## 4. Cache hai tầng L1 / L2

**Là gì.** **Cache** (bộ nhớ đệm: chỗ giữ tạm dữ liệu để đọc lại nhanh, khỏi hỏi DB) có thể xếp
thành nhiều tầng. **L1** là cache nằm trong bộ nhớ của **từng instance** (một tiến trình ứng dụng
đang chạy). **L2** là cache dùng chung cho mọi instance, ở đây nằm trên Redis master.

**Vì sao cần.** Đọc tầng gần trước, trượt mới xuống tầng sau, để ít phải hỏi DB.

**Trong bài này.** `GET /products/{id}` đi qua L1 → trượt thì L2 → trượt nữa thì DB.

- L1: [`ProductLocalCache`](src/main/java/com/example/leaderboard/service/ProductLocalCache.java) — một `ConcurrentHashMap<Long, ProductView>`.
- L2: `@Cacheable` trong `ProductService`, key trong Redis dạng `products::1`.
- DB: H2 in-memory, seed 3 sản phẩm trong [`data.sql`](src/main/resources/data.sql).

Cả L1 và L2 đều **không có TTL** (thời gian sống: hết hạn thì mục cache tự biến mất). Vì L1 không
tự hết hạn, nếu không ai xoá thì instance đó trả bản cũ mãi — lý do phần 2 cần Pub/Sub.

## 5. `@Cacheable`, `@CacheEvict` và proxy của Spring

**Là gì.** Hai **annotation** (chú thích gắn lên method để Spring xử lý thêm):
`@Cacheable("products")` — có trong cache thì trả luôn, chưa có thì chạy method rồi lưu kết quả;
`@CacheEvict("products")` — xoá mục tương ứng khỏi cache.

**Vì sao cần.** Không phải tự viết code đọc/ghi/xoá Redis cho cache. `@EnableCaching` trong
[`ProductCacheConfig`](src/main/java/com/example/leaderboard/config/ProductCacheConfig.java)
bật cơ chế này; `spring.cache.type: redis` trong [`application.yml`](src/main/resources/application.yml) chọn Redis làm nơi lưu.

**Trong bài này.** [`ProductService`](src/main/java/com/example/leaderboard/service/ProductService.java):

```java
@CacheEvict(CACHE)
public void delete(Long id) {
    products.deleteById(id);
}
```

Hai điều README đã kiểm chứng trong tài liệu Spring:
- `@CacheEvict` mặc định chạy **sau** khi method trả về thành công.
- Annotation chỉ chạy khi gọi qua **proxy** (lớp bọc Spring tạo quanh bean để chèn việc trước/sau
  method). Gọi từ **cùng một class** thì không qua proxy, annotation im lặng không chạy.

## 6. Redis Pub/Sub

**Là gì.** **Pub/Sub** (publish/subscribe: gửi/đăng ký nhận tin) là cơ chế nhắn tin của Redis. Bên
gửi **publish** một tin vào một **channel** (kênh, đặt tên bằng chuỗi); mọi bên đang **subscribe**
channel đó đều nhận được tin.

**Vì sao cần.** L1 nằm trong bộ nhớ của từng instance, instance khác không chạm vào được. Cách duy
nhất là **nhắn** cho chúng: "sản phẩm 2 đã bị xoá, tự xoá L1 đi".

**Trong bài này.** Channel `product-deletion`, nội dung tin là id sản phẩm. Gửi bằng
`master.convertAndSend(...)` trong `ProductFacade`. Nhận: `ProductCacheConfig` tạo một
`RedisMessageListenerContainer` subscribe channel đó, rồi
[`ProductDeletionListener`](src/main/java/com/example/leaderboard/service/ProductDeletionListener.java) xoá L1 trong `onMessage`.

README ghi: Pub/Sub của Redis là **at-most-once** (mỗi tin tới nhiều nhất một lần, có thể không
tới) — instance nào đang mất kết nối lúc tin được gửi thì mất tin đó vĩnh viễn.

## 7. Thứ tự các bước xoá — vì sao có `ProductFacade`

**Là gì.** Một bean riêng đứng giữa controller và `ProductService`, gọi các bước xoá theo đúng thứ
tự: DB → L2 → L1 của mình → báo instance khác.

**Vì sao cần.** Ghép hai điều ở mục 5: nếu gửi Pub/Sub **bên trong** `ProductService.delete` thì
tin đi **trước** khi `@CacheEvict` xoá L2. Instance khác nhận tin, xoá L1, gặp request ngay sau →
trượt L1 → đọc L2 vẫn còn → nạp lại sản phẩm đã xoá vào L1, và nó nằm đó mãi vì không có TTL.

**Trong bài này.** [`ProductFacade`](src/main/java/com/example/leaderboard/service/ProductFacade.java)
gọi `ProductService` từ bên ngoài (nên đi qua proxy), và chỉ gửi tin sau khi nó trả về (README,
mục *Vì sao có `ProductFacade`, và thứ tự xoá*):

```java
public void delete(Long id) {
    productService.delete(id);
    localCache.evict(id);
    master.convertAndSend(deletionChannel, String.valueOf(id));
}
```

## Hay nhầm

- **"`ZADD` cộng thêm điểm."** Sai — `ZADD` ghi đè: gọi lại với `300` thì điểm là `300`; muốn cộng
  dồn phải dùng `ZINCRBY`.
- **"Replica cũng ghi được."** Sai — replica read-only theo mặc định, ghi vào sẽ nhận
  `READONLY You can't write against a read only replica.`
- **"Vừa `POST` xong thì `GET` chắc chắn thấy điểm mới."** Không chắc — `GET` đọc replica, mà
  replication bất đồng bộ nên replica có thể chưa kịp chép.
- **"Gửi Pub/Sub ở đâu trong lúc xoá cũng được."** Sai — phải gửi sau khi `@CacheEvict` đã xoá L2,
  nên lệnh gửi nằm ở `ProductFacade`, không nằm trong `ProductService.delete`.

## Tự kiểm tra

**1. Vì sao lấy top N dùng `ZREVRANGE ... 0 n-1` chứ không phải `0 n`?**

<details><summary>Đáp án</summary>Vị trí trong Sorted Set đánh số từ <code>0</code> và khoảng <code>start stop</code> lấy cả hai đầu, nên <code>0</code> đến <code>n-1</code> là đúng <code>n</code> phần tử.</details>

**2. Container thứ hai biết master ở đâu bằng cách nào?**

<details><summary>Đáp án</summary>Qua tham số <code>--replicaof redis-master 6379</code>; <code>redis-master</code> là tên service trong compose, DNS nội bộ của compose network phân giải ra địa chỉ.</details>

**3. Vì sao phần 2 bắt buộc có Pub/Sub, chỉ `@CacheEvict` là chưa đủ?**

<details><summary>Đáp án</summary><code>@CacheEvict</code> chỉ xoá L2 (Redis, dùng chung). L1 nằm trong bộ nhớ riêng của từng instance và không có TTL, nên instance khác chỉ xoá được L1 của mình khi nhận tin qua channel <code>product-deletion</code>.</details>

**4. Nếu đặt `convertAndSend` vào cuối `ProductService.delete` thì chuyện gì có thể xảy ra?**

<details><summary>Đáp án</summary>Tin đi trước khi <code>@CacheEvict</code> xoá L2 (eviction chạy sau khi method trả về). Instance khác xoá L1, đọc lại thấy L2 vẫn còn, nạp sản phẩm đã xoá vào L1 và giữ nó mãi.</details>
