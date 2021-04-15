package com.redhat.labs.lodestar.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementAttribute {

    @Setter
    private String id;
    private String created;
    private String updated;

    public void generateId() {
        if (null == id) {
            id = UUID.randomUUID().toString();
        }
    }

    public void setUpdated() {
        String dateTime = getNowAsString();
        updated = dateTime;
    }

    public void setCreatedAndUpdated() {
        String dateTime = getNowAsString();
        created = dateTime;
        updated = dateTime;
    }

    private String getNowAsString() {
        return LocalDateTime.now(ZoneId.of("Z")).toString();
    }

}
