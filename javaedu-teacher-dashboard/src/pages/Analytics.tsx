import { useEffect, useState } from 'react';
import { getCourses, getExercises, getExerciseAnalytics } from '../api/client';
import type { Course, Exercise, ExerciseAnalytics } from '../types';

export default function Analytics() {
  const [courses, setCourses] = useState<Course[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [selectedExercise, setSelectedExercise] = useState<number | null>(null);
  const [analytics, setAnalytics] = useState<ExerciseAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadCourses();
  }, []);

  useEffect(() => {
    if (selectedCourse) {
      loadExercises(selectedCourse);
    }
  }, [selectedCourse]);

  useEffect(() => {
    if (selectedExercise) {
      loadAnalytics(selectedExercise);
    }
  }, [selectedExercise]);

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

  const loadExercises = async (courseId: number) => {
    try {
      const data = await getExercises(courseId);
      setExercises(data);
      if (data.length > 0) {
        setSelectedExercise(data[0].id);
      } else {
        setSelectedExercise(null);
        setAnalytics(null);
      }
    } catch (error) {
      console.error('Failed to load exercises:', error);
    }
  };

  const loadAnalytics = async (exerciseId: number) => {
    try {
      const data = await getExerciseAnalytics(exerciseId);
      setAnalytics(data);
    } catch (error) {
      console.error('Failed to load analytics:', error);
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
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Analytics</h1>
        <div className="flex items-center gap-4">
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
          <select
            value={selectedExercise || ''}
            onChange={(e) => setSelectedExercise(Number(e.target.value))}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          >
            {exercises.map((exercise) => (
              <option key={exercise.id} value={exercise.id}>
                {exercise.title}
              </option>
            ))}
          </select>
        </div>
      </div>

      {exercises.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-500">No exercises in this course yet</p>
        </div>
      ) : analytics ? (
        <>
          <AnalyticsCards analytics={analytics} />
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <CommonErrorsCard errors={analytics.commonErrors} />
            <PerformanceMetrics analytics={analytics} />
          </div>
        </>
      ) : null}
    </div>
  );
}

function AnalyticsCards({ analytics }: { analytics: ExerciseAnalytics }) {
  const completionRate =
    analytics.uniqueStudents > 0
      ? Math.round((analytics.completedStudents / analytics.uniqueStudents) * 100)
      : 0;

  const cards = [
    { label: 'Total Submissions', value: analytics.totalSubmissions, icon: '📤' },
    { label: 'Unique Students', value: analytics.uniqueStudents, icon: '👥' },
    { label: 'Completion Rate', value: `${completionRate}%`, icon: '✅' },
    { label: 'Avg Attempts', value: analytics.averageAttempts.toFixed(1), icon: '🔄' },
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

function CommonErrorsCard({ errors }: { errors: ExerciseAnalytics['commonErrors'] }) {
  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-6 py-4 border-b border-gray-200">
        <h2 className="text-lg font-semibold text-gray-900">Common Errors</h2>
      </div>
      <div className="divide-y divide-gray-200">
        {errors.length === 0 ? (
          <p className="px-6 py-4 text-gray-500">No errors detected yet</p>
        ) : (
          errors.map((error, index) => (
            <div key={index} className="px-6 py-4">
              <div className="flex items-center justify-between mb-2">
                <code className="text-sm font-mono bg-red-50 text-red-700 px-2 py-1 rounded">
                  {error.pattern}
                </code>
                <span className="text-sm text-gray-500">
                  {error.occurrenceCount} occurrences
                </span>
              </div>
              {error.suggestedHint && (
                <p className="text-sm text-gray-600 mt-2">
                  <span className="font-medium">Suggested hint:</span> {error.suggestedHint}
                </p>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}

function PerformanceMetrics({ analytics }: { analytics: ExerciseAnalytics }) {
  const metrics = [
    {
      label: 'Average Time to Complete',
      value: analytics.averageTimeToComplete > 0
        ? `${analytics.averageTimeToComplete.toFixed(0)} min`
        : 'N/A',
      description: 'Average time students take to complete this exercise',
    },
    {
      label: 'Students Completed',
      value: `${analytics.completedStudents}/${analytics.uniqueStudents}`,
      description: 'Number of students who successfully completed the exercise',
    },
    {
      label: 'Average Attempts',
      value: analytics.averageAttempts.toFixed(1),
      description: 'Average number of submissions before passing',
    },
  ];

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-6 py-4 border-b border-gray-200">
        <h2 className="text-lg font-semibold text-gray-900">Performance Metrics</h2>
      </div>
      <div className="divide-y divide-gray-200">
        {metrics.map((metric) => (
          <div key={metric.label} className="px-6 py-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-gray-900">{metric.label}</p>
                <p className="text-sm text-gray-500">{metric.description}</p>
              </div>
              <span className="text-xl font-bold text-blue-600">{metric.value}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
