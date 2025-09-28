package org.java.mqprojectionservice.mapper;

import org.java.mqprojectionservice.dto.MqDTO;
import org.java.mqprojectionservice.model.TicketInfo;
import org.java.mqprojectionservice.model.TicketStatus;
import org.springframework.stereotype.Component;

@Component
public class MqMapperImpl implements MqMapper {

    @Override
    public TicketInfo toTicketInfo(MqDTO mqDTO) {
        if (mqDTO == null) {
            return null;
        }

        TicketInfo ticketInfo = new TicketInfo();
        ticketInfo.setTicketId(mqDTO.getTicketId());
        ticketInfo.setVenueId(mqDTO.getVenueId());
        ticketInfo.setEventId(mqDTO.getEventId());
        ticketInfo.setZoneId(mqDTO.getZoneId());
        ticketInfo.setRow(mqDTO.getRow());
        ticketInfo.setColumn(mqDTO.getColumn());
        ticketInfo.setCreatedOn(mqDTO.getCreatedOn());
        ticketInfo.setStatus(mapStatus(mqDTO.getStatus()));

        return ticketInfo;
    }

    @Override
    public TicketStatus mapStatus(TicketStatus s) {
        return s == null ? TicketStatus.PENDING_PAYMENT : s;
    }
}