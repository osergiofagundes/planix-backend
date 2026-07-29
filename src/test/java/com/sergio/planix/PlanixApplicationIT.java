package com.sergio.planix;

import com.sergio.planix.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class PlanixApplicationIT extends IntegrationTest {

    @Test
    void contextLoads() {
        // se o contexto sobe, as migrations rodaram e todas as @Entity batem com o schema
    }
}
