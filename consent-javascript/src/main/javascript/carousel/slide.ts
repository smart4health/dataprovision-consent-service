interface SlideOptions {
  show: boolean;
}

const SLIDE_ID_ATTRIBUTE_NAME = "data-slide-id";
const HIDDEN_CLASS = "carousel-hidden";

export class Slide {
  public id: string;
  private slide: HTMLElement;

  constructor(private elem: HTMLElement, options: SlideOptions) {
    const slideId = elem.getAttribute(SLIDE_ID_ATTRIBUTE_NAME);
    if (!slideId) {
      throw new Error(`Carousel element does not contain attribute with ${SLIDE_ID_ATTRIBUTE_NAME}`);
    }
    this.id = slideId;
    this.slide = elem;
    if (!options.show) {
      this.slide.classList.add(HIDDEN_CLASS);
    }
  }

  public hide = () => {
    this.elem.classList.add(HIDDEN_CLASS);
  };

  public show = () => {
    this.elem.classList.remove(HIDDEN_CLASS);
  };

  public getAttribute = (name: string): string | null => this.elem.getAttribute(name);
}
