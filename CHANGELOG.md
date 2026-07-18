# Changelog

## 1.3.1 — "The Tuning Forge" (2026-07-19)

### Tối ưu hóa (giữ nguyên 100% lối chơi & 91 mục config)

- **`ZTSnapshot` (mới)** — lớp bake-toàn-bộ-cấu-hình: mọi scalar nóng thành field nguyên
  thủy; toàn bộ toán tăng trưởng theo đợt (tốc độ, máu, sát thương, tầm phát hiện/nghe,
  nhịp retarget, trí nhớ, cooldown tiếng động, gọi-bạn, KBR, trần sinh, cổng phá khối,
  khoảng-nghỉ/thởi-lượng riêng từng đợt) thành **mảng phẳng index-theo-đợt** (~6 KB ở 50
  đợt). Refresh đúng 1 lần khi config load/reload/sửa lệnh. Mọi AI tick (10 Hz × hàng trăm
  zombie), spawn attempt, combat/sense/spawn filter giờ đọc field/array — **không còn
  map-lookup của NeoForge config trong đường nóng**.
- **CPU spawn engine**: hệ số ngày/đêm giờ *co số lần thử sinh* (ban ngày tốn ít CPU hẳn,
  không còn gieo xúc xắc vứt công quét); đếm zombie-quanh-ngưởi tối đa 1 lần/chu kỳ qua
  **ring-buffer 32 slot** thay UUID-map (không GC, tự dọn ngưởi thoát); probe vị trí bằng
  `MutableBlockPos` tái sử dụng; bubble-no-spawn tính thuần tọa độ.
- **CPU zombie mutation**: gate rẻ-trước; yardstick máu lấy target sẵn có thay cho
  `getNearestPlayer`; sweep/đếm fast-path khi chỉ chơi overworld.
- **CPU combat/sense/blockbreak**: pool hiệu ứng cắn cache theo (epoch × wave); gate đốt-
  nắng (mọi entity, mọi tick) đọc đúng 2 primitive; BlockBreakGoal bỏ cấp phát
  `BlockPos[]`/`Vec3` mỗi lần quét 2 Hz.
- **CPU wave conductor**: còi + holder âm thanh cache theo chu kỳ; lọc dimension bị bỏ qua
  ở cấu hình mặc định; mọi đọc config per-tick bị loại bỏ.
- **Đĩa**: ghi file config qua lệnh có **debounce 300 ms** (spam `/zombietide interval`
  không còn IO-spam).
- **GPU/frame**: HUD chỉ rebuild chuỗi+layout khi nội dung đổi (≤ 1 lần/giây, hoặc ngay khi
  sync/sửa config); overlay máu gieo chòm droplet **1 lần mỗi cú đánh** vào mảng int phẳng —
  mỗi frame chỉ là vài chục `fill` nguyên thủy, không RNG/Gaussian/cấp phát; tôn trọng F1.
- **RAM**: không còn allocation của mod trong tick/render loop; bảng theo-đợt ~6 KB.

### Sửa chữa

- Biên số `wave` ở lệnh `wave|interval|duration` nới 1000 → 100000 (khớp range ×100).
- README: bỏ tham chiếu còn sót tới `calmMinutes` (đã xóa từ 1.3.0), chuẩn hóa số mục config.

## 1.3.0 — "Sparse Blood, Twin Pace, Every Wave Its Own Clock" (2026-07-19)

