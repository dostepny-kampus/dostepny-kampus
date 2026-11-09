package io.github.dostepnykampus.sonda;

import org.junit.jupiter.api.Test;

class SondaTest {
    @Test
    void precedence() {
        boolean a = true;
        boolean b = false;
        boolean c = true;
        if (a || b && c) {
            return;
        }
    }
}
