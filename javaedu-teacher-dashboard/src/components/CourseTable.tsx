import type { Course } from '../types';
import SyncStatusBadge from './SyncStatusBadge';
import CourseActions from './CourseActions';

interface CourseTableProps {
  courses: Course[];
  onEdit: (course: Course) => void;
  onDelete: (course: Course) => void;
  onArchive: (course: Course) => void;
  onUnarchive: (course: Course) => void;
  onSyncStudents?: (course: Course) => void;
  onExportGrades?: (course: Course) => void;
  onLinkGoogle?: (course: Course) => void;
  onUnlinkGoogle?: (course: Course) => void;
  onManageStudents?: (course: Course) => void;
  onViewEnrollmentRequests?: (course: Course) => void;
  onClick?: (course: Course) => void;
  syncingCourseId?: number | null;
}

export default function CourseTable({
  courses,
  onEdit,
  onDelete,
  onArchive,
  onUnarchive,
  onSyncStudents,
  onExportGrades,
  onLinkGoogle,
  onUnlinkGoogle,
  onManageStudents,
  onViewEnrollmentRequests,
  onClick,
  syncingCourseId,
}: CourseTableProps) {
  if (courses.length === 0) {
    return (
      <div className="text-center py-12 bg-white rounded-lg shadow">
        <h2 className="text-xl font-semibold text-gray-900 mb-2">No courses found</h2>
        <p className="text-gray-600">Create your first course or adjust filters.</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Name
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Students
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Exercises
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Status
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Google Classroom
            </th>
            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {courses.map((course) => {
            const isLinked = !!course.googleClassroomId;
            const isArchived = course.isActive === false;
            const isSyncing = syncingCourseId === course.id;

            return (
              <tr
                key={course.id}
                className={`hover:bg-gray-50 ${isArchived ? 'opacity-60' : ''}`}
              >
                <td className="px-6 py-4">
                  <div
                    className="cursor-pointer"
                    onClick={() => onClick?.(course)}
                  >
                    <div className="text-sm font-medium text-gray-900">{course.name}</div>
                    {course.description && (
                      <div className="text-sm text-gray-500 truncate max-w-xs">
                        {course.description}
                      </div>
                    )}
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="text-sm text-gray-900">{course.studentCount}</span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="text-sm text-gray-900">{course.exerciseCount}</span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  {isArchived ? (
                    <span className="px-2 py-1 text-xs font-medium rounded-full bg-gray-100 text-gray-700">
                      Archived
                    </span>
                  ) : (
                    <span className="px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700">
                      Active
                    </span>
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  {isLinked ? (
                    <SyncStatusBadge
                      isLinked={isLinked}
                      lastSyncedAt={course.lastSyncedAt}
                      isSyncing={isSyncing}
                    />
                  ) : (
                    <span className="text-sm text-gray-400">Not linked</span>
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right">
                  <CourseActions
                    course={course}
                    onEdit={() => onEdit(course)}
                    onDelete={() => onDelete(course)}
                    onArchive={() => onArchive(course)}
                    onUnarchive={() => onUnarchive(course)}
                    onSyncStudents={onSyncStudents ? () => onSyncStudents(course) : undefined}
                    onExportGrades={onExportGrades ? () => onExportGrades(course) : undefined}
                    onLinkGoogle={onLinkGoogle ? () => onLinkGoogle(course) : undefined}
                    onUnlinkGoogle={onUnlinkGoogle ? () => onUnlinkGoogle(course) : undefined}
                    onManageStudents={onManageStudents ? () => onManageStudents(course) : undefined}
                    onViewEnrollmentRequests={
                      onViewEnrollmentRequests ? () => onViewEnrollmentRequests(course) : undefined
                    }
                  />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
