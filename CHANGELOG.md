# Changelog

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
