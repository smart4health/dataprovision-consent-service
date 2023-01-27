interface ConsentOption {
  optionId: number;
  consented: boolean;
}

interface AppState {
  options?: ConsentOption[];
}

export const appState: AppState = {
};