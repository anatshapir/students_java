import { useState } from 'react';

interface TestCodeDocumentationProps {
  onInsertTemplate: (template: string) => void;
}

const TEMPLATES = [
  {
    name: 'Simple return value',
    code: `Solution s = new Solution();
assert s.methodName(input) == expected : "Expected " + expected;`,
    description: 'Test a method that returns a primitive value',
  },
  {
    name: 'Array comparison',
    code: `Solution s = new Solution();
int[] result = s.methodName(new int[]{1, 2, 3});
assert java.util.Arrays.equals(result, new int[]{3, 2, 1}) : "Arrays not equal";`,
    description: 'Test a method that returns an array',
  },
  {
    name: 'String equality',
    code: `Solution s = new Solution();
assert s.methodName("input").equals("expected") : "String mismatch";`,
    description: 'Test a method that returns a String',
  },
  {
    name: 'Boolean check',
    code: `Solution s = new Solution();
assert s.isValid(input) == true : "Expected true";`,
    description: 'Test a method that returns a boolean',
  },
  {
    name: 'Exception expected',
    code: `try {
    new Solution().methodName(invalidInput);
    assert false : "Should have thrown an exception";
} catch (IllegalArgumentException e) {
    // Expected exception
}`,
    description: 'Test that a method throws an expected exception',
  },
  {
    name: 'Null check',
    code: `Solution s = new Solution();
assert s.findItem(key) == null : "Expected null for missing item";`,
    description: 'Test that a method returns null',
  },
  {
    name: 'List comparison',
    code: `Solution s = new Solution();
java.util.List<Integer> result = s.getNumbers();
java.util.List<Integer> expected = java.util.Arrays.asList(1, 2, 3);
assert result.equals(expected) : "Lists not equal";`,
    description: 'Test a method that returns a List',
  },
];

export default function TestCodeDocumentation({ onInsertTemplate }: TestCodeDocumentationProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="bg-blue-50 border border-blue-200 rounded-lg overflow-hidden">
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full px-4 py-3 flex items-center justify-between text-left hover:bg-blue-100 transition-colors"
      >
        <div className="flex items-center gap-2">
          <svg className="w-5 h-5 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span className="text-sm font-medium text-blue-800">Test Code Format Guide</span>
        </div>
        <svg
          className={`w-4 h-4 text-blue-600 transition-transform ${isExpanded ? 'rotate-180' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isExpanded && (
        <div className="px-4 pb-4 space-y-4">
          {/* Explanation */}
          <div className="text-sm text-blue-900 space-y-2">
            <p>
              Test code is Java code that runs to verify student solutions. The student&apos;s{' '}
              <code className="bg-blue-100 px-1 rounded">Solution</code> class is available.
            </p>
            <p className="font-medium">Rules:</p>
            <ul className="list-disc list-inside space-y-1 text-blue-800">
              <li>Use <code className="bg-blue-100 px-1 rounded">assert</code> statements to check conditions</li>
              <li>Test passes if code completes without exception</li>
              <li>Test fails if <code className="bg-blue-100 px-1 rounded">AssertionError</code> or other exception is thrown</li>
              <li>Include descriptive error messages after the colon</li>
            </ul>
          </div>

          {/* Templates */}
          <div>
            <p className="text-sm font-medium text-blue-800 mb-2">Quick Templates:</p>
            <div className="space-y-2">
              {TEMPLATES.map((template) => (
                <div
                  key={template.name}
                  className="bg-white border border-blue-200 rounded-lg p-3"
                >
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div>
                      <p className="text-sm font-medium text-gray-800">{template.name}</p>
                      <p className="text-xs text-gray-500">{template.description}</p>
                    </div>
                    <button
                      onClick={() => onInsertTemplate(template.code)}
                      className="flex-shrink-0 px-2 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
                    >
                      Insert
                    </button>
                  </div>
                  <pre className="text-xs bg-gray-50 p-2 rounded overflow-x-auto text-gray-700">
                    {template.code}
                  </pre>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
