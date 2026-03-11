import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { getExercise, createExercise, updateExercise, getCourses } from '../api/client';
import type { Course, TestCase, GeneratedExercise } from '../types';
import AIAssistantPanel from '../components/AIAssistantPanel';
import TestCodeDocumentation from '../components/TestCodeDocumentation';

interface ExerciseForm {
  title: string;
  description: string;
  starterCode: string;
  solutionCode?: string;
  difficulty: 'BEGINNER' | 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT';
  points: number;
  category: string;
  courseId: number;
  testCases: TestCase[];
}

const CATEGORIES = [
  'BASICS',
  'DATA_STRUCTURES',
  'ALGORITHMS',
  'OOP',
  'DESIGN_PATTERNS',
  'CONCURRENCY',
  'IO',
  'COLLECTIONS',
] as const;

const CATEGORY_LABELS: Record<string, string> = {
  BASICS: 'Basics',
  DATA_STRUCTURES: 'Data Structures',
  ALGORITHMS: 'Algorithms',
  OOP: 'Object-Oriented Programming',
  DESIGN_PATTERNS: 'Design Patterns',
  CONCURRENCY: 'Concurrency',
  IO: 'I/O',
  COLLECTIONS: 'Collections',
};

const defaultStarterCode = `public class Solution {
    public static void main(String[] args) {
        // Your code here
    }
}`;

const defaultForm: ExerciseForm = {
  title: '',
  description: '',
  starterCode: defaultStarterCode,
  solutionCode: '',
  difficulty: 'EASY',
  points: 100,
  category: 'BASICS',
  courseId: 0,
  testCases: [],
};

