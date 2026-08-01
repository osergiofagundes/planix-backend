package com.sergio.planix.config;

import com.sergio.planix.common.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    private static final String API_ERROR_REF = "#/components/schemas/ApiError";

    @Bean
    public OpenAPI planixOpenAPI() {
        Components components = new Components()
                .addSecuritySchemes(BEARER_AUTH, bearerScheme());
        ModelConverters.getInstance().readAll(ApiError.class).forEach(components::addSchemas);

        return new OpenAPI()
                .info(info())
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .tags(tags());
    }

    private Info info() {
        return new Info()
                .title("Planix API")
                .version("1.0.0")
                .description("""
                        API REST do Planix.
                        """)
                .contact(new Contact()
                        .name("Sergio Fagundes")
                        .email("contato.sergiofagundes@gmail.com"))
                .license(new License().name("MIT"));
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Access token devolvido por `/api/auth/login`. "
                           + "Vai no header `Authorization: Bearer <token>`.");
    }

    private List<Tag> tags() {
        return List.of(
                tag("Autenticação", "Cadastro, login, refresh e logout. O ponto de partida."),
                tag("Quadros", "O contêiner de tudo. Cada quadro tem um dono."),
                tag("Convites", "Links de convite para trazer colaboradores ao quadro."),
                tag("Membros do quadro", "Quem tem acesso ao quadro, e como sair ou remover alguém."),
                tag("Listas", "As colunas de um quadro (\"A Fazer\", \"Fazendo\"...), com ordenação."),
                tag("Cartões", "As tarefas. Inclui mover, concluir e o histórico de alterações."),
                tag("Responsáveis", "Quem cuida de cada cartão. Só aceita membros do quadro."),
                tag("Etiquetas", "Etiquetas reutilizáveis do quadro, aplicadas aos cartões."),
                tag("Checklist", "Os itens marcáveis dentro de um cartão."),
                tag("Comentários", "Conversa em torno de um cartão."),
                tag("Links", "Links externos anexados a um cartão."),
                tag("Anexos", "Upload e download de arquivos de um cartão."));
    }

    private Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }

    @Bean
    public OperationCustomizer respostasPadraoDeErro() {
        return (operation, handlerMethod) -> {
            var responses = operation.getResponses();
            if (responses == null) {
                return operation;
            }

            boolean publica = operation.getSecurity() != null && operation.getSecurity().isEmpty();
            if (!publica) {
                addIfAbsent(responses, "401", "Token ausente, expirado ou inválido");
                addIfAbsent(responses, "403", "Autenticado, mas sem permissão sobre este recurso");
            }

            responses.forEach((code, response) -> {
                if (ehErro(code) && !jaEhApiError(response)) {
                    response.setContent(apiErrorContent());
                }
            });
            return operation;
        };
    }

    private boolean ehErro(String code) {
        return code.startsWith("4") || code.startsWith("5");
    }

    private boolean jaEhApiError(ApiResponse response) {
        Content content = response.getContent();
        if (content == null) {
            return false;
        }
        MediaType json = content.get("application/json");
        return json != null && json.getSchema() != null
               && API_ERROR_REF.equals(json.getSchema().get$ref());
    }

    private void addIfAbsent(ApiResponses responses, String code, String description) {
        if (!responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse().description(description));
        }
    }

    private Content apiErrorContent() {
        return new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref(API_ERROR_REF)));
    }
}
