import { test, expect } from '@playwright/test';

// 登录辅助函数 - 通过 API 获取 token 并设置 Cookie 和 localStorage
async function login(page, context) {
  const loginResponse = await page.request.post('/dev-api/api/auth/login', {
    data: { username: 'admin', password: 'admin123' }
  });
  const loginData = await loginResponse.json();
  const token = loginData.data.token;

  await context.addCookies([{
    name: 'Admin-Token',
    value: token,
    domain: 'localhost',
    path: '/',
  }]);

  await page.addInitScript((token) => {
    localStorage.setItem('Admin-Token', token);
  }, token);

  await page.goto('/');
  await page.waitForTimeout(3000);
}

test.describe('行政区管理模块', () => {
  test.beforeEach(async ({ page, context }) => {
    await login(page, context);
  });

  test('行政区维护页面', async ({ page }) => {
    await page.goto('/area/manage');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-results/30-area-management.png' });

    const table = page.locator('.el-table, [class*="table"]');
    await expect(table.first()).toBeVisible({ timeout: 10000 }).catch(async () => {
      await page.screenshot({ path: 'test-results/30-area-error.png' });
      throw new Error('行政区管理页面加载失败');
    });
  });
});
