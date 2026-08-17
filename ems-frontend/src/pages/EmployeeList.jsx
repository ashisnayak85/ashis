import { useEffect, useState, useCallback } from 'react';
import { getEmployees, createEmployee, updateEmployee, deleteEmployee } from '../api/employees';
import { uploadFile, QUALIFICATION_CERT_ENTITY_TYPE } from '../api/files';
import Pagination from '../components/Pagination';
import EmployeeFormModal from '../components/EmployeeFormModal';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';

export default function EmployeeList() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null); // null = closed, {} = new, {...} = edit
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    getEmployees({ page, size: 10, search })
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [page, search]);

  useEffect(() => { load(); }, [load]);

  async function handleSave(payload, certificateFile) {
    let saved;
    if (editing?.id) {
      const res = await updateEmployee(editing.id, payload);
      saved = res.data;
      setSuccess('Employee updated');
    } else {
      const res = await createEmployee(payload);
      saved = res.data;
      setSuccess('Employee created');
    }

    // The certificate upload needs a real employee id, so it can only happen
    // after the employee row exists (i.e. after create/update above).
    // A failure here doesn't roll back the employee save - the record is
    // already saved, so we surface the upload error but keep going.
    if (certificateFile && saved?.id) {
      try {
        await uploadFile(certificateFile, QUALIFICATION_CERT_ENTITY_TYPE, saved.id);
      } catch (err) {
        setError(`Employee saved, but certificate upload failed: ${err.message}`);
      }
    }

    setEditing(null);
    load();
  }

  async function handleDelete(id) {
    try {
      await deleteEmployee(id);
      setSuccess('Employee deleted');
      setConfirmDeleteId(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Employees</h1>
        <button className="btn btn-primary" onClick={() => setEditing({})}>+ New Employee</button>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <input
        className="search-input"
        placeholder="Search by name, email, or code…"
        value={search}
        onChange={(e) => { setPage(0); setSearch(e.target.value); }}
      />

      {loading ? <Loading /> : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Email</th>
                <th>Department</th>
                <th>Location</th>
                <th>Designation</th>
                <th>Active</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {data?.content?.length ? data.content.map((emp) => (
                <tr key={emp.id}>
                  <td>{emp.employeeCode}</td>
                  <td>{emp.firstName} {emp.lastName}</td>
                  <td>{emp.email}</td>
                  <td>{emp.departmentName}</td>
                  <td>{emp.locationName || '-'}</td>
                  <td>{emp.designation}</td>
                  <td>{emp.active ? 'Yes' : 'No'}</td>
                  <td className="row-actions">
                    <button className="btn btn-link" onClick={() => setEditing(emp)}>Edit</button>
                    <button className="btn btn-link btn-danger" onClick={() => setConfirmDeleteId(emp.id)}>Delete</button>
                  </td>
                </tr>
              )) : (
                <tr><td colSpan={8} className="empty-row">No employees found</td></tr>
              )}
            </tbody>
          </table>

          {data && (
            <Pagination
              pageNumber={data.pageNumber}
              totalPages={data.totalPages}
              first={data.first}
              last={data.last}
              onChange={setPage}
            />
          )}
        </>
      )}

      {editing !== null && (
        <EmployeeFormModal
          employee={editing.id ? editing : null}
          onClose={() => setEditing(null)}
          onSubmit={handleSave}
        />
      )}

      {confirmDeleteId !== null && (
        <div className="modal-backdrop" onClick={() => setConfirmDeleteId(null)}>
          <div className="modal-card modal-card-small" onClick={(e) => e.stopPropagation()}>
            <h2>Delete employee?</h2>
            <p>This can't be undone.</p>
            <div className="modal-actions">
              <button className="btn btn-link" onClick={() => setConfirmDeleteId(null)}>Cancel</button>
              <button className="btn btn-danger" onClick={() => handleDelete(confirmDeleteId)}>Delete</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
