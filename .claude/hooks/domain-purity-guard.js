#!/usr/bin/env node
/**
 * Domain Purity Guard (PreToolUse)
 *
 * Enforces the #1 architecture rule in CLAUDE.md:
 *   "domain/ 패키지: 순수 Java, JPA/Spring 어노테이션 금지"
 *
 * Blocks any Write/Edit that would introduce framework imports or
 * persistence/DI annotations into files under `.../domain/`.
 * This keeps the hexagonal core free of adapter concerns so the
 * dependency direction (adapter -> port -> domain) is never violated.
 *
 * Hook contract: reads the PreToolUse JSON payload on stdin.
 * Exit 0 = allow, Exit 2 = block (stderr is surfaced to the agent).
 */

const FORBIDDEN_PATTERNS = [
  { rx: /\bjakarta\.persistence\b/, label: 'jakarta.persistence import (JPA)' },
  { rx: /\borg\.springframework\b/, label: 'org.springframework import (Spring)' },
  { rx: /@Entity\b/, label: '@Entity annotation' },
  { rx: /@Table\b/, label: '@Table annotation' },
  { rx: /@Service\b/, label: '@Service annotation' },
  { rx: /@Component\b/, label: '@Component annotation' },
  { rx: /@Repository\b/, label: '@Repository annotation' },
  { rx: /@Autowired\b/, label: '@Autowired annotation' },
  { rx: /@Column\b/, label: '@Column annotation' },
];

// Only guard the pure domain layer; model/ is the strictest core.
const DOMAIN_PATH_RX = /\/domain\/[^\s]*\.java$/;

function readStdin() {
  return new Promise((resolve) => {
    let data = '';
    process.stdin.on('data', (c) => (data += c));
    process.stdin.on('end', () => resolve(data));
    // If nothing arrives, don't hang the toolchain.
    setTimeout(() => resolve(data), 2000).unref?.();
  });
}

(async () => {
  let payload;
  try {
    payload = JSON.parse((await readStdin()) || '{}');
  } catch {
    process.exit(0); // Never block on a malformed payload.
  }

  const tool = payload.tool_name || '';
  if (!/^(Write|Edit|MultiEdit)$/.test(tool)) process.exit(0);

  const input = payload.tool_input || {};
  const filePath = input.file_path || input.path || '';
  if (!DOMAIN_PATH_RX.test(filePath)) process.exit(0);

  // Gather the text that would land in the file.
  const candidates = [
    input.content,
    input.new_string,
    ...(Array.isArray(input.edits) ? input.edits.map((e) => e?.new_string) : []),
  ].filter((s) => typeof s === 'string');
  const text = candidates.join('\n');
  if (!text) process.exit(0);

  const hits = FORBIDDEN_PATTERNS.filter((p) => p.rx.test(text)).map((p) => p.label);
  if (hits.length === 0) process.exit(0);

  process.stderr.write(
    [
      '🚫 Domain Purity Guard: blocked write to the hexagonal domain layer.',
      `   File: ${filePath}`,
      `   Forbidden: ${hits.join(', ')}`,
      '',
      '   domain/ must stay pure Java (no JPA/Spring). Move persistence',
      '   concerns to adapter/out/persistence and keep the model framework-free.',
      '   See CLAUDE.md → "패키지 구조 규칙" / "의존성 방향".',
    ].join('\n') + '\n'
  );
  process.exit(2);
})();
