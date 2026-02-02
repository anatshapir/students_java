import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Exercises from './pages/Exercises';
import ExerciseEditor from './pages/ExerciseEditor';
import Students from './pages/Students';
import Analytics from './pages/Analytics';
import Login from './pages/Login';
import Register from './pages/Register';
import Courses from './pages/Courses';
import CourseWizard from './pages/CourseWizard';
import EnrollmentRequests from './pages/EnrollmentRequests';
import StudentEnroll from './pages/StudentEnroll';
import GoogleOAuthCallback from './pages/GoogleOAuthCallback';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('token');
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/enroll/:code" element={<StudentEnroll />} />
      <Route path="/auth/google/callback" element={<GoogleOAuthCallback />} />

      {/* Protected routes */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="courses" element={<Courses />} />
        <Route path="courses/new" element={<CourseWizard />} />
        <Route path="courses/:courseId/enrollment-requests" element={<EnrollmentRequests />} />
        <Route path="exercises" element={<Exercises />} />
        <Route path="exercises/new" element={<ExerciseEditor />} />
        <Route path="exercises/:id" element={<ExerciseEditor />} />
        <Route path="students" element={<Students />} />
        <Route path="analytics" element={<Analytics />} />
      </Route>
    </Routes>
  );
}
