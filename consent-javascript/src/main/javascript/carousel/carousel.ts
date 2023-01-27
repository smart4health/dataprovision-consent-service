import { BrowserNavigator } from "../browser-navigator";
import { urlState } from "../consent-state";
import { Slide } from "./slide";
import { Platform } from "../config";
import { Progress } from "./progress";
import { Message } from "../consent/message";
import { QuitConsent, Toggle } from "../consent";
import { Popover } from "bootstrap";
import { DescriptionModal } from "../consent/description-modal";

const SLIDE_TRANSITION_CLASS_NAME = "carousel-transition";
const SLIDE_TRANSITION_DATA_ATTRIBUTE_NAME = "data-load-slide";
const SLIDE_CLASS_NAME = "carousel-item";
const DESCRIPTION_MODAL_OPENER_CLASS_NAME = "consent-description-option";
const SLIDE_QUESTION_ID = "data-question-id";
const CANCEL_REDIRECT_URL = "data-cancel-redirect-url";
const OPTION_NEXT_BUTTON = "option-next-button";
const CAROUSEL_SELECTOR = "carousel";

export class Carousel {
  private readonly carousel: HTMLElement;
  private readonly slideMap: Record<string, Slide>;
  private currentSlide: Slide;

  constructor(private readonly platform: Platform, toggles: Toggle[]) {
    const carouselElement = document.getElementById(CAROUSEL_SELECTOR);
    if (!carouselElement) {
      throw new Error("HTML element with id #carousel not present");
    }

    this.carousel = carouselElement;

    const carouselItems = Array.prototype.slice.call(this.carousel.getElementsByClassName(SLIDE_CLASS_NAME));
    const slides = carouselItems.map((elem, index) => new Slide(elem, { show: index === 0 }));
    this.slideMap = slides.reduce((acc, currentSlide) => {
      return {
        ...acc,
        [currentSlide.id]: currentSlide,
      };
    }, {});

    // The page is never reloaded on anything other than the first page
    this.currentSlide = slides[0];
    this.progress(slides[0].id, slides.length);

    if (window.location.hash) {
      const nextSlide = this.slideMap[window.location.hash.slice(1, window.location.hash.length)];
      BrowserNavigator.replaceState(nextSlide.id);
      this.loadNextSlide(nextSlide.id);
    } else if (urlState) {
      for (const [slideId, slide] of Object.entries(this.slideMap)) {
        if (slide.getAttribute("data-review-consent")) {
          const nextSlide = slide;
          BrowserNavigator.replaceState(nextSlide.id);
          this.loadNextSlide(nextSlide.id);
        }
      }
    } else {
      BrowserNavigator.replaceState(this.currentSlide.id);
    }

    // Configure Slide Transitions
    Array.prototype.slice
      .call(this.carousel.getElementsByClassName(SLIDE_TRANSITION_CLASS_NAME))
      .forEach((button: HTMLAnchorElement) => {
        const nextSlideId = button.getAttribute(SLIDE_TRANSITION_DATA_ATTRIBUTE_NAME);
        if (!nextSlideId) {
          throw new Error(`Transition button must have attribute ${SLIDE_TRANSITION_DATA_ATTRIBUTE_NAME}`);
        }

        // Additional checks for the Option Page
        if (button.classList.contains(OPTION_NEXT_BUTTON)) {
          const questionId = button.getAttribute(SLIDE_QUESTION_ID);
          if (!questionId) {
            throw new Error(`Transition button must have questionId ${SLIDE_QUESTION_ID}`);
          }

          button.onclick = (event) => {
            event.preventDefault();
            const toggle = toggles.find((t) => t.questionId == questionId);
            if (!toggle) {
              throw new Error(`No toggle ${questionId} found on page`);
            } else if (toggle.isEmpty()) {
              // Toggle is neither rejected nor accepted => Show Popover Hint
              Carousel.showTogglePopover(questionId);
            } else if (toggle.canLoadNextSlide()) {
              // Toggle is required and accepted or not required => Proceed
              this.progress(nextSlideId, slides.length);
              this.loadNextSlide(nextSlideId);
            } else {
              // Toggle is required, but user rejects => Show QuitConsent confirmation modal
              const quitConsentModal = new QuitConsent(button.getAttribute(CANCEL_REDIRECT_URL), questionId);
              quitConsentModal.show();
            }
          };
        } else {
          // Default
          button.onclick = (event) => {
            event.preventDefault();
            this.progress(nextSlideId, slides.length);
            this.loadNextSlide(nextSlideId);
          };
        }
      });

    // Configure Description Item Modals
    Array.prototype.slice
      .call(this.carousel.getElementsByClassName(DESCRIPTION_MODAL_OPENER_CLASS_NAME))
      .forEach((button: HTMLDivElement) => new DescriptionModal(button));
  }

  public loadNextSlide = (slideId: string): void => {
    const id = slideId.startsWith("#") ? slideId.slice(1, slideId.length) : slideId;
    const nextSlide = this.slideMap[id];
    if (!nextSlide) {
      throw new Error(`Cannot load next slide with id ${id} because there is no slide with that id`);
    }
    this.currentSlide.hide();
    nextSlide.show();
    window.scrollTo(0, 0);
    this.currentSlide = nextSlide;
    BrowserNavigator.addState(this.currentSlide.id);
  };

  public progress = (nextSlideId: string, slidesTotal: number) => {
    const nextSlideNum = nextSlideId.split("-")?.pop();
    if (nextSlideNum) {
      const slide = +nextSlideNum + 1;
      if (slide >= 1 && slide <= slidesTotal) {
        const message = new Message(this.platform);
        const progress: Progress = { slide: slide, slidesTotal: slidesTotal };
        message.progress(progress);
      }
    }
  };

  /**
   * Will display the Bootstrap Popover and listen to the event that's triggered after it is shown to the user.
   * Then any click will remove the element again.
   */
  private static showTogglePopover(questionId: string) {
    const popoverElement = document.getElementById(`option-box-${questionId}`);
    if (popoverElement) {
      const popover = new Popover(popoverElement, { trigger: "manual" });
      popover.show();
      document.addEventListener(
        "shown.bs.popover",
        () => {
          document.addEventListener(
            "click",
            () => {
              document.querySelector(".popover")?.remove();
            },
            {
              once: true,
            }
          );
        },
        {
          once: true,
        }
      );
    }
  }
}
