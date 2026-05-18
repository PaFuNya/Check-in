# UI 重设计规范 — 校园寝室签到系统

## Design Read
**"校园工具类产品，面向学生群体，需要干净、专业、温暖的视觉语言。不是SaaS，不是营销页。"**

## 核心原则
1. **浅色为主** — 默认浅色模式，支持深色/跟随系统
2. **不要花哨** — 不要渐变背景、不要毛玻璃、不要浮动光斑
3. **干净排版** — 大量留白，清晰层级，专业感
4. **温暖配色** — 暖白底色，不是冰冷的纯白

## 三档模式切换
必须实现 `data-theme` 属性切换：
- `data-theme="light"` — 浅色（默认）
- `data-theme="dark"` — 深色
- 跟随系统 — 用 `prefers-color-scheme` 媒体查询自动切换

```css
:root, [data-theme="light"] {
    --bg-canvas: #F7F6F3;          /* 暖白画布 */
    --bg-surface: #FFFFFF;          /* 卡片白 */
    --bg-elevated: #F9F9F8;         /* 次级表面 */
    --text-primary: #1a1a1a;        /* 主文字（不用纯黑） */
    --text-secondary: #6b7280;      /* 次要文字 */
    --text-tertiary: #9ca3af;       /* 辅助文字 */
    --border: #E5E5E3;              /* 边框 */
    --border-subtle: #F0F0EE;       /* 弱边框 */
    --accent: #2563EB;              /* 主强调色（蓝色） */
    --accent-hover: #1d4ed8;        /* 强调色hover */
    --accent-light: #EFF6FF;        /* 强调色浅底 */
    --success: #059669;             /* 成功绿 */
    --success-light: #ECFDF5;       /* 成功浅底 */
    --error: #DC2626;               /* 错误红 */
    --error-light: #FEF2F2;         /* 错误浅底 */
    --warning: #D97706;             /* 警告橙 */
    --shadow-sm: 0 1px 2px rgba(0,0,0,0.04);
    --shadow-md: 0 2px 8px rgba(0,0,0,0.06);
    --radius-sm: 6px;
    --radius-md: 10px;
    --radius-lg: 14px;
}

[data-theme="dark"] {
    --bg-canvas: #0f0f0f;
    --bg-surface: #1a1a1a;
    --bg-elevated: #242424;
    --text-primary: #f0f0f0;
    --text-secondary: #9ca3af;
    --text-tertiary: #6b7280;
    --border: #2a2a2a;
    --border-subtle: #1f1f1f;
    --accent: #3b82f6;
    --accent-hover: #60a5fa;
    --accent-light: #1e293b;
    --success: #10b981;
    --success-light: #064e3b;
    --error: #ef4444;
    --error-light: #450a0a;
    --warning: #f59e0b;
    --shadow-sm: 0 1px 2px rgba(0,0,0,0.2);
    --shadow-md: 0 2px 8px rgba(0,0,0,0.3);
}

@media (prefers-color-scheme: dark) {
    :root:not([data-theme="light"]) {
        /* 同 dark 变量 */
    }
}
```

## 字体
```css
/* 禁止: Inter, Roboto, Arial, Open Sans */
font-family: 'SF Pro Display', 'PingFang SC', -apple-system, 'Segoe UI', sans-serif;
```
用系统原生字体栈，中文用 PingFang SC / 苹方，西文用 SF Pro。
标题可用 `font-weight: 600-700`，正文 `400`，行高 `1.6`。

## 布局规则
- 卡片圆角: `10-14px`，不要 pill 形
- 卡片边框: `1px solid var(--border)`，不要阴影或极轻阴影
- 内边距: `20-32px`，呼吸感
- 间距: `16-24px` 模块间距
- 最大宽度: `1200px` 居中

## 按钮
```css
/* 主按钮 */
.btn-primary {
    background: var(--accent);
    color: #fff;
    border: none;
    border-radius: 8px;
    padding: 10px 20px;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.15s;
}
.btn-primary:hover { background: var(--accent-hover); }

/* 次按钮 */
.btn-secondary {
    background: var(--bg-surface);
    color: var(--text-primary);
    border: 1px solid var(--border);
    border-radius: 8px;
    padding: 10px 20px;
}
```

## 输入框
```css
input {
    background: var(--bg-surface);
    border: 1px solid var(--border);
    border-radius: 8px;
    padding: 10px 14px;
    color: var(--text-primary);
    transition: border-color 0.15s;
}
input:focus {
    outline: none;
    border-color: var(--accent);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
```

## 状态标签
```css
.badge {
    display: inline-flex;
    padding: 2px 10px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 500;
    letter-spacing: 0.02em;
}
.badge-success { background: var(--success-light); color: var(--success); }
.badge-error { background: var(--error-light); color: var(--error); }
```

## 主题切换按钮
在页面右上角（导航栏）放一个主题切换按钮：
- 三个状态: ☀️ 浅色 / 🌙 深色 / 💻 跟随系统
- 点击循环切换
- 用 localStorage 持久化用户选择
- 切换时设置 `document.documentElement.setAttribute('data-theme', ...)`

```javascript
function initTheme() {
    const saved = localStorage.getItem('theme') || 'system';
    applyTheme(saved);
}
function applyTheme(mode) {
    if (mode === 'system') {
        document.documentElement.removeAttribute('data-theme');
        // 用 matchMedia 检测
    } else {
        document.documentElement.setAttribute('data-theme', mode);
    }
    localStorage.setItem('theme', mode);
}
```

## 禁止事项
- ❌ 不要渐变背景（纯色画布）
- ❌ 不要毛玻璃 backdrop-filter
- ❌ 不要浮动光斑动画
- ❌ 不要 emoji 作为图标（用 SVG 或文字）
- ❌ 不要 Inter / Roboto 字体
- ❌ 不要 3D 效果
- ❌ 不要超过 2 种颜色的渐变
- ❌ 不要 neon / 霓虹色

## 要修改的文件
1. **login.html** — 登录页，浅色暖白背景，居中卡片，简洁干净
2. **checkin.html** — 签到页，同风格，摄像头+GPS 双卡片
3. **index.html** — 主页，只改 CSS 变量和样式，不改 HTML 结构和 JS

## 技术要求
- 三个页面的 CSS 变量统一
- 主题切换 JS 逻辑提取为公共片段（每个页面内联）
- index.html 的 CSS 改动不能破坏现有功能
- 保持所有元素 ID 和 class 名不变
- 响应式设计
