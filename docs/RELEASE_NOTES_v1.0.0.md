# ZombieTide v1.0.0 — "The Tide Rises" 🧟☣️

**Minecraft 1.21.1 · NeoForge 21.1.x · Java 21** — mod đại dịch zombie với hệ thống đợt leo thang,
AI cải tiến, HUD siêu nhỏ và toàn bộ cấu hình chỉnh được trong game.

## ⚡ Cài đặt / Install
1. Cài NeoForge **21.1.x** cho Minecraft **1.21.1**.
2. Thả `zombietide-1.0.0.jar` vào `mods/`.
3. Vào game — đợt 1 đến sau **10 phút** bình yên đầu tiên (`waves.calmMinutes`).

> **Trạng thái JAR:** JAR build-sẵn sẽ tự động được đính kèm vào release này ngay khi
> pipeline CI được bật (workflow ở `ci/build.yml.template`, đang chờ quyền `workflows`
> trên token GitHub). Bạn cũng có thể tự build tức thì: `git clone` repo → `./gradlew build`
> → nhận `build/libs/zombietide-1.0.0.jar`.

## ✨ Tính năng chính / Highlights
- 50 đợt (đợt 1 = 8 phút, mỗi đợt +2 phút), còi báo động 5 giây trước mỗi đợt.
- Zombie "có mũi và tai": săn xuyên tường, nghe tiếng chạy/đào/ăn/nổ — đến đợt thì điên cuồng, càng về sau càng thông minh.
- Trong đợt: sinh cả ban ngày, không cháy nắng; chỉ zombie thường được sinh (biến thể/quái khác ~2%).
- Trần cứng: 2 tim sát thương, tốc độ tối đa 1.2× ngưởi chơi, không giáp — chỉ cầm khối.
- Từ đợt 20: một số zombie phá khối (cửa, kính, gỗ…).
- Đòn đánh có tỉ lệ gây hiệu ứng xấu ngẫu nhiên; màn hình mờ đỏ khi trúng nhiều sát thương.
- HUD cực nhỏ trên cùng: thanh tiến trình + đếm ngày/giờ/phút/giây; trong đợt đếm ngược kết thúc.
- ~60 khóa config, chỉnh trong game (Mod list → Config) và bằng lệnh `/zombietide` + alias `/zt`.

## 📜 Chi tiết
Xem `CHANGELOG.md` và `README.md` trong repo.
