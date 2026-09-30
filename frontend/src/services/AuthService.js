import axios from './axios';

/** Access token kept in memory only (SEC-07). */
let accessToken = null;
/** User profile without the access token. */
let currentUser = null;

const applySession = (payload) => {
  accessToken = payload?.token ?? null;
  if (payload) {
    const { token, ...profile } = payload;
    currentUser = profile;
  } else {
    currentUser = null;
  }
};

const login = (body) => {
  const url = '/auth/login';
  return axios.post(url, body).then((response) => {
    applySession(response.data);
    return response.data;
  });
};

const signup = (body) => {
  const url = '/auth/signup';
  return axios.post(url, body).then((response) => response.data);
};

const refresh = () => {
  const url = '/auth/refresh';
  return axios.post(url).then((response) => {
    applySession(response.data);
    return response.data;
  });
};

const logout = () => {
  const url = '/auth/logout';
  const headers = accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
  return axios
    .post(url, null, { headers })
    .catch(() => undefined)
    .finally(() => {
      accessToken = null;
      currentUser = null;
    });
};

const getCurrentUser = () => currentUser;

const getAccessToken = () => accessToken;

const AuthService = {
  login,
  signup,
  logout,
  refresh,
  getCurrentUser,
  getAccessToken,
};

export default AuthService;
