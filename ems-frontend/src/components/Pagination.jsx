export default function Pagination({ pageNumber, totalPages, first, last, onChange }) {
  if (totalPages <= 1) return null;

  return (
    <div className="pagination">
      <button disabled={first} onClick={() => onChange(pageNumber - 1)}>Prev</button>
      <span>Page {pageNumber + 1} of {totalPages}</span>
      <button disabled={last} onClick={() => onChange(pageNumber + 1)}>Next</button>
    </div>
  );
}
