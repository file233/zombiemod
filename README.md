<p align="center">
  <img src="src/main/resources/logo.png" alt="ZombieTide logo" width="220"/>
</p>

<h1 align="center">ZombieTide — Đại Dịch Zombie</h1>

<p align="center">
  <a href="https://github.com/file233/zombiemod/releases/tag/v1.3.1"><b>⬇ Tải bản mới nhất: v1.3.1</b></a><br/>
  <b>Minecraft 1.21.1 · NeoForge 21.1.x · Java 21</b><br/>
  Hệ thống đợt tấn công leo thang, AI zombie được tái thiết hoàn toàn, HUD chiến thuật siêu nhỏ,
  cấu hình 100% trong game và bằng lệnh, tối ưu hiệu năng cho server.<br/>
  <i>An escalating 50-wave zombie apocalypse with rebuilt horde AI, a tiny tactical HUD,
  fully in-game configuration, and server-friendly performance.</i>
</p>

---

## ✨ Tính năng (Features)

| Đặc tả (the brief) | Thực hiện trong ZombieTide | Chỉnh ở đâu |
|---|---|---|
| Tái tạo AI zombie, thuật toán tối ưu | Goal săn ngưởi chơi **không cần nhìn thấy** (mũi + tai), quét có nhịp, tăng tốc bằng staggered-tick | `zombies.*` |
| Zombie nhạy bén, điên cuồng, **nghe thấy ngưởi chơi** | "Bộ tai" thu tiếng: chạy nước rút, đào khối, ăn uống, mở cửa/rương, bắn tên, nổ TNT… mỗi hành động một độ ồn riêng | `zombies.hearingRadius`, `zombies.noiseCooldownTicks` |
| Ngày thưởng zombie nhạy hơn, **trong đợt điên cuồng**, **sau mỗi đợt thông minh hơn** | Doctrine 2 trạng thái (bình thưởng / cuồng loạn); tầm phát hiện, thính giác, tốc độ, gọi bạn, kháng knockback tăng theo số đợt | `zombies.followRangePerWave`, `hearingPerWave`, `speedPerWave`, `reinforcementFromWave` |
| Đợt đầu **8 phút**, mỗi đợt **+2 phút**, mặc định **50 đợt** | Máy trạng thái đợt chính xác: nghỉ → báo động → vây hãm | `waves.maxWaves=50`, `firstWaveMinutes=8`, `waveIncrementMinutes=2` |
| Trong đợt zombie **không cháy nắng**, đi lại bình thưởng | Tắt lửa từ nắng mỗi tick + gỡ goal trốn nắng trong lúc đợt | `zombies.noBurningDuringWaves` |
| **Sinh cả ban ngày** khi đang trong đợt, dùng cơ chế sinh tự nhiên | Engine sinh dùng đúng placement predicate của vanilla (`SpawnPlacementTypes.ON_GROUND`) + cho phép placement bỏ qua ánh sáng | `spawning.daylightSpawn`, `boostNaturalPlacement` |
| **Báo động ~5 giây** trước khi vào đợt (kèm âm thanh dài tròn 5s) | Còi báo động custom `zombietide:wave_alarm.ogg` dài đúng 5.0s + title + chat | `waves.alarmSeconds=5`, `waves.alarmSound` |
| Trong đợt **chỉ zombie thưởng** được sinh; quái khác & biến thể tỉ lệ **cực thấp**; zombie con/nước/lây nhiễm cũng cực hiếm | Bộ lọc `FinalizeSpawnEvent`: biến thể → đổi thành zombie thường, quái khác → hủy 98% | `variantKeepChance=0.02`, `babyKeepChance=0.02`, `nonZombieKeepChance=0.02` |
| Zombie gây **tối đa 2 tim** sát thương | Hard-cap ở `LivingIncomingDamageEvent` (mọi biến thể zombie đều bị kẹp) | `zombies.maxDamageHearts=2.0` |
| Tốc độ zombie nhanh nhất **gấp 2 lần ngưởi chơi** | Trần speed attribute mặc định = `0.20` (ngưởi chơi `0.10`) — không gđoạn nào vượt được | `zombies.maxSpeed=0.20` |
| **Không mặc giáp, chỉ cầm khối** | Tước giáp mỗi spawn (sau khi vanilla trang bị hệ), vũ khí bị tước, 35% cầm khối ngẫu nhiên, không nhặt đồ | `zombies.stripArmor`, `allowOnlyBlockItems`, `heldBlockChance`, `heldBlocks` |
| **Từ đợt 20** một số zombie có thể **phá khối** | `BlockBreakGoal`: nhai khối chặn đường bằng cơ chế vanilla (vết nứt + tiếng), tôn trọng `mobGriefing` | `blockBreakFromWave=20`, `blockBreakChance=0.35`, `blockBreakMaxHardness`, `blockBreakBlacklist` |
| Bị vài con đánh trúng → **hiệu ứng xấu ngẫu nhiên** | Pool hiệu ứng có trọng số, mở khóa độc hơn theo đợt (wither từ đợt 30, darkness từ đợt 40) | `effects.list`, `effects.procChance=0.35` |
| Sát thương → **hạt pixel máu thưa bắn lên màn hình** | Mỗi cú đánh bắn **vài chục hạt máu pixel** từ 2–4 điểm va chạm (seed ổn định → không flicker), rụng dần khi trauma hạ — ít nhưng đau | `damageOverlay.intensity`, `maxAlpha`, `fadePerTick`, `dropletCount=22`, `dropletSize`, `dropletSpread` |
| **Bám theo cả ngưởi chơi chế độ Sáng tạo** | Goal săn tự viết lại (không qua bộ lọc creative của vanilla) + cảm biến + engine sinh đều tôn trọng công tắc; spectator là tùy chọn riêng | `targeting.targetCreativePlayers`, `targeting.targetSpectators`, `spawning.pressureCreativePlayers` |
| **Máu zombie tối đa = ngưởi chơi + 5 tim** | Trần `MAX_HEALTH` = maxHealth ngưởi chơi gần nhất + `healthMaxHeartsAbovePlayer` (mặc định 5 tim → trâu nhất 15 tim); tăng dần theo đợt nhưng không bao giờ qua trần | `zombies.healthBaseHearts`, `healthPerWaveHearts`, `healthMaxHeartsAbovePlayer` |
| **Khoảng-đợt chỉnh TỪNG ĐỢT RIÊNG (không knob chung)** | Bỏ hẳn calmMinutes áp-dụng-cho-tất-cả; mọi đợt mặc định 600s, mỗi đợt một override `"đợt=giây"` hoặc lệnh trực tiếp trong game | `waves.intervalOverrides` + `/zombietide interval`, `duration` |
| **maxWaves đổi → tự cân bằng, đợt cuối luôn khó nhất** | Mọi tăng trưởng chạy trên `designWave = wave × 50/maxWaves` — 10 hay 500 đợt thì đỉnh khó vẫn chuẩn wave-50 thiết kế | tự động theo `waves.maxWaves` |
| **Chỉnh ĐỘ THÔNG MINH zombie** | Nhóm `intelligence.*`: tăng theo đợt, ảnh hưởng tầm phát hiện, bán kính nghe, độ trễ phản ứng, tốc độ re-target, trí nhớ mục tiêu | `intelligence.baseLevel=1`, `perWaveBonus=0.04`, `unseenMemoryTicks=60` |
| **Chỉnh ĐỘ ĐIÊN CUỒNG trong đợt** | Nhóm `frenzy.*`: một multiplier nhân mọi buff trong đợt (tầm phát hiện, tốc độ (+trần), thính giác, KBR, gọi bạn, phản ứng) | `frenzy.intensity=1`, `speedBoost`, `hearingBonus`, `noiseReactionFactor` |
| **Ban ngày sinh ít hơn 2 lần** | Hệ số sinh ban ngày so với ban đêm; 0.5 = thưa hơn đúng 50% giữa trưa (đêm vẫn full) | `spawning.daySpawnFactor=0.5` |
| **HUD cực nhỏ** giữa cạnh trên: thanh tiến trình + đếm **ngày/giờ/phút/giây** | Lớp HUD scale 0.7 mặc định, thởi nhịp tim, màu trạng thái | `hud.*` |
| Trong đợt HUD **đếm ngược hết đợt** từng giây, hết đợt quay lại đếm ngày | Thanh tiến trình: bình thường "đầy dần", chiến tranh "rỗng dần" | `hud.timeFormat` |
| **Logo riêng, làm như mod chuyên nghiệp** | Emblem độc quyền 1024² (tay zombie + vòng biohazard), mods.toml đầy đủ | `logo.png` |
| **Config chỉnh mọi thứ trong game** (ấn Mod → Config) | **91 mục** trong COMMON + CLIENT, mở qua màn hình cấu hình bản địa của NeoForge, có dịch EN/VI | `zombietide-*.toml` |
| **Lệnh chỉnh mọi thứ + gọi/reset đợt** | `/zombietide` | xem dưới |
| **Tối ưu CPU/GPU/RAM có hệ thống (v1.3.1)** | Snapshot config dạng công-thức-đóng O(1) (~0 RAM), cache-render HUD/overlay, ring-buffer đếm zombie, debounce ghi đĩa | ⚡ Hiệu năng |

