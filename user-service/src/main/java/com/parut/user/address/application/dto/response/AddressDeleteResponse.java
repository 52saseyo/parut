package com.parut.user.address.application.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.ZonedDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record AddressDeleteResponse(
        UUID addressId,
        ZonedDateTime deletedAt
) {
    public static AddressDeleteResponse of(UUID addressId, ZonedDateTime deletedAt) {
        return new AddressDeleteResponse(addressId, deletedAt);
    }
}
