# ZombieTide v1.3.1 — "The Tuning Forge"

**Minecraft:** 1.21.1 · **NeoForge:** 21.1.x · **Kênh:** prerelease (jar đính kèm sau khi build)

Bản này **giữ nguyên 100% lối chơi, 91 mục config & mọi lệnh** của v1.3.0 — nhưng thay toàn
bộ cách các hệ thống đọc dữ liệu, nhằm mục tiêu duy nhất: **CPU mát hơn, GPU nhàn hơn, RAM
không phình**, kể cả ở đợt cuối với cấu hình 100.000 đợt và trần chỉ số nhân-100.

## ⚡ Trung tâm: ZTSnapshot (mới)

`dev.file233.zombietide.config.ZTSnapshot` bake, ngay khi config load/reload/sửa bằng lệnh:

- mọi scalar nóng (~60 giá trị) thành **field nguyên thủy**;
- toàn bộ toán tăng theo đợt và giữ **dạng công thức đóng O(1)** thay cho mảng tra: tốc độ, máu (chưa cap), sát
  thương (đã cap), tầm phát hiện/nghe (đã nhân hệ số trí tuệ), nhịp retarget, trí nhớ mất
  dấu, cooldown tiếng động, tỉ lệ gọi bạn, kháng knockback, trần sinh, cổng phá khối —
  kết quả bit-identical với bảng tra ở mọi maxWaves (đã kiểm chứng diff = 0.0), còn
  khoảng-nghỉ & thởi-lượng **riêng từng đợt** đọc thẳng override-map khi cần.

Hậu quả: AI tick của hàng trăm zombie, mỗi spawn attempt, gate combat/sense/spawn-filter và
khung hình client giờ chỉ còn **đọc field / 1 array-index** — không còn một map-lookup nào
của NeoForge config trong đường nóng. Snapshot tiêu tốn **≈ 0 byte heap** dù maxWaves = 100.000.

## 🖥 CPU (server)

- **`ZTHuntPlayerGoal`** (10 Hz/zombie): gate + cadence + trí nhớ đều là snapshot primitives;
  duyệt danh sách player trực tiếp, không stream, không cấp phát.
- **`WaveSpawner`**: tỉ lệ ngày/đêm giờ **co số lần thử sinh** (ban ngày tốn ít CPU hơn hẳn,
  thay vì gieo xúc xắc để vứt công quét); **đếm zombie quanh ngưởi tối đa 1 lần/chu kỳ** qua
  ring-buffer 32 slot cố định (bỏ map theo UUID, không GC, tự dọn ngưởi thoát); probe vị trí
  bằng `MutableBlockPos` tái sử dụng; khoảng-cách-no-spawn-bubble tính thuần tọa độ.
- **`ZombieMutation`**: thứ tự gate rẻ-trước; yardstick máu đối-thủ lấy *target sẵn có* của
  zombie (chỉ fallback duyệt player-list khi con đó chưa khóa ai — bỏ `getNearestPlayer`);
  sweep/đếm zombie có fast-path khi chỉ chơi overworld (mặc định).
- **`CombatRules`**: pool hiệu ứng cắn cache theo (epoch × wave) — một cú cắn chỉ là một
  dice-roll + một weighted-pick, không còn build list mỗi hit; gate đốt-nắng (chạy cho MỌI
  entity mỗi tick) đọc đúng 2 primitive.
- **`SpawnFilter` / `BlockBreakGoal` / `SenseEngine`**: ăn snapshot hết; BlockBreakGoal quét
  2 Hz bằng scratch mutable, không còn `new BlockPos[]`/`Vec3` mỗi lần.
- **`WaveManager`**: còi báo động + holder âm thanh cache theo chu kỳ; lọc dimension bị bỏ
  qua khi chỉ có overworld.
- **Ghi file config debounce 300 ms** — spam `/zombietide interval` hàng loạt không còn
  IO mỗi phát.

## 🎮 GPU/frame (client)

- **HUD**: chuỗi hiển thị + layout được **cache**; chỉ rebuild khi nội dung hiển thị đổi
  (tối đa 1 lần/giây — theo độ mịn đếm ngược) hoặc khi server sync / sửa config. Giữa hai
  lần rebuild: vài `fill` + 2 lệnh vẽ chữ.
- **Overlay máu (droplet)**: toàn bộ chòm máu (vị trí, kích thước, sắc độ, mờ) được gieo
  **một lần mỗi cú đánh** theo `splatterSeed` vào **mảng int phẳng**; mỗi frame chỉ là vài
  chục `fill` nguyên thủy — không RNG, không Gaussian, không cấp phát mảng. Tự rebuild khi
  đổi window/độ phân giải hay vặn dial droplet.
- Cả hai layer bỏ qua hoàn toàn khi F1 ẩn HUD; overlay thoát sớm khi trauma = 0.

## 🧠 RAM

- Không còn allocation nào do mod tự tạo trong tick loop / render loop (ngoài vật liệu
  Minecraft tự sinh: attribute map, navigation…).
- Ring-buffer spawn-cycle 32 slot × mấy long — cố định, không WeakHashMap.
- Snapshot công-thức-đóng: ≈ 0 byte (không mảng theo-đợt nào cả).
- **logo.png nén 1024²/2.1 MB → 512²/468 KB** — jar từ ~2.3 MB còn **~0.7 MB**, RAM decode ở
  danh sách mod 4 MB → 1 MB.

## 🔧 Khác

- Biên số `wave` của lệnh `/zombietide wave|interval|duration` nới 1000 → 100000 cho khớp
  range `waves.maxWaves` đã nhân-100.
- Sửa text README còn sót `calmMinutes` (đã bỏ từ v1.3.0).

## ✅ Không đổi

Gameplay, cấu hình mặc định, id âm thanh, lang EN/VI (162 khóa), lệnh, HUD, lối sinh đợt…
tất cả vận hành hệt v1.3.0 — chỉ nhanh hơn và nhẹ hơn.
