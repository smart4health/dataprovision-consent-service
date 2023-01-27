import { Carousel } from "./carousel";
import { EligibilityCheck, ExitConsent, Quiz, Toggle } from "./consent";
import { Config } from "./config";
import { Share } from "./consent/share";
import { Glossary } from "./consent/glossary";
import { GlossaryAccordion } from "./consent/glossary-accordion";

const OPTION_NEXT_BUTTON = ".option-next-button";
const SLIDE_QUESTION_ID = "data-question-id";

export class ConsentFlow {
  private exitConsent: ExitConsent;
  private quiz: Quiz;
  private eligibilityCheck?: EligibilityCheck;
  private share: Share;
  private glossary: Glossary;
  private glossaryAccordion: GlossaryAccordion;

  constructor() {
    const config = new Config();

    const dynamicPreviewTogglesActive: boolean = document.getElementById('dynamic-review-options') != null;

    const toggles = Array.from(document.querySelectorAll(OPTION_NEXT_BUTTON)).map((elem) => {
      const questionId: string | null = elem.getAttribute(SLIDE_QUESTION_ID);
      if (!questionId) {
        throw new Error(`Transition button must have questionId `);
      }
      const approvalRequired = elem.getAttribute("data-required") === "true";

      return new Toggle(questionId, approvalRequired, dynamicPreviewTogglesActive);
    });

    const carousel = new Carousel(config.platform, toggles);

    window.addEventListener("hashchange", function (_event: HashChangeEvent) {
      carousel.loadNextSlide(window.location.hash);
    });

    if (!!document.getElementById("eligibility-container")) {
      this.eligibilityCheck = new EligibilityCheck(config.platform);
    }

    this.quiz = new Quiz(carousel, config.platform);
    this.share = new Share(config.platform, toggles);
    this.glossary = new Glossary();
    this.exitConsent = new ExitConsent(toggles);
    this.glossaryAccordion = new GlossaryAccordion();
  }

  public init = (): void => {
    this.eligibilityCheck?.listenForChecks();
    this.exitConsent.init();
    this.quiz.init();
    this.share.init();
    this.glossary.init();
    this.glossaryAccordion.init();
  };
}
