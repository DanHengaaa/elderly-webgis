<template>
  <div class="ai-page">
    <header class="ai-topbar">
      <button class="ai-back-btn" @click="goBackMap">
        ← 返回地图
      </button>

      <div class="ai-topbar-title">
        <h1>AI 伴诊助手</h1>
        <p>通过自然语言描述老人情况，系统自动理解需求并联动智能推荐</p>
      </div>
    </header>

    <main class="ai-layout">
      <!-- 左侧：对话区 -->
      <section class="ai-chat-card">
        <div class="ai-chat-header ai-chat-header-row">
  <div>
    <h2>对话咨询</h2>
    <p>请描述老人的年龄、护理需求、预算、意向区域和偏好</p>
  </div>

  <div class="ai-provider-box">
    <label>AI 模型</label>
    <select v-model="selectedProviderId">
      <option
        v-for="provider in providers"
        :key="provider.id"
        :value="provider.id"
        :disabled="!provider.enabled"
      >
        {{ provider.name }} · {{ provider.model }}
      </option>
    </select>
  </div>

  <button class="ai-reset-btn" @click="resetConversation">
    重新开始
  </button>
</div>

        <div class="ai-start-box">
          <div class="ai-section-title">探视起点，可选</div>

          <div class="ai-start-search">
            <input
              v-model="startKeyword"
              placeholder="例如：新街口、鼓楼医院"
              @keyup.enter="searchStart"
            />
            <button @click="searchStart">
              设置
            </button>
          </div>

          <div v-if="startInfo" class="ai-start-info">
            <span>当前探视起点</span>
            <strong>{{ startInfo.formattedAddress }}</strong>
            <p>{{ Number(startInfo.lon).toFixed(6) }}, {{ Number(startInfo.lat).toFixed(6) }}</p>
          </div>
        </div>

        <div ref="messageListRef" class="ai-message-list">
          <article
            v-for="msg in messages"
            :key="msg.id"
            :class="['ai-message', msg.role]"
          >
            <div class="ai-avatar">
              {{ msg.role === 'user' ? '我' : 'AI' }}
            </div>

            <div class="ai-bubble">
  <div
    class="ai-bubble-content"
    v-html="renderMessageContent(msg.content)"
  ></div>

  <div
    v-if="msg.role === 'assistant' && msg.meta"
    class="ai-answer-meta"
  >
    {{ msg.meta }}
  </div>
</div>
          </article>
        </div>

        <div class="ai-suggestions">
          <button
            v-for="item in suggestions"
            :key="item"
            @click="useSuggestion(item)"
          >
            {{ item }}
          </button>
        </div>

        <div class="ai-input-row">
          <textarea
            v-model="inputMessage"
            placeholder="例如：我妈妈78岁半失能，预算7000，希望离医院近，最好在鼓楼区"
            @keydown.enter.exact.prevent="sendMessage"
          ></textarea>

          <button :disabled="loading" @click="sendMessage">
            {{ loading ? '分析中...' : '发送' }}
          </button>
        </div>

        <div v-if="errorMessage" class="ai-error">
          {{ errorMessage }}
        </div>
      </section>

      <!-- 右侧：推荐结果 -->
      <section class="ai-result-card">
        <div class="ai-result-header">
          <div>
            <h2>联动推荐结果</h2>
            <p v-if="hasRecommendations">
              AI 已根据对话内容生成推荐方案
            </p>
            <p v-else>
              推荐机构会在 AI 理解需求后显示在这里
            </p>
          </div>

          <strong v-if="hasRecommendations">
            {{ recommendationTotal }} 家
          </strong>
        </div>

        <div v-if="!hasRecommendations" class="ai-empty">
          <p>你可以输入类似：</p>
          <strong>“我妈妈78岁半失能，预算7000，希望离医院近，最好在鼓楼区”</strong>
          <p>系统会自动识别需求，并调用【智能推荐】模块。</p>
        </div>

        <div v-if="hasRecommendations" class="ai-recommend-list">
          <article
            v-for="item in recommendationItems"
            :key="item.id"
            class="ai-recommend-card"
          >
            <div class="ai-score">
              <strong>{{ Math.round(item.finalScore * 100) }}</strong>
              <span>推荐分</span>
            </div>

            <div class="ai-recommend-info">
              <div class="ai-recommend-title">
                <h3>{{ item.name }}</h3>
                <span :class="['ai-match-tag', item.matchLevel]">
                  {{ item.matchLevelText }}
                </span>
              </div>

              <p class="ai-address">
                {{ item.district }} · {{ item.address || '暂无地址' }}
              </p>

              <div class="ai-meta">
                <span>{{ item.institutionCategoryText }}</span>
                <span>月费 {{ formatMoney(item.monthlyFeeBase) }}</span>
                <span>床位 {{ item.availableBeds ?? '未知' }}/{{ item.totalBeds ?? '未知' }}</span>
                <span v-if="item.estimatedVisitMinutes">
                  探视约 {{ item.estimatedVisitMinutes }} 分钟
                </span>
              </div>

              <div class="ai-reasons">
                <span
                  v-for="reason in item.reasons"
                  :key="reason"
                >
                  {{ reason }}
                </span>
              </div>

             <div class="ai-actions">
  <button @click="goDetail(item.id)">
    查看详情
  </button>

  <button class="secondary" @click="askCommunityOpinion(item)">
    AI 口碑摘要
  </button>

  <button class="secondary" @click="goRecommend">
    完整推荐页
  </button>
