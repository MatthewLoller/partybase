"""Remove the space alignment astyle adds to continued array initialisers, so lines use tabs only.

A line indented with tabs then spaces becomes one tab deeper than its tabs; a lone closing "};" or "});" goes back
on the line before. Block comment lines (" * ...") are left alone.
"""
import re
import sys
from pathlib import Path

ALIGNED = re.compile(r'^(\t+) +(\S.*)$')
CLOSERS = {'};', '});', '})', '}'}


def fix(text):
    out = []
    for line in text.split('\n'):
        m = ALIGNED.match(line)
        if m and not m.group(2).startswith('*'):
            if m.group(2).strip() in CLOSERS and out:
                out[-1] = out[-1].rstrip() + m.group(2).strip()
                continue
            line = m.group(1) + '\t' + m.group(2)
        out.append(line)
    return '\n'.join(out)


def main(roots):
    changed = 0
    for root in roots:
        for p in Path(root).rglob('*.java'):
            s = p.read_text(encoding='utf-8')
            new = fix(s)
            if new != s:
                p.write_text(new, encoding='utf-8')
                changed += 1
    print('re-indented', changed, 'files')


if __name__ == '__main__':
    main(sys.argv[1:])
