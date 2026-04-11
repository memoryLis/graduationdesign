/**
 * 分转元（后端价格单位为分，前端展示为元）
 */
export function formatPrice(value) {
  return Number((Number(value || 0) / 100).toFixed(2));
}

/**
 * 格式化价格显示字符串
 */
export function displayPrice(value) {
  return formatPrice(value).toFixed(2);
}

/**
 * 从 sessionStorage / localStorage 获取用户信息
 */
export function getStoredUserInfo() {
  try {
    const raw = localStorage.getItem("xl_user");
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

/**
 * 存储用户信息
 */
export function setStoredUserInfo(info) {
  if (info) {
    localStorage.setItem("xl_user", JSON.stringify(info));
  } else {
    localStorage.removeItem("xl_user");
  }
}

export function isAdminUser(info) {
  return info?.username === "123";
}
