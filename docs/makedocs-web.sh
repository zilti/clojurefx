#!/bin/sh
cd docs
makeinfo --css-ref=datamash-texinfo.css --html --no-split manual.texinfo
echo '0a
<div class="fossil-doc" data-title="ClojureFX Manual">
.
$a
</div>
.
w' | ed clojurefx.html
