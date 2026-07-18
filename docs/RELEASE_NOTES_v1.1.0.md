# ZombieTide v1.1.0 — "They Even Hunt Builders" 🩸🧟

**Minecraft 1.21.1 · NeoForge 21.1.x · Java 21** — bản nâng cấp gameplay đầu tiên:
zombie bám theo cả ngưởi chơi Sáng tạo, trần máu = ngưởi chơi + 5 tim,
chỉnh khoảng cách/độ dài **từng đợt một**, và màn hình **máu bắn mờ nhòe** khi bị đánh.

## ⚡ Cài đặt / Install
1. Cài NeoForge **21.1.x** cho Minecraft **1.21.1**.
2. Thả `zombietide-1.1.0.jar` vào `mods/`.
3. Vào game — đợt 1 đến sau **10 phút** bình yên (`waves.calmMinutes`).

> **Trạng thái JAR:** JAR được đính kèm trực tiếp vào release này. Tự build:
> `git clone` repo → `./gradlew build` → `build/libs/zombietide-1.1.0.jar`.

## 🆕 Có gì mới trong 1.1.0

### 🎯 Zombie săn cả ngưởi chơi Sáng tạo
Goal săn tự viết lại từ đầu (`findTarget` + `canContinueToUse`) — vanilla **hard-drop**
mọi mục tiêu creative; ZombieTide thì không. Ngưởi chơi creative vẫn bị bầy đàn dỏm dạnh,
vây quanh — chỉ là không nhận sát thương (đúng luật vanilla). Cảm biến tiếng động và
engine sinh cũng đi theo công tắc này.
- `targeting.targetCreativePlayers` (mặc định **bật**)
- `targeting.targetSpectators` (mặc định tắt)
- `spawning.pressureCreativePlayers` (đợt vẫn vây quanh ngưởi creative)

### ❤️ Trần máu zombie = ngưởi chơi + 5 tim
- `zombies.healthBaseHearts=10` · `healthPerWaveHearts=0.25` · `healthMaxHeartsAbovePlayer=5`
- Zombie trâu dần theo số đợt, nhưng **không bao giờ** vượt quá `maxHealth ngưởi chơi + 5 tim`
  (ngưởi chơi vanilla: zombie hard nhất = 15 tim).

### ⏱ Chỉnh thởi gian TỪNG ĐỢT một
- Config: `waves.intervalOverrides` / `waves.durationOverrides` — định dạng `"đợt=giây"`.
- Lệnh trực quan:
  - `/zombietide interval` — xem công thức + mọi override khoảng nghỉ
  - `/zombietide interval <đợt>` — khoảng nghỉ trước đợt đó (nguồn: override/công thức)
  - `/zombietide interval <đợt> <giây>` — đặt override (lưu config, **hiệu lực ngay**)
  - `/zombietide interval <đợt> clear` — xóa override
  - `/zombietide duration …` — tương tự cho độ dài đợt
- Công thức nghỉ giờ có thêm tăng trưởng: `waves.calmMinutesPerWave` (âm/dương đều được).

### 🩸 Máu bắn lên màn hình (mờ hơn)
- **3 texture họa tiết máu** 512² sinh riêng (hoen ố, giọt chảy, phun văng) — chọn ngẫu nhiên mỗi cú đánh.
- Stack **nhiều pass mờ xếp chồng** (blurPasses 1–4, mặc định 3) → hiệu ứng màn hình nhòe nhẹt đỏ.
- Config client: `intensity`, `maxAlpha` (mặc định nâng 0.82), `fadePerTick`, `blurPasses`, `splatterVariants`.

### ➕ Config mới khác
`zombies.damagePerWaveHearts`, `waves.alarmPitch`, `effects.amplifier` — tổng **~75 khóa**,
toàn bộ chỉnh được trong game (Mods → ZombieTide → Config) hoặc `/zombietide config`.

## 🔧 Sửa lỗi kỹ thuật
- Sửa tên API Mojmap 1.21.1: `SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(...)` và `Explosion#center()`.

## 📜 Chi tiết đầy đủ
Xem `CHANGELOG.md` và `README.md` trong repo.
