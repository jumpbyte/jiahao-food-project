import { test } from '@playwright/test';

test('调试 Vue 组件实例', async ({ page }) => {
  await page.goto('/');
  await page.waitForSelector('.login-form', { timeout: 10000 });

  // 调试 Vue 组件实例
  const debugInfo = await page.evaluate(() => {
    const app = document.querySelector('#app');
    const result = {
      hasApp: !!app,
      hasVueParentComponent: !!app?.__vueParentComponent,
      hasVue: !!app?.__vue__,
      componentKeys: app?.__vueParentComponent ? Object.keys(app.__vueParentComponent) : [],
      proxyKeys: app?.__vueParentComponent?.proxy ? Object.keys(app.__vueParentComponent.proxy) : [],
      loginFormExists: app?.__vueParentComponent?.proxy?.loginForm !== undefined,
      loginFormValue: app?.__vueParentComponent?.proxy?.loginForm ? JSON.stringify(app.__vueParentComponent.proxy.loginForm) : 'not found'
    };
    return result;
  });

  console.log('Vue 组件调试信息:', JSON.stringify(debugInfo, null, 2));

  // 尝试直接设置 loginForm
  const setResult = await page.evaluate(() => {
    const app = document.querySelector('#app');
    if (app?.__vueParentComponent?.proxy?.loginForm) {
      const form = app.__vueParentComponent.proxy.loginForm;
      console.log('设置前的 loginForm:', JSON.stringify(form));
      form.username = 'admin';
      form.password = 'admin123';
      console.log('设置后的 loginForm:', JSON.stringify(form));
      return { success: true, form: JSON.stringify(form) };
    }
    return { success: false, error: '找不到 loginForm' };
  });

  console.log('设置结果:', JSON.stringify(setResult, null, 2));

  // 截图
  await page.screenshot({ path: 'test-results/debug-vue-component.png' });
});
