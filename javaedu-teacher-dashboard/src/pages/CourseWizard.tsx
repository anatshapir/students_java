import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  createCourseWizard,
  getGoogleClassroomCourses,
  getGoogleConnectionStatus,
  getGoogleClassroomStudents,
} from '../api/client';
import GoogleConnectButton from '../components/GoogleConnectButton';
import type { GoogleClassroomCourse, GoogleClassroomStudent, CourseWizardData } from '../types';

type WizardStep = 'method' | 'details' | 'students' | 'review';
type CreationMethod = 'manual' | 'google' | null;

export default function CourseWizard() {
  const navigate = useNavigate();
  const [step, setStep] = useState<WizardStep>('method');
  const [method, setMethod] = useState<CreationMethod>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Google connection state
  const [googleConnected, setGoogleConnected] = useState(false);
  const [classroomCourses, setClassroomCourses] = useState<GoogleClassroomCourse[]>([]);
  const [selectedClassroom, setSelectedClassroom] = useState<string>('');
  const [classroomStudents, setClassroomStudents] = useState<GoogleClassroomStudent[]>([]);
  const [selectedStudents, setSelectedStudents] = useState<Set<string>>(new Set());
  const [loadingStudents, setLoadingStudents] = useState(false);

  // Course data
  const [courseName, setCourseName] = useState('');
  const [courseDescription, setCourseDescription] = useState('');
  const [autoSyncEnabled, setAutoSyncEnabled] = useState(true);
  const [manualEmails, setManualEmails] = useState('');

  // Check Google connection on mount
  useEffect(() => {
    checkGoogleConnection();
  }, []);

  const checkGoogleConnection = async () => {
    try {
      const status = await getGoogleConnectionStatus();
      setGoogleConnected(status.connected);
      if (status.connected) {
        loadClassroomCourses();
      }
    } catch (err) {
      console.error('Failed to check Google connection:', err);
    }
  };

  const loadClassroomCourses = async () => {
    try {
      const courses = await getGoogleClassroomCourses();
      setClassroomCourses(courses);
    } catch (err) {
      console.error('Failed to load classroom courses:', err);
    }
  };

  const handleGoogleConnected = () => {
    setGoogleConnected(true);
    loadClassroomCourses();
  };

  const handleClassroomSelect = async (courseId: string) => {
    setSelectedClassroom(courseId);
    const course = classroomCourses.find((c) => c.id === courseId);
    if (course) {
      setCourseName(course.name);
      setCourseDescription(course.description || '');
    }

    // Load students
    setLoadingStudents(true);
    try {
      const students = await getGoogleClassroomStudents(courseId);
      setClassroomStudents(students);
      setSelectedStudents(new Set(students.map((s) => s.email)));
    } catch (err) {
      console.error('Failed to load students:', err);
    } finally {
      setLoadingStudents(false);
    }
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError('');

    try {
      const data: CourseWizardData = {
        name: courseName,
        description: courseDescription || undefined,
      };

      if (method === 'google' && selectedClassroom) {
        data.googleClassroomId = selectedClassroom;
        data.autoSyncEnabled = autoSyncEnabled;
        data.syncStudentsNow = true;
      } else if (method === 'manual' && manualEmails.trim()) {
        data.studentEmails = manualEmails
          .split('\n')
          .map((e) => e.trim())
          .filter((e) => e);
      }

      await createCourseWizard(data);
      navigate('/courses');
    } catch (err) {
      setError('Failed to create course. Please try again.');
      console.error('Failed to create course:', err);
    } finally {
      setLoading(false);
    }
  };

  const canProceed = () => {
    switch (step) {
      case 'method':
        return method !== null;
      case 'details':
        return courseName.trim().length > 0;
      case 'students':
        return true; // Students are optional
      case 'review':
        return true;
      default:
        return false;
    }
  };

  const nextStep = () => {
    if (step === 'method') setStep('details');
    else if (step === 'details') setStep('students');
    else if (step === 'students') setStep('review');
  };

  const prevStep = () => {
    if (step === 'review') setStep('students');
    else if (step === 'students') setStep('details');
    else if (step === 'details') setStep('method');
  };

  const steps: WizardStep[] = ['method', 'details', 'students', 'review'];
  const stepIndex = steps.indexOf(step);

  return (
    <div className="max-w-3xl mx-auto">
      {/* Progress indicator */}
      <div className="mb-8">
        <div className="flex items-center justify-between">
          {steps.map((s, i) => (
            <div key={s} className="flex items-center">
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                  i <= stepIndex
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-200 text-gray-600'
                }`}
              >
                {i + 1}
              </div>
              <span
                className={`ml-2 text-sm ${
                  i <= stepIndex ? 'text-blue-600 font-medium' : 'text-gray-500'
                }`}
              >
                {s === 'method' && 'Choose Method'}
                {s === 'details' && 'Course Details'}
                {s === 'students' && 'Add Students'}
                {s === 'review' && 'Review'}
              </span>
              {i < steps.length - 1 && (
                <div
                  className={`w-16 h-0.5 mx-4 ${
                    i < stepIndex ? 'bg-blue-600' : 'bg-gray-200'
                  }`}
                />
              )}
            </div>
          ))}
        </div>
      </div>

      {error && (
        <div className="mb-6 bg-red-50 text-red-600 p-4 rounded-lg">{error}</div>
      )}

      <div className="bg-white rounded-lg shadow-lg p-8">
        {/* Step 1: Choose Method */}
        {step === 'method' && (
          <div className="space-y-6">
            <h2 className="text-2xl font-bold text-gray-900 text-center">
              Create New Course
            </h2>
            <p className="text-gray-600 text-center">
              How would you like to create your course?
            </p>

            <div className="grid grid-cols-2 gap-6 mt-8">
              <button
                onClick={() => setMethod('manual')}
                className={`p-6 border-2 rounded-lg text-left transition-all ${
                  method === 'manual'
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mb-4">
                  <svg
                    className="w-6 h-6 text-blue-600"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                    />
                  </svg>
                </div>
                <h3 className="text-lg font-semibold text-gray-900">Create Manually</h3>
                <p className="text-sm text-gray-600 mt-2">
                  Start from scratch and add students later
                </p>
              </button>

              <button
                onClick={() => setMethod('google')}
                className={`p-6 border-2 rounded-lg text-left transition-all ${
                  method === 'google'
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <div className="w-12 h-12 bg-white border rounded-lg flex items-center justify-center mb-4">
                  <svg className="w-6 h-6" viewBox="0 0 24 24">
                    <path
                      fill="#4285F4"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="#34A853"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="#FBBC05"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                    />
                    <path
                      fill="#EA4335"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                    />
                  </svg>
                </div>
                <h3 className="text-lg font-semibold text-gray-900">
                  Import from Google Classroom
                </h3>
                <p className="text-sm text-gray-600 mt-2">
                  Import course and students automatically
                </p>
              </button>
            </div>
          </div>
        )}

        {/* Step 2: Course Details */}
        {step === 'details' && (
          <div className="space-y-6">
            <h2 className="text-2xl font-bold text-gray-900">Course Details</h2>

            {method === 'google' && !googleConnected && (
              <div className="bg-gray-50 rounded-lg p-6 mb-6">
                <p className="text-gray-600 mb-4">
                  Connect your Google account to access your Google Classroom courses.
                </p>
                <GoogleConnectButton onConnected={handleGoogleConnected} showStatus={false} />
              </div>
            )}

            {method === 'google' && googleConnected && (
              <div className="space-y-4">
                <label className="block text-sm font-medium text-gray-700">
                  Select Google Classroom Course
                </label>
                <select
                  value={selectedClassroom}
                  onChange={(e) => handleClassroomSelect(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Choose a course...</option>
                  {classroomCourses.map((course) => (
                    <option key={course.id} value={course.id}>
                      {course.name} {course.section && `(${course.section})`}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Course Name *
              </label>
              <input
                type="text"
                value={courseName}
                onChange={(e) => setCourseName(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="Introduction to Java Programming"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                value={courseDescription}
                onChange={(e) => setCourseDescription(e.target.value)}
                rows={3}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="Learn the fundamentals of Java programming..."
              />
            </div>
          </div>
        )}

        {/* Step 3: Add Students */}
        {step === 'students' && (
          <div className="space-y-6">
            <h2 className="text-2xl font-bold text-gray-900">Add Students</h2>
            <p className="text-gray-600">
              You can add students now or skip and add them later.
            </p>

            {method === 'google' && loadingStudents && (
              <div className="text-center py-8 text-gray-500">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-2"></div>
                Loading students from Google Classroom...
              </div>
            )}

            {method === 'google' && !loadingStudents && classroomStudents.length === 0 && (
              <div className="bg-yellow-50 rounded-lg p-4">
                <p className="text-sm text-yellow-800">
                  No students found in the selected Google Classroom course, or the course hasn't been selected yet.
                  You can add students manually below or skip this step.
                </p>
              </div>
            )}

            {method === 'google' && !loadingStudents && classroomStudents.length > 0 && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">
                    {selectedStudents.size} of {classroomStudents.length} students selected
                  </span>
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={selectedStudents.size === classroomStudents.length}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedStudents(new Set(classroomStudents.map((s) => s.email)));
                        } else {
                          setSelectedStudents(new Set());
                        }
                      }}
                      className="rounded"
                    />
                    <span className="text-sm">Select all</span>
                  </label>
                </div>

                <div className="max-h-64 overflow-y-auto border rounded-lg divide-y">
                  {loadingStudents ? (
                    <div className="p-4 text-center text-gray-500">Loading students...</div>
                  ) : (
                    classroomStudents.map((student) => (
                      <label
                        key={student.userId}
                        className="flex items-center gap-3 p-3 hover:bg-gray-50 cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={selectedStudents.has(student.email)}
                          onChange={(e) => {
                            const newSet = new Set(selectedStudents);
                            if (e.target.checked) {
                              newSet.add(student.email);
                            } else {
                              newSet.delete(student.email);
                            }
                            setSelectedStudents(newSet);
                          }}
                          className="rounded"
                        />
                        <div>
                          <div className="text-sm font-medium text-gray-900">{student.name}</div>
                          <div className="text-sm text-gray-500">{student.email}</div>
                        </div>
                      </label>
                    ))
                  )}
                </div>

                <label className="flex items-center gap-2 mt-4">
                  <input
                    type="checkbox"
                    checked={autoSyncEnabled}
                    onChange={(e) => setAutoSyncEnabled(e.target.checked)}
                    className="rounded"
                  />
                  <span className="text-sm text-gray-700">
                    Enable auto-sync (automatically sync new students when they join Google Classroom)
                  </span>
                </label>
              </div>
            )}

            {/* Show manual email input for both manual method AND google method when no students loaded */}
            {(method === 'manual' || (method === 'google' && !loadingStudents && classroomStudents.length === 0)) && (
              <div className="space-y-4">
                <div className="bg-gray-50 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-gray-700 mb-2">
                    Add students manually
                  </h3>
                  <p className="text-sm text-gray-600 mb-3">
                    Enter email addresses, one per line:
                  </p>
                  <textarea
                    value={manualEmails}
                    onChange={(e) => setManualEmails(e.target.value)}
                    rows={5}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                    placeholder="student1@school.edu&#10;student2@school.edu&#10;student3@school.edu"
                  />
                </div>

                <div className="bg-blue-50 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-blue-800 mb-2">
                    Or share an enrollment link
                  </h3>
                  <p className="text-sm text-blue-700">
                    After creating the course, you'll get an enrollment link that students can use
                    to request access to your course.
                  </p>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Step 4: Review */}
        {step === 'review' && (
          <div className="space-y-6">
            <h2 className="text-2xl font-bold text-gray-900">Review Your Course</h2>

            <div className="bg-gray-50 rounded-lg p-6 space-y-4">
              <div>
                <span className="text-sm text-gray-500">Course Name</span>
                <p className="text-lg font-medium text-gray-900">{courseName}</p>
              </div>

              {courseDescription && (
                <div>
                  <span className="text-sm text-gray-500">Description</span>
                  <p className="text-gray-900">{courseDescription}</p>
                </div>
              )}

              <div>
                <span className="text-sm text-gray-500">Students</span>
                <p className="text-gray-900">
                  {method === 'google'
                    ? `${selectedStudents.size} students from Google Classroom`
                    : manualEmails.trim()
                    ? `${manualEmails.split('\n').filter((e) => e.trim()).length} students`
                    : 'No students added yet'}
                </p>
              </div>

              {method === 'google' && (
                <div>
                  <span className="text-sm text-gray-500">Google Sync</span>
                  <p className="text-gray-900">
                    {autoSyncEnabled ? 'Enabled (auto-sync on)' : 'Manual sync only'}
                  </p>
                </div>
              )}
            </div>

            <div className="bg-green-50 rounded-lg p-4">
              <p className="text-sm text-green-800">
                <strong>Ready to create!</strong> After creation, you can add exercises,
                track student progress, and manage your course.
              </p>
            </div>
          </div>
        )}

        {/* Navigation buttons */}
        <div className="flex justify-between mt-8 pt-6 border-t">
          <button
            onClick={step === 'method' ? () => navigate('/courses') : prevStep}
            className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
          >
            {step === 'method' ? 'Cancel' : 'Back'}
          </button>

          <div className="flex gap-3">
            {step === 'students' && (
              <button
                onClick={nextStep}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
              >
                Skip
              </button>
            )}

            {step === 'review' ? (
              <button
                onClick={handleSubmit}
                disabled={loading}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
              >
                {loading ? 'Creating...' : 'Create Course'}
              </button>
            ) : (
              <button
                onClick={nextStep}
                disabled={!canProceed()}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
              >
                Continue
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
