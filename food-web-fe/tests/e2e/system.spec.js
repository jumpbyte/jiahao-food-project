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

test.describe('系统管理模块', () => {
  test.beforeEach(async ({ page, context }) => {
    await login(page, context);
  });

  test('用户管理页面', async ({ page }) => {
    await page.goto('/system/user');
    await page.waitForTimeout(2000);

    await page.screenshot({ path: 'test-results/10-user-management.png' });

    // 检查页面基本结构 - 使用更宽松的选择器
    const table = page.locator('.el-table, [class*="table"]');
    await expect(table.first()).toBeVisible({ timeout: 10000 }).catch(async () => {
      await page.screenshot({ path: 'test-results/10-user-error.png' });
      throw new Error('用户管理页面加载失败');
    });
  });

  test('API Key 管理页面', async ({ page }) => {
    await page.goto('/system/api-key');
    await page.waitForTimeout(2000);

    await page.screenshot({ path: 'test-results/11-apikey-management.png' });

    const table = page.locator('.el-table, [class*="table"]');
    await expect(table.first()).toBeVisible({ timeout: 10000 }).catch(async () => {
      await page.screenshot({ path: 'test-results/11-apikey-error.png' });
      throw new Error('API Key 管理页面加载失败');
    });
  });
});