export default function ExerciseEditor() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form, setForm] = useState<ExerciseForm>(defaultForm);
  const [courses, setCourses] = useState<Course[]>([]);
  const [loading, setLoading] = useState(!!id);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<'details' | 'code' | 'tests'>('details');
  const [isAIPanelOpen, setIsAIPanelOpen] = useState(false);
  const activeTestEditorIndex = useRef<number | null>(null);

  useEffect(() => {
    loadCourses();
    if (id) {
      loadExercise(Number(id));
    }
  }, [id]);

  const loadCourses = async () => {
    try {
      const data = await getCourses();
      setCourses(data);
      if (!id && data.length > 0) {
        setForm((f) => ({ ...f, courseId: data[0].id }));
      }
    } catch (error) {
      console.error('Failed to load courses:', error);
    }
  };

  const loadExercise = async (exerciseId: number) => {
    try {
      const data = await getExercise(exerciseId);
      setForm({
        title: data.title,
        description: data.description,
        starterCode: data.starterCode,
        difficulty: data.difficulty,
        points: data.points,
        category: data.category,
        courseId: data.courseId,
        testCases: [],
      });
    } catch (error) {
      console.error('Failed to load exercise:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (id) {
        await updateExercise(Number(id), form);
      } else {
        await createExercise(form);
      }
      navigate('/exercises');
    } catch (error) {
      console.error('Failed to save exercise:', error);
    } finally {
      setSaving(false);
    }
  };

  const addTestCase = () => {
    setForm((f) => ({
      ...f,
      testCases: [
        ...f.testCases,
        { id: 0, name: '', testCode: '', input: '', expectedOutput: '', isHidden: false, points: 10, description: '' },
      ],
    }));
  };

  const handleApplyAIExercise = (exercise: GeneratedExercise) => {
    setForm((f) => ({
      ...f,
      title: exercise.title,
      description: exercise.description,
      starterCode: exercise.starterCode,
      solutionCode: exercise.solutionCode,
      difficulty: exercise.difficulty,
      category: exercise.category,
      points: exercise.points,
      testCases: exercise.testCases.map((tc) => ({
        id: 0,
        name: tc.name,
        testCode: tc.testCode,
        input: '',
        expectedOutput: '',
        isHidden: tc.isHidden,
        points: tc.points,
        description: tc.description,
      })),
    }));
    setIsAIPanelOpen(false);
  };

  const handleInsertTemplate = (template: string) => {
    if (activeTestEditorIndex.current !== null) {
      const index = activeTestEditorIndex.current;
      const currentTestCode = form.testCases[index]?.testCode || '';
      const newTestCode = currentTestCode ? currentTestCode + '\n\n' + template : template;
      updateTestCase(index, { testCode: newTestCode });
    }
  };

  const updateTestCase = (index: number, updates: Partial<TestCase>) => {
    setForm((f) => ({
      ...f,
      testCases: f.testCases.map((tc, i) => (i === index ? { ...tc, ...updates } : tc)),
    }));
  };

  const removeTestCase = (index: number) => {
    setForm((f) => ({
      ...f,
      testCases: f.testCases.filter((_, i) => i !== index),
    }));
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
        <h1 className="text-2xl font-bold text-gray-900">
          {id ? 'Edit Exercise' : 'New Exercise'}
        </h1>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsAIPanelOpen(true)}
            className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-lg hover:from-purple-700 hover:to-indigo-700 transition-all shadow-sm"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
            </svg>
            AI Assistant
          </button>
          <button
            onClick={() => navigate('/exercises')}
            className="text-gray-600 hover:text-gray-900"
          >
            Cancel
          </button>
        </div>
      </div>

      <AIAssistantPanel
        isOpen={isAIPanelOpen}
        onClose={() => setIsAIPanelOpen(false)}
        onApply={handleApplyAIExercise}
      />

      <div className="bg-white rounded-lg shadow">
        <div className="border-b border-gray-200">
          <nav className="flex">
            {(['details', 'code', 'tests'] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-6 py-4 text-sm font-medium capitalize ${
                  activeTab === tab
                    ? 'border-b-2 border-blue-600 text-blue-600'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                {tab}
              </button>
            ))}
          </nav>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
          {activeTab === 'details' && (
            <DetailsTab form={form} setForm={setForm} courses={courses} />
          )}
          {activeTab === 'code' && <CodeTab form={form} setForm={setForm} />}
          {activeTab === 'tests' && (
            <TestsTab
              testCases={form.testCases}
              onAdd={addTestCase}
              onUpdate={updateTestCase}
              onRemove={removeTestCase}
              onInsertTemplate={handleInsertTemplate}
              onEditorFocus={(index) => { activeTestEditorIndex.current = index; }}
            />
          )}

          <div className="mt-6 flex justify-end gap-4">
            <button
              type="button"
              onClick={() => navigate('/exercises')}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save Exercise'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function DetailsTab({
  form,
  setForm,
  courses,
}: {
  form: ExerciseForm;
  setForm: React.Dispatch<React.SetStateAction<ExerciseForm>>;
  courses: Course[];
}) {
  return (
    <div className="space-y-6">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
        <input
          type="text"
          value={form.title}
          onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
          required
          dir="auto"
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          placeholder="Exercise title"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
        <textarea
          value={form.description}
          onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
          required
          dir="auto"
          rows={4}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          placeholder="Describe what the student needs to implement..."
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Course</label>
          <select
            value={form.courseId}
            onChange={(e) => setForm((f) => ({ ...f, courseId: Number(e.target.value) }))}
            required
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          >
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Difficulty</label>
          <select
            value={form.difficulty}
            onChange={(e) =>
              setForm((f) => ({
                ...f,
                difficulty: e.target.value as ExerciseForm['difficulty'],
              }))
            }
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          >
            <option value="BEGINNER">Beginner</option>
            <option value="EASY">Easy</option>
            <option value="MEDIUM">Medium</option>
            <option value="HARD">Hard</option>
            <option value="EXPERT">Expert</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Points</label>
          <input
            type="number"
            value={form.points}
            onChange={(e) => setForm((f) => ({ ...f, points: Number(e.target.value) }))}
            min={0}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
          <select
            value={form.category}
            onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          >
            {CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>
                {CATEGORY_LABELS[cat]}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
}

function CodeTab({
  form,
  setForm,
}: {
  form: ExerciseForm;
  setForm: React.Dispatch<React.SetStateAction<ExerciseForm>>;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-2">Starter Code</label>
      <div className="border border-gray-300 rounded-lg overflow-hidden">
        <Editor
          height="400px"
          language="java"
          value={form.starterCode}
          onChange={(value) => setForm((f) => ({ ...f, starterCode: value || '' }))}
          theme="vs-dark"
          options={{
            minimap: { enabled: false },
            fontSize: 14,
            tabSize: 4,
          }}
        />
      </div>
      <p className="mt-2 text-sm text-gray-500">
        This is the code students will start with.
      </p>
    </div>
  );
}

function TestsTab({
  testCases,
  onAdd,
  onUpdate,
  onRemove,
  onInsertTemplate,
  onEditorFocus,
}: {
  testCases: TestCase[];
  onAdd: () => void;
  onUpdate: (index: number, updates: Partial<TestCase>) => void;
  onRemove: (index: number) => void;
  onInsertTemplate: (template: string) => void;
  onEditorFocus: (index: number) => void;
}) {
  const [expandedTests, setExpandedTests] = useState<Set<number>>(new Set([0]));

  const toggleExpanded = (index: number) => {
    setExpandedTests(prev => {
      const next = new Set(prev);
      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }
      return next;
    });
  };

  return (
    <div className="space-y-4">
      {/* Documentation */}
      <TestCodeDocumentation onInsertTemplate={onInsertTemplate} />

      <div className="flex items-center justify-between">
        <p className="text-sm text-gray-600">
          Add test cases with Java assertion code to automatically grade student submissions.
        </p>
        <button
          type="button"
          onClick={() => {
            onAdd();
            setExpandedTests(prev => new Set(prev).add(testCases.length));
          }}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          + Add Test Case
        </button>
      </div>

      {testCases.length === 0 ? (
        <div className="text-center py-12 text-gray-500 border-2 border-dashed border-gray-300 rounded-lg">
          <svg className="w-12 h-12 mx-auto mb-3 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
          </svg>
          <p className="mb-1">No test cases yet</p>
          <p className="text-sm">Click "Add Test Case" to create one, or use the AI Assistant to generate tests.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {testCases.map((tc, index) => (
            <div key={index} className="border border-gray-200 rounded-lg overflow-hidden">
              {/* Header */}
              <div
                className={`flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors ${
                  tc.isHidden ? 'bg-amber-50' : 'bg-white'
                }`}
                onClick={() => toggleExpanded(index)}
              >
                <div className="flex items-center gap-3">
                  <svg
                    className={`w-4 h-4 text-gray-500 transition-transform ${expandedTests.has(index) ? 'rotate-90' : ''}`}
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                  <span className="text-sm font-medium text-gray-700" dir="auto">
                    {tc.name || `Test Case ${index + 1}`}
                  </span>
                  {tc.isHidden && (
                    <span className="px-2 py-0.5 text-xs bg-amber-200 text-amber-800 rounded">Hidden</span>
                  )}
                  <span className="text-xs text-gray-500">{tc.points} pts</span>
                </div>
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    onRemove(index);
                  }}
                  className="text-red-500 hover:text-red-700 p-1"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>

              {/* Content */}
              {expandedTests.has(index) && (
                <div className="p-4 border-t border-gray-200 space-y-4">
                  {/* Name and Description */}
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-medium text-gray-600 mb-1">Test Name</label>
                      <input
                        type="text"
                        value={tc.name}
                        onChange={(e) => onUpdate(index, { name: e.target.value })}
                        placeholder="e.g., Test basic addition"
                        dir="auto"
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-600 mb-1">Description (optional)</label>
                      <input
                        type="text"
                        value={tc.description || ''}
                        onChange={(e) => onUpdate(index, { description: e.target.value })}
                        placeholder="What this test checks"
                        dir="auto"
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>

                  {/* Test Code Editor */}
                  <div>
                    <label className="block text-xs font-medium text-gray-600 mb-1">Test Code (Java)</label>
                    <div className="border border-gray-300 rounded-lg overflow-hidden">
                      <Editor
                        height="150px"
                        language="java"
                        value={tc.testCode || ''}
                        onChange={(value) => onUpdate(index, { testCode: value || '' })}
                        theme="vs-dark"
                        options={{
                          minimap: { enabled: false },
                          fontSize: 13,
                          tabSize: 4,
                          lineNumbers: 'off',
                          scrollBeyondLastLine: false,
                          wordWrap: 'on',
                          padding: { top: 8, bottom: 8 },
                        }}
                        onMount={(editor) => {
                          editor.onDidFocusEditorText(() => onEditorFocus(index));
                        }}
                      />
                    </div>
                    <p className="mt-1 text-xs text-gray-500">
                      Use <code className="bg-gray-100 px-1 rounded">assert</code> statements to verify the Solution class output.
                    </p>
                  </div>

                  {/* Settings Row */}
                  <div className="flex items-center gap-6 pt-2 border-t border-gray-100">
                    <label className="flex items-center gap-2 text-sm text-gray-700">
                      <input
                        type="checkbox"
                        checked={tc.isHidden}
                        onChange={(e) => onUpdate(index, { isHidden: e.target.checked })}
                        className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                      />
                      Hidden from students
                    </label>
                    <div className="flex items-center gap-2 text-sm text-gray-700">
                      <label>Points:</label>
                      <input
                        type="number"
                        value={tc.points}
                        onChange={(e) => onUpdate(index, { points: Number(e.target.value) })}
                        min={0}
                        className="w-20 px-2 py-1 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Total Points Summary */}
      {testCases.length > 0 && (
        <div className="flex justify-end text-sm text-gray-600">
          Total test points: <span className="font-medium ml-1">{testCases.reduce((sum, tc) => sum + tc.points, 0)}</span>
        </div>
      )}
    </div>
  );
}
