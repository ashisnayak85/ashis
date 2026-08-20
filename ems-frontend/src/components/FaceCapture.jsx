import { useEffect, useRef, useState } from 'react';

/*
 * PURPOSE: Opens the device webcam, lets the person take a still photo, and
 * hands the captured image back to the caller as a Blob (JPEG) - used both
 * for the one-time face enrollment and for the photo taken at every
 * punch-in/punch-out when face verification is switched on.
 *
 * This component only CAPTURES a photo - it never decides whether the face
 * matches anything. That comparison always happens on the server
 * (FaceRecognitionService), never in the browser, so nothing here can be
 * tampered with to fake a match.
 *
 * Props:
 *   title       - short heading shown above the camera preview
 *   helperText  - one line of guidance under the title
 *   confirmLabel- text on the "use this photo" button
 *   busy        - true while the parent is uploading/verifying the photo
 *   onCapture(blob) - called once the person confirms a captured photo
 *   onCancel()  - called if they back out without capturing
 */
export default function FaceCapture({
  title = 'Capture your photo',
  helperText = 'Face the camera directly, in good light, with nothing covering your face.',
  confirmLabel = 'Use this photo',
  busy = false,
  onCapture,
  onCancel,
}) {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const [error, setError] = useState('');
  const [previewUrl, setPreviewUrl] = useState(null); // set once a photo is captured, before confirming
  const [capturedBlob, setCapturedBlob] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function startCamera() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'user', width: { ideal: 480 }, height: { ideal: 480 } },
          audio: false,
        });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
        }
      } catch (err) {
        setError(
          err.name === 'NotAllowedError'
            ? 'Camera access was blocked. Please allow camera access for this site and try again.'
            : 'Could not access the camera. Please check it is connected and not in use by another app.'
        );
      }
    }

    startCamera();
    return () => {
      cancelled = true;
      streamRef.current?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  function stopCamera() {
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
  }

  function handleTakePhoto() {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || !video.videoWidth) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);

    canvas.toBlob(
      (blob) => {
        if (!blob) {
          setError('Could not capture the photo - please try again.');
          return;
        }
        setCapturedBlob(blob);
        setPreviewUrl(URL.createObjectURL(blob));
        stopCamera();
      },
      'image/jpeg',
      0.9
    );
  }

  function handleRetake() {
    setPreviewUrl(null);
    setCapturedBlob(null);
    setError('');
    // Re-open the camera for another attempt.
    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: 'user', width: { ideal: 480 }, height: { ideal: 480 } }, audio: false })
      .then((stream) => {
        streamRef.current = stream;
        if (videoRef.current) videoRef.current.srcObject = stream;
      })
      .catch(() => setError('Could not re-open the camera. Please try again.'));
  }

  function handleConfirm() {
    if (capturedBlob) onCapture(capturedBlob);
  }

  function handleCancel() {
    stopCamera();
    onCancel?.();
  }

  return (
    <div className="face-capture">
      <h3>{title}</h3>
      <p className="page-subtitle">{helperText}</p>

      {error && <div className="banner banner-error">{error}</div>}

      <div className="face-capture-frame">
        {!previewUrl ? (
          <video ref={videoRef} autoPlay playsInline muted className="face-capture-video face-capture-mirrored" />
        ) : (
          <img src={previewUrl} alt="Captured face" className="face-capture-video" />
        )}
      </div>

      <canvas ref={canvasRef} style={{ display: 'none' }} />

      <div className="face-capture-actions">
        {!previewUrl ? (
          <>
            <button type="button" className="btn btn-primary" onClick={handleTakePhoto} disabled={!!error}>
              Take Photo
            </button>
            <button type="button" className="btn btn-link" onClick={handleCancel}>
              Cancel
            </button>
          </>
        ) : (
          <>
            <button type="button" className="btn btn-primary" onClick={handleConfirm} disabled={busy}>
              {busy ? 'Verifying…' : confirmLabel}
            </button>
            <button type="button" className="btn btn-link" onClick={handleRetake} disabled={busy}>
              Retake
            </button>
            <button type="button" className="btn btn-link" onClick={handleCancel} disabled={busy}>
              Cancel
            </button>
          </>
        )}
      </div>
    </div>
  );
}
