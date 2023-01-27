export class QuizAnswer {
  input: HTMLInputElement;
  isCorrect: boolean;

  constructor(private container: HTMLDivElement) {
    const input = container.querySelector(".consent-quiz-answer") as HTMLInputElement | null;
    if (!input) {
      throw new Error("Each quiz answer must contain an input with the class .consent-quiz-answer");
    }
    this.input = input;
    this.isCorrect = this.input.getAttribute("data-answer-correct") === "true";
  }

  public markCorrect = () => {
    this.input.checked = true;
  };

  markedCorrectly = () => (this.isCorrect ? this.input.checked : !this.input.checked);

  disable = () => {
    this.input.disabled = true;
  };

  hide = () => {
    this.container.classList.add("hidden");
  };
}
