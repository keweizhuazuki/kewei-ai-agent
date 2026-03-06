export function uid(prefix = 'id') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`
}

export function randomChatId() {
  return `chat_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 5)}`
}
