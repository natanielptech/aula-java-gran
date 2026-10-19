package br.com.exemplo.tarefas;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Serves GET /api/cep/{cep} and returns ViaCEP JSON for an eight-digit CEP.
 * Invalid CEPs return 400, missing routes or CEPs return 404, unsupported methods
 * return 405, and upstream or internal errors return 502 or 500, respectively.
 */
public class ViaCepServer {
    private static final String ENDPOINT = "/api/cep";
    private static final URI VIA_CEP_BASE_URI = URI.create("https://viacep.com.br/ws/");
    private static final String ERRO_CEP_INVALIDO = "{\"erro\":\"Informe um CEP com 8 dígitos.\"}";
    private static final String ERRO_METODO = "{\"erro\":\"Método não permitido. Use GET.\"}";
    private static final String ERRO_ENDPOINT = "{\"erro\":\"Endpoint não encontrado.\"}";
    private static final String ERRO_CEP_NAO_ENCONTRADO = "{\"erro\":\"CEP não encontrado.\"}";
    private static final String ERRO_CONSULTA = "{\"erro\":\"Não foi possível consultar o ViaCEP.\"}";
    private static final String ERRO_VIACEP = "{\"erro\":\"O serviço ViaCEP retornou um erro.\"}";
    private static final String ERRO_SERVIDOR = "{\"erro\":\"Erro interno do servidor.\"}";
    private static final Pattern CEP_PATTERN = Pattern.compile("\\d{8}");
    private static final Pattern CEP_NAO_ENCONTRADO_PATTERN =
            Pattern.compile("\"erro\"\\s*:\\s*true(?:\\s*[,}])");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final URI viaCepBaseUri;
    private final HttpClient client;

    /**
     * Creates a service with an injectable ViaCEP base URI and HTTP client for tests.
     */
    ViaCepServer(URI viaCepBaseUri, HttpClient client) {
        String base = viaCepBaseUri.toString();
        this.viaCepBaseUri = URI.create(base.endsWith("/") ? base : base + "/");
        this.client = client;
    }

    public static void main(String[] args) throws IOException {
        int porta = 8080;
        if (args.length > 0) {
            try {
                porta = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida: informe um número entre 1 e 65535.");
                return;
            }
        }
        if (porta < 1 || porta > 65535) {
            System.err.println("Porta inválida: informe um número entre 1 e 65535.");
            return;
        }

        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        ViaCepServer servico = new ViaCepServer(VIA_CEP_BASE_URI, CLIENT);
        servidor.createContext(ENDPOINT, servico::consultarCep);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        servidor.setExecutor(executor);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.stop(1);
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
        servidor.start();
        System.out.println("Serviço ViaCEP disponível em http://localhost:" + porta + ENDPOINT + "/{cep}");
    }

    /** Validates the request, calls ViaCEP, and writes the corresponding HTTP response. */
    void consultarCep(HttpExchange requisicao) throws IOException {
        String caminho = requisicao.getRequestURI().getPath();
        String prefixo = ENDPOINT + "/";

        if (!"GET".equals(requisicao.getRequestMethod())) {
            requisicao.getResponseHeaders().set("Allow", "GET");
            responder(requisicao, 405, ERRO_METODO);
            return;
        }

        if (!caminho.startsWith(prefixo)) {
            responder(requisicao, 404, ERRO_ENDPOINT);
            return;
        }

        String cep = caminho.substring(prefixo.length());
        if (!CEP_PATTERN.matcher(cep).matches()) {
            responder(requisicao, 400, ERRO_CEP_INVALIDO);
            return;
        }

        HttpResponse<String> resposta;
        try {
            URI uri = viaCepBaseUri.resolve(cep + "/json/");
            HttpRequest consulta = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            resposta = client.send(consulta, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            responder(requisicao, 502, ERRO_CONSULTA);
            return;
        } catch (RuntimeException e) {
            responder(requisicao, 500, ERRO_SERVIDOR);
            return;
        }

        if (resposta.statusCode() >= 200 && resposta.statusCode() < 300) {
            if (CEP_NAO_ENCONTRADO_PATTERN.matcher(resposta.body()).find()) {
                responder(requisicao, 404, ERRO_CEP_NAO_ENCONTRADO);
            } else {
                responder(requisicao, 200, resposta.body());
            }
        } else {
            responder(requisicao, 502, ERRO_VIACEP);
        }
    }

    private static void responder(HttpExchange requisicao, int status, String corpo) throws IOException {
        byte[] resposta = corpo.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        requisicao.sendResponseHeaders(status, resposta.length);
        try (var saida = requisicao.getResponseBody()) {
            saida.write(resposta);
        }
    }
}
