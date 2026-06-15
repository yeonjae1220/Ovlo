// 대학 자동완성 — admin 세션 체인 하위(/admin/**)에서 서빙되어 CSP script-src 'self' 통과.
// 입력 → 디바운스 → GET /admin/verifications/universities?q= → 결과 선택 시 hidden universityId 설정.
(function () {
  'use strict';

  var input = document.getElementById('uniSearch');
  var hidden = document.getElementById('universityId');
  var results = document.getElementById('uniResults');
  var selected = document.getElementById('uniSelected');
  var form = document.getElementById('issueForm');
  if (!input || !hidden || !results) return;

  var DEBOUNCE_MS = 220;
  var MIN_CHARS = 2;
  var timer = null;
  var lastQuery = '';

  function clearSelection() {
    hidden.value = '';
    if (selected) selected.textContent = '';
  }

  function hideResults() {
    results.innerHTML = '';
    results.style.display = 'none';
    input.setAttribute('aria-expanded', 'false');
  }

  function choose(u) {
    hidden.value = u.id;
    input.value = u.name;
    if (selected) {
      selected.textContent = '✓ 선택됨: ' + u.name + ' (#' + u.id + ')';
    }
    hideResults();
  }

  function render(list) {
    results.innerHTML = '';
    if (!list.length) {
      var empty = document.createElement('li');
      empty.className = 'ac-empty';
      empty.textContent = '검색 결과가 없습니다';
      results.appendChild(empty);
    } else {
      list.forEach(function (u) {
        var li = document.createElement('li');
        li.className = 'ac-item';
        li.setAttribute('role', 'option');
        li.tabIndex = 0;
        var cc = u.countryCode ? ' · ' + u.countryCode : '';
        var city = u.city ? ' · ' + u.city : '';
        li.textContent = u.name + cc + city;
        li.addEventListener('click', function () { choose(u); });
        li.addEventListener('keydown', function (e) {
          if (e.key === 'Enter') { e.preventDefault(); choose(u); }
        });
        results.appendChild(li);
      });
    }
    results.style.display = 'block';
    input.setAttribute('aria-expanded', 'true');
  }

  function search(q) {
    fetch('/admin/verifications/universities?q=' + encodeURIComponent(q), {
      headers: { 'Accept': 'application/json' },
      credentials: 'same-origin'
    })
      .then(function (r) { return r.ok ? r.json() : []; })
      .then(function (list) {
        if (q === lastQuery) render(list);
      })
      .catch(function () { hideResults(); });
  }

  input.addEventListener('input', function () {
    clearSelection();
    var q = input.value.trim();
    lastQuery = q;
    if (timer) clearTimeout(timer);
    if (q.length < MIN_CHARS) { hideResults(); return; }
    timer = setTimeout(function () { search(q); }, DEBOUNCE_MS);
  });

  document.addEventListener('click', function (e) {
    if (e.target !== input && !results.contains(e.target)) hideResults();
  });

  if (form) {
    form.addEventListener('submit', function (e) {
      if (!hidden.value) {
        e.preventDefault();
        input.focus();
        input.setAttribute('aria-invalid', 'true');
        if (selected) selected.textContent = '⚠ 목록에서 대학을 선택하세요.';
      }
    });
  }
})();
