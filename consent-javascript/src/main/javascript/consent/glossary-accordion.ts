export class GlossaryAccordion {
  public init = () => {
    const acc = document.getElementsByClassName("accordion");
    let i;

    for (i = 0; i < acc.length; i++) {
      acc[i].addEventListener("click", function (this: HTMLElement) {
        this.classList.toggle("active");
        const panel = this.nextElementSibling as HTMLElement | null;
        if (!panel) {
          throw new Error("Element with class accordion doesn't have a nextElementSibling to collapse");
        }
        if (panel.style.display === "block") {
          panel.style.display = "none";
        } else {
          panel.style.display = "block";
        }
      });
    }
  };
}