</div>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const inputMessage = ref('')
const loading = ref(false)
const errorMessage = ref('')
const latestResponse = ref(null)

const providers = ref([
  {
    id: 'deepseek',
    name: 'DeepSeek',
    model: 'deepseek-v4-flash',
    enabled: true
  }
])

const selectedProviderId = ref('deepseek')

const startKeyword = ref('')
const startInfo = ref(null)

const messageListRef = ref(null)

const suggestions = ref([
  '你用了什么大模型',
  '你能帮我做什么',
  '养老院应该怎么选',
  '我妈妈78岁半失能，预算7000，希望离医院近'
])

const messages = ref([
  {
    id: Date.now(),
    role: 'assistant',
    content: '你好，我是 AI 伴诊助手。你可以告诉我老人的年龄、护理需求、预算、意向区域和偏好，我会帮你联动智能推荐。'
  }
])

const recommendationItems = computed(() => {
  if (!latestResponse.value) {
    return []
  }

  if (!latestResponse.value.recommendations) {
    return []
  }

  if (!latestResponse.value.recommendations.items) {
    return []
  }

  return latestResponse.value.recommendations.items
})

const recommendationTotal = computed(() => {
  if (!latestResponse.value) {
    return 0
  }

  if (!latestResponse.value.recommendations) {
    return 0
  }

  return latestResponse.value.recommendations.total || recommendationItems.value.length
})

const hasRecommendations = computed(() => {
  return recommendationItems.value.length > 0
})

onMounted(() => {
  loadProviders()
})

async function loadProviders() {
  try {
    const response = await axios.get('/api/ai-assistant/providers')

    if (response.data && response.data.length > 0) {
      providers.value = response.data

      const firstEnabledProvider = response.data.find(item => item.enabled)

      if (firstEnabledProvider) {
        selectedProviderId.value = firstEnabledProvider.id
      }
    }
  } catch (error) {
    console.error('加载 AI 模型列表失败：', error)
  }
}

async function searchStart() {
  if (!startKeyword.value.trim()) {
    errorMessage.value = '请输入探视起点关键词。'
    return
  }

  errorMessage.value = ''

  try {
    const response = await axios.get('/api/geo/geocode', {
      params: {
        keyword: startKeyword.value.trim()
      }
    })

    startInfo.value = response.data
  } catch (error) {
    console.error('探视起点搜索失败：', error)
    errorMessage.value = '探视起点搜索失败，请换一个更明确的南京地址。'
  }
}

