# Changelog

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