---

## 🧠 Hệ AI được tái thiết như thế nào

1. **ZTHuntPlayerGoal** — `NearestAttackableTargetGoal` với `mustSee=false`: zombie khóa mục tiêu xuyên tường trong tầm `followRange` (tăng dần theo đợt). Không xóa goal vanilla — chồng thêm nên tương thích mod khác.
2. **SenseEngine** — bản đồ "độ ồn" (loudness) nhân với bán kính nghe (vốn được `intelligence`/`frenzy` scale): chạy 0.75×, đi bộ 0.45×, bới 0.15×, đào khối 1.0×, đánh nhau 0.7×, nổ 3.0×… Zombie chưa có mục tiêu sẽ hóng theo tiếng động; mỗi con có cooldown riêng (attachment, không lưu đĩa) nên không bị spam.
3. **Frenzy & Intelligence theo pha** — trong đợt: mọi buff điên cuồng được nhân bởi `frenzy.intensity` (tầm phát hiện +16 × frenzy, tốc độ +, KBR ×frenzy, gọi bạn từ đợt 6, thính giác +8), **không trốn nắng, không cháy**; hết đợt trả lại hành vi tránh nắng vanilla. Ngược lại, `intelligence` tăng dần vĩnh viễn theo số đợt — càng sống lâu, lũ zombie càng đọc vị bạn (re-target nhanh hơn, nhớ lâu hơn, nghe xa hơn).
4. **BlockBreakGoal (đợt 20+)** — khi navigation "bó tay", zombie soi khối giữa nó và con mồi (kể cả khối ngang tầm mắt/cửa), gặm có tiếng + crack-progress, xong hồi chiêu; tôn trọng độ cứng & blacklist, `mobGriefing=false` thì thôi.

