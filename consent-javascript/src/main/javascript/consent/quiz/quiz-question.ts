import { Carousel } from "../../carousel";
import { QuizAnswer } from "./quiz-answer";
import { Message } from "../message";
import { Platform } from "../../config";
import { QuizState } from "./quiz";

export class QuizQuestion {
  incorrectAnswerContainer: HTMLDivElement;
  correctAnswerContainer: HTMLDivElement;
  answers: QuizAnswer[];
  answerButton: HTMLAnchorElement;
  nextButton: HTMLAnchorElement;
  private readonly exitPage: string | null;

  constructor(
    private element: HTMLElement,
    private readonly carousel: Carousel,
    private readonly platform: Platform,
    private quizState: QuizState
  ) {
    const incorrectAnswerContainer = this.element.querySelector(
      ".consent-quiz-incorrect-answer-container"
    ) as HTMLDivElement | null;
    if (!incorrectAnswerContainer) {
      throw new Error("Div with class .consent-quiz-incorrect-answer-container not present");
    }
    this.incorrectAnswerContainer = incorrectAnswerContainer;

    const correctAnswerContainer = this.element.querySelector(
      ".consent-quiz-correct-answer-container"
    ) as HTMLDivElement | null;
    if (!correctAnswerContainer) {
      throw new Error("Div with class .consent-quiz-correct-answer-container not present");
    }
    this.correctAnswerContainer = correctAnswerContainer;

    this.answers = Array.from(this.element.querySelectorAll(".consent-quiz-answer-container")).map(
      (elem) => new QuizAnswer(elem as HTMLDivElement)
    );

    const answerButton = this.element.querySelector(".quiz-question-answer-button") as HTMLAnchorElement | null;
    if (!answerButton) {
      throw new Error("Answer button not present");
    }
    this.answerButton = answerButton;
    this.exitPage = this.answerButton.getAttribute("data-exit-page");

    const nextButton = this.element.querySelector(".quiz-question-next-button") as HTMLAnchorElement | null;
    if (!nextButton) {
      throw new Error("Button with class .quiz-question-next-button must be present");
    }
    this.nextButton = nextButton;

    const onSubmit = (event: Event) => {
      event.preventDefault();
      if (this.isAnsweredCorrectly()) {
        this.showCorrectAnswerContainer();
        this.answerButton.removeEventListener("click", onSubmit);
      } else {
        this.quizState.numIncorrect++;
        if (this.quizState.numIncorrect > 2 && this.exitPage) {
          this.carousel.loadNextSlide(this.exitPage);
          const message = new Message(this.platform);
          message.fail("quiz");
        } else {
          this.correctAnswerContainer.classList.add("hidden");
          this.incorrectAnswerContainer.classList.remove("hidden");
        }
      }
    };
    this.answerButton.onclick = onSubmit;
  }

  public markCorrect = () => {
    this.answers.find((answer) => answer.isCorrect)?.markCorrect();
    this.showCorrectAnswerContainer();
  };

  private showCorrectAnswerContainer = () => {
    this.correctAnswerContainer.classList.remove("hidden");
    this.incorrectAnswerContainer.classList.add("hidden");
    this.answerButton.classList.add("hidden");

    this.answers.filter((answer) => answer.isCorrect).forEach((answer) => answer.disable());

    this.answers.filter((answer) => !answer.isCorrect).forEach((answer) => answer.hide());

    this.nextButton.classList.remove("hidden");
  };

  private isAnsweredCorrectly = (): boolean => this.answers.every((answer) => answer.markedCorrectly());
}
