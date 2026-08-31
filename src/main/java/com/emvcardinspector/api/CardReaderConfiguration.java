package com.emvcardinspector.api;

import com.emvcardinspector.reader.CardReaderService;
import com.emvcardinspector.reader.PcscCardReaderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CardReaderConfiguration {
    @Bean
    CardReaderService cardReaderService() {
        return new PcscCardReaderService();
    }
}
