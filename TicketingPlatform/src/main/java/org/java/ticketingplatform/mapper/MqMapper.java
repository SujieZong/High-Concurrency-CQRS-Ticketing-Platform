package org.java.ticketingplatform.mapper;

import org.java.ticketingplatform.dto.MqDTO;
import org.java.ticketingplatform.model.TicketCreation;
import org.java.ticketingplatform.model.TicketInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface MqMapper {
	@Mapping(source = "ticketId", target = "id")
	TicketCreation toTicketCreation(MqDTO mqDTO);

	// for MySql usage
	TicketInfo toTicketInfo(MqDTO mqDTO);
}
