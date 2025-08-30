package org.java.queryservice.mapper;

import org.java.queryservice.dto.TicketInfoDTO;
import org.java.queryservice.model.TicketInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketMapper {

	// Entity -> DTO
	TicketInfoDTO toInfoDto(TicketInfo entity);
}
