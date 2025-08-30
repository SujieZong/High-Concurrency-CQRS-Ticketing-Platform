package org.java.rabbitcombinedconsumer.mapper;

import org.java.rabbitcombinedconsumer.dto.MqDTO;
import org.java.rabbitcombinedconsumer.model.TicketCreation;
import org.java.rabbitcombinedconsumer.model.TicketInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

// consume MQ message
@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface MqMapper {
	@Mapping(source = "ticketId", target = "id")
	TicketCreation toTicketCreation(MqDTO mqDTO);

	// for MySql usage
	TicketInfo toTicketInfo(MqDTO mqDTO);
}
