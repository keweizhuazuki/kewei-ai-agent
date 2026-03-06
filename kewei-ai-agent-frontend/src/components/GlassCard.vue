<template>
  <section class="glass-card" :class="[elevated ? 'is-elevated' : '']">
    <header v-if="title || $slots.header" class="glass-header">
      <slot name="header">
        <h3>{{ title }}</h3>
      </slot>
    </header>
    <div class="glass-content">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '',
  },
  elevated: {
    type: Boolean,
    default: false,
  },
})
</script>
