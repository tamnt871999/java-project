# ass6 — Leaderboard trên Redis Sorted Set

```
Xay dung Leaderboard
  - Su dung Sorted Set de luu diem so cua cac game thu
  - Viet mot REST API trong Spring Boot voi cac endpoint:
      POST /leaderboard/{username}  : Cap nhat diem cho mot user (su dung ZADD)
      GET  /leaderboard/top/{n}     : Lay ra top N game thu co diem cao nhat (su dung ZREVRANGE)

Note: Tao 2 container Redis, cau hinh mot cai lam Replica cua cai con lai, va sau do
      tich hop Redis vao mot ung dung Spring Boot de minh hoa.
```

Bài loại **code**. Spring Boot thường (`controller/ service/ config/ exception/`), cổng **8083**,
package gốc `com.example.leaderboard`.

---

## Chạy

Hai container Redis phải chạy trước — chúng là hạ tầng của bài, không phải thứ tuỳ chọn.

```bash
cd ass6
docker compose up -d        # redis-master :6379, redis-replica :6380
.\mvnw test                 # 13 test
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

## Test

13 test, xanh từ clean build (cần hai container đang chạy).

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

---

## Giả định của người làm bài

Những điểm đề không nói, tự quyết:

- **Một bảng xếp hạng toàn cục**, key `leaderboard:global` (đổi bằng `app.leaderboard.key`). Đề
  không nhắc tới nhiều mùa giải hay nhiều bảng.
- **Điểm là số thực** — `ZADD` của Redis nhận score kiểu double.
- **Không có TTL, không reset theo mùa, không xác thực người gọi.** Đề không yêu cầu.
- Test dùng key riêng `leaderboard:test` để không xoá dữ liệu của bảng thật khi chạy chung Redis.
