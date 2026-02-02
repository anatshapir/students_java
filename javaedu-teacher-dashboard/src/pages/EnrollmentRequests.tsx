import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getEnrollmentRequests,
  approveEnrollmentRequest,
  denyEnrollmentRequest,
  bulkApproveEnrollmentRequests,
  bulkDenyEnrollmentRequests,
  getCourse,
  getEnrollmentCode,
  regenerateEnrollmentCode,
} from '../api/client';
import type { EnrollmentRequest, Course, EnrollmentCodeResponse } from '../types';
import { formatDistanceToNow } from 'date-fns';

export default function EnrollmentRequests() {
  const { courseId } = useParams<{ courseId: string }>();
  const navigate = useNavigate();
  const [course, setCourse] = useState<Course | null>(null);
  const [requests, setRequests] = useState<EnrollmentRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [enrollmentCode, setEnrollmentCode] = useState<EnrollmentCodeResponse | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [processing, setProcessing] = useState<number | null>(null);
  const [showCopied, setShowCopied] = useState(false);

  useEffect(() => {
    if (courseId) {
      loadData();
    }
  }, [courseId]);

  const loadData = async () => {
    try {
      const [courseData, requestsData, codeData] = await Promise.all([
        getCourse(Number(courseId)),
        getEnrollmentRequests(Number(courseId)),
        getEnrollmentCode(Number(courseId)),
      ]);
      setCourse(courseData);
      setRequests(requestsData);
      setEnrollmentCode(codeData);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (requestId: number) => {
    setProcessing(requestId);
    try {
      await approveEnrollmentRequest(requestId);
      setRequests(requests.filter((r) => r.id !== requestId));
    } catch (error) {
      console.error('Failed to approve request:', error);
    } finally {
      setProcessing(null);
    }
  };

  const handleDeny = async (requestId: number) => {
    setProcessing(requestId);
    try {
      await denyEnrollmentRequest(requestId);
      setRequests(requests.filter((r) => r.id !== requestId));
    } catch (error) {
      console.error('Failed to deny request:', error);
    } finally {
      setProcessing(null);
    }
  };

  const handleBulkApprove = async () => {
    if (selectedIds.size === 0) return;
    setProcessing(-1);
    try {
      await bulkApproveEnrollmentRequests(Array.from(selectedIds));
      setRequests(requests.filter((r) => !selectedIds.has(r.id)));
      setSelectedIds(new Set());
    } catch (error) {
      console.error('Failed to bulk approve:', error);
    } finally {
      setProcessing(null);
    }
  };

  const handleBulkDeny = async () => {
    if (selectedIds.size === 0) return;
    setProcessing(-1);
    try {
      await bulkDenyEnrollmentRequests(Array.from(selectedIds));
      setRequests(requests.filter((r) => !selectedIds.has(r.id)));
      setSelectedIds(new Set());
    } catch (error) {
      console.error('Failed to bulk deny:', error);
    } finally {
      setProcessing(null);
    }
  };

  const handleRegenerateCode = async () => {
    if (!confirm('Regenerate enrollment code? The old code will stop working.')) return;
    try {
      const newCode = await regenerateEnrollmentCode(Number(courseId));
      setEnrollmentCode(newCode);
    } catch (error) {
      console.error('Failed to regenerate code:', error);
    }
  };

  const copyEnrollmentLink = () => {
    if (!enrollmentCode) return;
    const fullUrl = `${window.location.origin}${enrollmentCode.enrollmentUrl}`;
    navigator.clipboard.writeText(fullUrl);
    setShowCopied(true);
    setTimeout(() => setShowCopied(false), 2000);
  };

  const toggleSelect = (id: number) => {
    const newSet = new Set(selectedIds);
    if (newSet.has(id)) {
      newSet.delete(id);
    } else {
      newSet.add(id);
    }
    setSelectedIds(newSet);
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === requests.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(requests.map((r) => r.id)));
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <button
            onClick={() => navigate('/courses')}
            className="text-sm text-gray-500 hover:text-gray-700 flex items-center gap-1 mb-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to Courses
          </button>
          <h1 className="text-2xl font-bold text-gray-900">Enrollment Requests</h1>
          <p className="text-gray-600">{course?.name}</p>
        </div>
      </div>

      {/* Enrollment Link Card */}
      {enrollmentCode && (
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Enrollment Link</h2>
          <p className="text-sm text-gray-600 mb-4">
            Share this link with students so they can request to join your course.
          </p>
          <div className="flex items-center gap-3">
            <div className="flex-1 bg-gray-50 border rounded-lg px-4 py-2 font-mono text-sm">
              {window.location.origin}{enrollmentCode.enrollmentUrl}
            </div>
            <button
              onClick={copyEnrollmentLink}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              {showCopied ? (
                <>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  Copied!
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                  </svg>
                  Copy Link
                </>
              )}
            </button>
            <button
              onClick={handleRegenerateCode}
              className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
              title="Generate new code"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
          </div>
        </div>
      )}

      {/* Pending Requests */}
      <div className="bg-white rounded-lg shadow">
        <div className="p-6 border-b">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">
              Pending Requests ({requests.length})
            </h2>
            {requests.length > 0 && (
              <div className="flex items-center gap-2">
                <button
                  onClick={handleBulkApprove}
                  disabled={selectedIds.size === 0 || processing !== null}
                  className="px-3 py-1.5 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 disabled:opacity-50 transition-colors"
                >
                  Approve Selected
                </button>
                <button
                  onClick={handleBulkDeny}
                  disabled={selectedIds.size === 0 || processing !== null}
                  className="px-3 py-1.5 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors"
                >
                  Deny Selected
                </button>
              </div>
            )}
          </div>
        </div>

        {requests.length === 0 ? (
          <div className="p-12 text-center">
            <svg
              className="w-12 h-12 mx-auto mb-4 text-gray-300"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p className="text-gray-500">No pending enrollment requests</p>
          </div>
        ) : (
          <div className="divide-y">
            {/* Select all header */}
            <div className="px-6 py-3 bg-gray-50 flex items-center gap-4">
              <input
                type="checkbox"
                checked={selectedIds.size === requests.length}
                onChange={toggleSelectAll}
                className="rounded"
              />
              <span className="text-sm text-gray-600">Select all</span>
            </div>

            {/* Request rows */}
            {requests.map((request) => (
              <div
                key={request.id}
                className="px-6 py-4 flex items-center gap-4 hover:bg-gray-50"
              >
                <input
                  type="checkbox"
                  checked={selectedIds.has(request.id)}
                  onChange={() => toggleSelect(request.id)}
                  className="rounded"
                />
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-gray-900">{request.userName}</div>
                  <div className="text-sm text-gray-500">{request.userEmail}</div>
                </div>
                <div className="text-sm text-gray-500">
                  {formatDistanceToNow(new Date(request.requestedAt), { addSuffix: true })}
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleApprove(request.id)}
                    disabled={processing === request.id}
                    className="px-3 py-1.5 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 disabled:opacity-50 transition-colors"
                  >
                    {processing === request.id ? '...' : 'Approve'}
                  </button>
                  <button
                    onClick={() => handleDeny(request.id)}
                    disabled={processing === request.id}
                    className="px-3 py-1.5 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors"
                  >
                    {processing === request.id ? '...' : 'Deny'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
