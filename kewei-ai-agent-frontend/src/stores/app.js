import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { randomChatId } from '../utils/uid'

const STORAGE_CHAT_IDS = 'kewei.ai.chat.ids'
const STORAGE_CURRENT_CHAT_ID = 'kewei.ai.chat.current'

function readList(key, fallback = []) {
  try {
    const parsed = JSON.parse(localStorage.getItem(key) || 'null')
    return Array.isArray(parsed) ? parsed : fallback
  } catch {
    return fallback
  }
}

export const useAppStore = defineStore('app', () => {
  const activeApp = ref('love')
  const chatMode = ref('sync')
  const chatIds = ref(readList(STORAGE_CHAT_IDS, [randomChatId()]))
  const currentChatId = ref(localStorage.getItem(STORAGE_CURRENT_CHAT_ID) || chatIds.value[0])
  const logs = ref([])

  function setActiveApp(app) {
    activeApp.value = app
  }

  function setChatMode(mode) {
    chatMode.value = mode
  }

  function switchChatId(chatId) {
    if (!chatId) return
    currentChatId.value = chatId
    addChatId(chatId)
  }

  function addChatId(chatId) {
    if (!chatId) return
    const next = [chatId, ...chatIds.value.filter((id) => id !== chatId)].slice(0, 12)
    chatIds.value = next
  }

  function createNewChatId() {
    const id = randomChatId()
    switchChatId(id)
    return id
  }

  function addLog(log) {
    logs.value.unshift({
      id: `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
      time: new Date().toLocaleTimeString(),
      ...log,
    })
    logs.value = logs.value.slice(0, 80)
  }

  function clearLogs() {
    logs.value = []
  }

  watch(chatIds, (val) => {
    localStorage.setItem(STORAGE_CHAT_IDS, JSON.stringify(val))
  }, { deep: true })

  watch(currentChatId, (val) => {
    localStorage.setItem(STORAGE_CURRENT_CHAT_ID, val)
  })

  return {
    activeApp,
    chatMode,
    chatIds,
    currentChatId,
    logs,
    setActiveApp,
    setChatMode,
    switchChatId,
    addChatId,
    createNewChatId,
    addLog,
    clearLogs,
  }
})
