package com.ros.ewallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {

    private Long id;
    private Instant createdAt;
    private String eventType;
    private String summary;
    private Long actorUserId;
    private String actorUsername;
    private String actorFirstName;
    private String actorLastName;
    private String ipAddress;
    private Map<String, Object> metadata;
}
