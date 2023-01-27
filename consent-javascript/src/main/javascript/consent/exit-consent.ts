import { Toggle } from "./toggle";

export class ExitConsent {
  private exitConsentButton: HTMLAnchorElement;

  constructor(private toggles: Toggle[]) {
    const exitConsentButton = document.getElementById("exit-consent") as HTMLAnchorElement | null;
    if (!exitConsentButton) {
      throw new Error("There must be a button to exit the consent flow with the id #exit-consent");
    }

    this.exitConsentButton = exitConsentButton;
  }

  public init = (): void => {
    this.exitConsentButton.addEventListener("click", (_event) => {
      const baseUrl = this.exitConsentButton.getAttribute("data-redirect-url");
      window.location.href = `${baseUrl}?options=${this.encodeOptions()}`;
    });
  };

  private encodeOptions = (): string => {
    const options = this.toggles.map((toggle) => ({
      optionId: parseInt(toggle.questionId, 10),
      consented: toggle.isChecked(),
    }));
    const encodedOptions = JSON.stringify(options);
    return btoa(encodedOptions);
  };
}
