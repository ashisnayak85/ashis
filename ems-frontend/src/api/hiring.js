import api from './client';

// All hiring endpoints. Every function returns the ApiResponse ({ success, message, data });
// pages read `res.data`. Errors reject with { message, errors, fieldErrors, status } (see client.js).

// --- Who am I / lookups / dashboard ---
export const getHiringMe = () => api.get('/api/hiring/me');
export const getHiringDashboard = () => api.get('/api/hiring/dashboard');
export const getHiringLookups = () => api.get('/api/hiring/lookups');
export const searchHiringEmployees = (keyword) => api.get('/api/hiring/employees', { params: { keyword } });

// --- Requisitions ---
export const searchRequisitions = (params) => api.get('/api/requisitions/search', { params });
export const getRequisition = (id) => api.get(`/api/requisitions/${id}`);
export const createRequisition = (body) => api.post('/api/requisitions', body);
export const updateRequisition = (id, body) => api.put(`/api/requisitions/${id}`, body);
export const submitRequisition = (id) => api.post(`/api/requisitions/${id}/submit`);
export const approveRequisition = (id, remarks) => api.post(`/api/requisitions/${id}/approve`, { remarks });
export const rejectRequisition = (id, remarks) => api.post(`/api/requisitions/${id}/reject`, { remarks });
// action: OPEN | HOLD | RESUME | CLOSE | CANCEL
export const changeRequisitionStatus = (id, action, remarks) => api.post(`/api/requisitions/${id}/status`, { action, remarks });
export const assignRecruiter = (id, recruiterId) => api.post(`/api/requisitions/${id}/assign-recruiter`, { recruiterId });
export const getPendingRequisitionApprovals = () => api.get('/api/requisitions/pending-approvals');

// --- Candidates ---
export const searchCandidates = (params) => api.get('/api/candidates/search', { params });
export const getCandidate = (id) => api.get(`/api/candidates/${id}`);
export const createCandidate = (body) => api.post('/api/candidates', body);
export const updateCandidate = (id, body) => api.put(`/api/candidates/${id}`, body);
export const anonymiseCandidate = (id) => api.post(`/api/candidates/${id}/anonymise`);

export function uploadResume(id, file) {
  const form = new FormData();
  form.append('file', file);
  return api.post(`/api/candidates/${id}/resume`, form);
}
// Resumes are NOT public URLs: they are fetched with the session cookie and shown from a blob.
export const fetchResumeBlob = (id) => api.get(`/api/candidates/${id}/resume`, { responseType: 'blob' });

// --- Applications (pipeline) ---
export const applyCandidate = (candidateId, requisitionId) => api.post('/api/applications', { candidateId, requisitionId });
export const getApplication = (id) => api.get(`/api/applications/${id}`);
export const getApplicationsByRequisition = (requisitionId) => api.get(`/api/applications/by-requisition/${requisitionId}`);
export const getApplicationsByCandidate = (candidateId) => api.get(`/api/applications/by-candidate/${candidateId}`);
export const moveApplication = (id, toStage, remarks) => api.post(`/api/applications/${id}/move`, { toStage, remarks });
export const getApplicationHistory = (id) => api.get(`/api/applications/${id}/history`);

// --- Interviews ---
export const scheduleInterview = (applicationId, body) => api.post(`/api/interviews/by-application/${applicationId}`, body);
export const getInterviewsByApplication = (applicationId) => api.get(`/api/interviews/by-application/${applicationId}`);
export const getMyInterviews = () => api.get('/api/interviews/my');
export const updateInterview = (id, body) => api.put(`/api/interviews/${id}`, body);
export const cancelInterview = (id, reason) => api.post(`/api/interviews/${id}/cancel`, { reason });
export const markInterviewStatus = (id, status) => api.post(`/api/interviews/${id}/status`, { status });
export const submitFeedback = (id, body) => api.post(`/api/interviews/${id}/feedback`, body);

// --- Offers ---
export const createOffer = (applicationId, body) => api.post(`/api/offers/by-application/${applicationId}`, body);
export const getOffersByApplication = (applicationId) => api.get(`/api/offers/by-application/${applicationId}`);
export const searchOffers = (params) => api.get('/api/offers/search', { params });
export const getOffer = (id) => api.get(`/api/offers/${id}`);
export const updateOffer = (id, body) => api.put(`/api/offers/${id}`, body);
export const submitOffer = (id) => api.post(`/api/offers/${id}/submit`);
export const approveOffer = (id, remarks) => api.post(`/api/offers/${id}/approve`, { remarks });
export const rejectOffer = (id, remarks) => api.post(`/api/offers/${id}/reject`, { remarks });
export const sendOffer = (id) => api.post(`/api/offers/${id}/send`);
export const acceptOffer = (id) => api.post(`/api/offers/${id}/accept`);
export const declineOffer = (id, reason) => api.post(`/api/offers/${id}/decline`, { reason });
export const revokeOffer = (id, reason) => api.post(`/api/offers/${id}/revoke`, { reason });
export const convertOffer = (id, body) => api.post(`/api/offers/${id}/convert`, body || {});
export const fetchOfferLetterBlob = (id) => api.get(`/api/offers/${id}/letter`, { responseType: 'blob' });
export const getPendingOfferApprovals = () => api.get('/api/offers/pending-approvals');
