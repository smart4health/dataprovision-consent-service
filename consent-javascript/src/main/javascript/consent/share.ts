import { Platform } from "../config";
import { Toggle } from "./toggle";
import { Message } from "./message";
import { Option } from "./option";

const SHARE_BUTTON_ID = `share-consent-button`;

export class Share {
  private shareButton: HTMLButtonElement;

  constructor(private readonly platform: Platform, private toggles: Toggle[]) {
    const shareButton = document.getElementById(SHARE_BUTTON_ID) as HTMLButtonElement | null;
    if (!shareButton) {
      throw new Error(`Share button with id '${SHARE_BUTTON_ID}' does not exist`);
    }
    this.shareButton = shareButton;
  }

  public init = () => {
    this.shareButton.onclick = this.shareDocument;
  };

  private getOptions = (): Option[] =>
    this.toggles.map((toggle) => ({
      optionId: parseInt(toggle.questionId, 10),
      consented: toggle.isChecked(),
    }));

  public shareDocument = () => {
    const options = this.getOptions();
    const message = new Message(this.platform);
    message.share(options);
  };
}
