#!/bin/sh
# Formats all Java sources the same way: Allman braces, tab indents, one tab per continuation,
# then removes unused imports and sorts the rest. Needs astyle (brew install astyle / apt install astyle).
set -e
cd "$(dirname "$0")/.."
astyle --style=allman --indent=force-tab=4 --indent-switches --keep-one-line-blocks --indent-after-parens \
	--indent-continuation=1 --lineend=linux --suffix=none --recursive --quiet 'src/main/java/*.java' 'src/test/java/*.java'
python3 tools/tidy_imports.py src/main/java src/test/java
