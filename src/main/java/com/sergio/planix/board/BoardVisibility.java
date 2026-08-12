package com.sergio.planix.board;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Quem, dentro da equipe, enxerga o quadro.

        - `TEAM` — qualquer membro da equipe entra. É o padrão de quadro novo.
        - `RESTRICTED` — só quem for adicionado como membro do quadro.""")
public enum BoardVisibility {

    TEAM,
    RESTRICTED
}
