package com.mycomplex;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularArchitectureTest {

    @Test
    void verifiesModularBoundariesAndCycles() {
        ApplicationModules.of(MycomplexBackendApplication.class).verify();
    }
}
