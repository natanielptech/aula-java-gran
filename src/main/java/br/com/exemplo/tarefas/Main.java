package br.com.exemplo.tarefas;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner ENTRADA = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            executar(new TarefaDAO());
        } catch (SQLException e) {
            System.err.println("Erro ao acessar o banco de dados: " + e.getMessage());
        }
    }

    private static void executar(TarefaDAO tarefas) throws SQLException {
        boolean executando = true;

        while (executando) {
            System.out.println("""

                    === CRUD de tarefas ===
                    1. Criar tarefa
                    2. Listar tarefas
                    3. Atualizar tarefa
                    4. Excluir tarefa
                    0. Sair
                    """);
            System.out.print("Escolha uma opção: ");
            String opcao = ENTRADA.nextLine().trim();

            try {
                switch (Integer.parseInt(opcao)) {
                    case 1 -> criar(tarefas);
                    case 2 -> listar(tarefas);
                    case 3 -> atualizar(tarefas);
                    case 4 -> excluir(tarefas);
                    case 0 -> executando = false;
                    default -> System.out.println("Opção inválida.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Digite o número de uma opção válida.");
            }
        }
    }

    private static void criar(TarefaDAO tarefas) throws SQLException {
        String descricao = lerDescricao();
        Tarefa tarefa = tarefas.criar(descricao);
        System.out.println("Tarefa criada com ID " + tarefa.id() + ".");
    }

    private static void listar(TarefaDAO tarefas) throws SQLException {
        List<Tarefa> lista = tarefas.listar();
        if (lista.isEmpty()) {
            System.out.println("Nenhuma tarefa cadastrada.");
            return;
        }

        for (Tarefa tarefa : lista) {
            String status = tarefa.concluida() ? "Concluída" : "Pendente";
            System.out.printf("%d - %s [%s]%n", tarefa.id(), tarefa.descricao(), status);
        }
    }

    private static void atualizar(TarefaDAO tarefas) throws SQLException {
        Integer id = lerId();
        if (id == null) {
            return;
        }

        String descricao = lerDescricao();
        boolean concluida = lerStatus();
        if (tarefas.atualizar(id, descricao, concluida)) {
            System.out.println("Tarefa atualizada.");
        } else {
            System.out.println("Tarefa não encontrada.");
        }
    }

    private static void excluir(TarefaDAO tarefas) throws SQLException {
        Integer id = lerId();
        if (id == null) {
            return;
        }

        if (tarefas.excluir(id)) {
            System.out.println("Tarefa excluída.");
        } else {
            System.out.println("Tarefa não encontrada.");
        }
    }

    private static String lerDescricao() {
        while (true) {
            System.out.print("Descrição (até 255 caracteres): ");
            String descricao = ENTRADA.nextLine().trim();
            if (!descricao.isEmpty() && descricao.length() <= 255) {
                return descricao;
            }
            System.out.println("A descrição deve ter entre 1 e 255 caracteres.");
        }
    }

    private static Integer lerId() {
        System.out.print("ID da tarefa: ");
        try {
            int id = Integer.parseInt(ENTRADA.nextLine().trim());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException e) {
            System.out.println("Digite um ID numérico válido.");
            return null;
        }
        System.out.println("O ID deve ser maior que zero.");
        return null;
    }

    private static boolean lerStatus() {
        while (true) {
            System.out.print("Concluída? (s/n): ");
            String resposta = ENTRADA.nextLine().trim();
            if (resposta.equalsIgnoreCase("s")) {
                return true;
            }
            if (resposta.equalsIgnoreCase("n")) {
                return false;
            }
            System.out.println("Responda com s ou n.");
        }
    }
}
