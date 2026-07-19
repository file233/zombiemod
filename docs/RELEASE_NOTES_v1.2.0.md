# ZombieTide v1.2.0 — "Smarter, Madder, Day-Shy" 🧠🩸

**Minecraft 1.21.1 · NeoForge 21.1.x · Java 21** — bản mở rộng cân bằng thứ hai:
2 nhóm dial mới cho **độ thông minh** và **độ điên cuồng của zombie**, cùng luật
**ban ngày sinh thưa hơn 2 lần ban đêm**.

## ⚡ Cài đặt / Install
1. Cài NeoForge **21.1.x** cho Minecraft **1.21.1**.
2. Thả `zombietide-1.2.0.jar` vào `mods/`.
3. Vào game — đợt 1 đến sau **10 phút** bình yên (`waves.calmMinutes`).

> **Trạng thái JAR:** JAR được đính kèm trực tiếp vào release này. Tự build:
> `git clone` repo → `./gradlew build` → `build/libs/zombietide-1.2.0.jar`.

## 🆕 Có gì mới trong 1.2.0 (xếp chồng lên mọi tính năng 1.1.0)

### 🧠 Nhóm chỉnh ĐỘ THÔNG MINH — `intelligence.*`
- `baseLevel` (mặc định 1.0) · `perWaveBonus` (0.04/đợt) · `maxLevel` (trần)
- Thông minh quyết định: **tầm phát hiện**, **bán kính nghe** (nhân 0.75+0.25×trí tuệ),
  **độ trễ phản ứng tiếng động**, **tốc độ re-target** (zombie khôn quay đầu nhanh gấp đôi),
  **trí nhớ mục tiêu** khi mất tầm nhìn (`unseenMemoryTicks=60` + 2 tick/đợt).
- Đọc live từ config mỗi tick → `/zombietide config set intelligence.baseLevel 3` có tác dụng **ngay lập tức**.

### 🤪 Nhóm chỉnh ĐỘ ĐIÊN CUỒNG TRONG ĐỢT — `frenzy.*`
- `intensity` (mặc định 1.0; 0 = thiền sư, 3 = điên hoàn toàn)
- Một multiplier duy nhất nhân vào **mọi** buff chỉ-có-trong-đợt:
  bonus tầm phát hiện, `speedBoost` (+0.005 — **vẫn không bao giờ** vượt trần 1.2× ngưởi chơi),
  `hearingBonus` (+8 khối), kháng knockback, tỉ lệ gọi bạn, độ nhạy tiếng động (`noiseReactionFactor=0.75`).

### ☀️ Ban ngày sinh ít hơn 2 lần
- `spawning.daySpawnFactor=0.5` — khi ban ngày, engine đợt chỉ giữ **50%** nhịp sinh so với ban đêm
  (đợt ban ngày vẫn diễn ra bình thường, chỉ thưa quân hơn — đúng tính chất "sợ nắng" còn sót lại).
- Vặn 0 (ngày im hẳn) → 1 (ngày = đêm) tùy thích.

### 📊 Lệnh nâng cấp
- `/zombietide status` giờ in chỉ số bộ não: `Mind: intelligence ×N | frenzy ×M (RAGE ON/dormant)`.

## 📦 Toàn bộ tính năng tích lũy (1.0.0 → 1.2.0)
50 đợt (8′ + 2′/đợt), còi 5s, sinh ban ngày, không cháy nắng trong đợt, chỉ zombie thường (~2% biến thể),
trần 2 tim / 1.2× tốc / máu = ngưởi chơi +5 tim, không giáp — chỉ cầm khối, phá khối từ đợt 20,
hiệu ứng xấu ngẫu nhiên, **máu bắn mờ màn hình** (3 họa tiết), HUD siêu nhỏ đếm ngày/giờ/phút/giây,
bám theo cả ngưởi chơi creative, chỉnh thởi gian **từng đợt** (`interval`/`duration`),
~85 khóa config chỉnh trong game + lệnh, logo riêng.

## 📜 Chi tiết
Xem `CHANGELOG.md` và `README.md` trong repo.
