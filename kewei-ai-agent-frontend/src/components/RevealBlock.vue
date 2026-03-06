<template>
  <div ref="root" class="reveal-block" :class="{ visible }" :style="{ '--delay': `${delay}ms` }">
    <slot />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const props = defineProps({
  delay: {
    type: Number,
    default: 0,
  },
})

const root = ref(null)
const visible = ref(false)
let observer = null

onMounted(() => {
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          visible.value = true
          observer?.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.18 },
  )

  if (root.value) observer.observe(root.value)
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>