function useSuggestion(text) {
  inputMessage.value = text
}

async function sendMessage() {
  if (!inputMessage.value.trim()) {
    errorMessage.value = '请输入咨询内容。'
    return
  }

  const userText = inputMessage.value.trim()

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: userText
  })

  inputMessage.value = ''
  loading.value = true
  errorMessage.value = ''

  await scrollToBottom()

  try {
    const nearbyStartInfo = await resolveNearbyStartIfNeeded(userText)
    const activeStartInfo = nearbyStartInfo || startInfo.value

    const payload = {
      message: userText,
      providerId: selectedProviderId.value,
      history: buildHistoryForRequest(userText),
      previousRecommendations: buildPreviousRecommendationContext(userText),
      startLon: activeStartInfo ? Number(activeStartInfo.lon) : null,
      startLat: activeStartInfo ? Number(activeStartInfo.lat) : null,
      startName: activeStartInfo ? activeStartInfo.formattedAddress : null
    }

    const response = await axios.post('/api/ai-assistant/chat', payload)

    if (response.data.recommendations) {
      latestResponse.value = response.data
    }

    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: response.data.answer || '我已经为你生成了推荐结果，请查看右侧列表。',
      meta: response.data.answerSource || buildAnswerMeta(response.data)
    })

    if (response.data.suggestions && response.data.suggestions.length > 0) {
      suggestions.value = response.data.suggestions
    }
  } catch (error) {
    console.error('AI 伴诊失败：', error)
    errorMessage.value = 'AI 伴诊助手调用失败，请检查后端接口。'

    messages.value.push({
      id: Date.now() + 2,
      role: 'assistant',
      content: '抱歉，我暂时无法完成分析。请稍后重试，或直接前往智能推荐页面生成方案。'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()

  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function buildHistoryForRequest(currentText) {
  if (isNewRecommendationQuestion(currentText)) {
    return []
  }

  return messages.value
    .slice(0, -1)
    .slice(-8)
    .map(item => ({
      role: item.role === 'assistant' ? 'assistant' : 'user',
      content: item.content
    }))
}

function buildPreviousRecommendationContext(currentText) {
  if (!isFollowUpQuestion(currentText)) {
    return []
  }

  return recommendationItems.value
    .slice(0, 5)
    .map(item => ({
      id: item.id,
      name: item.name,
      address: item.address,
      district: item.district,
      institutionCategoryText: item.institutionCategoryText,
      monthlyFeeBase: item.monthlyFeeBase,
      totalBeds: item.totalBeds,
      availableBeds: item.availableBeds,
      finalScore: item.finalScore,
      matchLevelText: item.matchLevelText,
      budgetScore: item.budgetScore,
      medicalScore: item.medicalScore,
      lifeScore: item.lifeScore,
      greenScore: item.greenScore,
      visitScore: item.visitScore,
      estimatedVisitMinutes: item.estimatedVisitMinutes,
      reasons: item.reasons || []
    }))
}

function buildAnswerMeta(data) {
  if (!data) {
    return ''
  }

  if (data.providerName && data.model) {
    return `由 ${data.providerName} · ${data.model} 生成，基于平台智能推荐算法与 GIS 空间分析结果。`
  }

  return '由系统生成，基于平台智能推荐算法与 GIS 空间分析结果。'
}

function renderMessageContent(content) {
  if (!content) {
    return ''
  }

  const lines = String(content).split('\n')
  const htmlLines = []

  for (const rawLine of lines) {
    const line = rawLine.trim()

    if (!line) {
      htmlLines.push('<div class="md-space"></div>')
      continue
    }

    if (line.startsWith('### ')) {
      htmlLines.push(`<h4>${renderInlineMarkdown(line.replace(/^###\s+/, ''))}</h4>`)
      continue
    }

    if (line.startsWith('## ')) {
      htmlLines.push(`<h3>${renderInlineMarkdown(line.replace(/^##\s+/, ''))}</h3>`)
      continue
    }

    if (line.startsWith('# ')) {
      htmlLines.push(`<h2>${renderInlineMarkdown(line.replace(/^#\s+/, ''))}</h2>`)
      continue
    }

    if (line.startsWith('- ')) {
      htmlLines.push(`<p class="md-list-item">• ${renderInlineMarkdown(line.replace(/^-\s+/, ''))}</p>`)
      continue
    }

    if (/^\d+\.\s+/.test(line)) {
      htmlLines.push(`<p class="md-list-item">${renderInlineMarkdown(line)}</p>`)
      continue
    }

    htmlLines.push(`<p>${renderInlineMarkdown(line)}</p>`)
  }

  return htmlLines.join('')
}

function renderInlineMarkdown(value) {
  let html = escapeHtml(value)

  html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')

  return html
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function isFollowUpQuestion(text) {
  if (!text) {
    return false
  }

  return /(比较|对比|前\s*3|前三|这几家|这几所|上面|刚才|上一轮|哪个更好|哪一家更好|优缺点|区别|它们|这些机构)/.test(text)
}

function isNewRecommendationQuestion(text) {
  if (!text) {
    return false
  }

  if (isFollowUpQuestion(text)) {
    return false
  }

  return /(推荐|找|查查|看看|有没有|有无|养老院|养老机构|附近|周边|离.*近|周围)/.test(text)
}

function hasNearbyIntent(text) {
  if (!text) {
    return false
  }

  return /(附近|周边|周围|离.*近|距离.*近)/.test(text)
}

async function resolveNearbyStartIfNeeded(text) {
  if (!hasNearbyIntent(text)) {
    return null
  }

  if (
    startInfo.value &&
    /(学校|这里|这附近|附近|周边|周围|这个地点|该地点|刚才的地点)/.test(text)
  ) {
    return startInfo.value
  }

  const keyword = extractNearbyKeyword(text)

  if (!keyword) {
    return null
  }

  try {
    const response = await axios.get('/api/geo/geocode', {
      params: {
        keyword
      }
    })

    if (response.data && response.data.lon && response.data.lat) {
      startInfo.value = response.data
      startKeyword.value = keyword
      return response.data
    }
  } catch (error) {
    console.error('自动识别附近地点失败：', error)
  }

  return null
}

function extractNearbyKeyword(text) {
  if (!text) {
    return ''
  }

  const patterns = [
    /(?:查查|看看|帮我看看|帮我查查|找找|帮我找)?\s*([^，。！？,.!?]{2,24}?)(?:附近|周边|周围)/,
    /(?:离|距离)\s*([^，。！？,.!?]{2,24}?)(?:近|较近|不远)/
  ]

  for (const pattern of patterns) {
    const match = text.match(pattern)

    if (match && match[1]) {
      let keyword = match[1].trim()

      keyword = keyword
        .replace(/^(有没有|有无|是否有|合适的|适合的)/, '')
        .replace(/(有没有|有无|是否有|合适的|适合的)$/, '')
        .trim()

      if (isGenericLocationWord(keyword)) {
        return ''
      }

      return keyword
    }
  }

  return ''
}

function isGenericLocationWord(keyword) {
  if (!keyword) {
    return true
  }

  const genericWords = [
    '医院',
    '学校',
    '家',
    '附近',
    '周边',
    '周围',
    '这里',
    '这个地点',
    '该地点'
  ]

  return genericWords.includes(keyword)
}

function formatMoney(value) {
  if (value === null || value === undefined) {
    return '暂无'
  }

  return `${Number(value).toLocaleString()} 元/月`
}

function resetConversation() {
  messages.value = [
    {
      id: Date.now(),
      role: 'assistant',
      content: '你好，我是 AI 伴诊助手。你可以告诉我老人的年龄、护理需求、预算、意向区域和偏好，我会帮你联动智能推荐。'
    }
  ]

  latestResponse.value = null
  errorMessage.value = ''
}

function goBackMap() {
  router.push('/map')
}

function goDetail(id) {
  router.push(`/institutions/${id}`)
}

async function askCommunityOpinion(item) {
  if (!item || !item.id) {
    return
  }

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: `请总结一下 ${item.name} 的社区口碑`
  })

  loading.value = true
  errorMessage.value = ''

  await scrollToBottom()

  try {
    const response = await axios.get(`/api/community/institutions/${item.id}/summary`, {
      params: {
        providerId: selectedProviderId.value
      }
    })

    const summary = response.data

    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: summary.summary || '当前该机构暂无足够社区笔记，建议结合机构详情和实地探访进一步判断。',
      meta: `由 ${summary.generatedBy || '系统'} 生成，基于社区探院笔记与用户经验内容。`
    })
  } catch (error) {
    console.error('生成社区口碑摘要失败：', error)

    messages.value.push({
      id: Date.now() + 2,
      role: 'assistant',
      content: '暂时无法生成该机构的社区口碑摘要，请稍后重试。'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function goRecommend() {
  router.push('/recommend')
}
</script>

<style scoped>
.ai-page {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  background:
    radial-gradient(circle at top left, rgba(46, 125, 107, 0.12), transparent 34%),
    linear-gradient(180deg, #f3f7f6 0%, #eef3f5 100%);
  color: #1f2933;
}

.ai-topbar {
  min-height: 96px;
  padding: 0 36px;
  background: linear-gradient(135deg, #2e7d6b 0%, #5fa891 100%);
  color: #ffffff;
  display: grid;
  grid-template-columns: 160px 1fr 160px;
  align-items: center;
  box-shadow: 0 10px 28px rgba(46, 125, 107, 0.24);
}

.ai-back-btn {
  width: fit-content;
  height: 42px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 999px;
  padding: 0 18px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
}

.ai-back-btn:hover {
  background: rgba(255, 255, 255, 0.22);
}

.ai-topbar-title {
  text-align: center;
}

.ai-topbar-title h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 1px;
}

.ai-topbar-title p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.88);
}

.ai-layout {
  max-width: 1580px;
  margin: 0 auto;
  padding: 26px 26px 48px;
  display: grid;
  grid-template-columns: 660px minmax(0, 1fr);
  gap: 24px;
}

.ai-chat-card,
.ai-result-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #d9e0e8;
  border-radius: 26px;
  padding: 24px;
  box-shadow: 0 14px 34px rgba(31, 41, 51, 0.08);
}

.ai-chat-card {
  min-height: calc(100vh - 150px);
  height: auto;
  display: flex;
  flex-direction: column;
}

.ai-chat-header h2,
.ai-result-header h2 {
  margin: 0;
  color: #2e7d6b;
  font-size: 23px;
  font-weight: 900;
}

.ai-chat-header p,
.ai-result-header p {
  margin: 8px 0 0;
  color: #52606d;
}

.ai-section-title {
  margin-top: 18px;
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 4px solid #2e7d6b;
  color: #1f2933;
  font-size: 15px;
  font-weight: 900;
}

.ai-start-search {
  display: grid;
  grid-template-columns: 1fr 80px;
  gap: 10px;
}

.ai-start-search input {
  height: 42px;
  border: 1px solid #d9e0e8;
  border-radius: 13px;
  padding: 0 12px;
  outline: none;
}

.ai-start-search input:focus {
  border-color: #2e7d6b;
  box-shadow: 0 0 0 4px rgba(46, 125, 107, 0.12);
}

.ai-start-search button,
.ai-input-row button,
.ai-actions button {
  border: none;
  border-radius: 13px;
  background: #2e7d6b;
  color: #ffffff;
  font-weight: 900;
  cursor: pointer;
}

.ai-start-info {
  margin-top: 10px;
  border-radius: 16px;
  background: #e3f1ed;
  padding: 12px;
}

.ai-start-info span {
  color: #52606d;
  font-size: 13px;
  font-weight: 700;
}

.ai-start-info strong {
  display: block;
  margin-top: 5px;
  color: #2e7d6b;
}

.ai-start-info p {
  margin: 5px 0 0;
  color: #52606d;
  font-size: 13px;
}

.ai-message-list {
  flex: 1;
  min-height: 430px;
  max-height: 58vh;
  margin-top: 18px;
  padding: 18px;
  overflow-y: auto;
  border-radius: 20px;
  background: #f5f7f9;
  border: 1px solid #e3e9ee;
}

.ai-message {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.ai-message.user {
  flex-direction: row-reverse;
}

.ai-avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #2e7d6b;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 13px;
}

.ai-message.user .ai-avatar {
  background: #e8845b;
}

.ai-bubble {
  max-width: 78%;
  border-radius: 18px;
  padding: 12px 14px;
  background: #ffffff;
  color: #1f2933;
  box-shadow: 0 4px 14px rgba(31, 41, 51, 0.06);
}

.ai-message.user .ai-bubble {
  background: #e3f1ed;
}

.ai-bubble p {
  margin: 0;
  white-space: pre-line;
  line-height: 1.7;
}

.ai-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.ai-suggestions button {
  border: none;
  border-radius: 999px;
  padding: 7px 11px;
  background: #e3f1ed;
  color: #2e7d6b;
  font-weight: 700;
  cursor: pointer;
}

.ai-input-row {
  display: grid;
  grid-template-columns: 1fr 82px;
  gap: 10px;
  margin-top: 14px;
}

.ai-input-row textarea {
  height: 88px;
  resize: none;
  border: 1px solid #d9e0e8;
  border-radius: 16px;
  padding: 12px;
  outline: none;
  line-height: 1.6;
  font-size: 15px;
}

.ai-input-row textarea:focus {
  border-color: #2e7d6b;
  box-shadow: 0 0 0 4px rgba(46, 125, 107, 0.12);
}

.ai-input-row button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.ai-error {
  margin-top: 12px;
  border-radius: 14px;
  background: rgba(217, 83, 79, 0.10);
  color: #d9534f;
  padding: 12px;
}

.ai-result-card {
  min-height: calc(100vh - 150px);
}

.ai-result-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid #eef2f5;
  padding-bottom: 18px;
}

.ai-result-header strong {
  min-width: 76px;
  height: 42px;
  border-radius: 999px;
  background: #e3f1ed;
  color: #2e7d6b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.ai-empty {
  margin-top: 24px;
  border-radius: 22px;
  background: #f7faf9;
  color: #52606d;
  padding: 42px 32px;
  line-height: 1.9;
  text-align: center;
  border: 1px dashed #b8c9c3;
}

.ai-empty p {
  margin: 8px 0;
}

.ai-empty strong {
  display: block;
  margin: 12px 0;
  color: #2e7d6b;
}

.ai-recommend-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 20px;
}

