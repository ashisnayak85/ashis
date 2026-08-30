// The axios response interceptor already unwraps to response.data, so a
// blob request resolves directly to the Blob - just wire it to an <a> click.
export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
