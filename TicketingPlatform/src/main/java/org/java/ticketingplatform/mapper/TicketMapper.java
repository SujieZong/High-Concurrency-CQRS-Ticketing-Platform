package org.java.ticketingplatform.mapper;

import org.java.ticketingplatform.dto.TicketInfoDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.model.TicketCreation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {

	@Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
	@Mapping(target = "status", constant = "CREATED")
	TicketCreation ticketInfoDTOToTicketCreation(TicketInfoDTO dto);

	@Mapping(source = "id", target = "ticketId")
	TicketRespondDTO ticketCreationToTicketRespondDTO(TicketCreation ticket);


}
