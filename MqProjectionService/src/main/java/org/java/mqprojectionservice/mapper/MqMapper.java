package org.java.mqprojectionservice.mapper;

import org.java.mqprojectionservice.dto.MqDTO;
import org.java.mqprojectionservice.model.TicketInfo;
import org.java.mqprojectionservice.model.TicketStatus;

// consume MQ message - Manual implementation
public interface MqMapper {

	TicketInfo toTicketInfo(MqDTO mqDTO);

	TicketStatus mapStatus(TicketStatus s);
}