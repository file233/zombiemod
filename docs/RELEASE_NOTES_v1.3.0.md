# ZombieTide v1.3.0 — "Sparse Blood, Twin Pace, Every Wave Its Own Clock" 🩸⚙️

**Minecraft 1.21.1 · NeoForge 21.1.x · Java 21** — bản tinh chỉnh theo phản hồi:
màn máu chuyển sang **hạt pixel thưa**, trần tốc độ **gấp 2 lần ngưởi chơi**, khoảng-đợt
**chỉ chỉnh theo từng đợt riêng**, và **maxWaves đổi thì game tự cân bằng** để đợt cuối luôn khó nhất.

## ⚡ Cài đặt / Install
1. Cài NeoForge **21.1.x** cho Minecraft **1.21.1**.
2. Thả `zombietide-1.3.0.jar` vào `mods/`.
3. Vào game — đợt 1 đến sau mặc định **600 giây** bình yên (chỉnh từng đợt bằng `/zombietide interval`).

> **Trạng thái JAR:** JAR được đính kèm trực tiếp vào release này. Tự build:
> `git clone` repo → `./gradlew build` → `build/libs/zombietide-1.3.0.jar`.

## 🆕 Có gì mới trong 1.3.0

### 🩸 Màn máu — hạt pixel thưa (ít nhưng đau)
- Bỏ hoàn toàn texture mờ: mỗi cú đánh vắt ra một chòm **hạt máu pixel vuông** tập trung
  ở 2–4 "điểm va chạm" trên kính — số lượng ít, thưa, không che tầm nhìn.
- Seed ổn định theo từng cú đánh → họa tiết **đứng yên**, không nhấp nháy; rớt dần khi bạn hồi lại.
- Config: `damageOverlay.dropletCount=22` · `dropletSize=3` · `dropletSpread=70` · `maxAlpha=0.85` · `fadePerTick`.

### 🏃 Trần tốc độ = 2× ngưởi chơi
- `zombies.maxSpeed` mặc định **0.20** — đúng gấp đôi attribute tốc độ ngưởi chơi (0.10).

### ⏱ Khoảng-đợt chỉ còn TỪNG ĐỢT RIÊNG (trực tiếp trong game)
- **Đã gỡ** `waves.calmMinutes` + `calmMinutesPerWave` — không còn con số "áp dụng cho tất cả".
- Mặc định mỗi đợt nghỉ 600s; mỗi đợt một đồng hồ riêng qua `waves.intervalOverrides`
  hoặc lệnh `/zombietide interval <đợt> [giây|clear]` (độ dài đợt: `/zombietide duration …`).

### 🧮 maxWaves thay đổi → game TỰ CÂN BẰNG
- Mọi tăng trưởng chạy trên **designWave = wave × 50 / maxWaves**.
- Chỉnh 10 hay 500 đợt: **đợt cuối vẫn khó đúng chuẩn wave-50 thiết kế** — thông minh, máu
  (≤ ngưởi chơi +5 tim), sát thương (≤ trần), tốc độ (≤ trần), tầm phát hiện, gọi bạn,
  KBR, ngưỡng phá khối, minWave hiệu ứng… tất cả đều chuẩn hóa.

### 🎚 Range cấu hình ×100
- Toàn bộ 60 range số (COMMON + CLIENT) đã **nới trần ×100** (sàn float ÷100).
  Muốn 20.000 đợt, tốc độ 100 hay thính giác 25.600 khối — tùy bạn.
- Bounds lệnh `interval`/`duration` nới tới ~120.960.000 giây.

## 📦 Giữ nguyên từ 1.0.0 → 1.2.0
50 đợt (8′ + 2′), còi 5s, sinh ban ngày (thưa hơn đêm 2 lần, `daySpawnFactor=0.5`), không
cháy nắng trong đợt, chỉ zombie thường (~2%), trần sát thương 2 tim, máu ≤ ngưởi chơi +5
tim, không giáp — chỉ cầm khối, phá khối từ đợt 20, hiệu ứng xấu theo đợt, bám theo cả
ngưởi chơi creative, 2 dial thông minh/điên cuồng, HUD siêu nhỏ ngày/giờ/phút/giây,
91 mục config chỉnh live trong game + lệnh, logo riêng.

## 📜 Chi tiết
Xem `CHANGELOG.md` và `README.md` trong repo.
