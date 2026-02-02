import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getCourses, getDashboardStats } from '../api/client';
import type { Course, DashboardStats, Submission, StrugglingStudent } from '../types';

export default function Dashboard() {
  const [courses, setCourses] = useState<Course[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadCourses();
  }, []);

  useEffect(() => {
    if (selectedCourse) {
      loadStats(selectedCourse);
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

  const loadStats = async (courseId: number) => {
    try {
      const data = await getDashboardStats(courseId);
      setStats(data);
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  if (loading) {
    return <LoadingSpinner />;
  }

  if (courses.length === 0) {
    return <EmptyState />;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
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
      </div>

      {stats && (
        <>
          <StatsCards stats={stats} />
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <RecentSubmissions submissions={stats.recentSubmissions} />
            <StrugglingStudentsCard students={stats.strugglingStudents} />
          </div>
        </>
      )}
    </div>
  );
}

function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center h-64">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="text-center py-12">
      <h2 className="text-xl font-semibold text-gray-900 mb-2">No courses yet</h2>
      <p className="text-gray-600 mb-4">Create your first course to get started.</p>
      <Link
        to="/exercises"
        className="inline-block bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
      >
        Create Course
      </Link>
    </div>
  );
}

function StatsCards({ stats }: { stats: DashboardStats }) {
  const cards = [
    { label: 'Total Students', value: stats.totalStudents, icon: '👥' },
    { label: 'Exercises', value: stats.totalExercises, icon: '📝' },
    { label: 'Submissions', value: stats.totalSubmissions, icon: '📤' },
    {
      label: 'Completion Rate',
      value: `${Math.round(stats.averageCompletionRate)}%`,
      icon: '✅',
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => (
        <div key={card.label} className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-500 text-sm">{card.label}</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{card.value}</p>
            </div>
            <span className="text-3xl">{card.icon}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

function RecentSubmissions({ submissions }: { submissions: Submission[] }) {
  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-6 py-4 border-b border-gray-200">
        <h2 className="text-lg font-semibold text-gray-900">Recent Submissions</h2>
      </div>
      <div className="divide-y divide-gray-200">
        {submissions.length === 0 ? (
          <p className="px-6 py-4 text-gray-500">No submissions yet</p>
        ) : (
          submissions.slice(0, 5).map((submission) => (
            <div key={submission.id} className="px-6 py-4 flex items-center justify-between">
              <div>
                <p className="font-medium text-gray-900">{submission.userName}</p>
                <p className="text-sm text-gray-500">{submission.exerciseTitle}</p>
              </div>
              <div className="text-right">
                <StatusBadge status={submission.status} />
                {submission.score !== undefined && (
                  <p className="text-sm text-gray-500 mt-1">
                    {submission.score}/{submission.maxScore}
                  </p>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: Submission['status'] }) {
  const styles: Record<string, string> = {
    COMPLETED: 'bg-green-100 text-green-800',
    PENDING: 'bg-yellow-100 text-yellow-800',
    RUNNING: 'bg-blue-100 text-blue-800',
    FAILED: 'bg-red-100 text-red-800',
  };

  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${styles[status]}`}>
      {status}
    </span>
  );
}

function StrugglingStudentsCard({ students }: { students: StrugglingStudent[] }) {
  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-6 py-4 border-b border-gray-200">
        <h2 className="text-lg font-semibold text-gray-900">Students Needing Help</h2>
      </div>
      <div className="divide-y divide-gray-200">
        {students.length === 0 ? (
          <p className="px-6 py-4 text-gray-500">All students are on track!</p>
        ) : (
          students.slice(0, 5).map((student) => (
            <div key={student.userId} className="px-6 py-4 flex items-center justify-between">
              <div>
                <p className="font-medium text-gray-900">{student.userName}</p>
                <p className="text-sm text-gray-500">{student.email}</p>
              </div>
              <div className="text-right">
                <p className="text-sm font-medium text-red-600">
                  {student.incompleteExercises} incomplete
                </p>
                <p className="text-sm text-gray-500">{student.totalAttempts} attempts</p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
