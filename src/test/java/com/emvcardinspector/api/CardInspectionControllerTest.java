package com.emvcardinspector.api;

import com.emvcardinspector.reader.CardReaderService;
import com.emvcardinspector.reader.CardSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import javax.smartcardio.CardException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardInspectionControllerTest {
    @Test
    void returnsCapturedDiagnosticOutputWhenNoReaderExistsForBothInterfaces() {
        CardInspectionService service = new CardInspectionService(
                new NoReaderService(), Duration.ofMillis(10));
        CardInspectionController controller = new CardInspectionController(service);

        assertNoReader(controller.inspectContactCard());
        assertNoReader(controller.inspectContactlessCard());
    }

    private static void assertNoReader(ResponseEntity<CardInspectionResponse> entity) {
        assertEquals(200, entity.getStatusCode().value());
        CardInspectionResponse response = entity.getBody();
        assertNotNull(response);
        assertEquals("no_reader", response.status());
        assertTrue(response.output().contains("No PC/SC reader found."));
    }

    private static final class NoReaderService implements CardReaderService {
        @Override
        public List<String> listReaderNames() {
            return List.of();
        }

        @Override
        public boolean waitForCard(String readerName, Duration timeout) {
            throw new AssertionError("No card wait expected");
        }

        @Override
        public CardSession connect(String readerName) throws CardException {
            throw new AssertionError("No connection expected");
        }
    }
}
