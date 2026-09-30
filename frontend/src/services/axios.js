import axios from 'axios';

const instance = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  withCredentials: true,
});
instance.defaults.headers.common['Content-Type'] = 'application/json';

let refreshPromise = null;

instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const url = original?.url || '';

    if (
      status === 401 &&
      original &&
      !original._retry &&
      !url.includes('/auth/login') &&
      !url.includes('/auth/signup') &&
      !url.includes('/auth/refresh')
    ) {
      original._retry = true;
      const AuthService = (await import('./AuthService')).default;
      try {
        if (!refreshPromise) {
          refreshPromise = AuthService.refresh().finally(() => {
            refreshPromise = null;
          });
        }
        await refreshPromise;
        original.headers = {
          ...original.headers,
          Authorization: `Bearer ${AuthService.getAccessToken()}`,
        };
        return instance(original);
      } catch (refreshError) {
        await AuthService.logout();
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default instance;
