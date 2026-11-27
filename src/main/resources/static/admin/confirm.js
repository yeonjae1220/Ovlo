'use strict';

const dialog = document.getElementById('action-confirm');
let pendingForm;

if (typeof dialog.showModal === 'function') {
  for (const form of document.querySelectorAll('form[data-confirm]')) {
    form.querySelector('button[type="submit"]').disabled = false;
    form.addEventListener('submit', event => {
      event.preventDefault();
      if (dialog.open) return;
      pendingForm = form;
      document.getElementById('confirm-message').textContent = form.dataset.confirm;
      dialog.returnValue = 'cancel';
      dialog.showModal();
    });
  }
  dialog.addEventListener('close', () => {
    const form = pendingForm;
    pendingForm = null;
    if (form && dialog.returnValue === 'confirm' && form.reportValidity()) {
      HTMLFormElement.prototype.submit.call(form);
    }
  });
}
