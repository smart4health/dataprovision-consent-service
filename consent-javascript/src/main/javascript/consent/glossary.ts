const GLOSSARY_MODAL_HTML_TAG_NAME = "popup";

export class Glossary {
  public init = () => {
    const glossaryItems = document.getElementsByTagName(GLOSSARY_MODAL_HTML_TAG_NAME);
    if (glossaryItems.length == 0) {
      return;
    }
    for (let i of Array.from(glossaryItems)) {
      init(i as HTMLElement);
    }

    function init(htmlElement: HTMLElement) {
      let item = htmlElement.getAttribute("data-id");
      if (!item) {
        throw Error("error");
      }
      let modal = document.getElementById(`glossary_${item}`);
      if (!modal) {
        console.warn(`No Glossary item registered for: ${item}`);
        return;
      }

      let closeButton1 = document.getElementById(`glossary_${item}_close1`);
      let closeButton2 = document.getElementById(`glossary_${item}_close2`);
      if (!(closeButton1 && closeButton2)) {
        console.warn("Unable to register custom glossary item, no closing items found!");
        return;
      }
      htmlElement.onclick = (e) => show(e, modal!!);
      closeButton1.onclick = (e) => hide(e, modal!!);
      closeButton2.onclick = (e) => hide(e, modal!!);

      const img = document.createElement("img");
      img.src = "/consent-assets/svgs/info-circle.svg";
      img.className = "info-circle";
      htmlElement.appendChild(img);
    }

    function hide(e: Event, modal: HTMLElement) {
      e.preventDefault();
      modal.style.visibility = "hidden";
      modal.style.opacity = "0";
    }

    function show(e: Event, modal: HTMLElement) {
      e.preventDefault();
      modal.style.visibility = "visible";
      modal.style.opacity = "1";
    }
  };
}
