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

test.describe('组织架构模块', () => {
  test.beforeEach(async ({ page, context }) => {
    await login(page, context);
  });

  test('大区管理页面', async ({ page }) => {
    await page.goto('/org/region');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-results/20-region-management.png' });

    const table = page.locator('.el-table, [class*="table"]');
    await expect(table.first()).toBeVisible({ timeout: 10000 }).catch(async () => {
      await page.screenshot({ path: 'test-results/20-region-error.png' });
      throw new Error('大区管理页面加载失败');
    });
  });

  test('办事处管理页面', async ({ page }) => {
    await page.goto('/org/office');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-results/21-office-management.png' });

    const table = page.locator('.el-table, [class*="table"]');
    await expect(table.first()).toBeVisible({ timeout: 10000 }).catch(async () => {
      await page.screenshot({ path: 'test-results/21-office-error.png' });
      throw new Error('办事处管理页面加载失败');
    });
  });

  test('片区管理页面', async ({ page }) => {
    await page.goto('/org/district');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-results/22-district-management.png' });

    const table = page.locator('.el-table, [class*="table"]');
    await expect(table.first()).toBeVisible({ timeout: 10000 }).catch(async () => {
      await page.screenshot({ path: 'test-results/22-district-error.png' });
      throw new Error('片区管理页面加载失败');
    });
  });
});