## 🌊 Vòng đợt chuẩn xác

```
bình yên (mặc định 600s — hoặc override RIÊNG của đợt N)
   └─[5s còi báo động]→ ĐỢT N (8 + 2×(N-1) phút) → bình yên → …
```

- Đợt 1 = **8 phút**, đợt 50 = **106 phút**. Sau đợt 50: `waves.afterLastWave = CONTINUE | LOOP | STOP`.
- Khoảng nghỉ & độ dài **chỉ chỉnh theo từng đợt riêng** (override `"đợt=giây"` hoặc lệnh `interval`/`duration`) — không còn knob áp dụng cho tất cả; mặc định mỗi đợt nghỉ 600s.
- Trạng thái lưu trong `SavedData` của overworld → tắt server không mất tiến trình.
- NGOẠI LỆ: không có zombie nào nếu difficulty = Peaceful và vẫn tôn trọng `doMobSpawning` (tắt được).

## ⌨️ Lệnh (Commands)

| Lệnh | Quyền | Tác dụng |
|---|---|---|
| `/zombietide status` | mọi ngưởi | Đợt hiện tại, thởi gian còn lại, số zombie, **chỉ số thông minh/điên cuồng** |
| `/zombietide start [instant]` | OP | Gài còi gọi đợt kế (`instant` = bỏ báo động) — alias `/zombietide summon` |
| `/zombietide end` | OP | Kết thúc đợt đang chạy |
| `/zombietide wave <n> [instant]` | OP | Nhảy thẳng tới đợt n |
| `/zombietide interval [n] [giây\|clear]` | xem: mọi ngưởi; sửa: OP | **Khoảng nghỉ trước từng đợt**: xem hiệu lực / đặt / xóa override |
| `/zombietide duration [n] [giây\|clear]` | xem: mọi ngưởi; sửa: OP | **Độ dài từng đợt**: xem hiệu lực / đặt / xóa override |
| `/zombietide reset` | OP | **Reset về ngày đầu** (đợt 1, đếm lại từ đầu) |
| `/zombietide pause` / `resume` | OP | Đóng băng / chạy tiếp chu kỳ |
| `/zombietide config list [lọc]` | mọi ngưởi | Liệt kê 73 khóa config có bridge |
| `/zombietide config get <key>` | mọi ngưởi | Đọc giá trị |
| `/zombietide config set <key> <giá_trị>` | OP | Đổi + **lưu thẳng vào file config**, hiệu lực ngay |

