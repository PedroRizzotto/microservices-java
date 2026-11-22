package br.edu.atitus.agendaapi.components;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Serviços internos respondem com o JSON de erro padrão; aqui sai só o campo "message"
public final class ErrorBodyParser {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ErrorBodyParser() {
    }

    // Devolve o "message" do JSON; se o corpo não for um JSON assim, devolve o texto original
    public static String extractMessage(String body) {
        if (body == null || body.isBlank())
            return body;
        try {
            JsonNode message = MAPPER.readTree(body).get("message");
            if (message != null && message.isString())
                return message.asString();
        } catch (Exception e) {
            // não é JSON: usa o texto cru
        }
        return body;
    }
}
