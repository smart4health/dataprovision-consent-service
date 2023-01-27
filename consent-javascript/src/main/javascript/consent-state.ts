interface ConsentOption {
  optionId: number;
  consented: boolean;
}

interface ConsentState {
  options: ConsentOption[];
}

export const urlState = getUrlState();

function getUrlState(): ConsentState | null {
  const searchParams = new URLSearchParams(window.location.search);
  const state = searchParams.get("state");
  if (!state) {
    return null;
  }

  let deserializedState: string;
  try {
    deserializedState = atob(state);
  } catch (error) {
    console.error(`Error deserializing state from base64-encoded string: ${error}`);
    return null;
  }

  let parsedState: ConsentState;
  try {
    parsedState = JSON.parse(deserializedState);
  } catch (error) {
    console.error(`Error deserializing state from JSON: ${error}`);
    return null;
  }

  return parsedState;
}
