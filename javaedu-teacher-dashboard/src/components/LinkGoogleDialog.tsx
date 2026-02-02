import { useState, useEffect } from 'react';
import {
  getGoogleConnectionStatus,
  getGoogleClassroomCourses,
  linkToGoogleClassroom,
} from '../api/client';
import GoogleConnectButton from './GoogleConnectButton';
import type { Course, GoogleClassroomCourse } from '../types';

interface LinkGoogleDialogProps {
  course: Course;
  onClose: () => void;
  onLinked: (course: Course) => void;
}

export default function LinkGoogleDialog({
  course,
  onClose,
  onLinked,
}: LinkGoogleDialogProps) {
  const [googleConnected, setGoogleConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [linking, setLinking] = useState(false);
  const [classroomCourses, setClassroomCourses] = useState<GoogleClassroomCourse[]>([]);
  const [selectedCourse, setSelectedCourse] = useState('');
  const [autoSyncEnabled, setAutoSyncEnabled] = useState(true);
  const [syncNow, setSyncNow] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    checkConnection();
  }, []);

  const checkConnection = async () => {
    try {
      const status = await getGoogleConnectionStatus();
      setGoogleConnected(status.connected);
      if (status.connected) {
        await loadCourses();
      }
    } catch (err) {
      console.error('Failed to check connection:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadCourses = async () => {
    try {
      const courses = await getGoogleClassroomCourses();
      setClassroomCourses(courses);
    } catch (err) {
      console.error('Failed to load courses:', err);
    }
  };

  const handleGoogleConnected = () => {
    setGoogleConnected(true);
    loadCourses();
  };

  const handleLink = async () => {
    if (!selectedCourse) return;

    setLinking(true);
    setError('');

    try {
      const updated = await linkToGoogleClassroom(
        course.id,
        selectedCourse,
        autoSyncEnabled,
        syncNow
      );
      onLinked(updated);
    } catch (err) {
      setError('Failed to link course');
      console.error('Failed to link course:', err);
    } finally {
      setLinking(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-lg w-full max-w-lg">
        <div className="p-6 border-b">
          <h2 className="text-xl font-semibold text-gray-900">
            Link to Google Classroom
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            Connect {course.name} to a Google Classroom course
          </p>
        </div>

        <div className="p-6">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          ) : !googleConnected ? (
            <div className="text-center py-6">
              <p className="text-gray-600 mb-4">
                Connect your Google account to link this course to Google Classroom.
              </p>
              <GoogleConnectButton
                onConnected={handleGoogleConnected}
                showStatus={false}
              />
            </div>
          ) : (
            <div className="space-y-4">
              {error && (
                <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm">
                  {error}
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Select Google Classroom Course
                </label>
                <select
                  value={selectedCourse}
                  onChange={(e) => setSelectedCourse(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Choose a course...</option>
                  {classroomCourses.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} {c.section && `(${c.section})`}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-3 pt-2">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={syncNow}
                    onChange={(e) => setSyncNow(e.target.checked)}
                    className="rounded"
                  />
                  <span className="text-sm text-gray-700">
                    Import students from Google Classroom now
                  </span>
                </label>

                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={autoSyncEnabled}
                    onChange={(e) => setAutoSyncEnabled(e.target.checked)}
                    className="rounded"
                  />
                  <span className="text-sm text-gray-700">
                    Enable auto-sync (keep student roster in sync)
                  </span>
                </label>
              </div>

              <div className="bg-blue-50 rounded-lg p-4 mt-4">
                <p className="text-sm text-blue-800">
                  After linking, you can sync students, export grades, and keep your
                  course in sync with Google Classroom.
                </p>
              </div>
            </div>
          )}
        </div>

        <div className="p-6 border-t bg-gray-50 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-700 hover:bg-gray-200 rounded-lg transition-colors"
          >
            Cancel
          </button>
          {googleConnected && (
            <button
              onClick={handleLink}
              disabled={linking || !selectedCourse}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {linking ? 'Linking...' : 'Link Course'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
