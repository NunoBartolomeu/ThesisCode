
export const fetchWithAuth = async (url: string, options: RequestInit = {}) => {
  const stored = localStorage.getItem('auth_tokens');
  const tokens = stored ? JSON.parse(stored) : null;

  if (tokens?.accessToken) {
    options.headers = {
      ...(options.headers || {}),
      Authorization: `Bearer ${tokens.accessToken}`,
    };
  }

  let response = await fetch(url, options);

  if (response.status === 401 && tokens?.refreshToken) {
    // Try refreshing the token
    const refreshResponse = await fetch('http://localhost:8080/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: tokens.refreshToken }),
    });

    if (refreshResponse.ok) {
      const newTokens = await refreshResponse.json();
      localStorage.setItem('auth_tokens', JSON.stringify(newTokens));

      // Retry the original request with new token
      options.headers = {
        ...(options.headers || {}),
        Authorization: `Bearer ${newTokens.accessToken}`,
      };
      response = await fetch(url, options);
    } else {
      // Refresh failed, logout user
      localStorage.removeItem('auth_tokens');
      window.location.href = '/login'; // force logout/redirect
    }
  }

  return response;
};


