package com.emvcardinspector.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inspections")
public final class CardInspectionController {
    private final CardInspectionService inspectionService;

    public CardInspectionController(CardInspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @PostMapping("/contact")
    public ResponseEntity<CardInspectionResponse> inspectContactCard() {
        return responseFor(inspectionService.inspectContact());
    }

    @PostMapping("/contactless")
    public ResponseEntity<CardInspectionResponse> inspectContactlessCard() {
        return responseFor(inspectionService.inspectContactless());
    }

    private static ResponseEntity<CardInspectionResponse> responseFor(CardInspectionResponse response) {
        HttpStatus status = response.status().equals("busy") ? HttpStatus.CONFLICT : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
