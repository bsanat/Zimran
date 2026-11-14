package kz.astyq.zimran.guest.mapper;

import kz.astyq.zimran.guest.model.domain.IssuedGuestToken;
import kz.astyq.zimran.guest.model.dto.GuestSessionViewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GuestMapper {

    @Mapping(target = "guestToken", source = "token")
    GuestSessionViewResponse toViewResponse(IssuedGuestToken token);
}