### Thay đổi theo yêu cầu mới
- **Màn hình máu = hạt pixel thưa**: bỏ hẳn texture mờ; mỗi cú đánh vắt ra một chòm **vài chục hạt máu pixel hình vuông** (2–4 điểm va chạm, kích thước/màu/độ đục ngẫu nhiên theo seed ổn định — không flicker), rơi rụng dần khi trauma hạ. Mới: `dropletCount=22`, `dropletSize=3`, `dropletSpread=70`. (Gỡ `blurPasses`, `splatterVariants`.)
- **Trần tốc độ zombie = 2× ngưởi chơi**: `zombies.maxSpeed` mặc định **0.20** (từ 0.12).
- **Khoảng-đợt chỉ còn chỉnh TỪNG ĐỢT**: **gỡ** `waves.calmMinutes` + `calmMinutesPerWave` (knob áp-dụng-cho-tất-cả). Mọi đợt mặc định 600s, chỉnh riêng qua `waves.intervalOverrides` hoặc `/zombietide interval <đợt> [giây|clear]` — trực tiếp trong game.
- **Tự cân bằng theo maxWaves**: mọi công thức tăng trưởng chạy trên **designWave** = wave × (50/maxWaves) — dù chỉnh 10 hay 500 đợt, **đợt cuối luôn khó đúng chuẩn wave-50 thiết kế** (intelligence, máu, sát thương, tốc độ, tầm phát hiện, reinforcements, KBR, minWave hiệu ứng, ngưỡng phá khối đều chuẩn hóa).
- **Range config ×100**: toàn bộ 60 `defineInRange` (COMMON+CLIENT) — trần trên nhân 100, sàn dưới (float) chia 100; min int giữ nguyên để không vỡ game (vd maxWaves ≥ 1). Bounds lệnh interval/duration ×100 (tới ~3.8 năm).

### Kỹ thuật
- `ZTConfig.designWave(int)` — trung tâm chuẩn hóa; `DEFAULT_CALM_SECONDS=600`; `calmSource()` → override/default.
- ZTTraumaOverlayLayer: renderer giọt-pixel deterministic (`GuiGraphics#fill`), không còn texture (giảm ~360KB resources).
- `/zombietide interval` header giờ hiển thị mặc định 600s/đợt.

## 1.2.0 — "Smarter, Madder, Day-Shy" (2026-07-19)

### Thêm mới
- **Nhóm chỉnh ĐỘ THÔNG MINH** (`intelligence.*`): baseLevel (mặc định 1.0), perWaveBonus (0.04/đợt), maxLevel, trí nhớ mục tiêu (unseenMemoryTicks=60, +2/đợt).
  - Thông minh ảnh hưởng: tầm phát hiện, bán kính nghe (×0.75+0.25×I), độ trễ phản ứng tiếng động, tốc độ re-target, trí nhớ mục tiêu khi mất tầm nhìn.
- **Nhóm chỉnh ĐỘ ĐIÊN CUỒNG trong đợt** (`frenzy.*`): intensity (mặc định 1.0, 0=thiền sư, 3=điên loạn).
  - Nhân vào: bonus tầm phát hiện trong đợt, speedBoost (+0.005, vẫn dưới trần 1.2×), hearingBonus (+8 khối), kháng knockback, tỉ lệ gọi bạn, độ nhạy tiếng động (noiseReactionFactor 0.75).
- **Ban ngày sinh ít hơn 2 lần**: `spawning.daySpawnFactor=0.5` — giữa trưa vẫn có đợt nhưng hàng ngũ thưa hơn đúng 50% (chỉnh được 0→1).
- `/zombietide status` giờ hiển thị chỉ số bộ não: thông minh × N, điên cuồng × M (ON/dormant).

### Kỹ thuật
- Tất cả dial đi qua `ZTConfig.intelligence(wave)` / `frenzy()` + helper retarget/unseen/noiseCooldown — đọc live mỗi lần dùng, chỉnh config hiệu lực tức thì.
- ZTHuntPlayerGoal tự own retarget cadence (không dùng randomInterval cố định của vanilla nữa) + setUnseenMemoryTicks theo wave.

## 1.1.0 — "They Even Hunt Builders" (2026-07-19)

### Thêm mới
- **Bám theo ngưởi chơi Sáng tạo**: goal săn, bộ tai và engine sinh đều tự kiểm soát bộ lọc creative/spectator (vanilla hard-drop mục tiêu creative) — công tắc `targeting.targetCreativePlayers` (mặc định bật), `targetSpectators`, `spawning.pressureCreativePlayers`.
- **Trần máu zombie = ngưởi chơi + 5 tim** (mặc định): `zombies.healthBaseHearts=10`, `healthPerWaveHearts=0.25`, `healthMaxHeartsAbovePlayer=5` — zombie trâu dần theo đợt nhưng không bao giờ quá 15 tim với ngưởi chơi vanilla.
- **Chỉnh khoảng cách & độ dài TỪNG ĐỢT một**:
  - config `waves.intervalOverrides` / `durationOverrides` (định dạng `"đợt=giây"`),
  - lệnh `/zombietide interval [n] [giây|clear]` và `/zombietide duration [n] [giây|clear]` — lưu vào file config, **áp dụng ngay** vào đếm ngược đang chạy (giữ tỉ lệ thởi gian còn lại),
  - công thức nghỉ tăng trưởng theo đợt `waves.calmMinutesPerWave`.
