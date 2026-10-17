import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PadroesCriacionais {

    private PadroesCriacionais() {
    }

    public static void main(String[] args) {
        demonstrarSingleton();
        demonstrarFactoryMethod();
        demonstrarAbstractFactory();
        demonstrarBuilder();
        demonstrarPrototype();
    }

    private static void demonstrarSingleton() {
        System.out.println("=== Singleton ===");
        ConfiguracaoAplicacao primeira = ConfiguracaoAplicacao.getInstance();
        ConfiguracaoAplicacao segunda = ConfiguracaoAplicacao.getInstance();
        System.out.println("Mesma instância: " + (primeira == segunda));
    }

    private static void demonstrarFactoryMethod() {
        System.out.println("\n=== Factory Method ===");
        NotificacaoFactory factory = new EmailNotificacaoFactory();
        factory.enviarNotificacao("Bem-vindo ao sistema de alunos.");
    }

    private static void demonstrarAbstractFactory() {
        System.out.println("\n=== Abstract Factory ===");
        ComponentesInterfaceFactory factory = new InterfaceWebFactory();
        factory.criarBotao().renderizar();
        factory.criarCampoTexto().renderizar();
    }

    private static void demonstrarBuilder() {
        System.out.println("\n=== Builder ===");
        Relatorio relatorio = new Relatorio.Builder("Análise de alunos")
                .conteudo("Quantidade de alunos: 25")
                .autor("Secretaria")
                .build();
        System.out.println(relatorio);
    }

    private static void demonstrarPrototype() {
        System.out.println("\n=== Prototype ===");
        ModeloRelatorio original = new ModeloRelatorio("Relatório escolar");
        original.adicionarSecao("Matrículas");
        ModeloRelatorio copia = original.clone();
        copia.adicionarSecao("Notas");
        System.out.println("Original: " + original.getSecoes());
        System.out.println("Cópia: " + copia.getSecoes());
    }

    private static final class ConfiguracaoAplicacao {
        private static final ConfiguracaoAplicacao INSTANCE = new ConfiguracaoAplicacao();

        private ConfiguracaoAplicacao() {
        }

        private static ConfiguracaoAplicacao getInstance() {
            return INSTANCE;
        }
    }

    private interface Notificacao {
        void enviar(String mensagem);
    }

    private static final class EmailNotificacao implements Notificacao {
        @Override
        public void enviar(String mensagem) {
            System.out.println("E-mail enviado: " + mensagem);
        }
    }

    private static final class SmsNotificacao implements Notificacao {
        @Override
        public void enviar(String mensagem) {
            System.out.println("SMS enviado: " + mensagem);
        }
    }

    private abstract static class NotificacaoFactory {
        protected abstract Notificacao criarNotificacao();

        private void enviarNotificacao(String mensagem) {
            criarNotificacao().enviar(mensagem);
        }
    }

    private static final class EmailNotificacaoFactory extends NotificacaoFactory {
        @Override
        protected Notificacao criarNotificacao() {
            return new EmailNotificacao();
        }
    }

    private static final class SmsNotificacaoFactory extends NotificacaoFactory {
        @Override
        protected Notificacao criarNotificacao() {
            return new SmsNotificacao();
        }
    }

    private interface Botao {
        void renderizar();
    }

    private interface CampoTexto {
        void renderizar();
    }

    private interface ComponentesInterfaceFactory {
        Botao criarBotao();

        CampoTexto criarCampoTexto();
    }

    private static final class InterfaceWebFactory implements ComponentesInterfaceFactory {
        @Override
        public Botao criarBotao() {
            return () -> System.out.println("Botão web renderizado.");
        }

        @Override
        public CampoTexto criarCampoTexto() {
            return () -> System.out.println("Campo de texto web renderizado.");
        }
    }

    private static final class InterfaceDesktopFactory implements ComponentesInterfaceFactory {
        @Override
        public Botao criarBotao() {
            return () -> System.out.println("Botão desktop renderizado.");
        }

        @Override
        public CampoTexto criarCampoTexto() {
            return () -> System.out.println("Campo de texto desktop renderizado.");
        }
    }

    private static final class Relatorio {
        private final String titulo;
        private final String conteudo;
        private final String autor;

        private Relatorio(Builder builder) {
            this.titulo = builder.titulo;
            this.conteudo = builder.conteudo;
            this.autor = builder.autor;
        }

        @Override
        public String toString() {
            return "Relatório: " + titulo + " | Conteúdo: " + conteudo + " | Autor: " + autor;
        }

        private static final class Builder {
            private final String titulo;
            private String conteudo = "";
            private String autor = "Não informado";

            private Builder(String titulo) {
                if (titulo == null || titulo.trim().isEmpty()) {
                    throw new IllegalArgumentException("O título do relatório é obrigatório.");
                }
                this.titulo = titulo;
            }

            private Builder conteudo(String conteudo) {
                if (conteudo == null) {
                    throw new IllegalArgumentException("O conteúdo não pode ser nulo.");
                }
                this.conteudo = conteudo;
                return this;
            }

            private Builder autor(String autor) {
                if (autor == null || autor.trim().isEmpty()) {
                    throw new IllegalArgumentException("O autor não pode ser vazio.");
                }
                this.autor = autor;
                return this;
            }

            private Relatorio build() {
                return new Relatorio(this);
            }
        }
    }

    private static final class ModeloRelatorio implements Cloneable {
        private final String titulo;
        private final List<String> secoes;

        private ModeloRelatorio(String titulo) {
            this.titulo = titulo;
            this.secoes = new ArrayList<>();
        }

        private void adicionarSecao(String secao) {
            secoes.add(secao);
        }

        private List<String> getSecoes() {
            return Collections.unmodifiableList(secoes);
        }

        @Override
        protected ModeloRelatorio clone() {
            ModeloRelatorio copia = new ModeloRelatorio(titulo);
            copia.secoes.addAll(secoes);
            return copia;
        }
    }
}
