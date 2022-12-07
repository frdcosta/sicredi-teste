package br.com.frd.sicrediteste;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SincronizacaoService {

    private static final char CSV_LIMITER = ';';
    private static final String ATUALIZADO = "Atualizado";
    private static final String NAO_ATUALIZADO = "Nao Atualizado";

    private static final LocalTime TIME_LIMIT = LocalTime.of(10, 0, 0);

    private List<String> headers = List.of("agencia", "conta", "saldo", "status", "resultado");

    public void sincronizar(String file){
        if(beforeTenAM()){
            var data = readRecords(getFilePath(file));
            sendDataToReceita(data);
            printData(data);
        } else {
            throw new RuntimeException("Fora do horário limite de envio!");
        }
    }

    public List<DadosConta> readRecords(String filePath) {

        var csvFormatReader = CSVFormat.Builder.create()
                .setHeader()
                .setDelimiter(CSV_LIMITER)
                .build();

        try (var reader = Files.newBufferedReader(Paths.get(filePath));
             var parser = csvFormatReader.parse(reader)) {

            if(parser.getHeaderNames().containsAll(headers.subList(0,4)) && (parser.getHeaderNames().size() != 4)){
                throw new RuntimeException("Cabecalho invalido!");
            }

            return parser.getRecords().stream()
                    .map(record -> {
                        try {
                            return DadosConta.of(record);
                        } catch (ParseException e) {
                            log.error("Saldo da conta " + record.get("conta") + " invalido!");
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Arquivo não encontrado!");
        }
    }

    public void sendDataToReceita(List<DadosConta> data) {
        var receitaService = new ReceitaService();
        data.forEach(conta ->{
            boolean atualizado = false;
            try {
                atualizado = receitaService.atualizarConta(conta.getAgencia(), conta.getConta().replace("-", ""), conta.getSaldo(), conta.getStatus());
            } catch (InterruptedException e) {
                log.error("Erro no servico da Receita!");
            }
            conta.setAtualizado(atualizado ? ATUALIZADO : NAO_ATUALIZADO);
        });
    }

    public void printData(List<DadosConta> data) {

        var csvFormatWriter = CSVFormat.Builder.create()
                .setHeader(headers.toArray(new String[0]))
                .setDelimiter(CSV_LIMITER)
                .build();

        try (var writer = Files.newBufferedWriter(Paths.get("./resultado.csv"));
             var printer = new CSVPrinter(writer, csvFormatWriter)) {

            data.forEach(conta -> {
                try {
                    printer.printRecord(conta.getAgencia(), conta.getConta(), conta.getSaldo(), conta.getStatus(), conta.getAtualizado());
                } catch (IOException e) {
                    log.error("Nao foi possivel imprimir as informacoes da conta:" + conta.getConta());
                }
            });
            printer.flush();
        } catch (IOException e) {
            log.error("Erro ao criar arquivo de resultados!");
        }
    }

    private boolean beforeTenAM() {
        return LocalTime.now().isBefore(TIME_LIMIT);
    }

    private String getFilePath(String file) {
        final var fileUserDir = System.getProperty("user.dir");
        var filePath = fileUserDir + "/" + file;
        return filePath;
    }
}
