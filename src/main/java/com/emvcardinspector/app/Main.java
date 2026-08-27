package com.emvcardinspector.app;

import com.emvcardinspector.reader.PcscCardReaderService;

import java.time.Duration;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        ReaderDiagnosticCommand command = new ReaderDiagnosticCommand(
                new PcscCardReaderService(),
                new Scanner(System.in),
                System.out,
                Duration.ofSeconds(15));
        command.run();
    }
}
