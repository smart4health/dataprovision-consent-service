import { PreviewToggle } from "./preview-toggle";

class NextButton {
  button: HTMLAnchorElement;

  constructor(questionId: string) {
    const button = document.querySelector(`.next-button[data-question-id='${questionId}']`) as HTMLAnchorElement | null;

    if (!button) {
      throw new Error(`
        Each "Toggle" must have a corresponding next button attached to it. Next buttons are mapped to a toggle
        through the "data-question-id" attribute. Each next button should contain this attribute and it should be
        the same as the attribute on the corresponding "Toggle."
      `);
    }

    this.button = button;
  }

  public disable = (): void => this.button.classList.add("disabled");
  public enable = (): void => this.button.classList.remove("disabled");
  public inactive = (): void => this.button.classList.add("inactive");
  public active = (): void => this.button.classList.remove("inactive");
}

export class Toggle {
  options: HTMLInputElement[];
  required: boolean;
  nextButton: NextButton;
  previewToggle: PreviewToggle | null = null;

  constructor(readonly questionId: string, required: boolean, addPreviewToggle: boolean) {
    this.required = required;
    if (addPreviewToggle) {
      this.previewToggle = new PreviewToggle(questionId);
    }
    this.nextButton = new NextButton(questionId);
    const accept = document.getElementById(`option-accept-${questionId}`);
    if (!accept) {
      throw new Error(`Required Accept option for questionId ${questionId}`);
    }
    const reject = document.getElementById(`option-reject-${questionId}`);
    if (!reject) {
      throw new Error(`Required Reject option for questionId ${questionId}`);
    }
    this.options = [accept as HTMLInputElement, reject as HTMLInputElement];
    this.changeButtonState();
    this.options.forEach((value) => {
      value.addEventListener("change", (_event: Event) => {
        this.changeButtonState();
      });
    });
  }

  private changeButtonState = () => {
    if (this.options.find((r) => r.checked)?.value) {
      this.nextButton.active();
      this.previewToggle?.setChecked(this.isChecked());
    } else {
      this.nextButton.inactive();
    }
  };

  public isChecked = () => this.options.find((r) => r.checked)?.value === "accept";
  public isEmpty = () => this.options.find((r) => r.checked)?.value === undefined;
  public canLoadNextSlide = () => !this.required || (this.required && this.isChecked());
}
