import { useState, useRef, useCallback } from 'react';
import { generateExercise } from '../api/client';
import type { GeneratedExercise, GenerateExerciseRequest } from '../types';

interface AIAssistantPanelProps {
  isOpen: boolean;
  onClose: () => void;
  onApply: (exercise: GeneratedExercise) => void;
}

export default function AIAssistantPanel({ isOpen, onClose, onApply }: AIAssistantPanelProps) {
  const [prompt, setPrompt] = useState('');
  const [image, setImage] = useState<string | null>(null);
  const [imageMediaType, setImageMediaType] = useState<string | null>(null);
  const [imageName, setImageName] = useState<string | null>(null);
  const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD' | ''>('');
  const [category, setCategory] = useState('');
  const [numberOfTestCases, setNumberOfTestCases] = useState(4);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [generatedExercise, setGeneratedExercise] = useState<GeneratedExercise | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImageUpload = useCallback((file: File) => {
    if (file.size > 5 * 1024 * 1024) {
      setError('Image must be under 5MB.');
      return;
    }

    const validTypes = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];
    if (!validTypes.includes(file.type)) {
      setError('Please upload a PNG, JPEG, GIF, or WebP image.');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      const result = e.target?.result as string;
      // Remove data URL prefix to get just the base64 data
      const base64Data = result.split(',')[1];
      setImage(base64Data);
      setImageMediaType(file.type);
      setImageName(file.name);
      setError(null);
    };
    reader.readAsDataURL(file);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) {
      handleImageUpload(file);
    }
  }, [handleImageUpload]);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
  }, []);

  const handleGenerate = async () => {
    if (!prompt.trim() && !image) {
      setError('Please describe the exercise you want to create.');
      return;
    }

    setLoading(true);
    setError(null);
    setGeneratedExercise(null);

    try {
      const request: GenerateExerciseRequest = {
        prompt: prompt.trim() || 'Create an exercise based on this image',
        numberOfTestCases,
      };

      if (image) {
        request.image = image;
        request.imageMediaType = imageMediaType || 'image/png';
      }

      if (difficulty) {
        request.difficulty = difficulty;
      }

      if (category) {
        request.category = category;
      }

      const result = await generateExercise(request);
      setGeneratedExercise(result);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error
        ? err.message
        : (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Failed to generate exercise. Please try again.';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleApply = () => {
    if (generatedExercise) {
      onApply(generatedExercise);
      // Reset state
      setPrompt('');
      setImage(null);
      setImageMediaType(null);
      setImageName(null);
      setGeneratedExercise(null);
      setError(null);
    }
  };

  const clearImage = () => {
    setImage(null);
    setImageMediaType(null);
    setImageName(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-y-0 right-0 w-96 bg-white shadow-xl z-50 flex flex-col border-l border-gray-200">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 bg-gradient-to-r from-purple-600 to-indigo-600">
        <div className="flex items-center gap-2 text-white">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
          <span className="font-semibold">AI Assistant</span>
        </div>
        <button
          onClick={onClose}
          className="text-white hover:text-gray-200 transition-colors"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Prompt Input */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Describe the exercise
          </label>
          <textarea
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="e.g., Create a method to reverse an array of integers"
            rows={4}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent text-sm"
          />
        </div>

        {/* Image Upload */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Or upload an image
          </label>
          <div
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            className="border-2 border-dashed border-gray-300 rounded-lg p-4 text-center hover:border-purple-400 transition-colors cursor-pointer"
            onClick={() => fileInputRef.current?.click()}
          >
            {image ? (
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-sm text-gray-600">
                  <svg className="w-5 h-5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span className="truncate max-w-[200px]">{imageName}</span>
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    clearImage();
                  }}
                  className="text-red-500 hover:text-red-700"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            ) : (
              <div className="text-gray-500 text-sm">
                <svg className="w-8 h-8 mx-auto mb-2 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                <p>Drag & drop or click to upload</p>
                <p className="text-xs text-gray-400 mt-1">PNG, JPEG, GIF, WebP (max 5MB)</p>
              </div>
            )}
          </div>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/png,image/jpeg,image/gif,image/webp"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) handleImageUpload(file);
            }}
            className="hidden"
          />
        </div>

        {/* Settings Toggle */}
        <button
          onClick={() => setShowSettings(!showSettings)}
          className="flex items-center gap-1 text-sm text-gray-600 hover:text-gray-800"
        >
          <svg
            className={`w-4 h-4 transition-transform ${showSettings ? 'rotate-90' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
          Advanced settings
        </button>

        {/* Settings */}
        {showSettings && (
          <div className="space-y-3 pl-2 border-l-2 border-gray-200">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Difficulty</label>
              <select
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value as typeof difficulty)}
                className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm focus:ring-2 focus:ring-purple-500"
              >
                <option value="">Auto-detect</option>
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Category</label>
              <input
                type="text"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="e.g., ARRAYS, LOOPS, OOP"
                className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm focus:ring-2 focus:ring-purple-500"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">
                Number of test cases: {numberOfTestCases}
              </label>
              <input
                type="range"
                min={1}
                max={10}
                value={numberOfTestCases}
                onChange={(e) => setNumberOfTestCases(Number(e.target.value))}
                className="w-full accent-purple-600"
              />
            </div>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {/* Generate Button */}
        <button
          onClick={handleGenerate}
          disabled={loading || (!prompt.trim() && !image)}
          className="w-full py-2.5 bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-lg font-medium hover:from-purple-700 hover:to-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              Generating...
            </>
          ) : (
            <>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              Generate Exercise
            </>
          )}
        </button>

        {/* Preview */}
        {generatedExercise && (
          <div className="border border-gray-200 rounded-lg overflow-hidden">
            <div className="bg-gray-50 px-3 py-2 border-b border-gray-200">
              <h3 className="font-medium text-gray-800">Preview</h3>
            </div>
            <div className="p-3 space-y-3 text-sm max-h-80 overflow-y-auto">
              <div>
                <span className="font-medium text-gray-600">Title:</span>
                <p className="text-gray-800">{generatedExercise.title}</p>
              </div>
              <div>
                <span className="font-medium text-gray-600">Difficulty:</span>
                <span className={`ml-2 px-2 py-0.5 rounded text-xs font-medium ${
                  generatedExercise.difficulty === 'EASY' ? 'bg-green-100 text-green-800' :
                  generatedExercise.difficulty === 'MEDIUM' ? 'bg-yellow-100 text-yellow-800' :
                  'bg-red-100 text-red-800'
                }`}>
                  {generatedExercise.difficulty}
                </span>
              </div>
              <div>
                <span className="font-medium text-gray-600">Category:</span>
                <span className="ml-2 text-gray-800">{generatedExercise.category}</span>
              </div>
              <div>
                <span className="font-medium text-gray-600">Points:</span>
                <span className="ml-2 text-gray-800">{generatedExercise.points}</span>
              </div>
              <div>
                <span className="font-medium text-gray-600">Test Cases:</span>
                <span className="ml-2 text-gray-800">{generatedExercise.testCases.length}</span>
              </div>
              <div>
                <span className="font-medium text-gray-600">Description:</span>
                <p className="text-gray-700 text-xs mt-1 whitespace-pre-wrap line-clamp-4">
                  {generatedExercise.description}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      {generatedExercise && (
        <div className="p-4 border-t border-gray-200 bg-gray-50">
          <button
            onClick={handleApply}
            className="w-full py-2.5 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 transition-colors flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            Apply to Form
          </button>
        </div>
      )}
    </div>
  );
}
