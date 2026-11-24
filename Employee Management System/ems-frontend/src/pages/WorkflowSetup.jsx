import { useEffect, useState, useCallback } from 'react';
import {
  getWorkflowRoles, createWorkflowRole, updateWorkflowRole,
  deactivateWorkflowRole, reactivateWorkflowRole,
} from '../api/workflowRoles';
import {
  getWorkflowStagesForModule, createWorkflowStage, updateWorkflowStage,
  deactivateWorkflowStage, reactivateWorkflowStage, reorderWorkflowStages,
} from '../api/workflowStages';
import {
  assignEmployeeRole, revokeEmployeeRole, getRolesForEmployee,
} from '../api/employeeWorkflowRoles';
import {
  getPrivilegeModules, deactivatePrivilegeModule, reactivatePrivilegeModule,
} from '../api/privilegeModules';
import { getActiveDepartments } from '../api/departments';
import { getEmployees } from '../api/employees';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';

const APPROVER_TYPES = ['REPORTING_MANAGER', 'ROLE_IN_DEPARTMENT', 'SPECIFIC_ROLE', 'SPECIFIC_USER'];
const DEPARTMENT_SCOPES = ['EMPLOYEE_DEPARTMENT', 'FIXED_DEPARTMENT'];

const TABS = [
  { key: 'stages', label: 'Approval Chain' },
  { key: 'roles', label: 'Workflow Roles' },
  { key: 'access', label: 'Employee Access' },
  { key: 'modules', label: 'Modules' },
];

// PURPOSE: single admin screen for EVERY module's approval workflow - one
// module dropdown instead of a new page (and a new sidebar entry) per
// module. Workflow Roles and Employee Access are module-agnostic and shown
// as-is; only Approval Chain is filtered to the selected module. See
// PrivilegeModule / WorkflowStage entity javadocs on the backend.
export default function WorkflowSetup() {
  const [tab, setTab] = useState('stages');
  const [departments, setDepartments] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [modules, setModules] = useState([]);
  const [selectedModuleId, setSelectedModuleId] = useState('');
  const [modulesLoading, setModulesLoading] = useState(true);
  const [error, setError] = useState('');

  const loadModules = useCallback(() => {
    setModulesLoading(true);
    getPrivilegeModules(false)
      .then((res) => {
        const list = res.data || [];
        setModules(list);
        setSelectedModuleId((prev) => prev || (list[0] ? String(list[0].id) : ''));
      })
      .catch((err) => setError(err.message))
      .finally(() => setModulesLoading(false));
  }, []);

  useEffect(() => { loadModules(); }, [loadModules]);
  useEffect(() => {
    getActiveDepartments().then((res) => setDepartments(res.data || [])).catch(() => {});
    getEmployees({ page: 0, size: 500 }).then((res) => setEmployees(res.data?.content || [])).catch(() => {});
  }, []);

  return (
    <div>
      <div className="page-header">
        <h1>Workflow Setup</h1>
      </div>

      <ErrorBanner message={error} />

      <div className="form-grid" style={{ maxWidth: 360, marginBottom: '1rem' }}>
        <label>
          Module
          <select value={selectedModuleId} onChange={(e) => setSelectedModuleId(e.target.value)} disabled={modulesLoading}>
            {modules.length === 0 && <option value="">No modules configured</option>}
            {modules.map((m) => <option key={m.id} value={m.id}>{m.name}{!m.active ? ' (inactive)' : ''}</option>)}
          </select>
        </label>
      </div>

      <div className="tab-strip">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`tab-btn${tab === t.key ? ' tab-btn-active' : ''}`}
            onClick={() => setTab(t.key)}
            type="button"
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'stages' && (
        selectedModuleId
          ? <ApprovalChainPanel moduleId={Number(selectedModuleId)} departments={departments} employees={employees} />
          : <p className="empty-row">Create a module in the Modules tab first.</p>
      )}
      {tab === 'roles' && <WorkflowRolesPanel />}
      {tab === 'access' && <EmployeeAccessPanel departments={departments} employees={employees} />}
      {tab === 'modules' && <ModulesPanel onChanged={loadModules} />}
    </div>
  );
}