Alias rút gọn: `/zt …`.

## ⚙️ Cấu hình trong game

`Esc → Mods → ZombieTide → Config` — màn hình cấu hình bản địa của NeoForge, đầy đủ nhãn song ngữ (EN/VI).
**91 mục** chia thành các nhóm:

### `zombietide-common.toml` (79 mục — chỉnh trong SP & cả server qua lệnh)

| Nhóm | Nội dung | Tiêu biểu |
|---|---|---|
| `general` | Công tắc tổng | `enabled` |
| `waves` | Chu kỳ đợt, báo động, **override từng đợt (không knob chung)** | `maxWaves`, `firstWaveMinutes`, `waveIncrementMinutes`, `intervalOverrides`, `durationOverrides`, `alarmSeconds/Sound/Volume/Pitch`, `afterLastWave` |
| `zombies` | Giới hạn & tăng trưởng zombie | `maxSpeed=0.12`, `maxDamageHearts=2`, `healthBaseHearts/PerWave/MaxAbovePlayer=5`, `stripArmor`, `heldBlocks`, `blockBreak*`, `variantKeepChance=0.02`… |
| `targeting` | Ai bị săn | `targetCreativePlayers=true`, `targetSpectators=false` |
| `intelligence` | **Độ thông minh** (tăng theo đợt) | `baseLevel`, `perWaveBonus`, `maxLevel`, `unseenMemoryTicks` |
| `frenzy` | **Độ điên cuồng trong đợt** | `intensity=1`, `speedBoost`, `hearingBonus`, `noiseReactionFactor` |
| `spawning` | Engine sinh đợt | `ringMin/Max=24–48`, `capPerPlayer`, `daylightSpawn`, `daySpawnFactor=0.5`, `pressureCreativePlayers`, `dimensions` |
| `effects` | Hiệu ứng xấu khi bị đánh | `procChance=0.35`, `amplifier`, `list` (pool theo đợt) |

### `zombietide-client.toml` (12 mục — mỗi máy tự chỉnh)

| Nhóm | Nội dung | Tiêu biểu |
|---|---|---|
| `hud` | Mini-HUD trên cùng | `enabled`, `scale=0.7`, `offsetY`, `showZombieCount`, `timeFormat` |
| `damageOverlay` | **Hạt máu pixel thưa** | `intensity`, `maxAlpha=0.85`, `fadePerTick`, `dropletCount=22`, `dropletSize=3`, `dropletSpread=70` |

Mọi con số "chuẩn đặc tả" là **mặc định được ghim sẵn**, và **toàn bộ range chỉnh đã được nới ×100** (muốn quái đà cũng được) — mod vẫn kẹp theo trần an toàn (ví dụ zombie không bao giờ qua `maxSpeed` dù buff thế nào).

