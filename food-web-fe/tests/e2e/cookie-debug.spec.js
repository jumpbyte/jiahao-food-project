import { test, expect } from '@playwright/test';

test.describe('Cookie 调试', () => {
  test('验证 Cookie 设置和读取', async ({ page, context }) => {
    // 通过 API 登录获取 token
    const loginResponse = await page.request.post('/dev-api/api/auth/login', {
      data: { username: 'admin', password: 'admin123' }
    });
    const loginData = await loginResponse.json();
    const token = loginData.data.token;
    console.log('获取到的 token:', token);

    // 使用 context.addCookies 设置 Cookie（对所有页面有效）
    await context.addCookies([{
      name: 'Admin-Token',
      value: token,
      domain: 'localhost',
      path: '/',
    }]);

    // 监听所有请求，查看 Authorization 头
    page.on('request', request => {
      if (request.url().includes('getInfo')) {
        console.log('getInfo 请求 URL:', request.url());
        console.log('getInfo 请求头 Authorization:', request.headers()['authorization']);
      }
    });

    // 监听所有响应
    page.on('response', async response => {
      if (response.url().includes('getInfo')) {
        console.log('getInfo 响应状态:', response.status());
        const body = await response.json().catch(() => null);
        console.log('getInfo 响应内容:', body);
      }
    });

    // 现在导航到首页
    await page.goto('/');
    await page.waitForTimeout(3000);

    // 截图
    await page.screenshot({ path: 'test-results/debug-cookie-home.png' });

    // 检查 Cookie 是否存在
    const cookies = await context.cookies();
    console.log('导航后浏览器中的 Cookie:', cookies);

    // 检查 document.cookie
    const docCookies = await page.evaluate(() => document.cookie);
    console.log('document.cookie:', docCookies);

    // 输出 URL
    console.log('当前 URL:', page.url());
  });
});
