package org.java.mqprojectionservice.mapper;

import org.java.mqprojectionservice.dto.MqDTO;
import org.java.mqprojectionservice.model.TicketInfo;
import org.java.mqprojectionservice.model.TicketStatus;
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