import axios from 'axios';
import type {
  Course,
  Exercise,
  TestCase,
  Submission,
  StudentProgress,
  ExerciseAnalytics,
  DashboardStats,
  User,
  GoogleClassroomCourse,
  GoogleClassroomStudent,
  GoogleAuthUrlResponse,
  GoogleConnectionStatus,
  GoogleConnectionResponse,
  SyncStatus,
  SyncResult,
  SyncPreview,
  GradeExportPreview,
  EnrollmentCodeResponse,
  EnrollmentCourseInfo,
  EnrollmentRequest,
  EnrollmentRequestResponse,
  BulkActionResponse,
  CourseWizardData,
  CourseStudent,
  GenerateExerciseRequest,
  GeneratedExercise,
} from '../types';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth
interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  email: string;
  name: string;
  role: string;
}

export const login = async (email: string, password: string) => {
  const response = await api.post<LoginResponse>('/auth/login', {
    email,
    password,
  });
  localStorage.setItem('token', response.data.accessToken);
  const user: User = {
    id: response.data.userId,
    email: response.data.email,
    name: response.data.name,
    role: response.data.role as 'STUDENT' | 'TEACHER' | 'ADMIN',
  };
  return { token: response.data.accessToken, user };
};

export const register = async (name: string, email: string, password: string) => {
  const response = await api.post<{ accessToken: string; userId: number; email: string; name: string; role: string }>('/auth/register', {
    name,
    email,
    password,
    role: 'TEACHER',
  });
  localStorage.setItem('token', response.data.accessToken);
  return response.data;
};

export const logout = () => {
  localStorage.removeItem('token');
};

export const getCurrentUser = async () => {
  const response = await api.get<User>('/auth/me');
  return response.data;
};

// Courses
export const getCourses = async (filter?: 'all' | 'active' | 'archived') => {
  const params = filter && filter !== 'all' ? { filter } : {};
  const response = await api.get<Course[]>('/courses', { params });
  return response.data;
};

export const getCourse = async (id: number) => {
  const response = await api.get<Course>(`/courses/${id}`);
  return response.data;
};

export const createCourse = async (data: Partial<Course>) => {
  const response = await api.post<Course>('/courses', data);
  return response.data;
};

export const createCourseWizard = async (data: CourseWizardData) => {
  const response = await api.post<Course>('/courses/wizard', data);
  return response.data;
};

export const updateCourse = async (id: number, data: Partial<Course>) => {
  const response = await api.put<Course>(`/courses/${id}`, data);
  return response.data;
};

export const deleteCourse = async (id: number) => {
  await api.delete(`/courses/${id}`);
};

export const archiveCourse = async (id: number) => {
  const response = await api.post<Course>(`/courses/${id}/archive`);
  return response.data;
};

export const unarchiveCourse = async (id: number) => {
  const response = await api.post<Course>(`/courses/${id}/unarchive`);
  return response.data;
};

// Course Students
export const getCourseStudents = async (courseId: number) => {
  const response = await api.get<CourseStudent[]>(`/courses/${courseId}/students`);
  return response.data;
};

export const enrollStudentByEmail = async (courseId: number, emails: string[]) => {
  const response = await api.post<{ added: number; alreadyEnrolled: number }>(
    `/courses/${courseId}/students/bulk`,
    { emails }
  );
  return response.data;
};

export const removeStudent = async (courseId: number, studentId: number) => {
  await api.delete(`/courses/${courseId}/students/${studentId}`);
};

// Course Sync
export const getSyncStatus = async (courseId: number) => {
  const response = await api.get<SyncStatus>(`/courses/${courseId}/sync-status`);
  return response.data;
};

export const syncCourse = async (courseId: number) => {
  const response = await api.post<SyncResult>(`/courses/${courseId}/sync`);
  return response.data;
};

export const previewSync = async (courseId: number) => {
  const response = await api.get<SyncPreview>(`/courses/${courseId}/sync/preview`);
  return response.data;
};

export const applySyncChanges = async (
  courseId: number,
  emailsToAdd: string[],
  idsToRemove: number[]
) => {
  const response = await api.post<SyncResult>(`/courses/${courseId}/sync/apply`, {
    emailsToAdd,
    idsToRemove,
  });
  return response.data;
};

export const linkToGoogleClassroom = async (
  courseId: number,
  googleClassroomId: string,
  autoSyncEnabled?: boolean,
  syncNow?: boolean
) => {
  const response = await api.post<Course>(`/courses/${courseId}/link-google`, {
    googleClassroomId,
    autoSyncEnabled,
    syncNow,
  });
  return response.data;
};

export const unlinkFromGoogleClassroom = async (courseId: number) => {
  const response = await api.post<Course>(`/courses/${courseId}/unlink-google`);
  return response.data;
};

// Grade Export
export const previewGradeExport = async (courseId: number) => {
  const response = await api.get<GradeExportPreview>(
    `/courses/${courseId}/grades/export/preview`
  );
  return response.data;
};

export const exportGrades = async (courseId: number) => {
  await api.post(`/courses/${courseId}/grades/export`);
};

// Enrollment
export const getEnrollmentCode = async (courseId: number) => {
  const response = await api.get<EnrollmentCodeResponse>(
    `/courses/${courseId}/enrollment-code`
  );
  return response.data;
};

export const regenerateEnrollmentCode = async (courseId: number) => {
  const response = await api.post<EnrollmentCodeResponse>(
    `/courses/${courseId}/enrollment-code/regenerate`
  );
  return response.data;
};

export const getCourseByEnrollmentCode = async (code: string) => {
  const response = await api.get<EnrollmentCourseInfo>(`/enroll/${code}`);
  return response.data;
};

