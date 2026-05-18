// UserManagement.js : admin-only panel for creating, editing, and deleting users
import React, { useState, useEffect, useCallback } from 'react';
import { adminApi } from '../api/api';

const ROLES = ['EMPLOYEE', 'MANAGER', 'ADMIN'];

const emptyForm = { username: '', email: '', password: '', role: 'EMPLOYEE' };

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // 'create' | 'edit' | null
  const [modal, setModal] = useState(null);
  const [editTarget, setEditTarget] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchUsers = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const res = await adminApi.listUsers();
      setUsers(res.data);
    } catch (err) {
      setError('Failed to load users.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const openCreate = () => {
    setForm(emptyForm);
    setFormError('');
    setModal('create');
  };

  const openEdit = (user) => {
    setEditTarget(user);
    setForm({ email: user.email, role: user.role, password: '', enabled: user.enabled });
    setFormError('');
    setModal('edit');
  };

  const closeModal = () => { setModal(null); setEditTarget(null); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError(''); setSubmitting(true);
    try {
      if (modal === 'create') {
        await adminApi.createUser(form);
        setSuccess('User created successfully.');
      } else {
        const payload = {};
        if (form.email) payload.email = form.email;
        if (form.role) payload.role = form.role;
        if (form.password) payload.password = form.password;
        if (form.enabled !== undefined) payload.enabled = form.enabled;
        await adminApi.updateUser(editTarget.id, payload);
        setSuccess('User updated successfully.');
      }
      closeModal();
      fetchUsers();
    } catch (err) {
      setFormError(err.response?.data?.error || 'Operation failed.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (user) => {
    if (!window.confirm(`Delete user "${user.username}"? This cannot be undone.`)) return;
    setError(''); setSuccess('');
    try {
      await adminApi.deleteUser(user.id);
      setSuccess(`User "${user.username}" deleted.`);
      fetchUsers();
    } catch (err) {
      setError(err.response?.data?.error || 'Delete failed.');
    }
  };

  const roleColor = (role) => {
    if (role === 'ADMIN') return '#7c3aed';
    if (role === 'MANAGER') return '#1d4ed8';
    return '#166534';
  };

  return (
    <div style={s.card}>
      <div style={s.cardHeader}>
        <h2 style={s.title}>User Management</h2>
        <button style={s.createBtn} onClick={openCreate}>+ Add User</button>
      </div>

      {error && <div style={s.alert}>{error}</div>}
      {success && <div style={s.success}>{success}</div>}

      {loading ? (
        <div style={s.center}>Loading users...</div>
      ) : (
        <table style={s.table}>
          <thead>
            <tr>
              {['ID', 'Username', 'Email', 'Role', 'Status', 'Actions'].map(h => (
                <th key={h} style={s.th}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr key={user.id} style={s.tr}>
                <td style={s.td}>{user.id}</td>
                <td style={s.td}><strong>{user.username}</strong></td>
                <td style={s.td}>{user.email}</td>
                <td style={s.td}>
                  <span style={{ ...s.badge, background: roleColor(user.role) }}>{user.role}</span>
                </td>
                <td style={s.td}>
                  <span style={{ ...s.badge, background: user.enabled ? '#166534' : '#991b1b' }}>
                    {user.enabled ? 'Active' : 'Disabled'}
                  </span>
                </td>
                <td style={s.td}>
                  <button style={s.editBtn} onClick={() => openEdit(user)}>Edit</button>
                  <button style={s.deleteBtn} onClick={() => handleDelete(user)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {modal && (
        <div style={s.overlay}>
          <div style={s.modal}>
            <h3 style={s.modalTitle}>{modal === 'create' ? 'Create New User' : `Edit: ${editTarget?.username}`}</h3>
            {formError && <div style={s.alert}>{formError}</div>}
            <form onSubmit={handleSubmit}>
              {modal === 'create' && (
                <>
                  <Field label="Username" required>
                    <input style={s.input} value={form.username}
                      onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
                      minLength={3} maxLength={50} required />
                  </Field>
                  <Field label="Password" required>
                    <input style={s.input} type="password" value={form.password}
                      onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                      minLength={8} required placeholder="Min 8 chars, upper+lower+digit" />
                  </Field>
                </>
              )}

              <Field label="Email" required={modal === 'create'}>
                <input style={s.input} type="email" value={form.email}
                  onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                  required={modal === 'create'} />
              </Field>

              <Field label="Role">
                <select style={s.input} value={form.role}
                  onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
                  {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </Field>

              {modal === 'edit' && (
                <>
                  <Field label="New Password (leave blank to keep current)">
                    <input style={s.input} type="password" value={form.password}
                      onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                      minLength={8} placeholder="Leave blank to keep unchanged" />
                  </Field>
                  <Field label="Account Status">
                    <select style={s.input} value={form.enabled ? 'true' : 'false'}
                      onChange={e => setForm(f => ({ ...f, enabled: e.target.value === 'true' }))}>
                      <option value="true">Active</option>
                      <option value="false">Disabled</option>
                    </select>
                  </Field>
                </>
              )}

              <div style={s.modalActions}>
                <button type="button" style={s.cancelBtn} onClick={closeModal}>Cancel</button>
                <button type="submit" style={s.createBtn} disabled={submitting}>
                  {submitting ? 'Saving...' : modal === 'create' ? 'Create User' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

function Field({ label, required, children }) {
  return (
    <div style={{ marginBottom: '1rem' }}>
      <label style={{ display: 'block', marginBottom: '0.3rem', fontWeight: '600', fontSize: '0.85rem' }}>
        {label}{required && <span style={{ color: '#c00' }}> *</span>}
      </label>
      {children}
    </div>
  );
}

const s = {
  card: { background: '#fff', borderRadius: '8px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)', padding: '1.5rem' },
  cardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  title: { margin: 0, fontSize: '1.1rem', color: '#003580' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  th: { textAlign: 'left', padding: '0.6rem 0.75rem', background: '#f5f7fa', borderBottom: '2px solid #e5e7eb', fontWeight: '600', color: '#374151' },
  tr: { borderBottom: '1px solid #e5e7eb' },
  td: { padding: '0.65rem 0.75rem', verticalAlign: 'middle' },
  badge: { color: '#fff', padding: '0.2rem 0.55rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '600' },
  editBtn: { background: '#1d4ed8', color: '#fff', border: 'none', borderRadius: '4px', padding: '0.3rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem', marginRight: '0.4rem' },
  deleteBtn: { background: '#dc2626', color: '#fff', border: 'none', borderRadius: '4px', padding: '0.3rem 0.75rem', cursor: 'pointer', fontSize: '0.8rem' },
  createBtn: { background: '#003580', color: '#fff', border: 'none', borderRadius: '6px', padding: '0.5rem 1.2rem', cursor: 'pointer', fontWeight: '600', fontSize: '0.9rem' },
  cancelBtn: { background: '#e5e7eb', color: '#374151', border: 'none', borderRadius: '6px', padding: '0.5rem 1.2rem', cursor: 'pointer', fontWeight: '600', fontSize: '0.9rem' },
  alert: { background: '#ffe0e0', color: '#c00', padding: '0.75rem 1rem', borderRadius: '6px', marginBottom: '1rem', fontSize: '0.9rem' },
  success: { background: '#d1fae5', color: '#065f46', padding: '0.75rem 1rem', borderRadius: '6px', marginBottom: '1rem', fontSize: '0.9rem' },
  center: { textAlign: 'center', color: '#888', padding: '2rem' },
  overlay: { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  modal: { background: '#fff', borderRadius: '8px', padding: '2rem', width: '460px', maxWidth: '95vw', maxHeight: '90vh', overflowY: 'auto' },
  modalTitle: { margin: '0 0 1.5rem', color: '#003580', fontSize: '1.1rem' },
  modalActions: { display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.5rem' },
  input: { width: '100%', padding: '0.5rem 0.75rem', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '0.9rem', boxSizing: 'border-box' },
};