## 🔊 Âm thanh báo động custom

`zombietide:wave_alarm` — còi không quân 5.0 giây được tổng hợp riêng cho mod (3 nhịp lên-xuống), phát đúng lúc T-5s trước mỗi đợt tới từng ngưởi chơi. Đổi sang âm khác bằng `waves.alarmSound`.

## ⚡ Hiệu năng — bộ máy được tối ưu từng đường nóng (v1.3.1)

Bản 1.3.1 giữ nguyên 100% lối chơi & 91 mục config, nhưng thay toàn bộ cách các hệ thống
*đọc* dữ liệu, hướng tới mục tiêu: **server 50+ zombie trong đợt cuối vẫn êm, client không
tụt FPS, RAM không phình**.

**Trung tâm: `ZTSnapshot`.** NeoForge config getter là một map-lookup mỗi lần gọi — vô hại
khi gọi 1 lần/giây, nhưng hàng trăm zombie × hàng chục giá trị × 20 tick/giây thì không.
Snapshot bake *mọi* scalar nóng + **toàn bộ toán tăng trưởng theo đợt** (tốc độ, máu, sát
thương, tầm phát hiện/nghe, nhịp retarget, trí nhớ, cooldown tiếng động, cổng phá khối,
trần sinh) — mọi đường cong theo đợt là **tuyến tính** nên được giữ dạng **công thức đóng O(1)**:
mỗi lần đọc = vài phép nhân-cộng trên field nguyên thủy, RAM tiêu tốn ≈ 0 byte (không màng
nào, kể cả maxWaves = 100.000), rebuild duy nhất khi config load/reload/sửa lệnh. Không còn lookup trong
bất kỳ AI tick / spawn attempt / frame render nào.

**CPU (server):**
- **AI săn ngưởi 10 Hz**: gate bằng snapshot primitives; nhịp retarget & trí nhớ theo đợt lấy từ mảng precompute.
- **Máy sinh đợt**: giữ lệch pha từng ngưởi (không spike tick); tỉ lệ ngày/đêm giờ **co số lần thử sinh** thay vì gieo xúc xắc vứt bỏ công quét (ban ngày rẻ hơn hẳn về CPU); đếm zombie-quanh-ngưởi **tối đa 1 lần mỗi chu kỳ** qua ring-buffer 32 slot thay thế UUID-map (không GC, tự dọn ngưởi thoát); probe vị trí dùng `MutableBlockPos` tái sử dụng.
- **Mutation**: thứ tự gate rẻ-trước (strip giáp/tay không rồi mới tới registry), yardstick máu-đối-thủ đọc *target hiện tại* của zombie thay cho `getNearestPlayer`, sweep/đếm zombie có fast-path overworld-only (mặc định).
- **Cảm biến tiếng động / luật combat / lọc spawn / goal phá khối** đều ăn snapshot; pool hiệu ứng cắn cache theo (epoch×wave) — một cú cắn chỉ là 1 dice-roll + 1 weighted-pick.
- **Ghi file config có debounce 300 ms** — `/zombietide interval` chạy hàng loạt cũng không IO-spam ổ đĩa.

**GPU/frame (client):**
- **HUD**: dựng sẵn chuỗi + layout, chỉ rebuild khi gì đó *hiển thị đổi* (tối đa 1 lần/giây theo độ mịn đếm ngược, hoặc ngay khi server sync / sửa config). Giữa hai lần rebuild: vài `fill` + 2 lệnh vẽ chữ — hết.
- **Overlay máu**: chòm droplet (vị trí, kích thước, sắc độ, độ mờ) được gieo **một lần mỗi cú đánh** theo seed vào mảng int phẳng; mỗi frame chỉ là vài chục `fill` thuần, không RNG, không Gaussian, không cấp phát. Tự rebuild khi đổi độ phân giải hoặc vặn dial.
- Cả hai layer đều tôn trọng ẩn HUD (F1) và không chạy khi trauma = 0.

