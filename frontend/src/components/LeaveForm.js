// LeaveForm.js : form for submitting a new leave application
import React, { useState } from 'react';
import { leaveApi } from '../api/api';

const LEAVE_TYPES = ['ANNUAL', 'SICK', 'FAMILY_RESPONSIBILITY', 'UNPAID', 'STUDY', 'MATERNITY', 'PATERNITY'];

// Pure function: validates leave form fields
const validateLeaveForm = (leaveType, startDate, endDate, reason) => {
  if (!leaveType) return 'Please select a leave type';
  if (!startDate) return 'Start date is required';
  if (!endDate) return 'End date is required';
  if (new Date(endDate) < new Date(startDate)) return 'End date must not be before start date';
  if (new Date(startDate) < new Date(new Date().toDateString())) return 'Start date must be today or in the future';
  if (!reason || reason.trim().length < 10) return 'Reason must be at least 10 characters';
  if (reason.length > 500) return 'Reason must not exceed 500 characters';
  return null;
};

export default function LeaveForm({ onSuccess }) {
  const [leaveType, setLeaveType] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');

    const validationError = validateLeaveForm(leaveType, startDate, endDate, reason);
    if (validationError) { setError(validationError); return; }

    setLoading(true);
    try {
      await leaveApi.createLeaveRequest({ leaveType, startDate, endDate, reason });
      setSuccess('Leave request submitted successfully.');
      setLeaveType(''); setStartDate(''); setEndDate(''); setReason('');
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to submit leave request.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <h3 style={styles.title}>Submit Leave Application</h3>

      {error && <div style={styles.error} role="alert">{error}</div>}
      {success && <div style={styles.success} role="status">{success}</div>}

      <form onSubmit={handleSubmit} style={styles.form}>
        <label style={styles.label}>
          Leave Type
          <select value={leaveType} onChange={(e) => setLeaveType(e.target.value)} style={styles.input} required>
            <option value="">-- Select --</option>
            {LEAVE_TYPES.map(t => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
          </select>
        </label>

        <label style={styles.label}>
          Start Date
          <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)}
                 style={styles.input} required min={new Date().toISOString().split('T')[0]} />
        </label>

        <label style={styles.label}>
          End Date
          <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)}
                 style={styles.input} required min={startDate || new Date().toISOString().split('T')[0]} />
        </label>

        <label style={styles.label}>
          Reason <span style={styles.charCount}>({reason.length}/500)</span>
          <textarea value={reason} onChange={(e) => setReason(e.target.value)}
                    style={styles.textarea} maxLength={500} rows={4} required />
        </label>

        <button type="submit" style={styles.button} disabled={loading}>
          {loading ? 'Submitting...' : 'Submit Leave Request'}
        </button>
      </form>
    </div>
  );
}

const styles = {
  container: { background: '#fff', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)', marginBottom: '1.5rem' },
  title: { marginTop: 0, color: '#003580' },
  form: { display: 'flex', flexDirection: 'column', gap: '1rem' },
  label: { display: 'flex', flexDirection: 'column', gap: '0.25rem', fontSize: '0.9rem', fontWeight: '600' },
  input: { padding: '0.6rem', border: '1px solid #ccc', borderRadius: '4px', fontSize: '1rem' },
  textarea: { padding: '0.6rem', border: '1px solid #ccc', borderRadius: '4px', fontSize: '0.95rem', resize: 'vertical' },
  button: { padding: '0.75rem', background: '#003580', color: '#fff', border: 'none', borderRadius: '4px', fontSize: '1rem', cursor: 'pointer' },
  error: { background: '#ffe0e0', color: '#c00', padding: '0.75rem', borderRadius: '4px', fontSize: '0.9rem' },
  success: { background: '#e0ffe0', color: '#060', padding: '0.75rem', borderRadius: '4px', fontSize: '0.9rem' },
  charCount: { fontWeight: 'normal', fontSize: '0.8rem', color: '#888' },
};
