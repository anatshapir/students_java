import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getCourses,
  deleteCourse,
  archiveCourse,
  unarchiveCourse,
  unlinkFromGoogleClassroom,
} from '../api/client';
import type { Course } from '../types';
import CourseCard from '../components/CourseCard';
import CourseTable from '../components/CourseTable';
import SyncConflictDialog from '../components/SyncConflictDialog';
import GradeExportDialog from '../components/GradeExportDialog';
import LinkGoogleDialog from '../components/LinkGoogleDialog';
import EditCourseModal from '../components/EditCourseModal';

type ViewMode = 'grid' | 'list';
type FilterMode = 'all' | 'active' | 'archived';

export default function Courses() {
  const navigate = useNavigate();
  const [courses, setCourses] = useState<Course[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [filter, setFilter] = useState<FilterMode>('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Modal states
  const [editCourse, setEditCourse] = useState<Course | null>(null);
  const [syncConflictCourse, setSyncConflictCourse] = useState<Course | null>(null);
  const [exportGradesCourse, setExportGradesCourse] = useState<Course | null>(null);
  const [linkGoogleCourse, setLinkGoogleCourse] = useState<Course | null>(null);

  useEffect(() => {
    loadCourses();
  }, []);

  const loadCourses = async () => {
    try {
      const data = await getCourses();
      setCourses(data);
    } catch (error) {
      console.error('Failed to load courses:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredCourses = useMemo(() => {
    let result = courses;

    // Apply status filter
    if (filter === 'active') {
      result = result.filter((c) => c.isActive !== false);
    } else if (filter === 'archived') {
      result = result.filter((c) => c.isActive === false);
    }

    // Apply search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      result = result.filter(
        (c) =>
          c.name.toLowerCase().includes(query) ||
          c.description?.toLowerCase().includes(query)
      );
    }

    return result;
  }, [courses, filter, searchQuery]);

  const handleDelete = async (course: Course) => {
    if (!confirm(`Are you sure you want to delete "${course.name}"? This cannot be undone.`))
      return;

    try {
      await deleteCourse(course.id);
      setCourses(courses.filter((c) => c.id !== course.id));
    } catch (error) {
      console.error('Failed to delete course:', error);
    }
  };

  const handleArchive = async (course: Course) => {
    try {
      const updated = await archiveCourse(course.id);
      setCourses(courses.map((c) => (c.id === course.id ? updated : c)));
    } catch (error) {
      console.error('Failed to archive course:', error);
    }
  };

  const handleUnarchive = async (course: Course) => {
    try {
      const updated = await unarchiveCourse(course.id);
      setCourses(courses.map((c) => (c.id === course.id ? updated : c)));
    } catch (error) {
      console.error('Failed to unarchive course:', error);
    }
  };

  const handleSyncWithConflictResolution = (course: Course) => {
    setSyncConflictCourse(course);
  };

  const handleUnlinkGoogle = async (course: Course) => {
    if (
      !confirm(
        'Unlink this course from Google Classroom? Students will remain enrolled but sync will stop.'
      )
    )
      return;

    try {
      const updated = await unlinkFromGoogleClassroom(course.id);
      setCourses(courses.map((c) => (c.id === course.id ? updated : c)));
    } catch (error) {
      console.error('Failed to unlink course:', error);
    }
  };

  const handleCourseUpdated = (updated: Course) => {
    setCourses(courses.map((c) => (c.id === updated.id ? updated : c)));
    setEditCourse(null);
  };

  const handleCourseLinked = (updated: Course) => {
    setCourses(courses.map((c) => (c.id === updated.id ? updated : c)));
    setLinkGoogleCourse(null);
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
        <h1 className="text-2xl font-bold text-gray-900">Courses</h1>
        <div className="flex items-center gap-3">
          {/* View toggle */}
          <div className="flex items-center bg-gray-100 rounded-lg p-1">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 rounded ${
                viewMode === 'grid' ? 'bg-white shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}
              title="Grid view"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"
                />
              </svg>
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded ${
                viewMode === 'list' ? 'bg-white shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}
              title="List view"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 6h16M4 10h16M4 14h16M4 18h16"
                />
              </svg>
            </button>
          </div>

          <button
            onClick={() => navigate('/courses/new')}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4v16m8-8H4"
              />
            </svg>
            New Course
          </button>
        </div>
      </div>

      {/* Search and Filters */}
      <div className="flex items-center gap-4">
        <div className="flex-1 relative">
          <svg
            className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            />
          </svg>
          <input
            type="text"
            placeholder="Search courses..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-500">Filter:</span>
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value as FilterMode)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          >
            <option value="all">All</option>
            <option value="active">Active</option>
            <option value="archived">Archived</option>
          </select>
        </div>
      </div>

      {/* Course List */}
      {filteredCourses.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-lg shadow">
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg
              className="w-8 h-8 text-gray-400"
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
          <h2 className="text-xl font-semibold text-gray-900 mb-2">
            {searchQuery || filter !== 'all' ? 'No courses found' : 'No courses yet'}
          </h2>
          <p className="text-gray-600 mb-4">
            {searchQuery || filter !== 'all'
              ? 'Try adjusting your search or filters.'
              : 'Create your first course or import from Google Classroom.'}
          </p>
          {!searchQuery && filter === 'all' && (
            <button
              onClick={() => navigate('/courses/new')}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Create Your First Course
            </button>
          )}
        </div>
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredCourses.map((course) => (
            <CourseCard
              key={course.id}
              course={course}
              onEdit={() => setEditCourse(course)}
              onDelete={() => handleDelete(course)}
              onArchive={() => handleArchive(course)}
              onUnarchive={() => handleUnarchive(course)}
              onSyncStudents={
                course.googleClassroomId ? () => handleSyncWithConflictResolution(course) : undefined
              }
              onExportGrades={
                course.googleClassroomId ? () => setExportGradesCourse(course) : undefined
              }
              onLinkGoogle={!course.googleClassroomId ? () => setLinkGoogleCourse(course) : undefined}
              onUnlinkGoogle={course.googleClassroomId ? () => handleUnlinkGoogle(course) : undefined}
              onManageStudents={!course.googleClassroomId ? () => navigate(`/students?courseId=${course.id}`) : undefined}
              onViewEnrollmentRequests={() => navigate(`/courses/${course.id}/enrollment-requests`)}
              onClick={() => navigate(`/exercises?courseId=${course.id}`)}
            />
          ))}
        </div>
      ) : (
        <CourseTable
          courses={filteredCourses}
          onEdit={(course) => setEditCourse(course)}
          onDelete={handleDelete}
          onArchive={handleArchive}
          onUnarchive={handleUnarchive}
          onSyncStudents={handleSyncWithConflictResolution}
          onExportGrades={(course) => setExportGradesCourse(course)}
          onLinkGoogle={(course) => setLinkGoogleCourse(course)}
          onUnlinkGoogle={handleUnlinkGoogle}
          onManageStudents={(course) => navigate(`/students?courseId=${course.id}`)}
          onViewEnrollmentRequests={(course) => navigate(`/courses/${course.id}/enrollment-requests`)}
          onClick={(course) => navigate(`/exercises?courseId=${course.id}`)}
        />
      )}

      {/* Modals */}
      {editCourse && (
        <EditCourseModal
          course={editCourse}
          onClose={() => setEditCourse(null)}
          onSaved={handleCourseUpdated}
        />
      )}

      {syncConflictCourse && (
        <SyncConflictDialog
          course={syncConflictCourse}
          onClose={() => setSyncConflictCourse(null)}
          onSynced={() => {
            loadCourses();
            setSyncConflictCourse(null);
          }}
        />
      )}

      {exportGradesCourse && (
        <GradeExportDialog
          course={exportGradesCourse}
          onClose={() => setExportGradesCourse(null)}
        />
      )}

      {linkGoogleCourse && (
        <LinkGoogleDialog
          course={linkGoogleCourse}
          onClose={() => setLinkGoogleCourse(null)}
          onLinked={handleCourseLinked}
        />
      )}
    </div>
  );
}
