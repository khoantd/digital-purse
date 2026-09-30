import AuthHeader from './AuthHeader';
import axios from './axios';

/** Normalize Spring Page `{ content }` or a bare JSON array into an array. */
const asList = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data?.content)) return payload.data.content;
  if (Array.isArray(payload?.data)) return payload.data;
  return [];
};

const postWithoutAuth = (url, body) => {
  const request = axios.post(url, body);
  return request.then((response) => response.data);
};

const getWithAuth = (url) => {
  const request = axios.get(url, { headers: AuthHeader() });
  return request.then((response) => response.data);
};

/**
 * Backend list endpoints throw 404 when there are no records.
 * Treat that as an empty collection so the UI can show empty states.
 */
const getListWithAuth = (url) =>
  getWithAuth(url)
    .then(asList)
    .catch((error) => {
      if (error?.response?.status === 404) {
        return [];
      }
      throw error;
    });

const postWithAuth = (url, body, extraHeaders = {}) => {
  const request = axios.post(url, body, { headers: { ...AuthHeader(), ...extraHeaders } });
  return request.then((response) => response.data);
};

const putWithAuth = (url, body) => {
  const request = axios.put(url, body, { headers: AuthHeader() });
  return request.then((response) => response.data);
};

const deleteWithAuth = (url) => {
  const request = axios.delete(url, { headers: AuthHeader() });
  return request.then((response) => response.data);
};

const HttpService = {
  postWithoutAuth,
  getWithAuth,
  getListWithAuth,
  postWithAuth,
  putWithAuth,
  deleteWithAuth,
  asList,
};

export default HttpService;
