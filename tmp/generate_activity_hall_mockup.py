from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 1440, 1160
ROOT = Path(__file__).resolve().parent
OUT = ROOT / "activity-hall-ui-mockup.png"

FONT_CANDIDATES = [
    Path("C:/Windows/Fonts/msyh.ttc"),
    Path("C:/Windows/Fonts/simhei.ttf"),
    Path("C:/Windows/Fonts/simsun.ttc"),
]
FONT_PATH = next((path for path in FONT_CANDIDATES if path.exists()), None)


def make_font(size):
    if FONT_PATH:
        return ImageFont.truetype(str(FONT_PATH), size=size, index=0)
    return ImageFont.load_default()


F = {
    "title": make_font(46),
    "h1": make_font(34),
    "h2": make_font(24),
    "h3": make_font(20),
    "body": make_font(16),
    "small": make_font(13),
    "tiny": make_font(11),
    "nav": make_font(17),
    "num": make_font(32),
}

COL = {
    "bg": "#F4FAF6",
    "card": "#FFFFFF",
    "primary": "#0B7D3B",
    "green": "#2DBE74",
    "teal": "#1ABC9C",
    "gold": "#D9A441",
    "ink": "#1F2937",
    "muted": "#64748B",
    "border": "#DDECE4",
    "soft": "#EAF7F0",
}

img = Image.new("RGB", (W, H), COL["bg"])
d = ImageDraw.Draw(img)


def rounded(rect, radius, fill, outline=None, width=1):
    d.rounded_rectangle(rect, radius=radius, fill=fill, outline=outline, width=width)


def shadow_box(rect, radius=24, fill="#FFFFFF", shadow=(18, 74, 45, 24), offset=(0, 12), blur=24, outline="#E5F1EA"):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    x1, y1, x2, y2 = rect
    ox, oy = offset
    ld.rounded_rectangle((x1 + ox, y1 + oy, x2 + ox, y2 + oy), radius=radius, fill=shadow)
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    base = Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")
    img.paste(base)
    d.rounded_rectangle(rect, radius=radius, fill=fill, outline=outline, width=1)


def text(x, y, value, font_key="body", fill=None, anchor=None):
    d.text((x, y), value, font=F[font_key], fill=fill or COL["ink"], anchor=anchor)


def pill(x, y, width, height, label, fill="#EEF8F2", color=None, outline=None, font_key="small"):
    rounded((x, y, x + width, y + height), height // 2, fill, outline or fill)
    text(x + width / 2, y + height / 2, label, font_key, color or COL["primary"], anchor="mm")


def icon_circle(cx, cy, radius, fill, label, color="#FFFFFF"):
    d.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=fill)
    text(cx, cy - 1, label, "h3", color, anchor="mm")


def draw_camera(cx, cy):
    d.ellipse((cx - 22, cy - 22, cx + 22, cy + 22), fill="#EAF7F0", outline="#CBE8D7")
    d.rounded_rectangle((cx - 11, cy - 7, cx + 12, cy + 9), radius=5, outline=COL["primary"], width=2)
    d.rectangle((cx - 5, cy - 12, cx + 5, cy - 7), fill="#EAF7F0", outline=COL["primary"], width=2)
    d.ellipse((cx - 4, cy - 3, cx + 5, cy + 6), outline=COL["primary"], width=2)


# Background accents.
for i in range(15):
    x = 95 + i * 96
    d.line((x, 170, x + 98, 72), fill="#E0F2E8", width=3)
    d.ellipse((x + 39, 112, x + 54, 127), fill="#DDF1E6")
for i in range(10):
    x = 38 + i * 150
    y = 1000 + (i % 2) * 30
    d.ellipse((x, y, x + 120, y + 44), outline="#E1F2E8", width=2)

# Topbar.
shadow_box((42, 28, 1398, 108), radius=28, shadow=(18, 74, 45, 16), offset=(0, 8), blur=18)
icon_circle(88, 68, 24, COL["primary"], "农")
text(124, 52, "活动魔方", "h3")
text(124, 78, "河南农业大学校园活动平台", "small", COL["muted"])
pill(530, 48, 110, 38, "活动大厅", fill="#E3F4EB", color=COL["primary"], font_key="nav")
text(710, 68, "消息中心", "nav", "#35534A", anchor="mm")
draw_camera(1186, 68)
d.ellipse((1232, 45, 1276, 89), fill="#DDF2E6", outline="#BFE6D0")
text(1254, 67, "张", "h3", COL["primary"], anchor="mm")
text(1290, 58, "张三", "body")
text(1290, 82, "2321241389", "tiny", COL["muted"])

left, right = 90, 1350

# Hero.
shadow_box((left, 140, right, 382), radius=30, shadow=(18, 74, 45, 20), offset=(0, 14), blur=30)
for i in range(380):
    t = i / 379
    color = (
        int(255 * (1 - t) + 234 * t),
        int(255 * (1 - t) + 248 * t),
        int(255 * (1 - t) + 240 * t),
    )
    d.line((left + 1 + i, 141, left + 1 + i, 381), fill=color)
rounded((left, 140, right, 382), 30, fill=None, outline="#DDECE4")
for r in [75, 118, 162]:
    d.arc((right - 260 - r, 116 - r, right - 260 + r, 116 + r), 8, 168, fill="#D9F1E4", width=18)
