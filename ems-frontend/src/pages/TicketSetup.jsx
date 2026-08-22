import { useEffect, useState, useCallback } from 'react';
import { getActiveDepartments } from '../api/departments';
import { getEmployees } from '../api/employees';
import { getTeam, addTeamMember, removeTeamMember } from '../api/ticketTeams';
import { getSlaPolicies, saveSlaPolicy } from '../api/slaPolicies';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

// Mirrors TicketServiceImpl.DEFAULT_SLA_HOURS - shown as placeholder hints so
// admins know what applies automatically before they configure anything.
const DEFAULT_HOURS = { HIGH: [4, 24], MEDIUM: [24, 48], LOW: [48, 72] };

function SlaRow({ departmentId, priority, existing, onSaved, setError }) {
  const [acceptanceHours, setAcceptanceHours] = useState(existing?.acceptanceHours ?? '');
  const [resolutionHours, setResolutionHours] = useState(existing?.resolutionHours ?? '');
  const [saving, setSaving] = useState(false);
  const [defaultAcceptance, defaultResolution] = DEFAULT_HOURS[priority];

  async function handleSave() {
    setError('');
    setSaving(true);
    try {
      await saveSlaPolicy({
        departmentId,
        priority,
        acceptanceHours: Number(acceptanceHours),
        resolutionHours: Number(resolutionHours),
      });
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <tr>
      <td>{priority}</td>
      <td>
        <input
          type="number"
          min={1}
          placeholder={`default ${defaultAcceptance}h`}
          value={acceptanceHours}
          onChange={(e) => setAcceptanceHours(e.target.value)}
          style={{ width: 100 }}
        />
      </td>
      <td>
        <input
          type="number"
          min={1}
          placeholder={`default ${defaultResolution}h`}
          value={resolutionHours}
          onChange={(e) => setResolutionHours(e.target.value)}
          style={{ width: 100 }}
        />
      </td>
      <td>
        <button
          type="button"
          className="btn btn-link"
          onClick={handleSave}
          disabled={saving || !acceptanceHours || !resolutionHours}
        >
          {saving ? 'Saving…' : 'Save'}
        </button>
      </td>
    </tr>
  );
}

export default function TicketSetup() {
  const [departments, setDepartments] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [departmentId, setDepartmentId] = useState('');
  const [team, setTeam] = useState([]);
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [newMember, setNewMember] = useState({ employeeId: '', roleInTeam: 'MEMBER' });

  useEffect(() => {
    Promise.all([getActiveDepartments(), getEmployees({ page: 0, size: 200 })])
      .then(([deptRes, empRes]) => {
        const depts = deptRes.data || [];
        setDepartments(depts);
        setEmployees(empRes.data?.content || []);
        if (depts.length) setDepartmentId(depts[0].id);
      })
      .catch((err) => setError(err.message));
  }, []);

  const load = useCallback(() => {
    if (!departmentId) return;
    setLoading(true);
    Promise.all([getTeam(departmentId), getSlaPolicies(departmentId)])
      .then(([teamRes, slaRes]) => {
        setTeam(teamRes.data || []);
        setPolicies(slaRes.data || []);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [departmentId]);

  useEffect(() => { load(); }, [load]);

  async function handleAddMember(e) {
    e.preventDefault();
    setError('');
    try {
      await addTeamMember({ departmentId: Number(departmentId), employeeId: Number(newMember.employeeId), roleInTeam: newMember.roleInTeam });
      setSuccess('Team member added');
      setNewMember({ employeeId: '', roleInTeam: 'MEMBER' });
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleRemove(id) {
    if (!confirm('Remove this person from the ticket team?')) return;
    setError('');
    try {
      await removeTeamMember(id);
      setSuccess('Removed from team');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  const members = team.filter((t) => t.roleInTeam === 'MEMBER');
  const escalation = team.filter((t) => t.roleInTeam === 'ESCALATION');

  return (
    <div>
      <div className="page-header">
        <h1>Ticket Setup</h1>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <label style={{ display: 'block', maxWidth: 320, marginBottom: 20 }}>
        Department
        <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
          {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
        </select>
      </label>

      {loading ? <Loading /> : (
        <div className="two-col">
          <div className="card-panel">
            <h2>Ticket Team</h2>
            <p className="ticket-meta">
              MEMBERs can claim/be transferred tickets for this department. ESCALATION contacts are where an
              unsatisfied raiser's escalation lands (falling back to the department's Head of Department if
              none are configured - set that on the Departments page).
            </p>

            <form className="form-grid" onSubmit={handleAddMember} style={{ marginBottom: 16 }}>
              <label>
                Employee
                <select value={newMember.employeeId} onChange={(e) => setNewMember({ ...newMember, employeeId: e.target.value })} required>
                  <option value="" disabled>Select employee</option>
                  {employees.map((e) => <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.employeeCode})</option>)}
                </select>
              </label>
              <label>
                Role
                <select value={newMember.roleInTeam} onChange={(e) => setNewMember({ ...newMember, roleInTeam: e.target.value })}>
                  <option value="MEMBER">Member</option>
                  <option value="ESCALATION">Escalation contact</option>
                </select>
              </label>
              <button type="submit" className="btn btn-primary">+ Add</button>
            </form>

            <h3>Members</h3>
            {members.length ? (
              <ul className="plain-list">
                {members.map((m) => (
                  <li key={m.id} className="row-actions">
                    {m.employeeName}
                    <button className="btn btn-link btn-danger" onClick={() => handleRemove(m.id)}>Remove</button>
                  </li>
                ))}
              </ul>
            ) : <p className="empty-row">No members yet</p>}

            <h3>Escalation Contacts</h3>
            {escalation.length ? (
              <ul className="plain-list">
                {escalation.map((m) => (
                  <li key={m.id} className="row-actions">
                    {m.employeeName}
                    <button className="btn btn-link btn-danger" onClick={() => handleRemove(m.id)}>Remove</button>
                  </li>
                ))}
              </ul>
            ) : <p className="empty-row">No escalation contacts yet</p>}
          </div>

          <div className="card-panel">
            <h2>SLA Policy</h2>
            <p className="ticket-meta">
              How long this department has to accept and then resolve a ticket of each priority, before it's
              flagged as an SLA breach. Leave blank to use the built-in defaults shown as placeholders.
            </p>
            <div className="data-table-scroll">
              <table className="data-table">
                <thead>
                  <tr><th>Priority</th><th>Acceptance (hrs)</th><th>Resolution (hrs)</th><th></th></tr>
                </thead>
                <tbody>
                  {PRIORITIES.map((p) => (
                    <SlaRow
                      key={p}
                      departmentId={Number(departmentId)}
                      priority={p}
                      existing={policies.find((pol) => pol.priority === p)}
                      onSaved={() => { setSuccess('SLA policy saved'); load(); }}
                      setError={setError}
                    />
                  ))}
                </tbody>
              </table>
            </div>          </div>
        </div>
      )}
    </div>
  );
}
