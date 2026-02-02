import type { Course } from '../types';
import SyncStatusBadge from './SyncStatusBadge';
import CourseActions from './CourseActions';

interface CourseCardProps {
  course: Course;
  onEdit: () => void;
  onDelete: () => void;
  onArchive: () => void;
  onUnarchive: () => void;
  onSyncStudents?: () => void;
  onExportGrades?: () => void;
  onLinkGoogle?: () => void;
  onUnlinkGoogle?: () => void;
  onManageStudents?: () => void;
  onViewEnrollmentRequests?: () => void;
  onClick?: () => void;
  isSyncing?: boolean;
}

export default function CourseCard({
  course,
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
  isSyncing = false,
}: CourseCardProps) {
  const isLinked = !!course.googleClassroomId;
  const isArchived = course.isActive === false;

  return (
    <div
      className={`bg-white rounded-lg shadow hover:shadow-md transition-shadow ${
        isArchived ? 'opacity-60' : ''
      }`}
    >
      <div className="p-6">
        <div className="flex items-start justify-between">
          <div
            className="flex-1 cursor-pointer"
            onClick={onClick}
          >
            <div className="flex items-center gap-2">
              <h3 className="text-lg font-semibold text-gray-900">{course.name}</h3>
              {isArchived && (
                <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded-full">
                  Archived
                </span>
              )}
            </div>
            {course.description && (
              <p className="text-gray-600 text-sm mt-2 line-clamp-2">{course.description}</p>
            )}
          </div>
          <CourseActions
            course={course}
            onEdit={onEdit}
            onDelete={onDelete}
            onArchive={onArchive}
            onUnarchive={onUnarchive}
            onSyncStudents={onSyncStudents}
            onExportGrades={onExportGrades}
            onLinkGoogle={onLinkGoogle}
            onUnlinkGoogle={onUnlinkGoogle}
            onManageStudents={onManageStudents}
            onViewEnrollmentRequests={onViewEnrollmentRequests}
          />
        </div>

        <div className="mt-4 flex items-center gap-4 text-sm text-gray-500">
          <div className="flex items-center gap-1">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
              />
            </svg>
            <span>{course.studentCount} students</span>
          </div>
          <div className="flex items-center gap-1">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
              />
            </svg>
            <span>{course.exerciseCount} exercises</span>
          </div>
        </div>

        {isLinked && (
          <div className="mt-3">
            <SyncStatusBadge
              isLinked={isLinked}
              lastSyncedAt={course.lastSyncedAt}
              isSyncing={isSyncing}
            />
          </div>
        )}
      </div>
    </div>
  );
}
