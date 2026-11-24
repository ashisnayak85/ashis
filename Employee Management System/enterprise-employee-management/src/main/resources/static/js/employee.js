/**
 * =============================================================================
 * EMPLOYEE.JS - AJAX Module (Phase 5)
 * =============================================================================
 * 
 * FLOW:
 * 1. Page loads -> loadEmployees() fires AJAX GET
 * 2. User clicks Save -> saveEmployee() fires AJAX POST/PUT with JSON body
 * 3. Controller returns ApiResponse JSON
 * 4. JavaScript updates table without page reload
 * 
 * KEY CONCEPTS:
 * - $.ajax(): jQuery AJAX wrapper
 * - JSON.stringify(): Converts JS object to JSON string for request body
 * - @RequestBody in Controller: Deserializes JSON to EmployeeDTO
 * - @Valid: Triggers backend validation on DTO
 */

let currentPage = 0;
const pageSize = 10;

$(document).ready(function () {
    loadEmployees();

    // Debounced search - fires AJAX on keyup
    let searchTimeout;
    $('#searchInput').on('keyup', function () {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            currentPage = 0;
            loadEmployees($(this).val());
        }, 300);
    });
});

function loadEmployees(search) {
    let url = `/api/employees?page=${currentPage}&size=${pageSize}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;

    $.ajax({
        url: url,
        method: 'GET',
        success: function (response) {
            if (response.success) {
                renderTable(response.data.content);
                renderPagination(response.data);
            }
        },
        error: function (xhr) {
            showAlert('Failed to load employees', 'danger');
        }
    });
}

function renderTable(employees) {
    const tbody = $('#employeeTableBody');
    tbody.empty();

    if (!employees || employees.length === 0) {
        tbody.append('<tr><td colspan="7" class="text-center">No employees found</td></tr>');
        return;
    }

    employees.forEach(emp => {
        tbody.append(`
            <tr>
                <td>${emp.employeeCode}</td>
                <td>${emp.firstName} ${emp.lastName}</td>
                <td>${emp.email}</td>
                <td>${emp.mobile || '-'}</td>
                <td>${emp.departmentName || '-'}</td>
                <td>${emp.designation || '-'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="editEmployee(${emp.id})">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteEmployee(${emp.id})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `);
    });
}

function renderPagination(pageData) {
    const pagination = $('#pagination');
    pagination.empty();

    for (let i = 0; i < pageData.totalPages; i++) {
        pagination.append(`
            <li class="page-item ${i === pageData.pageNumber ? 'active' : ''}">
                <a class="page-link" onclick="goToPage(${i})">${i + 1}</a>
            </li>
        `);
    }
}

function goToPage(page) {
    currentPage = page;
    loadEmployees($('#searchInput').val());
}

function openCreateModal() {
    $('#modalTitle').text('Add Employee');
    $('#employeeForm')[0].reset();
    $('#employeeId').val('');
    $('#formErrors').addClass('d-none');
}

function editEmployee(id) {
    $.ajax({
        url: `/api/employees/${id}`,
        method: 'GET',
        success: function (response) {
            if (response.success) {
                const emp = response.data;
                $('#modalTitle').text('Edit Employee');
                $('#employeeId').val(emp.id);
                $('#employeeCode').val(emp.employeeCode);
                $('#firstName').val(emp.firstName);
                $('#lastName').val(emp.lastName);
                $('#email').val(emp.email);
                $('#mobile').val(emp.mobile);
                $('#departmentId').val(emp.departmentId);
                $('#dateOfJoining').val(emp.dateOfJoining);
                $('#designation').val(emp.designation);
                $('#salary').val(emp.salary);
                new bootstrap.Modal('#employeeModal').show();
            }
        }
    });
}

function saveEmployee() {
    // Frontend validation
    const form = $('#employeeForm')[0];
    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        return;
    }

    const id = $('#employeeId').val();
    const data = {
        employeeCode: $('#employeeCode').val(),
        firstName: $('#firstName').val(),
        lastName: $('#lastName').val(),
        email: $('#email').val(),
        mobile: $('#mobile').val(),
        departmentId: parseInt($('#departmentId').val()),
        dateOfJoining: $('#dateOfJoining').val(),
        designation: $('#designation').val(),
        salary: parseFloat($('#salary').val()) || null
    };

    const isUpdate = id !== '';
    const url = isUpdate ? `/api/employees/${id}` : '/api/employees';
    const method = isUpdate ? 'PUT' : 'POST';

    $.ajax({
        url: url,
        method: method,
        headers: getCsrfHeaders(),
        data: JSON.stringify(data),
        success: function (response) {
            if (response.success) {
                bootstrap.Modal.getInstance('#employeeModal').hide();
                showAlert(response.message || 'Employee saved', 'success');
                loadEmployees();
            }
        },
        error: function (xhr) {
            const resp = xhr.responseJSON;
            if (resp && resp.errors) {
                $('#formErrors').removeClass('d-none').html(resp.errors.join('<br>'));
            } else {
                showAlert(resp?.message || 'Save failed', 'danger');
            }
        }
    });
}

function deleteEmployee(id) {
    if (!confirm('Are you sure you want to delete this employee?')) return;

    $.ajax({
        url: `/api/employees/${id}`,
        method: 'DELETE',
        headers: getCsrfHeaders(),
        success: function (response) {
            showAlert('Employee deleted', 'success');
            loadEmployees();
        },
        error: function (xhr) {
            showAlert(xhr.responseJSON?.message || 'Delete failed', 'danger');
        }
    });
}
