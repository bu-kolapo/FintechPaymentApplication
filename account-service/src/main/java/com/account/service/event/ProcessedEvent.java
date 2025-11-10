package com.account.service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "processed_events")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    private String id;

    private String eventId;

    private Instant processedAt;

}
