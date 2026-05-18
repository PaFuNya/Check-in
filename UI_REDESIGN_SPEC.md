# UI 大改设计规范 — Glassmorphism 风格

## 风格定义
**Glassmorphism（毛玻璃）**— 半透明磨砂玻璃质感，叠层深度，彩色背景透出。

### 核心视觉特征
1. **磨砂玻璃卡片** — `backdrop-filter: blur(16px)`, `background: rgba(255,255,255,0.1)`, `border: 1px solid rgba(255,255,255,0.2)`
2. **彩色渐变背景** — 不是纯黑/纯白，而是生动的渐变色背景（蓝紫粉）
3. **叠层深度** — 多层半透明元素叠加，营造空间感
4. **柔和阴影** — `box-shadow: 0 8px 32px rgba(0,0,0,0.1)`
5. **圆角** — `border-radius: 16-24px`

### 色彩方案
```
背景渐变: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)
或者更柔和: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #d299c2 100%)
或者夜空: linear-gradient(135deg, #0c0c1d 0%, #1a1a3e 40%, #2d1b69 70%, #11001c 100%)

玻璃卡片: 
  background: rgba(255, 255, 255, 0.12)
  backdrop-filter: blur(16px) saturate(180%)
  border: 1px solid rgba(255, 255, 255, 0.18)
  border-radius: 20px
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15)

文字:
  标题: #ffffff (白)
  正文: rgba(255, 255, 255, 0.85)
  次要: rgba(255, 255, 255, 0.6)
  
强调色:
  主按钮: rgba(255, 255, 255, 0.2) 带白色边框, 或者亮色渐变
  成功: #10b981
  错误: #ef4444
  
输入框:
  background: rgba(255, 255, 255, 0.08)
  border: 1px solid rgba(255, 255, 255, 0.15)
  border-radius: 12px
  color: #fff
  placeholder: rgba(255, 255, 255, 0.4)
```

### 背景装饰
- 页面背景用大尺寸渐变
- 添加 2-3 个模糊的彩色圆形光斑（blob）作为装饰
- 光斑用 `filter: blur(80px)` + `position: absolute` + 动画缓慢移动

```css
.blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.6;
    animation: float 20s ease-in-out infinite;
}
.blob-1 { width: 400px; height: 400px; background: #667eea; top: -100px; left: -100px; }
.blob-2 { width: 300px; height: 300px; background: #f093fb; bottom: -50px; right: -50px; animation-delay: -7s; }
.blob-3 { width: 350px; height: 350px; background: #4facfe; top: 50%; left: 50%; animation-delay: -14s; }
```

### 按钮风格
```css
.btn-glass {
    background: rgba(255, 255, 255, 0.15);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 12px;
    color: #fff;
    padding: 12px 24px;
    cursor: pointer;
    transition: all 0.3s ease;
}
.btn-glass:hover {
    background: rgba(255, 255, 255, 0.25);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}
.btn-primary {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.8), rgba(118, 75, 162, 0.8));
    border: none;
}
```

## 要修改的文件

### 1. login.html — 登录页
- 背景: 彩色渐变 + 浮动光斑
- 登录卡片: 磨砂玻璃效果
- 输入框: 半透明底 + 白色边框
- 按钮: 渐变色玻璃按钮
- 添加 "寝室签到" 图标/logo

### 2. checkin.html — 签到页
- 顶部导航: 磨砂玻璃导航栏
- 人脸卡片 + GPS卡片: 磨砂玻璃卡片
- 签到按钮: 大号渐变按钮
- 结果弹窗: 磨砂玻璃弹窗
- 背景: 同样渐变 + 光斑

### 3. index.html — 主页仪表盘
- 侧边栏: 磨砂玻璃侧边栏
- 统计卡片: 磨砂玻璃卡片
- 快捷操作: 磨砂玻璃按钮网格
- 整体背景: 彩色渐变 + 光斑
- 保持现有功能不变，只改视觉风格

## 技术要求
- 所有 CSS 变量集中定义在 :root
- 背景光斑动画用 CSS @keyframes
- backdrop-filter 需要 -webkit- 前缀
- 响应式设计 (移动端适配)
- 保持现有 HTML 结构和 JS 逻辑不变
- 不改后端代码

## 禁止
- 不要用纯黑背景
- 不要用 Linear 暗色主题
- 不要用 emoji 作为图标（用 SVG 或 CSS 图形）
- 不要太花哨导致文字不可读
