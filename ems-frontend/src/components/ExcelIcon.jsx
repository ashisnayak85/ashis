// A small spreadsheet-with-checkmark glyph used on every "Export to Excel"
// button, so a green .btn-excel always reads as "download a spreadsheet" at
// a glance - consistent with the app's other stroke-based icons (see
// Layout.jsx) rather than a filled/branded logo.
export default function ExcelIcon({ size = 16 }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <line x1="3" y1="9" x2="21" y2="9" />
      <line x1="9" y1="9" x2="9" y2="21" />
      <path d="M12.5 13.5l3 3M15.5 13.5l-3 3" />
    </svg>
  );
}