**RAM:** snapshot chiếm **≈ 0 byte** (công thức đóng, không mảng theo-đợt); không còn allocation trong tick/render loop của mod; logo mod nén 2.1 MB → 0.47 MB (jar chỉ ~0.7 MB, decode RAM 4 MB → 1 MB); cache phân tích config giữ nguyên cơ chế volatile-một-bản.

**Kết tinh cũ vẫn giữ:** stagger theo chu kỳ, 1 zombie/ngưởi/chu kỳ, trần sống rõ ràng, tai 2.5 Hz + sự kiện rởi rạc, HUD sync đúng 1 packet/giây.

## 📦 Cài đặt

**Tải JAR:** [github.com/file233/zombiemod/releases/tag/v1.3.1](https://github.com/file233/zombiemod/releases/tag/v1.3.1)

1. Minecraft **1.21.1** + NeoForge **21.1.x** (khuyến nghị ≥ 21.1.100).
2. Thả `zombietide-1.3.1.jar` vào thư mục `mods/`.
3. Vào game — đợt 1 sẽ đến sau 600 giây nghỉ mặc định (chỉnh riêng từng đợt bằng `/zombietide interval`). Chúc sống sót.

## 🛠 Build từ mã nguồn

```bash
./gradlew build      # → build/libs/zombietide-1.3.1.jar
./gradlew runClient  # chạy thử client
./gradlew runServer  # chạy thử server headless
```

CI (GitHub Actions) tự build & phát hành JAR tại mục **Releases** của repo.
Workflow đóng gói sẵn tại `ci/build.yml.template`. Khi nào token GitHub được cấp quyền
`workflows` (kết nối lại GitHub trong Arena), chỉ cần đổi tên/di chuyển file về
`.github/workflows/build.yml` là pipeline tự chạy: build JAR, upload artifact và đính kèm
JAR vào release theo tag.

---

## 🇬🇧 English summary

**ZombieTide** turns survival into an escalating siege: 50 waves (first = 8 minutes, each +2 minutes), 5-second air-raid sirene before every assault, zombies that hunt by sound and smell (no line-of-sight needed), siege spawning that works at high noon, sunburn immunity mid-wave, 2-heart damage cap, 2×-player speed ceiling, armorless block-carrying ghouls, block-breaking from wave 20, weighted harmful-effect bites, sparse pixel blood-droplets peppering your screen on every bite, zombies that stalk even creative-mode players, zombie health hard-capped at player + 5 hearts, per-wave-only calm-gap & duration editing in-game (`/zombietide interval|duration`, no global knob), designWave auto-balancing so the LAST wave is always the hardest whatever maxWaves you set, intelligence & frenzy dials, daylight spawning thinned to half of night, and a tiny top-center HUD counting days/hours/minutes/seconds. Everything — 91 config entries with ×100-widened ranges covering every radius, cap, chance, list, per-wave timing, intelligence and frenzy — is tunable live, via `/zombietide config` or the NeoForge config screen.

**Performance (v1.3.1):** the whole engine was rebuilt around a config **snapshot** — every scalar plus every per-wave growth curve (speed, health, damage, reach, retarget cadence, memory, noise cooldowns, breach gates, spawn caps) is kept in closed O(1) form — every wave curve is linear, so reads are a multiply-add on plain fields with ~0 bytes of heap, refreshed the moment config loads or changes. On top of that: the spawn engine scales daylight *attempt counts* instead of dice-rolling work away, counts nearby zombies at most once per cycle via a tiny fixed ring buffer, and probes positions with reusable mutable block-positions; mutations pick the zombie's existing target instead of a nearest-player search; config saving is debounced; the HUD rebuilds its strings at most once per second; and the blood droplet constellation is rolled once per hit into flat int arrays — every frame after that is just a few dozen raw pixel fills. Zero allocations in the mod's own tick/render loops, a ~0-byte closed-form snapshot, a logo shrunk from 2.1 MB to 0.47 MB (jar ≈ 0.7 MB), and unchanged gameplay.

<p align="center"><i>“Bạn không trốn được thứ nghe thấy bạn.”</i></p>
