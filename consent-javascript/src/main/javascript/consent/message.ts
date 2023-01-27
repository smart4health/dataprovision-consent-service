import { Platform } from "../config";
import { Option } from "./option";
import { Progress } from "../carousel/progress";

type FailReason = "eligibility" | "quiz";

declare global {
  interface Window {
    webkit: {
      messageHandlers: {
        failedHandler: {
          postMessage: (reason: FailReason) => void;
        };
        shareHandler: {
          postMessage: (params: { options: Option[] }) => void;
        };
        progressHandler: {
          postMessage: (params: { progress: Progress }) => void;
        };
      };
    };
    Android: {
      fail: (reason: FailReason) => void;
      shareDocument: (params: string) => void;
      progress: (params: string) => void;
    };
  }
}

export class Message {
  private readonly isIos: Boolean;
  private readonly isAndroid: Boolean;

  constructor(private readonly platform: Platform) {
    this.isIos = platform === "ios";
    this.isAndroid = platform === "android";
  }

  public share = (options: Option[]): void => {
    if (this.isIos && window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.shareHandler) {
      window.webkit.messageHandlers.shareHandler.postMessage({ options });
    } else if (this.isAndroid && window.Android && window.Android.shareDocument) {
      window.Android.shareDocument(this.serialize({ options }));
    } else {
      console.log(options);
    }
  };

  public fail = (reason: FailReason): void => {
    if (this.isIos && window.webkit.messageHandlers.failedHandler) {
      window.webkit.messageHandlers.failedHandler.postMessage(reason);
    } else if (this.isAndroid && window.Android && window.Android.fail) {
      window.Android.fail(reason);
    } else {
      console.log(reason);
    }
  };

  public progress = (progress: Progress) => {
    const jsonProgress = JSON.stringify({ progress });
    if (this.isIos && window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.progressHandler) {
      window.webkit.messageHandlers.progressHandler.postMessage({ progress });
    } else if (this.isAndroid && window.Android && window.Android.progress) {
      window.Android.progress(jsonProgress);
    } else {
      console.log(jsonProgress);
    }
  };

  private serialize = (data: any): string => btoa(JSON.stringify(data));
}
