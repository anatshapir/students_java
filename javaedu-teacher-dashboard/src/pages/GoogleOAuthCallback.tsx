import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';

export default function GoogleOAuthCallback() {
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const code = searchParams.get('code');
    const error = searchParams.get('error');

    if (error) {
      // Close popup and notify parent of error
      if (window.opener) {
        window.opener.postMessage({ type: 'google-oauth-error', error }, window.location.origin);
        window.close();
      }
      return;
    }

    if (code) {
      // Send the code back to the parent window
      if (window.opener) {
        window.opener.postMessage({ type: 'google-oauth-callback', code }, window.location.origin);
        window.close();
      }
    }
  }, [searchParams]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <p className="text-gray-600">Completing authentication...</p>
        <p className="text-sm text-gray-500 mt-2">This window will close automatically.</p>
      </div>
    </div>
  );
}
