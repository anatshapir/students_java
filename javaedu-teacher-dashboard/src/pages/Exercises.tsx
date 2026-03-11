import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getExercises, deleteExercise, publishExercise } from '../api/client';
import type { Exercise } from '../types';

export default function Exercises() {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'published' | 'draft'>('all');

  useEffect(() => {
    loadExercises();
  }, []);

  const loadExercises = async () => {
    try {
      const data = await getExercises();
      setExercises(data);
    } catch (error) {
      console.error('Failed to load exercises:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this exercise?')) return;
    try {
      await deleteExercise(id);
      setExercises(exercises.filter((e) => e.id !== id));
    } catch (error) {
      console.error('Failed to delete exercise:', error);
    }
  };

  const handlePublish = async (id: number) => {
    try {
      const updated = await publishExercise(id);
      setExercises(exercises.map((e) => (e.id === id ? updated : e)));
    } catch (error) {
      console.error('Failed to publish exercise:', error);
    }
  };

  const filteredExercises = exercises.filter((e) => {
    if (filter === 'published') return e.isPublished;
    if (filter === 'draft') return !e.isPublished;
    return true;
  });

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
        <h1 className="text-2xl font-bold text-gray-900">Exercises</h1>
        <Link
          to="/exercises/new"
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          + New Exercise
        </Link>
      </div>

      <div className="flex gap-2">
        {(['all', 'published', 'draft'] as const).map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-2 rounded-lg capitalize ${
              filter === f
                ? 'bg-blue-600 text-white'
                : 'bg-white text-gray-700 hover:bg-gray-50'
            }`}
          >
            {f}
          </button>
        ))}
      </div>

      {filteredExercises.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-500">No exercises found</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {filteredExercises.map((exercise) => (
            <ExerciseCard
              key={exercise.id}
              exercise={exercise}
              onDelete={handleDelete}
              onPublish={handlePublish}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function ExerciseCard({
  exercise,
  onDelete,
  onPublish,
}: {
  exercise: Exercise;
  onDelete: (id: number) => void;
  onPublish: (id: number) => void;
}) {
  const difficultyColors: Record<string, string> = {
    EASY: 'bg-green-100 text-green-800',
    MEDIUM: 'bg-yellow-100 text-yellow-800',
    HARD: 'bg-red-100 text-red-800',
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <h3 className="text-lg font-semibold text-gray-900" dir="auto">{exercise.title}</h3>
            <span
              className={`px-2 py-0.5 rounded text-xs font-medium ${
                difficultyColors[exercise.difficulty]
              }`}
            >
              {exercise.difficulty}
            </span>
            {!exercise.isPublished && (
              <span className="px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600">
                Draft
              </span>
            )}
          </div>
          <p className="text-gray-600 text-sm mb-3 line-clamp-2" dir="auto">{exercise.description}</p>
          <div className="flex items-center gap-4 text-sm text-gray-500">
            <span>{exercise.courseName}</span>
            <span>{exercise.points} points</span>
            <span>{exercise.category}</span>
          </div>
        </div>
        <div className="flex items-center gap-2 ml-4">
          {!exercise.isPublished && (
            <button
              onClick={() => onPublish(exercise.id)}
              className="px-3 py-1.5 text-sm bg-green-600 text-white rounded hover:bg-green-700"
            >
              Publish
            </button>
          )}
          <Link
            to={`/exercises/${exercise.id}`}
            className="px-3 py-1.5 text-sm bg-gray-100 text-gray-700 rounded hover:bg-gray-200"
          >
            Edit
          </Link>
          <button
            onClick={() => onDelete(exercise.id)}
            className="px-3 py-1.5 text-sm bg-red-100 text-red-700 rounded hover:bg-red-200"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}
