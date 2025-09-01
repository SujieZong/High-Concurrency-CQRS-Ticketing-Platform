package org.java.queryservice.mapper;

import org.java.queryservice.dto.TicketInfoDTO;
import org.java.queryservice.model.TicketInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {

	// Entity -> DTO
	@Mapping(source = "row", target = "row")
	@Mapping(source = "column", target = "column")
	TicketInfoDTO toInfoDto(TicketInfo entity);
}
