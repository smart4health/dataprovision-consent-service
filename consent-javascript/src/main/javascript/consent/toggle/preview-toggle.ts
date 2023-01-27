export class PreviewToggle {
  accept: HTMLInputElement;
  reject: HTMLInputElement;

  constructor(private questionId: string) {
    const accept = document.getElementById(`review-option-accept-${questionId}`) as HTMLInputElement | null;
    const reject = document.getElementById(`review-option-reject-${questionId}`) as HTMLInputElement | null;
    if (!accept || !reject) {
      throw new Error(`
        Each Toggle must also have corresponding PreviewToggles to display in the Review Consent page. These are mapped
        together via the "data-question-id" attribute, which should be the same on both the Toggle and the PreviewToggle.
      `);
    }
    this.accept = accept;
    this.reject = reject;
  }

  public setChecked = (accepted: boolean) => {
    this.accept.checked = accepted;
    this.reject.checked = !accepted;
    return this;
  };
}
