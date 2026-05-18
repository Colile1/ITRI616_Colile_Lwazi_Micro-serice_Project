// ManagerDashboard.js : manager/admin view to approve or reject leave requests
import React, { useState } from 'react';
import { leaveApi } from '../api/api';

const STATUS_COLOURS = { PENDING: '#e8a000', APPROVED: '#060', REJECTED: '#c00', CANCELLED: '#888' };
const formatDate = (dateStr) => new Date(dateStr).toLocaleDateString('en-ZA');

export default function ManagerDashboard({ requests, onRefresh }) {
  const [comment, setComment] = useState('');
  const [actionId, setActionId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleApprove = async (id) => {
    setError(''); setSuccess('');
    try {
      await leaveApi.approveLeaveRequest(id, comment);
      setSuccess(`Leave request #${id} approved.`);
      setComment(''); setActionId(null);
      if (onRefresh) onRefresh();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to approve leave request.');
    }
  };

  const handleReject = async (id) => {
    if (!comment.trim()) { setError('Please provide a comment when rejecting a request.'); return; }
    setError(''); setSuccess('');
    try {
      await leaveApi.rejectLeaveRequest(id, comment);
      setSuccess(`Leave request #${id} rejected.`);
      setComment(''); setActionId(null);
      if (onRefresh) onRefresh();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to reject leave request.');
    }
  };

  if (!requests || requests.length === 0) {
    return <div style={styles.empty}>No leave requests to review.</div>;
  }

  return (
    <div style={styles.container}>
      <h3 style={styles.title}>All Leave Requests — Manager View</h3>
      {error && <div style={styles.error}>{error}</div>}
      {success && <div style={styles.success}>{success}</div>}

      <div style={styles.tableWrapper}>
        <table style={styles.table}>
          <thead>
            <tr style={styles.thead}>
              <th style={styles.th}>ID</th>
              <th style={styles.th}>Employee</th>
              <th style={styles.th}>Type</th>
              <th style={styles.th}>Start</th>
              <th style={styles.th}>End</th>
              <th style={styles.th}>Reason</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {requests.map(req => (
              <tr key={req.id} style={styles.tr}>
                <td style={styles.td}>{req.id}</td>
                <td style={styles.td}>{req.employeeUsername}</td>
                <td style={styles.td}>{req.leaveType?.replace('_', ' ')}</td>
                <td style={styles.td}>{formatDate(req.startDate)}</td>
                <td style={styles.td}>{formatDate(req.endDate)}</td>
                <td style={styles.td}>{req.reason}</td>
                <td style={styles.td}>
                  <span style={{ color: STATUS_COLOURS[req.status] || '#333', fontWeight: '700' }}>
                    {req.status}
                  </span>
                </td>
                <td style={styles.td}>
                  {req.status === 'PENDING' && (
                    <div>
                      {actionId === req.id ? (
                        <div style={styles.actionPanel}>
                          <textarea
                            placeholder="Comment (required for rejection)"
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            style={styles.commentBox}
                            maxLength={500}
                          />
                          <div style={styles.actionBtns}>
                            <button onClick={() => handleApprove(req.id)} style={styles.approveBtn}>Approve</button>
                            <button onClick={() => handleReject(req.id)} style={styles.rejectBtn}>Reject</button>
                            <button onClick={() => { setActionId(null); setComment(''); }} style={styles.cancelBtn}>Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <button onClick={() => setActionId(req.id)} style={styles.reviewBtn}>Review</button>
                      )}
                    </div>
                  )}
                  {req.status !== 'PENDING' && (
                    <span style={styles.reviewed}>Reviewed by {req.reviewedBy || 'N/A'}</span>
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
  td: { padding: '0.6rem 0.8rem', fontSize: '0.85rem', verticalAlign: 'top' },
  reviewBtn: { background: '#003580', color: '#fff', border: 'none', padding: '0.3rem 0.6rem', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem' },
  approveBtn: { background: '#060', color: '#fff', border: 'none', padding: '0.3rem 0.6rem', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem' },
  rejectBtn: { background: '#c00', color: '#fff', border: 'none', padding: '0.3rem 0.6rem', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem' },
  cancelBtn: { background: '#888', color: '#fff', border: 'none', padding: '0.3rem 0.6rem', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem' },
  commentBox: { width: '100%', padding: '0.4rem', fontSize: '0.85rem', border: '1px solid #ccc', borderRadius: '4px', marginBottom: '0.5rem', boxSizing: 'border-box' },
  actionPanel: { display: 'flex', flexDirection: 'column', gap: '0.25rem' },
  actionBtns: { display: 'flex', gap: '0.25rem' },
  reviewed: { fontSize: '0.8rem', color: '#888', fontStyle: 'italic' },
  error: { background: '#ffe0e0', color: '#c00', padding: '0.75rem', borderRadius: '4px', marginBottom: '1rem', fontSize: '0.9rem' },
  success: { background: '#e0ffe0', color: '#060', padding: '0.75rem', borderRadius: '4px', marginBottom: '1rem', fontSize: '0.9rem' },
  empty: { background: '#fff', padding: '1.5rem', borderRadius: '8px', color: '#888', textAlign: 'center' },
};
