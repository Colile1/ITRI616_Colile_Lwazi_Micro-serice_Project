// LeaveList.js : displays the list of leave requests for the logged-in employee
import React, { useState } from 'react';
import { leaveApi } from '../api/api';

const STATUS_COLOURS = {
  PENDING: '#e8a000',
  APPROVED: '#060',
  REJECTED: '#c00',
  CANCELLED: '#888',
};

// Pure function: formats a date string to South African locale
const formatDate = (dateStr) => new Date(dateStr).toLocaleDateString('en-ZA');

export default function LeaveList({ requests, onRefresh }) {
  const [error, setError] = useState('');
  const [loadingId, setLoadingId] = useState(null);

  const handleCancel = async (id) => {
    if (!window.confirm('Are you sure you want to cancel this leave request?')) return;
    setError(''); setLoadingId(id);
    try {
      await leaveApi.cancelLeaveRequest(id);
      if (onRefresh) onRefresh();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to cancel leave request.');
    } finally {
      setLoadingId(null);
    }
  };

  if (!requests || requests.length === 0) {
    return <div style={styles.empty}>No leave requests found. Submit your first request above.</div>;
  }

  return (
    <div style={styles.container}>
      <h3 style={styles.title}>My Leave Requests</h3>
      {error && <div style={styles.error}>{error}</div>}
      <div style={styles.tableWrapper}>
        <table style={styles.table}>
          <thead>
            <tr style={styles.thead}>
              <th style={styles.th}>ID</th>
              <th style={styles.th}>Type</th>
              <th style={styles.th}>Start</th>
              <th style={styles.th}>End</th>
              <th style={styles.th}>Reason</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Comment</th>
              <th style={styles.th}>Action</th>
            </tr>
          </thead>
          <tbody>
            {requests.map(req => (
              <tr key={req.id} style={styles.tr}>
                <td style={styles.td}>{req.id}</td>
                <td style={styles.td}>{req.leaveType?.replace('_', ' ')}</td>
                <td style={styles.td}>{formatDate(req.startDate)}</td>
                <td style={styles.td}>{formatDate(req.endDate)}</td>
                <td style={styles.td}>{req.reason}</td>
                <td style={styles.td}>
                  <span style={{ color: STATUS_COLOURS[req.status] || '#333', fontWeight: '700' }}>
                    {req.status}
                  </span>
                </td>
                <td style={styles.td}>{req.managerComment || '-'}</td>
                <td style={styles.td}>
                  {req.status === 'PENDING' && (
                    <button onClick={() => handleCancel(req.id)}
                            style={styles.cancelBtn} disabled={loadingId === req.id}>
                      {loadingId === req.id ? '...' : 'Cancel'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

const styles = {
  container: { background: '#fff', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' },
  title: { marginTop: 0, color: '#003580' },
  tableWrapper: { overflowX: 'auto' },
  table: { width: '100%', borderCollapse: 'collapse' },
  thead: { background: '#003580', color: '#fff' },
  th: { padding: '0.6rem 0.8rem', textAlign: 'left', fontWeight: '600', fontSize: '0.85rem' },
  tr: { borderBottom: '1px solid #eee' },
  td: { padding: '0.6rem 0.8rem', fontSize: '0.85rem' },
  cancelBtn: { background: '#c00', color: '#fff', border: 'none', padding: '0.3rem 0.6rem', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem' },
  error: { background: '#ffe0e0', color: '#c00', padding: '0.75rem', borderRadius: '4px', marginBottom: '1rem', fontSize: '0.9rem' },
  empty: { background: '#fff', padding: '1.5rem', borderRadius: '8px', color: '#888', textAlign: 'center' },
};