// =============================================================================
// Panel: Privilege Modules - the master list backing the dropdown above.
// Read-only + deactivate/reactivate ONLY - there is deliberately no "create"
// here. A module's code is a hardcoded contract with the backend service
// that filters by it (see AppConstants.MODULE_CODE_* / SystemModule on the
// backend); a new module is registered automatically by the app on
// startup once a developer adds it to that enum alongside the feature code
// that uses it, never typed in through this screen.
// =============================================================================
function ModulesPanel({ onChanged }) {
  const [modules, setModules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    getPrivilegeModules(false)
      .then((res) => setModules(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  async function toggleActive(module) {
    setError('');
    try {
      if (module.active) {
        await deactivatePrivilegeModule(module.id);
        setSuccess(`${module.name} deactivated`);
      } else {
        await reactivatePrivilegeModule(module.id);
        setSuccess(`${module.name} reactivated`);
      }
      load();
      onChanged?.();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h2>Modules</h2>
      </div>
      <p className="hint-text">
        Modules ship together with their own approval-driven feature code and are registered automatically -
        there is no "create module" action here. Ask engineering if a module you expect isn't listed.
      </p>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr><th>Code</th><th>Name</th><th>Description</th><th>Active</th><th></th></tr>
          </thead>
          <tbody>
            {modules.length ? modules.map((m) => (
              <tr key={m.id}>
                <td>{m.code}</td>
                <td>{m.name}</td>
                <td>{m.description}</td>
                <td>{m.active ? 'Yes' : 'No'}</td>
                <td className="row-actions">
                  <button className="btn btn-link btn-danger" onClick={() => toggleActive(m)}>
                    {m.active ? 'Deactivate' : 'Reactivate'}
                  </button>
                </td>
              </tr>
            )) : (
              <tr><td colSpan={5} className="empty-row">No modules registered yet</td></tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}

// =============================================================================
// Panel: Workflow Role master - add/rename/retire a business role. Shared
// across every module (unchanged from the resignation-only version).
// =============================================================================
function WorkflowRolesPanel() {
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: '', description: '' });

  const load = useCallback(() => {
    setLoading(true);
    getWorkflowRoles(false)
      .then((res) => setRoles(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  function openNew() {
    setForm({ name: '', description: '' });
    setEditing({});
  }

  function openEdit(role) {
    setForm({ name: role.name, description: role.description || '' });
    setEditing(role);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      if (editing.id) {
        await updateWorkflowRole(editing.id, form.name, form.description);
        setSuccess('Workflow role updated');
      } else {
        await createWorkflowRole(form.name, form.description);
        setSuccess('Workflow role created');
      }
      setEditing(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function toggleActive(role) {
    setError('');
    try {
      if (role.active) {
        await deactivateWorkflowRole(role.id);
        setSuccess(`${role.name} deactivated`);
      } else {
        await reactivateWorkflowRole(role.id);
        setSuccess(`${role.name} reactivated`);
      }
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h2>Workflow Roles</h2>
        <button className="btn btn-primary" onClick={openNew}>+ New Role</button>
      </div>
      <p className="hint-text">
        Business roles used for approval routing (e.g. HOD, HR Head, Project Head) - shared across every
        module's approval chain, and separate from login/security roles.
      </p>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Description</th>
              <th>Active</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {roles.length ? roles.map((r) => (
              <tr key={r.id}>
                <td>{r.name}</td>
                <td>{r.description}</td>
                <td>{r.active ? 'Yes' : 'No'}</td>
                <td className="row-actions">
                  <button className="btn btn-link" onClick={() => openEdit(r)}>Edit</button>
                  <button className="btn btn-link btn-danger" onClick={() => toggleActive(r)}>
                    {r.active ? 'Deactivate' : 'Reactivate'}
                  </button>
                </td>
              </tr>
            )) : (
              <tr><td colSpan={4} className="empty-row">No workflow roles found</td></tr>
            )}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h2>{editing.id ? 'Edit Workflow Role' : 'New Workflow Role'}</h2>
            <div className="form-grid">
              <label>
                Name
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </label>
              <label>
                Description
                <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </label>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-link" onClick={() => setEditing(null)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

// =============================================================================
// Panel: Approval chain for ONE module - build/reorder/edit WorkflowStage
// rows. Scoped entirely to the moduleId passed down from the dropdown.
// =============================================================================
function ApprovalChainPanel({ moduleId, departments, employees }) {
  const [stages, setStages] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyStageForm());

  function emptyStageForm() {
    return {
      name: '', sequenceOrder: '', approverType: 'REPORTING_MANAGER',
      workflowRoleId: '', departmentScope: '', fixedDepartmentId: '', specificEmployeeId: '',
    };
  }

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([getWorkflowStagesForModule(moduleId), getWorkflowRoles(true)])
      .then(([stageRes, roleRes]) => {
        setStages(stageRes.data || []);
        setRoles(roleRes.data || []);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [moduleId]);

  useEffect(() => { load(); }, [load]);

  function openNew() {
    const nextSeq = stages.length ? Math.max(...stages.map((s) => s.sequenceOrder)) + 1 : 1;
    setForm({ ...emptyStageForm(), sequenceOrder: nextSeq });
    setEditing({});
  }

  function openEdit(stage) {
    setForm({
      name: stage.name,
      sequenceOrder: stage.sequenceOrder,
      approverType: stage.approverType,
      workflowRoleId: stage.workflowRoleId || '',
      departmentScope: stage.departmentScope || '',
      fixedDepartmentId: stage.fixedDepartmentId || '',
      specificEmployeeId: stage.specificEmployeeId || '',
    });
    setEditing(stage);
  }

  function toPayload() {
    return {
      privilegeModuleId: moduleId,
      name: form.name,
      sequenceOrder: Number(form.sequenceOrder),
      approverType: form.approverType,
      workflowRoleId: form.workflowRoleId ? Number(form.workflowRoleId) : null,
      departmentScope: form.approverType === 'ROLE_IN_DEPARTMENT' ? form.departmentScope : null,
      fixedDepartmentId: form.fixedDepartmentId ? Number(form.fixedDepartmentId) : null,
      specificEmployeeId: form.specificEmployeeId ? Number(form.specificEmployeeId) : null,
    };
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      if (editing.id) {
        await updateWorkflowStage(editing.id, toPayload());
        setSuccess('Approval stage updated');
      } else {
        await createWorkflowStage(toPayload());
        setSuccess('Approval stage created');
      }
      setEditing(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function toggleActive(stage) {
    setError('');
    try {
      if (stage.active) {
        await deactivateWorkflowStage(stage.id);
        setSuccess(`${stage.name} deactivated`);
      } else {
        await reactivateWorkflowStage(stage.id);
        setSuccess(`${stage.name} reactivated`);
      }
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function move(stage, direction) {
    const sorted = [...stages].sort((a, b) => a.sequenceOrder - b.sequenceOrder);
    const idx = sorted.findIndex((s) => s.id === stage.id);
    const swapIdx = idx + direction;
    if (swapIdx < 0 || swapIdx >= sorted.length) return;
    [sorted[idx], sorted[swapIdx]] = [sorted[swapIdx], sorted[idx]];
    setError('');
    try {
      await reorderWorkflowStages(moduleId, sorted.map((s) => s.id));
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  const sortedStages = [...stages].sort((a, b) => a.sequenceOrder - b.sequenceOrder);

  return (
    <div>
      <div className="page-header">
        <h2>Approval Chain</h2>
        <button className="btn btn-primary" onClick={openNew}>+ New Stage</button>
      </div>
      <p className="hint-text">
        The ordered chain every request in this module is routed through. Reorder with the arrows -
        sequence changes only affect requests submitted after the change.
      </p>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Name</th>
              <th>Approver Type</th>
              <th>Role</th>
              <th>Scope</th>
              <th>Active</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {sortedStages.length ? sortedStages.map((s, i) => (
              <tr key={s.id}>
                <td>{s.sequenceOrder}</td>
                <td>{s.name}</td>
                <td>{s.approverType}</td>
                <td>{s.workflowRoleName || '—'}</td>
                <td>
                  {s.approverType === 'ROLE_IN_DEPARTMENT'
                    ? (s.departmentScope === 'FIXED_DEPARTMENT' ? `Fixed: ${s.fixedDepartmentName}` : "Employee's own department")
                    : s.approverType === 'SPECIFIC_USER' ? s.specificEmployeeName : '—'}
                </td>
                <td>{s.active ? 'Yes' : 'No'}</td>
                <td className="row-actions">
                  <button className="btn btn-link" disabled={i === 0} onClick={() => move(s, -1)}>↑</button>
                  <button className="btn btn-link" disabled={i === sortedStages.length - 1} onClick={() => move(s, 1)}>↓</button>
                  <button className="btn btn-link" onClick={() => openEdit(s)}>Edit</button>
                  <button className="btn btn-link btn-danger" onClick={() => toggleActive(s)}>
                    {s.active ? 'Deactivate' : 'Reactivate'}
                  </button>
                </td>
              </tr>
            )) : (
              <tr><td colSpan={7} className="empty-row">No approval stages configured for this module yet</td></tr>
            )}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h2>{editing.id ? 'Edit Approval Stage' : 'New Approval Stage'}</h2>
            <div className="form-grid">
              <label>
                Name
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </label>
              <label>
                Sequence Order
                <input type="number" min={1} value={form.sequenceOrder}
                  onChange={(e) => setForm({ ...form, sequenceOrder: e.target.value })} required />
              </label>
              <label>
                Approver Type
                <select value={form.approverType} onChange={(e) => setForm({ ...form, approverType: e.target.value })}>
                  {APPROVER_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </label>

              {(form.approverType === 'ROLE_IN_DEPARTMENT' || form.approverType === 'SPECIFIC_ROLE') && (
                <label>
                  Workflow Role
                  <select value={form.workflowRoleId} onChange={(e) => setForm({ ...form, workflowRoleId: e.target.value })} required>
                    <option value="">Select a role</option>
                    {roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                  </select>
                </label>
              )}

              {form.approverType === 'ROLE_IN_DEPARTMENT' && (
                <>
                  <label>
                    Department Scope
                    <select value={form.departmentScope} onChange={(e) => setForm({ ...form, departmentScope: e.target.value })} required>
                      <option value="">Select scope</option>
                      {DEPARTMENT_SCOPES.map((s) => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </label>
                  {form.departmentScope === 'FIXED_DEPARTMENT' && (
                    <label>
                      Fixed Department
                      <select value={form.fixedDepartmentId} onChange={(e) => setForm({ ...form, fixedDepartmentId: e.target.value })} required>
                        <option value="">Select department</option>
                        {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                      </select>
                    </label>
                  )}
                </>
              )}

              {form.approverType === 'SPECIFIC_USER' && (
                <label>
                  Specific Employee
                  <select value={form.specificEmployeeId} onChange={(e) => setForm({ ...form, specificEmployeeId: e.target.value })} required>
                    <option value="">Select employee</option>
                    {employees.map((e) => <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.employeeCode})</option>)}
                  </select>
                </label>
              )}
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-link" onClick={() => setEditing(null)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

// =============================================================================
// Panel: Employee Access - assign/revoke a workflow role on an employee.
// Module-agnostic (unchanged from the resignation-only version) - a role
// like "HOD" is reused across every module's chain once assigned here.
// =============================================================================
function EmployeeAccessPanel({ departments, employees }) {
  const [selectedEmployeeId, setSelectedEmployeeId] = useState('');
  const [roles, setRoles] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [form, setForm] = useState({ workflowRoleId: '', scopeDepartmentId: '' });

  useEffect(() => {
    getWorkflowRoles(true).then((res) => setRoles(res.data || [])).catch((err) => setError(err.message));
  }, []);

  const loadAssignments = useCallback((employeeId) => {
    if (!employeeId) { setAssignments([]); return; }
    setLoading(true);
    getRolesForEmployee(employeeId)
      .then((res) => setAssignments(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadAssignments(selectedEmployeeId); }, [selectedEmployeeId, loadAssignments]);

  async function handleAssign(e) {
    e.preventDefault();
    setError('');
    try {
      await assignEmployeeRole(
        Number(selectedEmployeeId),
        Number(form.workflowRoleId),
        form.scopeDepartmentId ? Number(form.scopeDepartmentId) : null,
      );
      setSuccess('Role assigned');
      setForm({ workflowRoleId: '', scopeDepartmentId: '' });
      loadAssignments(selectedEmployeeId);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleRevoke(assignmentId) {
    if (!confirm('Remove this role from the employee?')) return;
    setError('');
    try {
      await revokeEmployeeRole(assignmentId);
      setSuccess('Role removed');
      loadAssignments(selectedEmployeeId);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h2>Employee Access</h2>
      </div>
      <p className="hint-text">
        Give or remove a workflow role on an employee. Leave scope department empty for a company-wide
        (global) hold of the role. Roles assigned here are shared across every module's approval chain.
      </p>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="form-grid" style={{ maxWidth: 480, marginBottom: '1.5rem' }}>
        <label>
          Employee
          <select value={selectedEmployeeId} onChange={(e) => setSelectedEmployeeId(e.target.value)}>
            <option value="">Select an employee</option>
            {employees.map((e) => <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.employeeCode})</option>)}
          </select>
        </label>
      </div>

      {selectedEmployeeId && (
        <>
          <form className="form-grid" style={{ maxWidth: 640, alignItems: 'end' }} onSubmit={handleAssign}>
            <label>
              Workflow Role
              <select value={form.workflowRoleId} onChange={(e) => setForm({ ...form, workflowRoleId: e.target.value })} required>
                <option value="">Select a role</option>
                {roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
              </select>
            </label>
            <label>
              Scope Department (optional - blank = global)
              <select value={form.scopeDepartmentId} onChange={(e) => setForm({ ...form, scopeDepartmentId: e.target.value })}>
                <option value="">Global (company-wide)</option>
                {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
            </label>
            <button type="submit" className="btn btn-primary">+ Assign Role</button>
          </form>

          {loading ? <Loading /> : (
            <table className="data-table" style={{ marginTop: '1.5rem' }}>
              <thead>
                <tr>
                  <th>Role</th>
                  <th>Scope</th>
                  <th>Status</th>
                  <th>Assigned</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {assignments.length ? assignments.map((a) => (
                  <tr key={a.id}>
                    <td>{a.workflowRoleName}</td>
                    <td>{a.scopeDepartmentName || 'Global'}</td>
                    <td>{a.active ? 'Active' : `Removed${a.removedByName ? ` by ${a.removedByName}` : ''}`}</td>
                    <td>{a.assignedAt ? new Date(a.assignedAt).toLocaleDateString() : '—'}</td>
                    <td className="row-actions">
                      {a.active && <button className="btn btn-link btn-danger" onClick={() => handleRevoke(a.id)}>Revoke</button>}
                    </td>
                  </tr>
                )) : (
                  <tr><td colSpan={5} className="empty-row">No role assignments for this employee</td></tr>
                )}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  );
}
