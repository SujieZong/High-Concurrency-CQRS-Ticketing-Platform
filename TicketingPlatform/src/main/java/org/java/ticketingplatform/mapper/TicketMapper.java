package org.java.ticketingplatform.mapper;

import org.java.ticketingplatform.dto.TicketInfoDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.model.TicketInfo;
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
