import { useState, useEffect } from 'react';
import { previewGradeExport, exportGrades } from '../api/client';
import type { Course, GradeExportPreview } from '../types';

interface GradeExportDialogProps {
  course: Course;
  onClose: () => void;
}

export default function GradeExportDialog({ course, onClose }: GradeExportDialogProps) {
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [preview, setPreview] = useState<GradeExportPreview | null>(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    loadPreview();
  }, []);

  const loadPreview = async () => {
    try {
      const data = await previewGradeExport(course.id);
      setPreview(data);
    } catch (err) {
      setError('Failed to load grade export preview');
      console.error('Failed to load preview:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async () => {
    setExporting(true);
    setError('');

    try {
      await exportGrades(course.id);
      setSuccess(true);
    } catch (err) {
      setError('Failed to export grades');
      console.error('Failed to export grades:', err);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-lg w-full max-w-lg">
        <div className="p-6 border-b">
          <h2 className="text-xl font-semibold text-gray-900">
            Export Grades to Google Classroom
          </h2>
          <p className="text-sm text-gray-600 mt-1">{course.name}</p>
        </div>

        <div className="p-6">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          ) : error ? (
            <div className="bg-red-50 text-red-600 p-4 rounded-lg">{error}</div>
          ) : success ? (
            <div className="text-center py-6">
              <svg
                className="w-12 h-12 mx-auto mb-4 text-green-500"
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
              <p className="font-medium text-gray-900">Grades exported successfully!</p>
              <p className="text-sm text-gray-600 mt-1">
                Students can now see their grades in Google Classroom.
              </p>
            </div>
          ) : preview ? (
            <div className="space-y-4">
              <div className="bg-gray-50 rounded-lg p-4">
                <p className="text-sm text-gray-600 mb-3">
                  The following exercises will be exported as assignments in Google
                  Classroom with their corresponding grades:
                </p>

                {preview.exercises.length === 0 ? (
                  <p className="text-sm text-gray-500 italic">No exercises to export</p>
                ) : (
                  <div className="space-y-2 max-h-48 overflow-y-auto">
                    {preview.exercises.map((exercise) => (
                      <div
                        key={exercise.exerciseId}
                        className="flex items-center justify-between text-sm"
                      >
                        <span className="text-gray-900">{exercise.exerciseTitle}</span>
                        <span className="text-gray-500">
                          {exercise.studentsWithGrades} students
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="bg-blue-50 rounded-lg p-4">
                <p className="text-sm text-blue-800">
                  <strong>Total:</strong> {preview.totalStudentsWithGrades} grade entries
                  will be exported across {preview.exercises.length} exercises.
                </p>
              </div>

              {preview.exercises.length === 0 && (
                <div className="bg-amber-50 rounded-lg p-4">
                  <p className="text-sm text-amber-800">
                    No grades to export. Students need to submit exercises and receive
                    grades before they can be exported.
                  </p>
                </div>
              )}
            </div>
          ) : null}
        </div>

        <div className="p-6 border-t bg-gray-50 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-700 hover:bg-gray-200 rounded-lg transition-colors"
          >
            {success ? 'Close' : 'Cancel'}
          </button>
          {!success && preview && preview.exercises.length > 0 && (
            <button
              onClick={handleExport}
              disabled={exporting}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {exporting ? 'Exporting...' : 'Export Grades'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
