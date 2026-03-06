<template>
  <div>
    <nav class="top-nav">
      <div class="top-nav-inner">
        <div class="brand">Kewei AI Agent</div>
        <div class="nav-links">
          <RouterLink class="nav-link" to="/">Landing</RouterLink>
          <RouterLink class="nav-link" to="/chat">Chat</RouterLink>
          <RouterLink class="nav-link" to="/console">Console</RouterLink>
        </div>
      </div>
    </nav>

    <main class="main-shell">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>