.ai-recommend-card {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 18px;
  border: 1px solid #d9e0e8;
  border-radius: 24px;
  padding: 18px;
  background: #ffffff;
}

.ai-score {
  width: 76px;
  height: 76px;
  border-radius: 23px;
  background: linear-gradient(135deg, #2e7d6b, #5fa891);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ai-score strong {
  font-size: 28px;
}

.ai-score span {
  font-size: 12px;
}

.ai-recommend-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.ai-recommend-title h3 {
  margin: 0;
  color: #1f2933;
  font-size: 20px;
  font-weight: 900;
}

.ai-match-tag {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 900;
}

.ai-match-tag.excellent {
  background: #e3f1ed;
  color: #2e7d6b;
}

.ai-match-tag.good {
  background: rgba(58, 158, 92, 0.12);
  color: #3a9e5c;
}

.ai-match-tag.medium {
  background: rgba(232, 132, 91, 0.14);
  color: #e8845b;
}

.ai-match-tag.weak {
  background: rgba(217, 83, 79, 0.12);
  color: #d9534f;
}

.ai-address {
  margin: 8px 0 0;
  color: #52606d;
  line-height: 1.6;
}

.ai-meta,
.ai-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 11px;
}

.ai-meta span {
  background: #f5f7f9;
  color: #52606d;
  border-radius: 999px;
  padding: 6px 11px;
  font-size: 13px;
  font-weight: 700;
}

