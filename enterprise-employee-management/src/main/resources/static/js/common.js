/**
 * COMMON.JS - Shared AJAX utilities
 * 
 * CSRF: Spring Security requires CSRF token in AJAX POST/PUT/DELETE requests
 * We read token from meta tags set in layout/fragments.html
 */
function getCsrfHeaders() {
    return {
        'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content'),
        'Content-Type': 'application/json'
    };
}

function showAlert(message, type) {
    const alertHtml = `<div class="alert alert-${type} alert-dismissible fade show" role="alert">
        ${message}<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>`;
    $('.container').first().prepend(alertHtml);
    setTimeout(() => $('.alert').fadeOut(), 3000);
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString();
}
