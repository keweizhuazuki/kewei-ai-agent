<template>
  <button
    class="app-pill"
    :class="{ active: active, disabled: disabled }"
    type="button"
    :disabled="disabled"
    @click="$emit('click')"
  >
    <span>{{ title }}</span>
    <small>{{ subtitle }}</small>
  </button>
</template>

<script setup>
defineProps({
  title: String,
  subtitle: String,
  active: Boolean,
  disabled: Boolean,
})

defineEmits(['click'])
</script>
