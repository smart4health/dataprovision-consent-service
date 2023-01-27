export class QuitConsent {
  modal: HTMLElement | undefined;

  constructor(private cancelRedirectUrl: string | null, questionId: string) {
    const confirmCancelModal = document.getElementById(`option-${questionId}-confirm-cancel-modal`);
    const cancelModal = document.getElementById(`option-${questionId}-close-confirm-cancel-modal`);
    const confirmModal = document.getElementById(`option-${questionId}-confirm-confirm-cancel-modal`);
    if (!(confirmCancelModal && cancelModal && confirmModal)) {
      return;
    }
    this.modal = confirmCancelModal;
    confirmModal.onclick = function () {
      if (cancelRedirectUrl) {
        window.location.href = cancelRedirectUrl;
      } else {
        console.warn("No cancelRedirectUrl is specified! Closing anyway.");
        confirmCancelModal.style.display = "none";
      }
    };
    cancelModal.onclick = function () {
      confirmCancelModal.style.display = "none";
    };
  }

  public show = () => {
    if (!this.modal) {
      return;
    }
    this.modal.style.display = "block";
  };
}
