import { Carousel } from "../../carousel";
import { urlState } from "../../consent-state";
import { QuizQuestion } from "./quiz-question";
import { Platform } from "../../config";

export class Quiz {
  questions?: QuizQuestion[];
  quizState: QuizState = { numIncorrect: 0 };

  constructor(private readonly carousel: Carousel, private readonly platform: Platform) {}

  public init = () => {
    this.questions = Array.from(document.querySelectorAll(".consent-quiz-container")).map(
      (elem) => new QuizQuestion(elem as HTMLDivElement, this.carousel, this.platform, this.quizState)
    );
    if (urlState) {
      this.complete();
    }
  };

  public complete = () => {
    this.questions?.forEach((question) => question.markCorrect());
  };
}

export interface QuizState {
  numIncorrect: number;
}
