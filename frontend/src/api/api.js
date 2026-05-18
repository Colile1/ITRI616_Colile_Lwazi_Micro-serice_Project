// api.js : centralised API client that always routes through the API Gateway
import axios from 'axios';

const GATEWAY_URL = process.env.REACT_APP_API_URL || '';

// Pure function: builds an axios instance with the stored JWT token
const createApiClient = () => {
  const token = localStorage.getItem('token');
  return axios.create({
    baseURL: GATEWAY_URL,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    timeout: 10000,
  });
};

// Input validation: strips HTML tags and dangerous characters client-side
const sanitiseInput = (value) => {
  if (typeof value !== 'string') return value;
  return value.replace(/[<>"'%;()&+]/g, '').trim();
};

// Pure function: sanitises all string fields in an object recursively
const sanitiseObject = (obj) => {
  if (typeof obj !== 'object' || obj === null) return obj;
  const sanitised = {};
  for (const key of Object.keys(obj)) {
    sanitised[key] = typeof obj[key] === 'string' ? sanitiseInput(obj[key]) : obj[key];
  }
  return sanitised;
};

export const authApi = {
  login: (username, password) =>
    createApiClient().post('/auth/login', sanitiseObject({ username, password })),

  register: (username, email, password, role) =>
    createApiClient().post('/auth/register', sanitiseObject({ username, email, password, role })),
};

export const leaveApi = {
  getLeaveRequests: () =>
    createApiClient().get('/leave'),

  createLeaveRequest: (data) =>
    createApiClient().post('/leave', data),

  cancelLeaveRequest: (id) =>
    createApiClient().delete(`/leave/${id}`),

  approveLeaveRequest: (id, comment) =>
    createApiClient().put(`/leave/${id}/approve`, { comment: sanitiseInput(comment || '') }),

  rejectLeaveRequest: (id, comment) =>
    createApiClient().put(`/leave/${id}/reject`, { comment: sanitiseInput(comment || '') }),
};
