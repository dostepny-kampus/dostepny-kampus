#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
# Biblioteka (do `source`): jedyne miejsce definiujące, które słowa w opisie PR zamykają issue.

# numery_zamykanych_issue: z tekstu ze standardowego wejścia wypisuje numery N z „<słowo> #N”, posortowane
# rosnąco i bez powtórzeń. Słowa to dziewięć form z dokumentacji GitHuba (close, closes, closed, fix, fixes,
# fixed, resolve, resolves, resolved), wielkość liter bez znaczenia; słowo nie może być końcem innego słowa.
numery_zamykanych_issue() {
  grep -oiP '(?<!\w)(?:close[sd]?|fix(?:es|ed)?|resolve[sd]?)\s+#\K\d+' | sort -un || true
}
