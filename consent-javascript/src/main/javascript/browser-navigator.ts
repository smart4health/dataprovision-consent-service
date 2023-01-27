export class BrowserNavigator {
  public static addState = (hash: string): void => {
    history.pushState(null, hash, `#${hash}`);
  };

  public static replaceState = (hash: string): void => {
    history.replaceState(null, hash, `#${hash}`);
  };
}
