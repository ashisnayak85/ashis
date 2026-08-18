import { useEffect, useState, useCallback } from 'react';
import { getAllEmployees, updateEmployee } from '../api/employees';
import Pagination from '../components/Pagination';
import EmployeeFormModal from '../components/EmployeeFormModal';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';

export default function AllEmployeeList() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    // This calls /api/allEmployees -> findAll(pageable), i.e. every row,
    // active or not - as opposed to /employees which filters active=true.
    getAllEmployees({ page, size: 10, search })
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [page, search]);

  useEffect(() => { load(); }, [load]);

  async function handleSave(payload) {
    await updateEmployee(editing.id, payload);
    setSuccess('Employee updated');
    setEditing(null);
    load();
  }

  return (
    <div>
      <div className="page-header">
        <h1>All Employees</h1>
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
                <th>Designation</th>
                <th>Status</th>
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
                  <td>{emp.designationName || '-'}</td>
                  <td>
                    <span className={emp.active ? 'badge badge-success' : 'badge badge-muted'}>
                      {emp.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="row-actions">
                    <button className="btn btn-link" onClick={() => setEditing(emp)}>Edit</button>
                  </td>
                </tr>
              )) : (
                <tr><td colSpan={7} className="empty-row">No employees found</td></tr>
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
          employee={editing}
          onClose={() => setEditing(null)}
          onSubmit={handleSave}
        />
      )}
    </div>
  );
}
