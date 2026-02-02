import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getCourseByEnrollmentCode, requestEnrollment } from '../api/client';
import type { EnrollmentCourseInfo } from '../types';

export default function StudentEnroll() {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const [course, setCourse] = useState<EnrollmentCourseInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (code) {
      loadCourse();
    }
  }, [code]);

  const loadCourse = async () => {
    try {
      const data = await getCourseByEnrollmentCode(code!);
      setCourse(data);
    } catch (err) {
      setError('Invalid or expired enrollment link');
      console.error('Failed to load course:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRequestEnrollment = async () => {
    setSubmitting(true);
    setError('');

    try {
      await requestEnrollment(code!);
      setSuccess(true);
    } catch (err: any) {
      if (err.response?.status === 401) {
        // Not logged in, redirect to login
        localStorage.setItem('enrollRedirect', `/enroll/${code}`);
        navigate('/login');
        return;
      }
      setError(err.response?.data?.message || 'Failed to submit enrollment request');
      console.error('Failed to request enrollment:', err);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error && !course) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-lg max-w-md w-full p-8 text-center">
          <svg
            className="w-16 h-16 mx-auto mb-4 text-red-500"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <h1 className="text-xl font-bold text-gray-900 mb-2">Invalid Link</h1>
          <p className="text-gray-600 mb-6">{error}</p>
          <button
            onClick={() => navigate('/')}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Go to Dashboard
          </button>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-lg max-w-md w-full p-8 text-center">
          <svg
            className="w-16 h-16 mx-auto mb-4 text-green-500"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <h1 className="text-xl font-bold text-gray-900 mb-2">Request Submitted!</h1>
          <p className="text-gray-600 mb-6">
            Your enrollment request for <strong>{course?.name}</strong> has been submitted.
            You'll receive access once your teacher approves the request.
          </p>
          <button
            onClick={() => navigate('/')}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Go to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-lg max-w-md w-full p-8">
        <div className="text-center mb-6">
          <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg
              className="w-8 h-8 text-blue-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
              />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Join Course</h1>
        </div>

        <div className="bg-gray-50 rounded-lg p-4 mb-6">
          <h2 className="font-semibold text-gray-900 text-lg">{course?.name}</h2>
          {course?.description && (
            <p className="text-gray-600 text-sm mt-2">{course.description}</p>
          )}
          <p className="text-sm text-gray-500 mt-3">
            Instructor: {course?.teacherName}
          </p>
        </div>

        {error && (
          <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm mb-4">
            {error}
          </div>
        )}

        <p className="text-gray-600 text-sm mb-6">
          By requesting access, your enrollment will be sent to the instructor for approval.
          You'll be notified once your request is approved.
        </p>

        <button
          onClick={handleRequestEnrollment}
          disabled={submitting}
          className="w-full px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors font-medium"
        >
          {submitting ? 'Submitting...' : 'Request Access'}
        </button>

        <p className="text-center text-sm text-gray-500 mt-4">
          Already enrolled?{' '}
          <button
            onClick={() => navigate('/')}
            className="text-blue-600 hover:text-blue-700"
          >
            Go to Dashboard
          </button>
        </p>
      </div>
    </div>
  );
}
