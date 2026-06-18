package com.example.serviceonec.Monitoring.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringPmoInvoiceOutputResponseDto {

    @JsonProperty("odata.metadata")
    private String odataMetadata;

    @JsonProperty("value")
    private List<ReceiptInvoice> value;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptInvoice {

        @JsonProperty("Ref_Key")
        private UUID refKey;

        @JsonProperty("Number")
        private String number;

        @JsonProperty("Date")
        private LocalDateTime date;

        @JsonProperty("Автор_Key")
        private UUID authorKey;

        @JsonProperty("Комментарий")
        private String comment;

        @JsonProperty("Контрагент_Key")
        private UUID counterpartyKey;

        @JsonProperty("Организация_Key")
        private UUID organizationKey;
    }

}
