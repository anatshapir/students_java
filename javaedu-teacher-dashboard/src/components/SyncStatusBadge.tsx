import { formatDistanceToNow } from 'date-fns';

interface SyncStatusBadgeProps {
  isLinked: boolean;
  lastSyncedAt?: string;
  isSyncing?: boolean;
  syncError?: string;
  size?: 'sm' | 'md';
}

export default function SyncStatusBadge({
  isLinked,
  lastSyncedAt,
  isSyncing = false,
  syncError,
  size = 'sm',
}: SyncStatusBadgeProps) {
  if (!isLinked) {
    return null;
  }

  if (isSyncing) {
    return (
      <div className={`flex items-center gap-1.5 ${size === 'sm' ? 'text-xs' : 'text-sm'}`}>
        <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-blue-600"></div>
        <span className="text-blue-600">Syncing...</span>
      </div>
    );
  }

  if (syncError) {
    return (
      <div className={`flex items-center gap-1.5 ${size === 'sm' ? 'text-xs' : 'text-sm'}`}>
        <span className="w-2 h-2 bg-red-500 rounded-full"></span>
        <span className="text-red-600">Sync failed</span>
      </div>
    );
  }

  const syncTime = lastSyncedAt
    ? formatDistanceToNow(new Date(lastSyncedAt), { addSuffix: true })
    : null;

  return (
    <div
      className={`flex items-center gap-1.5 ${size === 'sm' ? 'text-xs' : 'text-sm'} text-green-700`}
    >
      <span className="w-2 h-2 bg-green-500 rounded-full"></span>
      <span>
        Synced with Google Classroom{syncTime ? ` ${syncTime}` : ''}
      </span>
    </div>
  );
}
