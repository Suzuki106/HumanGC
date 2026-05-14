import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getServerStatus } from '../api'

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export const useAppStore = defineStore('app', () => {
  const anonymousId = ref('')
  const currentPaper = ref(null)
  const serverStatus = ref({
    running: true,
    daysRemaining: 0,
    hoursRemaining: 0,
    progressPercent: 0,
    totalCost: 0,
    donatedAmount: 0
  })

  const isServerAlive = computed(() => serverStatus.value.running)

  function initAnonymousId() {
    const stored = localStorage.getItem('humangc_anonymous_id')
    if (stored) {
      anonymousId.value = stored
    } else {
      const newId = generateUUID()
      localStorage.setItem('humangc_anonymous_id', newId)
      anonymousId.value = newId
    }
  }

  async function fetchServerStatus() {
    try {
      const res = await getServerStatus()
      serverStatus.value = { ...serverStatus.value, ...res.data }
    } catch {
      // Server may be down; keep default status
    }
  }

  function setCurrentPaper(paper) {
    currentPaper.value = paper
  }

  return {
    anonymousId,
    currentPaper,
    serverStatus,
    isServerAlive,
    initAnonymousId,
    fetchServerStatus,
    setCurrentPaper
  }
})
