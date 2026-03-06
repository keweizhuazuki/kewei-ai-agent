<template>
  <div class="page landing-page">
    <RevealBlock>
      <section class="hero">
        <div class="hero-text">
          <p class="hero-kicker">A Warm Multi-App AI Workspace</p>
          <h1>让 AI 更有温度，也更能扩展到你的每个业务场景。</h1>
          <p class="hero-sub">
            从情感陪伴到运动教练，从实时流式对话到多应用编排，Kewei AI Agent 提供统一入口与控制台，帮助你持续迭代 AI 产品矩阵。
          </p>
          <div class="hero-cta">
            <RouterLink class="btn" to="/chat">立即开始对话</RouterLink>
            <RouterLink class="btn-outline" :to="{ path: '/chat', query: { app: 'manus' } }">体验 Manus</RouterLink>
            <RouterLink class="btn-outline" to="/console">打开控制台</RouterLink>
          </div>
        </div>
        <div class="hero-visual">
          <div class="blob blob-1" />
          <div class="blob blob-2" />
          <svg viewBox="0 0 320 260" class="hero-svg" aria-hidden="true">
            <rect x="20" y="30" width="280" height="200" rx="34" fill="#fff7ee" stroke="#f3cfaa" />
            <circle cx="82" cy="92" r="28" fill="#f3ba6f" opacity=".58" />
            <rect x="125" y="77" width="140" height="18" rx="9" fill="#f8d9bf" />
            <rect x="125" y="108" width="112" height="12" rx="6" fill="#f6e9db" />
            <rect x="58" y="146" width="210" height="56" rx="16" fill="#fff" stroke="#f0ddca" />
            <rect x="78" y="167" width="130" height="10" rx="5" fill="#f0c8a1" />
          </svg>
        </div>
      </section>
    </RevealBlock>

    <RevealBlock :delay="120">
      <section class="section-block">
        <h2>平台能力亮点</h2>
        <div class="grid-3">
          <GlassCard v-for="item in capabilities" :key="item.title" elevated>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </GlassCard>
        </div>
      </section>
    </RevealBlock>

    <RevealBlock :delay="180">
      <section class="section-block">
        <h2>应用矩阵</h2>
        <div class="grid-3">
          <GlassCard v-for="app in apps" :key="app.title" elevated>
            <h3>{{ app.title }}</h3>
            <p>{{ app.desc }}</p>
            <span class="tag">{{ app.tag }}</span>
          </GlassCard>
        </div>
      </section>
    </RevealBlock>

    <RevealBlock :delay="260">
      <GlassCard class="cta-band" elevated>
        <h2>把问题交给合适的 AI 助手，获得更自然、更可靠的回应。</h2>
        <p>无论你想获得情感建议、调用多步骤智能体，还是查看系统状态，这里都提供统一、顺滑、易上手的入口。</p>
        <div class="hero-cta">
          <RouterLink class="btn" to="/chat">进入 Chat</RouterLink>
          <RouterLink class="btn-outline" :to="{ path: '/chat', query: { app: 'manus' } }">打开 Manus</RouterLink>
          <RouterLink class="btn-outline" to="/console">查看系统状态</RouterLink>
        </div>
      </GlassCard>
    </RevealBlock>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import GlassCard from '../components/GlassCard.vue'
import RevealBlock from '../components/RevealBlock.vue'

const capabilities = [
  { title: '多 Agent 协同', desc: '为不同应用定义专属提示词、上下文和能力边界。' },
  { title: '流式对话体验', desc: '支持同步响应与 SSE 流式输出，可快速对比效果。' },
  { title: '应用可扩展架构', desc: '通过路由和 API 抽象层，低成本挂载新应用。' },
  { title: '控制台可观测', desc: '健康检查、会话管理、调用耗时和错误日志统一可见。' },
  { title: '品牌化视觉系统', desc: '暖色渐变、柔和卡片与微交互，提升用户信任感。' },
  { title: '移动端可用', desc: '响应式布局与触控友好按钮，手机端可直接对话。' },
]

const apps = [
  { title: 'Love App', desc: '情感沟通与关系建议助手，适合需要细腻对话和图文提问的用户。', tag: 'online' },
  { title: 'Manus', desc: '通用任务型智能体，可执行多步检索、规划与结果整理。', tag: 'online' },
  { title: 'More AI Apps', desc: '后续可扩展学习、效率、创作等应用，统一在控制台配置。', tag: 'scalable' },
]
</script>

<style scoped>
.landing-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.hero {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: var(--space-4);
  align-items: center;
  padding: var(--space-4) 0;
}

.hero-kicker {
  color: var(--primary-deep);
  font-weight: 700;
  margin-bottom: 10px;
}

.hero h1 {
  font-size: clamp(30px, 5vw, 52px);
  margin-bottom: 14px;
}

.hero-sub {
  color: var(--text-subtle);
  max-width: 60ch;
}

.hero-cta {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.hero-visual {
  position: relative;
  min-height: 280px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  background: linear-gradient(145deg, rgba(255, 242, 225, 0.9), rgba(255, 233, 211, 0.5));
  overflow: hidden;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  animation: pulseGlow 6s ease-in-out infinite;
}

.blob-1 {
  width: 140px;
  height: 140px;
  left: 16px;
  top: 16px;
  background: rgba(240, 128, 98, 0.2);
}

.blob-2 {
  width: 120px;
  height: 120px;
  right: 24px;
  bottom: 20px;
  background: rgba(141, 183, 166, 0.22);
  animation-delay: 900ms;
}

.hero-svg {
  width: 100%;
  height: 100%;
  animation: floaty 4.8s ease-in-out infinite;
}

.section-block {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-block h2 {
  font-size: 28px;
}

.section-block p {
  color: var(--text-subtle);
}

.tag {
  margin-top: 12px;
  display: inline-flex;
  width: fit-content;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--text-subtle);
  border: 1px solid var(--border);
}

.cta-band {
  text-align: center;
}

.cta-band p {
  color: var(--text-subtle);
  margin-top: 12px;
}

.cta-band .hero-cta {
  justify-content: center;
}

@media (max-width: 768px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    min-height: 220px;
  }
}
</style>
