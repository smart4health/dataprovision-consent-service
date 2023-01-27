const DATA_DESCRIPTION_MODAL_ID = "data-description-modal-id";

export class DescriptionModal {
  constructor(private htmlElement: HTMLDivElement) {
    let modalId = htmlElement.getAttribute(DATA_DESCRIPTION_MODAL_ID);
    if (!modalId) {
      throw Error(`DescriptionModal can't be initialized: Button is missing attribute ${DATA_DESCRIPTION_MODAL_ID}`);
    }
    let modal = document.getElementById(modalId);
    if (!modal) {
      throw Error(`DescriptionModal can't be initialized: Modal with ID ${modal} is not available!`);
    }

    let closeButton1 = document.getElementById(`${modalId}-close1`);
    let closeButton2 = document.getElementById(`${modalId}-close2`);
    if (!(closeButton1 && closeButton2)) {
      throw Error(`Unable to register DescriptionModal ${modalId}, no closing items found!`);
    }
    htmlElement.onclick = (e) => this.show(e, modal!!);
    closeButton1.onclick = (e) => this.hide(e, modal!!);
    closeButton2.onclick = (e) => this.hide(e, modal!!);
  }

  hide(e: Event, modal: HTMLElement) {
    e.preventDefault();
    modal.style.visibility = "hidden";
    modal.style.opacity = "0";
  }

  show(e: Event, modal: HTMLElement) {
    e.preventDefault();
    modal.style.visibility = "visible";
    modal.style.opacity = "1";
  }
}
