package cz.uhk.ppro.horyavoda.presentation.dto;

import java.time.LocalDateTime;

/** Vrácení kusu; bez času se bere okamžik požadavku. */
public record ItemReturnRequest(LocalDateTime returnedAt) {
}
