<script setup>
import { onMounted } from 'vue'
import { useAppStore } from './stores/app'
import NavBar from './components/NavBar.vue'

const appStore = useAppStore()

onMounted(() => {
  appStore.initAnonymousId()
})
</script>

<template>
  <div id="app-root">
    <NavBar />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
#app-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
}
</style>