- **Máu bắn lên màn hình (mờ hơn)**: 3 họa tiết máu bắn (texture 512² sinh riêng) chọn ngẫu nhiên mỗi cú đánh + stack nhiều pass mờ xếp chồng → cảm giác màn hình nhòe máu; độ đục tối đa mặc định 0.82; thêm `fadePerTick`, `blurPasses`, `splatterVariants`.
- `zombies.damagePerWaveHearts` (tăng sát thương theo đợt, vẫn kẹp trần), `waves.alarmPitch`, `effects.amplifier` (cấp hiệu ứng I→IV).

### Sửa / cải tiến
- Fix tên API Mojmap 1.21.1: `SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(...)` (vị trí sinh theo đúng predicate vanilla 1.21.1) và `Explosion#center()` (vị trí vụ nổ cho SenseEngine).
- Nâng tổng số khóa config: ~75 (COMMON + CLIENT), tất cả vẫn chỉnh trong game hoặc bằng lệnh.

### Kỹ thuật
- `ZTConfig.isHuntable(Player)` — nguồn sự thật duy nhất cho quyền bị săn; ZTHuntPlayerGoal tự override `findTarget()` + `canContinueToUse()` để không bị vanilla rớt mục tiêu creative.
- WaveManager: `retuneCalm()` / `retuneActive(wave)` — chỉnh thởi gian theo tỉ lệ, có đồng bộ HUD ngay.

## 1.0.0 — "The Tide Rises" (2026-07-18)

Bản phát hành đầu tiên của **ZombieTide** cho Minecraft 1.21.1 (NeoForge 21.1.x).

### Thêm mới
- Hệ thống đợt: 50 đợt mặc định, đợt 1 = 8 phút, mỗi đợt +2 phút, sau đợt cuối `CONTINUE/LOOP/STOP`.
- Còi báo động custom 5 giây (`zombietide:wave_alarm`) + title/chat cảnh báo trước mỗi đợt.
- AI zombie tái thiết: săn xuyên tường (ZTHuntPlayerGoal), bộ tai SenseEngine với độ ồn theo hành động, frenzy theo pha đợt, thông minh tăng theo số đợt.
- Sinh vây hãm cả ban ngày khi trong đợt (vanilla placement + engine bổ sung), zombie không cháy nắng giữa đợt.
- Lọc sinh trong đợt: chỉ zombie thường; biến thể/baby/quái khác giữ ở tỉ lệ cực thấp (mặc định 2%).
- Trần cứng: sát thương tối đa 2 tim lên ngưởi chơi; tốc độ tối đa 1.2× ngưởi chơi; zombie không mặc giáp, chỉ cầm khối.
- Từ đợt 20: ~35% zombie có khả năng phá khối (vanilla-style, tôn trọng mobGriefing).
- Đòn đánh có tỉ lệ gây hiệu ứng xấu ngẫu nhiên (pool theo trọng số, mở khóa dần theo đợt).
- Màn hình mờ đỏ khi trúng nhiều sát thương (trauma vignette động).
- HUD siêu nhỏ trên cùng giữa màn hình: thanh tiến trình + đếm ngày/giờ/phút/giây; trong đợt đếm ngược kết thúc; số zombie đang sống.
- Config ~60 khóa: màn hình Config trong game (NeoForge ConfigurationScreen, có nhãn EN/VI) + lệnh `/zombietidem config get/set` (lưu file, hiệu lực ngay).
- Lệnh đầy đủ: status / start|summon [instant] / end / wave <n> [instant] / reset / pause / resume / config; alias `/zt`.
- Logo thương hiệu riêng, mods.toml đầy đủ, lang English + Tiếng Việt.
- GitHub Actions: build + test compile + release JAR tự động.
