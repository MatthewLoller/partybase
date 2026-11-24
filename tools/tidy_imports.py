"""Tidy Java imports: drop unused single-type imports, then sort (static block first, then the rest).

Conservative: wildcard imports are kept; a name counts as used if it appears as a word anywhere outside the
import lines (code, javadoc {@link}, annotations). Line endings are preserved per file.
"""
import re
import sys
from pathlib import Path

IMPORT = re.compile(r'^import (static )?([\w.]+(?:\.\*)?);[ \t]*$')


def tidy(text):
    lines = text.split('\n')
    idx = [i for i, l in enumerate(lines) if IMPORT.match(l.rstrip('\r'))]
    if not idx:
        return text
    first, last = idx[0], idx[-1]
    # only rewrite a block made of imports and blank lines
    block = lines[first:last + 1]
    if any(l.strip() and not IMPORT.match(l.rstrip('\r')) for l in block):
        return text
    cr = '\r' if lines[first].endswith('\r') else ''
    body = '\n'.join(lines[:first] + lines[last + 1:])
    # ignore comments' "import" mentions only by excluding the import block itself
    statics, normal = set(), set()
    for l in block:
        m = IMPORT.match(l.rstrip('\r'))
        if not m:
            continue
        name = m.group(2)
        simple = name.rsplit('.', 1)[-1]
        used = simple == '*' or re.search(r'(?<![\w.])' + re.escape(simple) + r'\b', body) is not None
        if not used:
            continue
        (statics if m.group(1) else normal).add(name)
    out = ['import static %s;%s' % (n, cr) for n in sorted(statics)]
    out += ['import %s;%s' % (n, cr) for n in sorted(normal)]
    return '\n'.join(lines[:first] + out + lines[last + 1:])


def main(paths):
    changed = 0
    for root in paths:
        for p in Path(root).rglob('*.java'):
            raw = p.read_bytes().decode('utf-8')
            new = tidy(raw)
            if new != raw:
                p.write_bytes(new.encode('utf-8'))
                changed += 1
    print('tidied', changed, 'files')


if __name__ == '__main__':
    main(sys.argv[1:])
