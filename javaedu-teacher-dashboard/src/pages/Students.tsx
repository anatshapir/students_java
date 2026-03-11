import { useEffect, useState } from 'react';
import { getCourses, getStudents, enrollStudentByEmail } from '../api/client';
import type { Course, StudentProgress } from '../types';

export default function Students() {
  const [courses, setCourses] = useState<Course[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [students, setStudents] = useState<StudentProgress[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);

  useEffect(() => {
    loadCourses();
  }, []);

  useEffect(() => {
    if (selectedCourse) {
      loadStudents(selectedCourse);
    }
  }, [selectedCourse]);

  const loadCourses = async () => {
    try {
      const data = await getCourses();
      setCourses(data);
      if (data.length > 0) {
        setSelectedCourse(data[0].id);
      }
    } catch (error) {
      console.error('Failed to load courses:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadStudents = async (courseId: number) => {
    try {
      const data = await getStudents(courseId);
      setStudents(data);
    } catch (error) {
      console.error('Failed to load students:', error);
    }
  };

  const handleStudentsAdded = () => {
    if (selectedCourse) {
      loadStudents(selectedCourse);
    }
  };

  const filteredStudents = students.filter(
    (s) =>
      s.userName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Students</h1>
        <div className="flex items-center gap-3">
          <select
            value={selectedCourse || ''}
            onChange={(e) => setSelectedCourse(Number(e.target.value))}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          >
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.name}
              </option>
            ))}
          </select>
          {selectedCourse && (
            <button
              onClick={() => setShowAddModal(true)}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              + Add Students
            </button>
          )}
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="flex-1">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search students..."
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="text-sm text-gray-500">{filteredStudents.length} students</div>
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Student
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Progress
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Points
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Avg Score
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Last Activity
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredStudents.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-gray-500">
                  No students found
                </td>
              </tr>
            ) : (
              filteredStudents.map((student) => (
                <StudentRow key={student.userId} student={student} />
              ))
            )}
          </tbody>
        </table>
      </div>

      {showAddModal && selectedCourse && (
        <AddStudentsModal
          courseId={selectedCourse}
          onClose={() => setShowAddModal(false)}
          onAdded={handleStudentsAdded}
        />
      )}
    </div>
  );
}

function AddStudentsModal({
  courseId,
  onClose,
  onAdded,
}: {
  courseId: number;
  onClose: () => void;
  onAdded: () => void;
}) {
  const [emails, setEmails] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ added: number; alreadyEnrolled: number } | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    const emailList = emails
      .split(/[\n,;]+/)
      .map((e) => e.trim())
      .filter((e) => e.length > 0);

    if (emailList.length === 0) {
      setError('Please enter at least one email address.');
      return;
    }

    setSubmitting(true);
    setError(null);
    setResult(null);

    try {
      const data = await enrollStudentByEmail(courseId, emailList);
      setResult(data);
      onAdded();
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to add students. Please try again.';
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Add Students</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Student email addresses
            </label>
            <textarea
              value={emails}
              onChange={(e) => setEmails(e.target.value)}
              rows={6}
              placeholder={"student1@example.com\nstudent2@example.com\nstudent3@example.com"}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-sm"
            />
            <p className="mt-1 text-xs text-gray-500">
              One email per line. Students without accounts will be created automatically.
            </p>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">
              {error}
            </div>
          )}

          {result && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-3 text-sm text-green-700">
              {result.added > 0 && <p>{result.added} student(s) added successfully.</p>}
              {result.alreadyEnrolled > 0 && (
                <p>{result.alreadyEnrolled} student(s) were already enrolled.</p>
              )}
              {result.added === 0 && result.alreadyEnrolled > 0 && (
                <p>All students were already enrolled.</p>
              )}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50 rounded-b-lg">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            {result ? 'Close' : 'Cancel'}
          </button>
          {!result && (
            <button
              onClick={handleSubmit}
              disabled={submitting || !emails.trim()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? 'Adding...' : 'Add Students'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function StudentRow({ student }: { student: StudentProgress }) {
  const progressPercent =
    student.totalExercises > 0
      ? Math.round((student.completedExercises / student.totalExercises) * 100)
      : 0;

  return (
    <tr className="hover:bg-gray-50">
      <td className="px-6 py-4 whitespace-nowrap">
        <div>
          <div className="text-sm font-medium text-gray-900">{student.userName}</div>
          <div className="text-sm text-gray-500">{student.email}</div>
        </div>
      </td>
      <td className="px-6 py-4 whitespace-nowrap">
        <div className="flex items-center gap-3">
          <div className="w-32 bg-gray-200 rounded-full h-2">
            <div
              className="bg-blue-600 h-2 rounded-full"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
          <span className="text-sm text-gray-600">
            {student.completedExercises}/{student.totalExercises}
          </span>
        </div>
      </td>
      <td className="px-6 py-4 whitespace-nowrap">
        <span className="text-sm text-gray-900">
          {student.totalPoints}/{student.maxPoints}
        </span>
      </td>
      <td className="px-6 py-4 whitespace-nowrap">
        <span
          className={`text-sm font-medium ${
            student.averageScore >= 70
              ? 'text-green-600'
              : student.averageScore >= 50
              ? 'text-yellow-600'
              : 'text-red-600'
          }`}
        >
          {student.averageScore.toFixed(1)}%
        </span>
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
        {student.lastActivity
          ? new Date(student.lastActivity).toLocaleDateString()
          : 'Never'}
      </td>
    </tr>
  );
}
