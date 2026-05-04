<template>
  <div class="home-page">
    <div class="welcome-content">
      <h1 class="title">嘉豪食品中台系统</h1>
      <p class="subtitle">Jia Hao Food Management System</p>
      <div class="info-row">
        <span>欢迎使用</span>
        <span v-if="loginTime" class="divider">|</span>
        <span v-if="loginTime">{{ loginTime }}</span>
      </div>
    </div>
  </div>
</template>

<script setup name="Index">
const loginTime = ref('')

onMounted(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  loginTime.value = `登录时间: ${year}-${month}-${day} ${hours}:${minutes}`
})
</script>

<style scoped lang="scss">
.home-page {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #e0f2fe 0%, #dbeafe 25%, #e0e7ff 50%, #ede9fe 75%, #fce7f3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-content {
  text-align: center;
  padding: 40px;

  .title {
    font-size: 48px;
    font-weight: 700;
    color: #1e293b;
    margin: 0;
    letter-spacing: 4px;
  }

  .subtitle {
    font-size: 16px;
    color: #64748b;
    margin: 8px 0 24px;
    letter-spacing: 2px;
    text-transform: uppercase;
  }

  .info-row {
    font-size: 14px;
    color: #94a3b8;

    .divider {
      margin: 0 8px;
    }
  }
}
</style>
