import { useState, useEffect, useCallback } from 'react';
import {
  getGoogleAuthUrl,
  handleGoogleCallback,
  getGoogleConnectionStatus,
  disconnectGoogle,
} from '../api/client';
import type { GoogleConnectionStatus } from '../types';

interface GoogleConnectButtonProps {
  onConnected?: (accessToken: string) => void;
  onDisconnected?: () => void;
  showStatus?: boolean;
  className?: string;
}

export default function GoogleConnectButton({
  onConnected,
  onDisconnected,
  showStatus = true,
  className = '',
}: GoogleConnectButtonProps) {
  const [status, setStatus] = useState<GoogleConnectionStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [connecting, setConnecting] = useState(false);

  const checkStatus = useCallback(async () => {
    try {
      const data = await getGoogleConnectionStatus();
      setStatus(data);
    } catch (error) {
      console.error('Failed to check Google connection status:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkStatus();
  }, [checkStatus]);

  const [configError, setConfigError] = useState<string | null>(null);

  const handleConnect = async () => {
    setConnecting(true);
    setConfigError(null);
    try {
      const { authUrl } = await getGoogleAuthUrl();

      // Open OAuth popup
      const width = 500;
      const height = 600;
      const left = window.screenX + (window.outerWidth - width) / 2;
      const top = window.screenY + (window.outerHeight - height) / 2;

      const popup = window.open(
        authUrl,
        'google-oauth',
        `width=${width},height=${height},left=${left},top=${top}`
      );

      if (!popup) {
        alert('Please allow popups for this site to connect to Google');
        setConnecting(false);
        return;
      }

      // Listen for the OAuth callback
      const handleMessage = async (event: MessageEvent) => {
        if (event.data.type === 'google-oauth-callback') {
          window.removeEventListener('message', handleMessage);
          popup.close();

          try {
            const result = await handleGoogleCallback(event.data.code);
            if (result.connected) {
              setStatus({
                connected: true,
                email: result.email,
                connectedAt: result.connectedAt,
              });
              if (onConnected && result.accessToken) {
                onConnected(result.accessToken);
              }
            }
          } catch (error) {
            console.error('Failed to complete Google connection:', error);
          } finally {
            setConnecting(false);
          }
        }
      };

      window.addEventListener('message', handleMessage);

      // Check if popup was closed without completing
      const checkClosed = setInterval(() => {
        if (popup.closed) {
          clearInterval(checkClosed);
          window.removeEventListener('message', handleMessage);
          setConnecting(false);
        }
      }, 1000);
    } catch (error: unknown) {
      console.error('Failed to get Google auth URL:', error);
      // Check if it's a configuration error from the backend
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { response?: { data?: { message?: string } } };
        if (axiosError.response?.data?.message) {
          setConfigError(axiosError.response.data.message);
        } else {
          setConfigError('Failed to connect to Google. Please try again.');
        }
      } else {
        setConfigError('Failed to connect to Google. Please try again.');
      }
      setConnecting(false);
    }
  };

  const handleDisconnect = async () => {
    if (!confirm('Disconnect from Google? You can reconnect at any time.')) return;

    try {
      await disconnectGoogle();
      setStatus({ connected: false });
      onDisconnected?.();
    } catch (error) {
      console.error('Failed to disconnect Google:', error);
    }
  };

  if (loading) {
    return (
      <div className={`flex items-center gap-2 ${className}`}>
        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-400"></div>
        <span className="text-sm text-gray-500">Checking connection...</span>
      </div>
    );
  }

  if (status?.connected) {
    return (
      <div className={`flex items-center gap-3 ${className}`}>
        {showStatus && (
          <div className="flex items-center gap-2 text-sm text-green-700 bg-green-50 px-3 py-1.5 rounded-lg">
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clipRule="evenodd"
              />
            </svg>
            <span>Connected as {status.email}</span>
          </div>
        )}
        <button
          onClick={handleDisconnect}
          className="text-sm text-gray-600 hover:text-gray-800 underline"
        >
          Disconnect
        </button>
      </div>
    );
  }

  return (
    <div className={className}>
      {configError && (
        <div className="mb-3 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
          <p className="text-sm text-yellow-800">{configError}</p>
        </div>
      )}
      <p className="mb-2 text-xs text-gray-500">
        Sign in with the Google account that owns your Google Classroom courses (typically your school account).
      </p>
      <button
        onClick={handleConnect}
        disabled={connecting}
        className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors"
      >
        <svg className="w-5 h-5" viewBox="0 0 24 24">
          <path
            fill="#4285F4"
            d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
          />
          <path
            fill="#34A853"
            d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
          />
          <path
            fill="#FBBC05"
            d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
          />
          <path
            fill="#EA4335"
            d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
          />
        </svg>
        {connecting ? 'Connecting...' : 'Connect Google Account'}
      </button>
    </div>
  );
}