.ai-reasons span {
  background: #e3f1ed;
  color: #2e7d6b;
  border-radius: 999px;
  padding: 7px 11px;
  font-size: 13px;
  font-weight: 700;
}

.ai-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.ai-actions button {
  height: 40px;
  padding: 0 18px;
}

.ai-actions button.secondary {
  background: #f5f7f9;
  color: #2e7d6b;
}

@media (max-width: 1180px) {
  .ai-layout {
    grid-template-columns: 1fr;
  }

  .ai-chat-card {
    height: auto;
    min-height: 760px;
  }

  .ai-message-list {
    min-height: 420px;
    max-height: none;
  }
}

@media (max-width: 760px) {
  .ai-topbar {
    min-height: auto;
    padding: 18px;
    grid-template-columns: 1fr;
    gap: 14px;
    justify-items: start;
  }

  .ai-topbar-title {
    text-align: left;
  }

  .ai-topbar-title h1 {
    font-size: 24px;
  }

  .ai-layout {
    padding: 14px 14px 36px;
  }

  .ai-recommend-card {
    grid-template-columns: 1fr;
  }

  .ai-recommend-title {
    flex-direction: column;
    align-items: flex-start;
  }

  .ai-input-row {
    grid-template-columns: 1fr;
  }

  .ai-input-row button {
    height: 42px;
  }

/* ========== AI 伴诊布局优化：固定对话区高度 ========== */

.ai-layout {
  max-width: 1660px;
  margin: 0 auto;
  padding: 22px 28px 56px;
  display: grid;
  grid-template-columns: 720px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

.ai-chat-card {
  min-height: 880px;
  height: auto;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.ai-result-card {
  min-height: 880px;
}

/* 探视起点区域固定，不再挤压对话框 */
.ai-start-box {
  flex: 0 0 auto;
}

/* 探视起点信息改成更紧凑的提示卡 */
.ai-start-info {
  margin-top: 10px;
  border-radius: 16px;
  background: #e3f1ed;
  padding: 10px 14px;
  display: grid;
  grid-template-columns: 96px 1fr;
  column-gap: 10px;
  row-gap: 4px;
  align-items: center;
}

.ai-start-info span {
  color: #52606d;
  font-size: 13px;
  font-weight: 800;
}

.ai-start-info strong {
  display: block;
  margin-top: 0;
  color: #2e7d6b;
  font-size: 15px;
  line-height: 1.5;
}

.ai-start-info p {
  grid-column: 2;
  margin: 0;
  color: #52606d;
  font-size: 13px;
}

/* 核心：对话显示框固定高度，不再被压缩 */
.ai-message-list {
  flex: 0 0 500px;
  height: 500px;
  min-height: 500px;
  max-height: 500px;
  margin-top: 18px;
  padding: 18px;
  overflow-y: auto;
  border-radius: 20px;
  background: #f5f7f9;
  border: 1px solid #e3e9ee;
}

/* 建议问题区域也固定，避免占太多空间 */
.ai-suggestions {
  flex: 0 0 auto;
  max-height: 76px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

/* 输入框稍微放大 */
.ai-input-row {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: 1fr 96px;
  gap: 10px;
  margin-top: 14px;
}

.ai-input-row textarea {
  height: 96px;
  resize: none;
  border: 1px solid #d9e0e8;
  border-radius: 16px;
  padding: 12px;
  outline: none;
  line-height: 1.6;
  font-size: 15px;
}

/* 对话气泡稍微优化 */
.ai-bubble {
  max-width: 84%;
  border-radius: 18px;
  padding: 13px 15px;
  background: #ffffff;
  color: #1f2933;
  box-shadow: 0 4px 14px rgba(31, 41, 51, 0.06);
}

.ai-bubble p {
  margin: 0;
  white-space: pre-line;
  line-height: 1.75;
}

/* 中等屏幕适配 */
@media (max-width: 1180px) {
  .ai-layout {
    grid-template-columns: 1fr;
  }

  .ai-chat-card {
    min-height: 860px;
  }

  .ai-result-card {
    min-height: auto;
  }

  .ai-message-list {
    flex-basis: 480px;
    height: 480px;
    min-height: 480px;
    max-height: 480px;
  }
}

/* 小屏幕适配 */
@media (max-width: 760px) {
  .ai-layout {
    padding: 14px 14px 36px;
  }

  .ai-chat-card {
    min-height: 820px;
  }

  .ai-message-list {
    flex-basis: 430px;
    height: 430px;
    min-height: 430px;
    max-height: 430px;
  }

  .ai-input-row {
    grid-template-columns: 1fr;
  }

  .ai-input-row button {
    height: 44px;
  }

  .ai-start-info {
    grid-template-columns: 1fr;
  }

  .ai-start-info p {
    grid-column: 1;
  }
}

/* ========== AI 模型选择 ========== */

.ai-chat-header-row {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.ai-provider-box {
  width: 210px;
  flex-shrink: 0;
}

.ai-provider-box label {
  display: block;
  margin-bottom: 7px;
  color: #52606d;
  font-size: 13px;
  font-weight: 900;
}

.ai-provider-box select {
  width: 100%;
  height: 38px;
  border: 1px solid #d9e0e8;
  border-radius: 12px;
  padding: 0 10px;
  color: #1f2933;
  background: #ffffff;
  outline: none;
  font-weight: 800;
}

.ai-provider-box select:focus {
  border-color: #2e7d6b;
  box-shadow: 0 0 0 4px rgba(46, 125, 107, 0.12);
}

@media (max-width: 760px) {
  .ai-chat-header-row {
    flex-direction: column;
  }

  .ai-provider-box {
    width: 100%;
  }
}

/* ========== AI 回复 Markdown 与来源说明 ========== */

.ai-bubble-content {
  margin: 0;
  white-space: normal;
  line-height: 1.78;
  font-size: 15px;
}

.ai-bubble-content strong {
  color: #1f2933;
  font-weight: 900;
}

.ai-bubble-content code {
  padding: 2px 6px;
  border-radius: 6px;
  background: #eef3f5;
  color: #2e7d6b;
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
}

.ai-answer-meta {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #eef2f5;
  color: #7b8794;
  font-size: 12px;
  line-height: 1.6;
}
/* ========== AI 回复 Markdown 样式优化 ========== */

.ai-bubble-content {
  margin: 0;
  white-space: normal;
  line-height: 1.78;
  font-size: 15px;
  color: #1f2933;
}

.ai-bubble-content p {
  margin: 0 0 8px;
}

.ai-bubble-content h2,
.ai-bubble-content h3,
.ai-bubble-content h4 {
  margin: 14px 0 8px;
  color: #1f2933;
  font-weight: 900;
  line-height: 1.45;
}

.ai-bubble-content h2 {
  font-size: 18px;
}

.ai-bubble-content h3 {
  font-size: 17px;
}

.ai-bubble-content h4 {
  font-size: 16px;
}

.ai-bubble-content strong {
  color: #1f2933;
  font-weight: 900;
}

.ai-bubble-content code {
  padding: 2px 6px;
  border-radius: 6px;
  background: #eef3f5;
  color: #2e7d6b;
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
}

.ai-bubble-content .md-space {
  height: 8px;
}

.ai-bubble-content .md-list-item {
  margin: 4px 0 6px;
  padding-left: 2px;
}

/* ========== AI 回复来源说明：单独隔开、弱化显示 ========== */

.ai-answer-meta {
  margin-top: 16px;
  padding-top: 11px;
  border-top: 1px solid #edf1f4;
  color: #9aa5b1;
  font-size: 12px;
  line-height: 1.6;
  font-weight: 500;
}
}
</style>