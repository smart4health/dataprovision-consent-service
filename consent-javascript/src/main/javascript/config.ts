export type Platform = "ios" | "android" | "web";
const PLATFORMS = ["android", "ios", "web"];

export class Config {
  public platform: Platform;

  constructor() {
    let platform = new URLSearchParams(window.location.search).get("platform") || "web";
    if (!PLATFORMS.includes(platform)) {
      console.warn(`Invalid platform ${platform}, defaulting to 'web'`);
      platform = "web";
    }
    this.platform = platform as Platform;
  }
}