export const requestEnrollment = async (code: string) => {
  const response = await api.post<EnrollmentRequestResponse>(`/enroll/${code}`);
  return response.data;
};

export const getEnrollmentRequests = async (
  courseId: number,
  status: 'PENDING' | 'APPROVED' | 'DENIED' = 'PENDING'
) => {
  const response = await api.get<EnrollmentRequest[]>(
    `/courses/${courseId}/enrollment-requests`,
    { params: { status } }
  );
  return response.data;
};

export const getAllPendingEnrollmentRequests = async () => {
  const response = await api.get<EnrollmentRequest[]>('/enrollment-requests/pending');
  return response.data;
};

export const getPendingEnrollmentCount = async () => {
  const response = await api.get<{ count: number }>('/enrollment-requests/pending/count');
  return response.data.count;
};

export const approveEnrollmentRequest = async (requestId: number) => {
  await api.post(`/enrollment-requests/${requestId}/approve`);
};

export const denyEnrollmentRequest = async (requestId: number) => {
  await api.post(`/enrollment-requests/${requestId}/deny`);
};

export const bulkApproveEnrollmentRequests = async (requestIds: number[]) => {
  const response = await api.post<BulkActionResponse>('/enrollment-requests/bulk-approve', {
    requestIds,
  });
  return response.data;
};

export const bulkDenyEnrollmentRequests = async (requestIds: number[]) => {
  const response = await api.post<BulkActionResponse>('/enrollment-requests/bulk-deny', {
    requestIds,
  });
  return response.data;
};

// Google OAuth
export const getGoogleAuthUrl = async () => {
  const response = await api.get<GoogleAuthUrlResponse>('/auth/google');
  return response.data;
};

export const handleGoogleCallback = async (code: string) => {
  const response = await api.post<GoogleConnectionResponse>('/auth/google/callback', {
    code,
  });
  return response.data;
};

export const getGoogleConnectionStatus = async () => {
  const response = await api.get<GoogleConnectionStatus>('/auth/google/status');
  return response.data;
};

export const disconnectGoogle = async () => {
  await api.post('/auth/google/disconnect');
};

export const refreshGoogleToken = async () => {
  const response = await api.post<{ accessToken: string }>('/auth/google/refresh');
  return response.data.accessToken;
};

// Exercises
export const getExercises = async (courseId?: number) => {
  const params = courseId ? { courseId } : {};
  const response = await api.get<Exercise[]>('/exercises', { params });
  return response.data;
};

export const getExercise = async (id: number) => {
  const response = await api.get<Exercise>(`/exercises/${id}`);
  return response.data;
};

export const createExercise = async (data: Partial<Exercise> & { testCases?: TestCase[] }) => {
  const response = await api.post<Exercise>('/exercises', data);
  return response.data;
};

export const updateExercise = async (
  id: number,
  data: Partial<Exercise> & { testCases?: TestCase[] }
) => {
  const response = await api.put<Exercise>(`/exercises/${id}`, data);
  return response.data;
};

export const deleteExercise = async (id: number) => {
  await api.delete(`/exercises/${id}`);
};

export const publishExercise = async (id: number) => {
  const response = await api.post<Exercise>(`/exercises/${id}/publish`);
  return response.data;
};

// Students
export const getStudents = async (courseId: number) => {
  const response = await api.get<StudentProgress[]>(`/courses/${courseId}/students`);
  return response.data;
};

export const getStudentProgress = async (courseId: number, studentId: number) => {
  const response = await api.get<StudentProgress>(
    `/courses/${courseId}/students/${studentId}`
  );
  return response.data;
};

// Submissions
export const getSubmissions = async (exerciseId?: number, userId?: number) => {
  const params: Record<string, number> = {};
  if (exerciseId) params.exerciseId = exerciseId;
  if (userId) params.userId = userId;
  const response = await api.get<Submission[]>('/submissions', { params });
  return response.data;
};

export const getSubmission = async (id: number) => {
  const response = await api.get<Submission>(`/submissions/${id}`);
  return response.data;
};

// Analytics
export const getDashboardStats = async (courseId: number) => {
  const response = await api.get<DashboardStats>(`/dashboard/${courseId}/stats`);
  return response.data;
};

export const getExerciseAnalytics = async (exerciseId: number) => {
  const response = await api.get<ExerciseAnalytics>(
    `/analytics/exercises/${exerciseId}`
  );
  return response.data;
};

export const getStrugglingStudents = async (courseId: number) => {
  const response = await api.get<StudentProgress[]>(
    `/analytics/courses/${courseId}/struggling`
  );
  return response.data;
};

// Google Classroom (legacy - using stored access token)
export const getGoogleClassroomCourses = async (googleToken?: string) => {
  const headers = googleToken ? { 'X-Google-Token': googleToken } : {};
  const response = await api.get<GoogleClassroomCourse[]>('/google-classroom/courses', {
    headers,
  });
  return response.data;
};

export const getGoogleClassroomStudents = async (classroomId: string, googleToken?: string) => {
  const headers = googleToken ? { 'X-Google-Token': googleToken } : {};
  const response = await api.get<GoogleClassroomStudent[]>(
    `/google-classroom/courses/${classroomId}/students`,
    { headers }
  );
  return response.data;
};

export const syncGoogleClassroom = async (
  googleToken: string,
  classroomId: string,
  courseId: number
) => {
  const response = await api.post('/google-classroom/sync', {
    googleToken,
    classroomId,
    courseId,
  });
  return response.data;
};

// AI Exercise Generation
export const generateExercise = async (request: GenerateExerciseRequest) => {
  const response = await api.post<GeneratedExercise>('/ai/generate-exercise', request);
  return response.data;
};

export default api;
