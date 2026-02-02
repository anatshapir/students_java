import { useState, useEffect } from 'react';
import { previewSync, applySyncChanges } from '../api/client';
import type { Course, SyncPreview, SyncStudentPreview } from '../types';

interface SyncConflictDialogProps {
  course: Course;
  onClose: () => void;
  onSynced: () => void;
}

export default function SyncConflictDialog({
  course,
  onClose,
  onSynced,
}: SyncConflictDialogProps) {
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [preview, setPreview] = useState<SyncPreview | null>(null);
  const [error, setError] = useState('');
  const [selectedToAdd, setSelectedToAdd] = useState<Set<string>>(new Set());
  const [selectedToRemove, setSelectedToRemove] = useState<Set<number>>(new Set());

  useEffect(() => {
    loadPreview();
  }, []);

  const loadPreview = async () => {
    try {
      const data = await previewSync(course.id);
      setPreview(data);
      // Select all to add by default
      setSelectedToAdd(new Set(data.toAdd.map((s) => s.email)));
      // Don't select any to remove by default
      setSelectedToRemove(new Set());
    } catch (err) {
      setError('Failed to load sync preview');
      console.error('Failed to load sync preview:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleApply = async () => {
    setSyncing(true);
    setError('');

    try {
      await applySyncChanges(
        course.id,
        Array.from(selectedToAdd),
        Array.from(selectedToRemove)
      );
      onSynced();
    } catch (err) {
      setError('Failed to apply sync changes');
      console.error('Failed to apply sync changes:', err);
    } finally {
      setSyncing(false);
    }
  };

  const toggleAddStudent = (email: string) => {
    const newSet = new Set(selectedToAdd);
    if (newSet.has(email)) {
      newSet.delete(email);
    } else {
      newSet.add(email);
    }
    setSelectedToAdd(newSet);
  };

  const toggleRemoveStudent = (id: number) => {
    const newSet = new Set(selectedToRemove);
    if (newSet.has(id)) {
      newSet.delete(id);
    } else {
      newSet.add(id);
    }
    setSelectedToRemove(newSet);
  };

  const hasNoChanges =
    preview && preview.toAdd.length === 0 && preview.toRemove.length === 0;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-lg w-full max-w-2xl max-h-[80vh] overflow-hidden flex flex-col">
        <div className="p-6 border-b">
          <h2 className="text-xl font-semibold text-gray-900">
            Google Classroom Sync - Review Changes
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            Review and select which changes to apply for {course.name}
          </p>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          ) : error ? (
            <div className="bg-red-50 text-red-600 p-4 rounded-lg">{error}</div>
          ) : hasNoChanges ? (
            <div className="text-center py-8 text-gray-500">
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
              <p className="font-medium text-gray-900">Already synced!</p>
              <p className="text-sm mt-1">
                The course roster matches Google Classroom.
              </p>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Students to Add */}
              {preview && preview.toAdd.length > 0 && (
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-sm font-medium text-gray-900">
                      Add ({preview.toAdd.length} new students)
                    </h3>
                    <button
                      onClick={() => {
                        if (selectedToAdd.size === preview.toAdd.length) {
                          setSelectedToAdd(new Set());
                        } else {
                          setSelectedToAdd(new Set(preview.toAdd.map((s) => s.email)));
                        }
                      }}
                      className="text-sm text-blue-600 hover:text-blue-700"
                    >
                      {selectedToAdd.size === preview.toAdd.length
                        ? 'Deselect all'
                        : 'Select all'}
                    </button>
                  </div>
                  <div className="border rounded-lg divide-y max-h-48 overflow-y-auto">
                    {preview.toAdd.map((student) => (
                      <StudentRow
                        key={student.email}
                        student={student}
                        selected={selectedToAdd.has(student.email)}
                        onToggle={() => toggleAddStudent(student.email)}
                        type="add"
                      />
                    ))}
                  </div>
                </div>
              )}

              {/* Students to Remove */}
              {preview && preview.toRemove.length > 0 && (
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-sm font-medium text-gray-900">
                      Remove ({preview.toRemove.length} students no longer in Google
                      Classroom)
                    </h3>
                    <button
                      onClick={() => {
                        if (selectedToRemove.size === preview.toRemove.length) {
                          setSelectedToRemove(new Set());
                        } else {
                          setSelectedToRemove(
                            new Set(preview.toRemove.map((s) => s.id!))
                          );
                        }
                      }}
                      className="text-sm text-blue-600 hover:text-blue-700"
                    >
                      {selectedToRemove.size === preview.toRemove.length
                        ? 'Deselect all'
                        : 'Select all'}
                    </button>
                  </div>
                  <div className="border rounded-lg divide-y max-h-48 overflow-y-auto">
                    {preview.toRemove.map((student) => (
                      <StudentRow
                        key={student.id}
                        student={student}
                        selected={selectedToRemove.has(student.id!)}
                        onToggle={() => toggleRemoveStudent(student.id!)}
                        type="remove"
                      />
                    ))}
                  </div>
                </div>
              )}
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
          {!hasNoChanges && (
            <button
              onClick={handleApply}
              disabled={
                syncing || (selectedToAdd.size === 0 && selectedToRemove.size === 0)
              }
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {syncing ? 'Applying...' : 'Apply Selected Changes'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function StudentRow({
  student,
  selected,
  onToggle,
  type,
}: {
  student: SyncStudentPreview;
  selected: boolean;
  onToggle: () => void;
  type: 'add' | 'remove';
}) {
  return (
    <label className="flex items-center gap-3 p-3 hover:bg-gray-50 cursor-pointer">
      <input
        type="checkbox"
        checked={selected}
        onChange={onToggle}
        className="rounded"
      />
      <div className="flex-1 min-w-0">
        <div className="text-sm font-medium text-gray-900 truncate">
          {student.name}
        </div>
        <div className="text-sm text-gray-500 truncate">{student.email}</div>
      </div>
      {type === 'remove' && student.submissionCount > 0 && (
        <div className="text-xs text-amber-600 bg-amber-50 px-2 py-1 rounded">
          {student.submissionCount} submissions
        </div>
      )}
    </label>
  );
}
