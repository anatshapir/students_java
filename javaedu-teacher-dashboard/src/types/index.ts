export interface User {
  id: number;
  email: string;
  name: string;
  role: 'TEACHER' | 'STUDENT' | 'ADMIN';
}

export interface Course {
  id: number;
  name: string;
  description?: string;
  teacherId?: number;
  teacherName: string;
  studentCount: number;
  exerciseCount: number;
  isActive?: boolean;
  googleClassroomId?: string;
  autoSyncEnabled?: boolean;
  lastSyncedAt?: string;
  enrollmentCode?: string;
  githubOrg?: string;
  createdAt?: string;
}

export interface Exercise {
  id: number;
  title: string;
  description: string;
  starterCode: string;
  difficulty: 'BEGINNER' | 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT';
  points: number;
  category: string;
  isPublished: boolean;
  courseId: number;
  courseName: string;
  dueDate?: string;
  createdAt: string;
}

export interface TestCase {
  id: number;
  name: string;
  testCode: string;           // Java assertion code for the test
  input: string;              // Kept for backward compatibility
  expectedOutput: string;     // Kept for backward compatibility
  isHidden: boolean;
  points: number;
  description?: string;       // Human-readable description of what this test checks
}

export interface Submission {
  id: number;
  exerciseId: number;
  exerciseTitle: string;
  userId: number;
  userName: string;
  code: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  score?: number;
  maxScore?: number;
  submittedAt: string;
}

export interface StudentProgress {
  userId: number;
  userName: string;
  email: string;
  completedExercises: number;
  totalExercises: number;
  totalPoints: number;
  maxPoints: number;
  averageScore: number;
  lastActivity?: string;
}

export interface ExerciseAnalytics {
  exerciseId: number;
  exerciseTitle: string;
  totalSubmissions: number;
  uniqueStudents: number;
  completedStudents: number;
  averageAttempts: number;
  averageTimeToComplete: number;
  commonErrors: CommonError[];
}

export interface CommonError {
  pattern: string;
  occurrenceCount: number;
  suggestedHint?: string;
}

export interface DashboardStats {
  totalStudents: number;
  totalExercises: number;
  totalSubmissions: number;
  averageCompletionRate: number;
  recentSubmissions: Submission[];
  strugglingStudents: StrugglingStudent[];
}

export interface StrugglingStudent {
  userId: number;
  userName: string;
  email: string;
  incompleteExercises: number;
  totalAttempts: number;
}

export interface GoogleClassroomCourse {
  id: string;
  name: string;
  section?: string;
  description?: string;
}

export interface GoogleClassroomStudent {
  userId: string;
  email: string;
  name: string;
}

// Google OAuth types
export interface GoogleAuthUrlResponse {
  authUrl: string;
}

export interface GoogleConnectionStatus {
  connected: boolean;
  email?: string;
  connectedAt?: string;
}

export interface GoogleConnectionResponse {
  connected: boolean;
  email?: string;
  connectedAt?: string;
  accessToken?: string;
}

// Sync types
export interface SyncStatus {
  isLinked: boolean;
  googleClassroomId?: string;
  lastSyncedAt?: string;
  autoSyncEnabled?: boolean;
}

export interface SyncResult {
  success: boolean;
  added: number;
  removed: number;
  error?: string;
}

export interface SyncStudentPreview {
  id?: number;
  name: string;
  email: string;
  submissionCount: number;
}

export interface SyncPreview {
  toAdd: SyncStudentPreview[];
  toRemove: SyncStudentPreview[];
}

// Grade export types
export interface ExerciseExportPreview {
  exerciseId: number;
  exerciseTitle: string;
  studentsWithGrades: number;
}

export interface GradeExportPreview {
  exercises: ExerciseExportPreview[];
  totalStudentsWithGrades: number;
}

// Enrollment types
export interface EnrollmentCodeResponse {
  code: string;
  enrollmentUrl: string;
}

export interface EnrollmentCourseInfo {
  id: number;
  name: string;
  description?: string;
  teacherName: string;
}

export interface EnrollmentRequest {
  id: number;
  courseId: number;
  courseName: string;
  userId: number;
  userName: string;
  userEmail: string;
  status: 'PENDING' | 'APPROVED' | 'DENIED';
  requestedAt: string;
}

export interface EnrollmentRequestResponse {
  id: number;
  status: string;
  message: string;
}

export interface BulkActionResponse {
  succeeded: number;
  failed: number;
}

// AI Exercise Generation types
export interface GenerateExerciseRequest {
  prompt: string;
  image?: string;              // Base64-encoded image
  imageMediaType?: string;     // "image/png", "image/jpeg", etc.
  difficulty?: 'BEGINNER' | 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT';
  category?: string;
  numberOfTestCases?: number;
}

export interface GeneratedTestCase {
  name: string;
  testCode: string;
  isHidden: boolean;
  points: number;
  description: string;
}

export interface GeneratedExercise {
  title: string;
  description: string;
  starterCode: string;
  solutionCode: string;
  difficulty: 'BEGINNER' | 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT';
  category: string;
  points: number;
  testCases: GeneratedTestCase[];
  hints?: string[];
}

// Course wizard types
export interface CourseWizardData {
  name: string;
  description?: string;
  googleClassroomId?: string;
  autoSyncEnabled?: boolean;
  syncStudentsNow?: boolean;
  studentEmails?: string[];
}

// Student type for course management
export interface CourseStudent {
  id: number;
  name: string;
  email: string;
  googleId?: string;
}
