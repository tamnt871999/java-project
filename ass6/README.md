# ass6 — Redis trong Spring Boot: Leaderboard và xoá sản phẩm qua cache hai tầng

**Phần 1**

```
Xay dung Leaderboard
  - Su dung Sorted Set de luu diem so cua cac game thu
  - Viet mot REST API trong Spring Boot voi cac endpoint:
      POST /leaderboard/{username}  : Cap nhat diem cho mot user (su dung ZADD)
      GET  /leaderboard/top/{n}     : Lay ra top N game thu co diem cao nhat (su dung ZREVRANGE)

Note: Tao 2 container Redis, cau hinh mot cai lam Replica cua cai con lai, va sau do
      tich hop Redis vao mot ung dung Spring Boot de minh hoa.
```

**Phần 2**

```
Yeu cau: Mo rong ung dung demo de xu ly thao tac DELETE.
Chi tiet:
  1. Tao mot API DELETE /products/{id}.
  2. Implement logic trong ProductService de xoa san pham khoi DB.
  3. Su dung @CacheEvict de xoa san pham khoi L2 cache.
  4. Su dung Redis Pub/Sub (co the dung lai channel cu hoac tao channel moi product-deletion)
     de gui thong bao cho cac instance khac xoa san pham khoi L1 cache cua chung.
```

Bài loại **code**. Spring Boot thường (`controller/ service/ repository/ entity/ config/
exception/`), cổng **8083**, package gốc `com.example.leaderboard` — giữ nguyên tên từ phần 1 để
không phải đổi code phần 1.

---

## Chạy

Hai container Redis phải chạy trước — chúng là hạ tầng của bài, không phải thứ tuỳ chọn.

```bash
cd ass6
docker compose up -d        # redis-master :6379, redis-replica :6380
.\mvnw test                 # 22 test
.\mvnw spring-boot:run      # cong 8083
```

> `.\mvnw test` **cần hai container đang chạy**. Test đánh vào Redis thật chứ không mock, vì thứ
> đề yêu cầu kiểm chứng — sorted set và replication — không tồn tại trong một bản mock. Nếu quên
> `docker compose up -d`, test dừng ngay với thông báo chỉ đúng việc cần làm.

Dọn dẹp: `docker compose down -v`.

---

## Hai endpoint

### `POST /leaderboard/{username}` — ghi điểm bằng `ZADD`

```bash
curl -X POST http://localhost:8083/leaderboard/an \
     -H 'Content-Type: application/json' -d '{"score": 1500}'
```
```json
{"username":"an","score":1500.0}
```

`ZADD` **ghi đè** điểm chứ không cộng dồn — gọi lại với `300` thì điểm thành `300`, không phải
`1800`. Đây là ngữ nghĩa của `ZADD` và cũng là ý của từ *"cập nhật điểm"* trong đề. Muốn cộng dồn
thì phải là `ZINCRBY`, một lệnh khác.

### `GET /leaderboard/top/{n}` — top N bằng `ZREVRANGE`

```bash
curl http://localhost:8083/leaderboard/top/3
```
```json
[{"username":"dung","score":3100.0},
 {"username":"binh","score":2700.0},
 {"username":"em","score":2050.0}]
```

Dịch xuống Redis đúng một lệnh: `ZREVRANGE leaderboard:global 0 n-1 WITHSCORES`. Thứ hạng chính
là thứ tự phần tử trong mảng.

Chạy thật để đối chiếu:

```
$ docker exec ass6-redis-master redis-cli ZREVRANGE leaderboard:global 0 2 WITHSCORES
dung 3100 binh 2700 em 2050
$ docker exec ass6-redis-master redis-cli TYPE leaderboard:global
zset
```

