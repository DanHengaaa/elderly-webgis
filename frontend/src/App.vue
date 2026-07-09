<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'

onMounted(() => {
  const enabled = localStorage.getItem('elderMode') === 'true'
  document.body.classList.toggle('elder-mode', enabled)
})
</script>

<style src="./assets/elder-mode.css"></style>

<style>
html,
body,
#app {
  width: 100%;
  min-height: 100%;
  height: auto;
  margin: 0;
  padding: 0;
}

html,
body {
  overflow-x: hidden;
  overflow-y: auto;
}

#app {
  overflow-x: hidden;
  overflow-y: visible;
}
</style>