text(130, 188, "活动魔方", "title")
text(132, 246, "发现校园活动，记录成长足迹", "h2", "#48625A")
text(132, 287, "服务龙子湖、文化路、许昌三校区，浏览报名、扫码签到、消息提醒和活动成果一站完成。", "body", COL["muted"])
pill(132, 326, 120, 42, "浏览活动", fill=COL["primary"], color="#FFFFFF", font_key="body")
pill(266, 326, 108, 42, "扫一扫", fill="#FFFFFF", color=COL["primary"], outline="#BEE5CF", font_key="body")
for idx, (num, label, color) in enumerate([("18", "当前可参与", COL["primary"]), ("8", "报名中", COL["green"]), ("3", "进行中", COL["teal"])]):
    x = 910 + idx * 135
    rounded((x, 208, x + 112, 314), 22, "#FFFFFF", "#DCEDE5")
    text(x + 56, 243, num, "num", color, anchor="mm")
    text(x + 56, 282, label, "small", COL["muted"], anchor="mm")

# Todos.
text(left, 426, "我的待办", "h2")
text(left + 108, 433, "今天需要关注的校园活动事项", "small", COL["muted"])
todos = [
    ("待签到活动", "2", "按时完成签到", "#EAF7F0", "签"),
    ("待反馈活动", "1", "活动结束后填写", "#FFF6DF", "评"),
    ("未读消息", "5", "查看最新通知", "#EAF8FF", "信"),
    ("即将开始", "3", "提前准备参加", "#F0FAF6", "近"),
]
for i, (title, num, desc, bg, mark) in enumerate(todos):
    x = left + i * 318
    shadow_box((x, 462, x + 288, 574), radius=22, shadow=(18, 74, 45, 13), offset=(0, 8), blur=16)
    icon_circle(x + 46, 518, 25, bg, mark, COL["primary"])
    text(x + 86, 489, title, "body")
    text(x + 86, 523, num, "num", COL["primary"])
    text(x + 138, 530, desc, "small", COL["muted"])

# Filter panel.
shadow_box((left, 606, right, 754), radius=24, shadow=(18, 74, 45, 14), offset=(0, 10), blur=20)
rounded((126, 638, 430, 684), 16, "#F9FCFA", "#DDECE4")
text(150, 661, "搜索活动名称", "body", "#9AA8A2", anchor="lm")
pill(454, 640, 84, 42, "筛选", fill=COL["primary"], color="#FFFFFF", font_key="body")
campuses = ["全部", "龙子湖校区", "文化路校区", "许昌校区", "线上"]
x = 126
for idx, label in enumerate(campuses):
    w = 54 if label == "全部" else 110
    pill(x, 704, w, 32, label, fill="#DFF3E8" if idx == 0 else "#F4FAF6", color=COL["primary"])
    x += w + 12
for i, label in enumerate(["公益活动", "实践活动", "志愿服务", "讲座培训", "文体活动", "竞赛活动", "社团活动"]):
    x = 660 + i * 88
    if x + 80 < right - 20:
        pill(x, 704, 76, 32, label, fill="#FFFDF7", color="#7A5B14", outline="#F0DFAE")

# Cards.
text(left, 802, "推荐活动", "h2")
text(left + 110, 809, "共 18 场活动，按报名状态和校区为你推荐", "small", COL["muted"])
cards = [
    ("麦田守望志愿服务", "志愿服务", "线下", "报名中", "龙子湖校区 · 志愿服务广场", "07月22日 14:00", "已报名 56/80", "2 课外学时", "#DFF5E9"),
    ("农科创客讲座：智慧农业", "讲座培训", "线上", "即将开始", "腾讯会议", "07月23日 19:30", "已报名 132/200", "1 积分", "#EAF8FF"),
    ("校园荧光夜跑活动", "文体活动", "线下", "进行中", "文化路校区操场", "07月20日 20:00", "已报名 88/100", "证书", "#FFF5DC"),
]
status_style = {
    "报名中": ("#0B7D3B", "#FFFFFF"),
    "即将开始": ("#DBEAFE", "#1D4ED8"),
    "进行中": ("#1ABC9C", "#FFFFFF"),
}
for i, card in enumerate(cards):
    x = left + i * 420
    y = 844
    shadow_box((x, y, x + 390, y + 282), radius=24, shadow=(18, 74, 45, 18), offset=(0, 12), blur=22)
    rounded((x + 14, y + 14, x + 376, y + 118), 18, card[8])
    d.line((x + 34, y + 96, x + 145, y + 44), fill="#B9E4CA", width=5)
    d.line((x + 138, y + 100, x + 250, y + 44), fill="#C9EED8", width=5)
    d.ellipse((x + 278, y + 42, x + 350, y + 98), fill="#FFFFFF")
    pill(x + 26, y + 28, 72, 28, card[3], fill=status_style[card[3]][0], color=status_style[card[3]][1], font_key="tiny")
    pill(x + 294, y + 28, 58, 28, card[2], fill="#FFFFFF", color=COL["primary"], outline="#C7E9D5", font_key="tiny")
    pill(x + 22, y + 140, 78, 28, card[1], fill="#EFF8F2", color=COL["primary"], font_key="tiny")
    text(x + 22, y + 178, card[0], "h3")
    text(x + 22, y + 212, "时间  " + card[5], "small", COL["muted"])
    text(x + 22, y + 236, "地点  " + card[4], "small", COL["muted"])
    text(x + 22, y + 260, card[6], "small", COL["muted"])
    text(x + 190, y + 260, "奖励  " + card[7], "small", COL["gold"])
    pill(x + 214, y + 232, 72, 36, "详情", fill="#F4FAF6", color=COL["primary"], outline="#DDECE4")
    pill(x + 298, y + 232, 72, 36, "报名", fill=COL["primary"], color="#FFFFFF")

img.save(OUT)
print(str(OUT))
