package org.java.rabbitcombinedconsumer.mapper;

import org.java.rabbitcombinedconsumer.dto.TicketInfoDTO;
import org.java.rabbitcombinedconsumer.model.TicketCreation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {

	@Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
	@Mapping(target = "status", constant = "CREATED")
	TicketCreation ticketInfoDTOToTicketCreation(TicketInfoDTO dto);

}
