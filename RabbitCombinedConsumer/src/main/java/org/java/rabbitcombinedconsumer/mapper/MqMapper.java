package org.java.rabbitcombinedconsumer.mapper;

import org.java.rabbitcombinedconsumer.dto.MqDTO;
import org.java.rabbitcombinedconsumer.model.TicketCreation;
import org.java.rabbitcombinedconsumer.model.TicketInfo;
import org.java.rabbitcombinedconsumer.model.TicketStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

// consume MQ message
@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface MqMapper {

	@Mapping(target = "status", expression = "java(mapStatus(mqDTO.getStatus()))")
	TicketInfo toTicketInfo(MqDTO mqDTO);

	default TicketStatus mapStatus(TicketStatus s) {
		return s == null ? TicketStatus.PENDING_PAYMENT : s;
	}
}