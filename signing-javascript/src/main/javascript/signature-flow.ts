import SignaturePad from "signature_pad";

export class SignatureFlow {
  init() {
    const DESKTOP_WIDTH = 992;

    const canvas = document.querySelector("canvas");
    // @ts-ignore
    const signaturePad = new SignaturePad(canvas);
    if (document.documentElement.clientWidth < DESKTOP_WIDTH) {
      resizeCanvas();
    }

    window.addEventListener("resize", handleResizeChange);
    window.addEventListener("orientationchange", handleOrientationChange);

    const signatureForm = document.getElementById("signature-form");
    // @ts-ignore
    const signatureToken = signatureForm.getAttribute("data-signature-token");
    const signatureErrorContainer = document.getElementById("signature-form-error");
    const clearSignatureButton = document.getElementById("clear-signature-button");
    const emailErrorContainer = document.getElementById("email-error");
    const emailValidation = new RegExp(
      /(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])/
    );
    const emailInput = document.getElementById("email");

    // @ts-ignore
    clearSignatureButton.onclick = function () {
      signaturePad.clear();
    };

    // @ts-ignore
    emailInput.oninput = function () {
      // @ts-ignore
      emailErrorContainer.classList.add("hidden");
    };
    // @ts-ignore
    signaturePad.onBegin = function () {
      // @ts-ignore
      signatureErrorContainer.classList.add("hidden");
    };
    const cancelButton = document.getElementById("cancel-button");
    if (cancelButton) {
      initCancelModal(cancelButton);
    }

    const firstNameInput = document.getElementById("first-name");
    const lastNameInput = document.getElementById("last-name");
    [firstNameInput, lastNameInput].forEach(function (input) {
      // @ts-ignore
      input.onkeyup = function () {
        const regex = /[0-9]/gi;
        // @ts-ignore
        this.value = this.value.replace(regex, "");
      };
    });

    // @ts-ignore
    signatureForm.addEventListener("submit", function (event) {
      event.preventDefault();

      // @ts-ignore
      const email = event.target["email"].value;
      if (!email || !emailValidation.test(email.toLowerCase())) {
        // @ts-ignore
        emailErrorContainer.classList.remove("hidden");
        return;
      }

      if (signaturePad.isEmpty()) {
        // @ts-ignore
        signatureErrorContainer.classList.remove("hidden");
        return;
      }

      const inputs = {
        // @ts-ignore
        firstName: event.target["first-name"].value,
        // @ts-ignore
        lastName: event.target["last-name"].value,
        // @ts-ignore
        email: event.target["email"].value,
      };

      // @ts-ignore
      fetch(signatureForm.action, {
        method: "PUT",
        redirect: "manual",
        mode: "cors",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          firstName: inputs.firstName,
          lastName: inputs.lastName,
          email: inputs.email,
          token: signatureToken,
          signature: signaturePad.toDataURL(),
        }),
      })
        .then(checkStatus)
        .then(function (response) {
          return response
            .json()
            .then(function (data: { successRedirectUrl: string }) {
              window.location.href = data.successRedirectUrl;
            })
            .catch(function (error: any) {
              console.log("Could not parse json or follow redirect", error);
            });
        })
        .catch(function (error) {
          console.log("The signature submission failed", error);
        });
    });

    function checkStatus(response: any) {
      if (response.status >= 200 && response.status < 400) {
        return response;
      } else {
        throw new Error(response);
      }
    }

    function resizeCanvas() {
      // @ts-ignore
      canvas.width = calculateCanvasWidth();
      signaturePad.clear();
    }

    function handleResizeChange() {
      if (document.documentElement.clientWidth < DESKTOP_WIDTH) {
        // ignore the changes for mobile and let handleOrientationChange will handle it on devices tilts.
        // Problem why this was fixed:
        // The user enters the email, signs and then clicks the checkbox. The keyboard disappears, fires the resize
        // and clears the already drawn signature.
        return;
      }
      resizeCanvas();
    }

    function handleOrientationChange() {
      resizeCanvas();
    }

    function calculateCanvasWidth() {
      return document.documentElement.clientWidth < DESKTOP_WIDTH
        ? document.documentElement.clientWidth - 32
        : document.documentElement.clientWidth / 3;
    }

    function initCancelModal(cancelButton: HTMLElement) {
      const cancelRedirectUrl = cancelButton.getAttribute("data-cancel-redirect-url");
      const confirmCancelModal = document.getElementById("confirm-cancel-modal");
      const cancelModal = document.getElementById("close-confirm-cancel-modal");
      const confirmModal = document.getElementById("confirm-confirm-cancel-modal");
      if (!(cancelRedirectUrl && confirmCancelModal && cancelModal && confirmModal)) {
        return;
      }
      cancelButton.onclick = function () {
        confirmCancelModal.style.display = "block";
      };
      confirmModal.onclick = function () {
        window.location.href = cancelRedirectUrl;
      };
      cancelModal.onclick = function () {
        confirmCancelModal.style.display = "none";
      };
    }
  }
}
