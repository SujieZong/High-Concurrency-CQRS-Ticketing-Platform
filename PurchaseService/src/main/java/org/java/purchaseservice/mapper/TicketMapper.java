package org.java.purchaseservice.mapper;

import org.java.purchaseservice.dto.TicketInfoDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;
import org.java.purchaseservice.dto.TicketCreationDTO;
import org.java.purchaseservice.model.TicketInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {

	// Entity -> DTO
	TicketInfoDTO toInfoDto(TicketInfo entity);

	// DTO -> Entity
	TicketRespondDTO toRespondDto(TicketInfo entity);

	@Mapping(target = "ticketId", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "createdOn", ignore = true)
	TicketInfo toEntity(TicketCreationDTO dto);
}
