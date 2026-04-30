import { test, expect } from '@playwright/test';

test.describe('登录模块', () => {
  test('成功登录并访问首页', async ({ page, context }) => {
    // 捕获浏览器 console 输出
    page.on('console', msg => {
      console.log('[Browser]', msg.type(), msg.text());
    });

    // 通过 API 登录获取 token
    const loginResponse = await page.request.post('/dev-api/api/auth/login', {
      data: { username: 'admin', password: 'admin123' }
    });
    const loginData = await loginResponse.json();
    expect(loginData.code).toBe(0);
    const token = loginData.data.token;
    expect(token).toBeTruthy();

    // 同时设置 Cookie 和 localStorage
    await context.addCookies([{
      name: 'Admin-Token',
      value: token,
      domain: 'localhost',
      path: '/',
    }]);

    await page.addInitScript((token) => {
      localStorage.setItem('Admin-Token', token);
    }, token);

    // 访问首页
    await page.goto('/');
    await page.waitForTimeout(3000);

    // 检查 localStorage
    const localStorageToken = await page.evaluate(() => localStorage.getItem('Admin-Token'));
    console.log('页面加载后 localStorage token:', localStorageToken);

    // 截图
    await page.screenshot({ path: 'test-results/01-after-login.png' });

    // 验证登录成功
    const url = page.url();
    console.log('登录后 URL:', url);

    // 验证成功：URL 不包含 /login
    const isNotLoginPage = !url.includes('/login');
    expect(isNotLoginPage).toBeTruthy();
  });

  test('登录后检查侧边栏菜单', async ({ page, context }) => {
    // 通过 API 登录获取 token
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

    // 截图
    await page.screenshot({ path: 'test-results/02-sidebar-menu.png' });

    // 检查侧边栏
    const sidebar = page.locator('.sidebar-container, .el-aside, .el-menu, [class*="sidebar"]');
    await expect(sidebar.first()).toBeVisible({ timeout: 10000 });
  });
});
