package br.com.frd.sicrediteste;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.csv.CSVRecord;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

@Data
@Builder
public class DadosConta {

    private String agencia;
    private String conta;
    private Double saldo;
    private String status;
    private String atualizado;

    public static DadosConta of(CSVRecord record) throws ParseException {
        return DadosConta.builder()
                .agencia(record.get("agencia"))
                .conta(record.get("conta"))
                .saldo(getSaldo(record.get("saldo")))
                .status(record.get("status"))
                .build();
    }

    private static Double getSaldo(String saldo) throws ParseException {
        NumberFormat format = NumberFormat.getInstance(Locale.FRENCH);
        Number number = format.parse(saldo);
        return number.doubleValue();
    }
}
