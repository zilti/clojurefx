#!/bin/sh
makeinfo --css-ref=datamash-texinfo.css --html --no-split manual.texinfo
makeinfo --no-split manual.texinfo
makeinfo -o linicms-manual.docbook --no-split --docbook manual.texinfo
makeinfo -o linicms-manual.pdf --pdf manual.texinfo
dbtoepub linicms-manual.docbook -o linicms-manual.epub