> `ZREVRANGE` bị **deprecated từ Redis 6.2**, bản thay thế là `ZRANGE key start stop REV`
> ([nguồn](https://redis.io/docs/latest/commands/zrevrange/)). Đề chỉ đích danh `ZREVRANGE` nên
> bài dùng đúng lệnh đó; ghi lại đây để biết là đã cân nhắc chứ không phải không biết.

### Mã lỗi

| Tình huống | HTTP | `code` |
|---|---|---|
| Body thiếu `score`, hoặc JSON hỏng | `400` | `BAD_REQUEST` |
| `n` không phải số nguyên (`/top/abc`) | `400` | `BAD_REQUEST` |
| `n <= 0` — cú pháp đúng, nghiệp vụ từ chối | `422` | `BUSINESS_RULE_VIOLATED` |

Không có nhánh `404`: API này không tra cứu một tài nguyên theo id. Bảng xếp hạng rỗng trả
`200` với mảng rỗng, vì "chưa ai chơi" không phải là "không tìm thấy".

---

## Hai container Redis và replication

`docker-compose.yml` dựng hai container từ cùng image `redis:7.4-alpine`. Container thứ hai nhận
đúng một tham số để thành replica:

```yaml
command: ["redis-server", "--replicaof", "redis-master", "6379"]
```

`redis-master` là **tên service trong compose**, và DNS nội bộ của compose network phân giải nó
— không cần biết IP. Directive đúng tên là `replicaof`
([nguồn](https://redis.io/docs/latest/operate/oss_and_stack/management/replication/)).

Trạng thái thật sau khi `docker compose up -d`:

```
$ docker exec ass6-redis-master redis-cli INFO replication
role:master
connected_slaves:1
slave0:ip=172.19.0.3,port=6379,state=online,offset=0,lag=1

$ docker exec ass6-redis-replica redis-cli INFO replication
role:slave
master_host:redis-master
master_link_status:up

$ docker exec ass6-redis-replica redis-cli ZADD test 1 a
READONLY You can't write against a read only replica.
```

Hai điều đã kiểm chứng trong tài liệu chính thức, không suy đoán:

- **Replica là read-only theo mặc định.** *"Since Redis 2.6, replicas support a read-only mode
  that is enabled by default"*, điều khiển bằng `replica-read-only`. Nên lỗi `READONLY` ở trên là
  hành vi đúng chứ không phải cấu hình sai.
- **Replication là bất đồng bộ.** *"Redis uses by default asynchronous replication"*. Hệ quả nằm
  ngay ở mục dưới.

---

## Cách ứng dụng dùng hai Redis

**Ghi vào master, đọc từ replica.** Nếu cả hai endpoint đều đánh vào master thì replica chỉ là
đồ trang trí, và phần "Note" của đề không được minh hoạ gì cả.

`RedisConfig` tạo **hai** `StringRedisTemplate` trỏ vào hai cổng khác nhau, `LeaderboardService`
nhận cả hai qua constructor và dùng đúng cái cần:

| Việc | Template | Lệnh Redis |
|---|---|---|
| `POST` ghi điểm | `masterRedisTemplate` | `ZADD` |
| `GET` top N | `replicaRedisTemplate` | `ZREVRANGE` |

**Đánh đổi phải nói rõ:** vì replication bất đồng bộ, một lần `GET` ngay sau `POST` **có thể chưa
thấy** điểm vừa ghi. Với bảng xếp hạng thì chấp nhận được — chậm vài mili giây không ai nhận ra.
Với dữ liệu mà người ghi phải thấy ngay thì phải đọc từ master, và đó là một quyết định khác.

Test không lảng tránh chuyện này: trước mỗi lần kiểm tra, `RedisTestBase.doiReplicaBatKip()` đọc
`master_repl_offset` trên master và `slave_repl_offset` trên replica rồi chờ tới khi replica bắt
kịp. Chờ theo offset chứ không `sleep` một con số đoán bừa.

---

## Phần 2 — Xoá sản phẩm qua cache hai tầng

### Nền mà đề giả định đã có sẵn

Đề viết *"mở rộng ứng dụng demo"* và nói tới DB, L1, L2 như thể đã tồn tại — nhưng phần 1 không
có cái nào. Nên dựng **đúng những gì đề giả định, không hơn** (đã hỏi trước khi làm):

| Nền | Vì sao phải có |
|---|---|
| `Product` + H2, seed 3 sản phẩm trong `data.sql` | Đề nói *"xoá sản phẩm khỏi DB"* |
| `GET /products/{id}` có `@Cacheable` | Không có đường đọc thì không gì từng vào L2, và `@CacheEvict` không có gì để xoá |
| `ProductLocalCache` — L1 trong bộ nhớ của từng instance | Đề nói *"L1 cache của chúng"* |

Không có `POST` / `PUT`: dữ liệu đến từ seed, đề không đòi tạo hay sửa. Không dùng lại channel cũ
vì không có channel cũ nào — tạo mới `product-deletion` như đề cho phép.

### Bốn ý của đề nằm ở đâu

| Ý | Code |
|---|---|
| 1. `DELETE /products/{id}` | `ProductController.delete` → `204 No Content` |
| 2. `ProductService` xoá khỏi DB | `ProductService.delete` → `products.deleteById(id)` |
| 3. `@CacheEvict` xoá khỏi L2 | `@CacheEvict(CACHE)` trên chính `ProductService.delete` |
| 4. Pub/Sub báo instance khác xoá L1 | `ProductFacade.delete` gửi lên `product-deletion`; `ProductDeletionListener` ở mỗi instance nhận và xoá L1 của nó |

Đọc đi qua ba tầng: **L1** (`ProductLocalCache`) → trượt thì **L2** (`@Cacheable`, Redis master) →
trượt nữa thì **DB**. Key L2 trong Redis có dạng `products::1` — đã kiểm bằng `KEYS 'products*'`.

### Vì sao có `ProductFacade`, và thứ tự xoá

Hai điều đã kiểm chứng trong
[tài liệu Spring](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)
ép code phải có hình dạng này:

- `@CacheEvict` mặc định chạy **sau** khi method trả về: *"Once the method completes
  successfully, an action (in this case, eviction) on the cache is run."*
- Gọi method có annotation từ **cùng một class** thì không đi qua proxy, annotation im lặng không
  chạy.

Nếu gửi Pub/Sub **bên trong** `ProductService.delete`, tin đi **trước** khi L2 bị xoá. Instance
khác nhận tin, xoá L1, rồi gặp một request ngay sau đó → trượt L1 → đọc L2 **vẫn còn** → nạp lại
sản phẩm đã xoá vào L1, và nó nằm đó mãi vì L1 không có TTL.

Nên lệnh gửi phải nằm ở bean **gọi** `ProductService.delete`, chạy sau khi nó trả về.
`ProductFacade.delete` làm đúng ba bước, đúng thứ tự:

```
1. productService.delete(id)   -> xoa DB, roi @CacheEvict xoa L2 khi method tra ve
2. localCache.evict(id)        -> instance nay tu xoa L1 cua minh, khong cho Pub/Sub
3. convertAndSend(product-deletion, id)  -> cac instance KHAC xoa L1 cua chung
```

Bước 2 làm trực tiếp vì đề nói Pub/Sub là để báo **"các instance khác"**. Instance đang xử lý
DELETE thì tự xoá L1 của mình ngay, nên một `GET` gọi liền sau `DELETE` trên cùng instance không
bao giờ thấy bản cũ.

### Mã lỗi phần 2

| Tình huống | HTTP | `code` |
|---|---|---|
| `GET` sản phẩm không tồn tại | `404` | `PRODUCT_NOT_FOUND` |
| `id` không phải số (`/products/abc`) | `400` | `BAD_REQUEST` |
| `DELETE` sản phẩm không tồn tại | `204` | — |

`DELETE` lên sản phẩm không tồn tại vẫn trả `204` vì `DELETE` là idempotent: xoá hai lần thì kết
quả cuối như nhau. `deleteById` của Spring Data JPA bản trong bài không ném lỗi khi id không tồn
tại — đã kiểm bằng test `xoaSanPhamKhongTonTaiVanTra204`.

### Chạy hai instance thật

Test giả lập instance thứ hai trong cùng JVM. Để thấy bằng mắt, chạy **hai JVM** ở hai cổng. Hai
instance phải dùng chung một DB — H2 in-memory thì mỗi JVM một bản riêng — nên truyền URL file H2
có `AUTO_SERVER=TRUE`:

```bash
cd ass6
docker compose up -d
.\mvnw package -DskipTests
java -jar target/ass6-1.0.0.jar --server.port=8083 "--spring.datasource.url=jdbc:h2:file:./target/products-db;AUTO_SERVER=TRUE"
java -jar target/ass6-1.0.0.jar --server.port=8084 "--spring.datasource.url=jdbc:h2:file:./target/products-db;AUTO_SERVER=TRUE"
```

Kết quả chạy thật, kèm một `redis-cli SUBSCRIBE product-deletion` nghe độc lập:

```
$ redis-cli PUBSUB NUMSUB product-deletion
product-deletion 3                       # instance A + instance B + redis-cli

GET  :8083/products/2  -> 200 {"id":2,"name":"Chuot khong day","price":450000.00}
GET  :8084/products/2  -> 200            # B nap san pham 2 vao L1 cua B
EXISTS products::2     -> 1

DELETE :8083/products/2 -> 204

EXISTS products::2     -> 0              # y 3: @CacheEvict xoa L2
GET  :8084/products/2  -> 404 PRODUCT_NOT_FOUND
GET  :8083/products/2  -> 404 PRODUCT_NOT_FOUND

redis-cli bat duoc:  message  product-deletion  2
```

Dòng đáng nhìn nhất là `GET :8084 -> 404`. Ngay trước đó B vừa nạp sản phẩm 2 vào L1 của nó, và
L1 không có TTL. B chỉ trả `404` được nếu L1 của nó đã bị xoá — tức là đã nhận được tin qua
Pub/Sub. Không nhận được tin thì B sẽ tiếp tục trả bản cũ `200` mãi mãi.

> Pub/Sub của Redis là **at-most-once**: instance nào đang mất kết nối lúc tin được gửi thì *"the
> message is forever lost"* ([nguồn](https://redis.io/docs/latest/develop/pubsub/)). Test ý 4 vì
> vậy chờ instance thứ hai **subscribe xong** rồi mới gửi `DELETE`, không gửi mò.

---

## Test

22 test, xanh từ clean build (cần hai container đang chạy).

**`LeaderboardApiTest`** — 9 test end-to-end qua `MockMvc`: thứ tự giảm dần đúng, `top/N` cắt
đúng N phần tử, `ZADD` lại thì ghi đè, bảng rỗng trả mảng rỗng, và bốn nhánh lỗi `400` / `422`.

**`RedisReplicationTest`** — 4 test cho đúng phần "Note" của đề:

| Test | Chứng minh điều gì |
|---|---|
| `vaiTroHaiContainerDungNhuCauHinh` | `role:master` / `role:slave` / `master_link_status:up` |
| `duLieuChayTuMasterSangReplica` | Điểm `ZADD` vào master xuất hiện trên replica |
| `replicaTuChoiGhi` | Replica trả `READONLY` — là bản sao thật, không phải Redis thứ hai rời rạc |
| `apiDocTuReplicaChuKhongPhaiMaster` | `GET` thật sự đọc replica |

Test cuối đáng nói: nó tạm bật `replica-read-only no`, ghi một thành viên **chỉ tồn tại trên
replica**, xác nhận master không có thành viên đó, rồi gọi `GET /leaderboard/top/10` và thấy nó
trong kết quả. Nếu endpoint đọc master thì thành viên này không thể xuất hiện. Dọn dẹp trong
`finally` và trả `replica-read-only` về `yes`.

**`ProductDeletionTest`** — 9 test cho phần 2. Trước mỗi test nạp lại `data.sql` và làm rỗng cả L1
lẫn L2, nên test nào xoá sản phẩm cũng không ảnh hưởng test sau.

| Test | Chứng minh điều gì |
|---|---|
| `docLanDauNapVaoCaL1LanL2` | Nền: một lần `GET` nạp sản phẩm vào cả L1 lẫn L2 |
| `xoaTra204` | Ý 1 |
| `xoaThiSanPhamMatKhoiDb` | Ý 2 — `existsById` từ `true` thành `false` |
| `xoaThiSanPhamMatKhoiL2` | Ý 3 — L2 có trước khi xoá, mất sau khi xoá |
| `xoaGuiThongBaoChoInstanceKhacXoaL1` | Ý 4 — xem dưới |
| `instanceXuLyXoaTuGoL1CuaMinh` | Instance đang xử lý `DELETE` tự xoá L1 của nó |
| `sauKhiXoaDocLaiTra404` | Đi lại cả ba tầng sau khi xoá: không tầng nào còn |
| `xoaSanPhamKhongTonTaiVanTra204` | `DELETE` idempotent |
| `idKhongPhaiSoTra400` | Nhánh `400` |

Test ý 4 dựng một **instance thứ hai**: một `ProductLocalCache` riêng đã có sẵn sản phẩm 2, cùng
một `RedisMessageListenerContainer` riêng subscribe `product-deletion`. Nó chờ container đó
`isListening()` xong mới gửi `DELETE /products/2` vào ứng dụng, rồi kiểm tra chính L1 của instance
thứ hai đã mất sản phẩm 2. L1 đó không dính dáng gì tới ứng dụng ngoài đường Pub/Sub, nên nó chỉ
mất sản phẩm được khi tin nhắn thật sự đi qua Redis.

---

## Giả định của người làm bài

Những điểm đề không nói, tự quyết:

- **Một bảng xếp hạng toàn cục**, key `leaderboard:global` (đổi bằng `app.leaderboard.key`). Đề
  không nhắc tới nhiều mùa giải hay nhiều bảng.
- **Điểm là số thực** — `ZADD` của Redis nhận score kiểu double.
- **Không có TTL, không reset theo mùa, không xác thực người gọi.** Đề không yêu cầu.
- Test dùng key riêng `leaderboard:test` để không xoá dữ liệu của bảng thật khi chạy chung Redis.
- **Phần 2:** dữ liệu sản phẩm là 3 dòng seed trong `data.sql`. Giá dùng `BigDecimal`. L1 và L2
  đều **không có TTL** — đề không nhắc, và chính vì L1 không tự hết hạn nên ý 4 mới bắt buộc phải
  có Pub/Sub. L2 nằm trên Redis master.
- `DELETE` sản phẩm không tồn tại trả `204` thay vì `404` — chọn theo ngữ nghĩa idempotent của
  `DELETE`, đề không nói.
