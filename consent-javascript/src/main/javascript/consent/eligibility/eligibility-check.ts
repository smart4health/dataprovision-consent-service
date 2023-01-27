import { Platform } from "../../config";
import { Message } from "../message";

class EligibilityCheckButton {
  button: HTMLAnchorElement;

  constructor() {
    const eligibilityCheckButton = document.getElementById("eligibility-check-next") as HTMLAnchorElement | null;
    if (!eligibilityCheckButton) {
      throw new Error("anchor element with id #eligibility-check-next must be present");
    }
    this.button = eligibilityCheckButton;
  }

  hide = () => this.button.classList.add("hidden");

  show = () => this.button.classList.remove("hidden");
}

class EligibilityCheckFailedButton {
  button: HTMLAnchorElement;

  constructor() {
    const eligibilityCheckFailedButton = document.getElementById(
      "eligibility-check-failed"
    ) as HTMLAnchorElement | null;
    if (!eligibilityCheckFailedButton) {
      throw new Error("anchor element with id #eligibility-check-failed must be present");
    }
    this.button = eligibilityCheckFailedButton;
  }

  hide = () => this.button.classList.add("hidden");

  show = () => this.button.classList.remove("hidden");
}

class EligibilityCriterion {
  private readonly container: HTMLDivElement;
  private inputGrouping: HTMLInputElement[];

  constructor(element: HTMLDivElement) {
    this.container = element;
    this.inputGrouping = Array.from(this.container.getElementsByClassName("eligibility-radio")) as HTMLInputElement[];
  }

  /**
   * An eligibility check may sometimes need to be "No" for the user to be eligible. Therefore, we
   * put a `data-eligible` attribute on the input that is required to be checked for eligibility to pass.
   */
  public isChecked = (): boolean =>
    this.inputGrouping.some((input) => input.getAttribute("data-eligible") && input.checked);

  public onChange = (callback: () => void) => {
    this.inputGrouping.forEach((input) => input.addEventListener("change", callback));
  };
}

export class EligibilityCheck {
  button: EligibilityCheckButton;
  failedButton: EligibilityCheckFailedButton;
  checks: EligibilityCriterion[];

  constructor(private readonly platform: Platform) {
    this.button = new EligibilityCheckButton();
    this.failedButton = new EligibilityCheckFailedButton();
    this.failedButton.button.onclick = function () {
      const message = new Message(platform);
      message.fail("eligibility");
    };
    this.checks = Array.from(document.querySelectorAll(".eligibility-check-container")).map(
      (elem) => new EligibilityCriterion(elem as HTMLDivElement)
    );
  }

  canContinue = (): boolean => this.checks.every((check) => check.isChecked());

  listenForChecks = (): void => {
    if (!this.canContinue()) {
      this.button.hide();
      this.failedButton.show();
    }

    this.checks.forEach((check) => {
      check.onChange(() => {
        if (this.canContinue()) {
          this.failedButton.hide();
          this.button.show();
        } else {
          this.button.hide();
          this.failedButton.show();
        }
      });
    });
  };
}
