// src/test/java/org/java/queryservice/service/query/QueryServiceTest.java
package org.java.queryservice.service.query;

import org.java.queryservice.dto.TicketInfoDTO;
import org.java.queryservice.exception.TicketNotFoundException;
import org.java.queryservice.mapper.TicketMapper;
import org.java.queryservice.model.TicketInfo;
import org.java.queryservice.repository.mysql.TicketInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryServiceTests {

	@Mock
	TicketInfoRepository ticketInfoRepository;

	@Mock
	TicketMapper ticketMapper;

	@InjectMocks
	QueryService queryService;

	@Nested
	@DisplayName("getTicket")
	class GetTicketTests {

		@Test
		@DisplayName("should return DTO when ticket exists")
		void getTicket_ok() {
			// given
			String ticketId = "T-123";
			TicketInfo entity = new TicketInfo(); // 不依赖字段，作为占位
			TicketInfoDTO dto = new TicketInfoDTO();
			dto.setTicketId(ticketId);

			when(ticketInfoRepository.findById(ticketId)).thenReturn(Optional.of(entity));
			when(ticketMapper.toInfoDto(entity)).thenReturn(dto);

			// when
			TicketInfoDTO result = queryService.getTicket(ticketId);

			// then
			assertNotNull(result);
			assertEquals(ticketId, result.getTicketId());
			verify(ticketInfoRepository, times(1)).findById(ticketId);
			verify(ticketMapper, times(1)).toInfoDto(entity);
			verifyNoMoreInteractions(ticketInfoRepository, ticketMapper);
		}

		@Test
		@DisplayName("should throw TicketNotFoundException when ticket does not exist")
		void getTicket_notFound() {
			// given
			String ticketId = "T-404";
			when(ticketInfoRepository.findById(ticketId)).thenReturn(Optional.empty());

			// when / then
			TicketNotFoundException ex = assertThrows(
					TicketNotFoundException.class,
					() -> queryService.getTicket(ticketId)
			);
			assertTrue(ex.getMessage().contains(ticketId));
			verify(ticketInfoRepository, times(1)).findById(ticketId);
			verifyNoInteractions(ticketMapper);
			verifyNoMoreInteractions(ticketInfoRepository);
		}
	}

	@Nested
	@DisplayName("countTicketSoldByEvent")
	class CountTests {

		@Test
		@DisplayName("should return repository count and log once")
		void count_ok() {
			// given
			String eventId = "E-1";
			when(ticketInfoRepository.countByEventId(eventId)).thenReturn(42);

			// when
			int count = queryService.countTicketSoldByEvent(eventId);

			// then
			assertEquals(42, count);
			verify(ticketInfoRepository, times(1)).countByEventId(eventId);
			verifyNoInteractions(ticketMapper);
			verifyNoMoreInteractions(ticketInfoRepository);
		}
	}

	@Nested
	@DisplayName("sumRevenueByVenueAndEvent")
	class RevenueTests {

		@Test
		@DisplayName("should return ZERO when repository returns null")
		void revenue_nullToZero() {
			// given
			String venueId = "V-1";
			String eventId = "E-1";
			when(ticketInfoRepository.sumRevenueByVenueAndEvent(venueId, eventId)).thenReturn(null);

			// when
			BigDecimal result = queryService.sumRevenueByVenueAndEvent(venueId, eventId);

			// then
			assertNotNull(result);
			assertEquals(0, result.compareTo(BigDecimal.ZERO), "null should map to 0");
			verify(ticketInfoRepository, times(1)).sumRevenueByVenueAndEvent(venueId, eventId);
			verifyNoInteractions(ticketMapper);
			verifyNoMoreInteractions(ticketInfoRepository);
		}

		@Test
		@DisplayName("should return repository revenue when non-null")
		void revenue_ok() {
			// given
			String venueId = "V-2";
			String eventId = "E-9";
			BigDecimal repoValue = new BigDecimal("123.45");
			when(ticketInfoRepository.sumRevenueByVenueAndEvent(venueId, eventId)).thenReturn(repoValue);

			// when
			BigDecimal result = queryService.sumRevenueByVenueAndEvent(venueId, eventId);

			// then
			assertNotNull(result);
			assertEquals(0, result.compareTo(repoValue));
			verify(ticketInfoRepository, times(1)).sumRevenueByVenueAndEvent(venueId, eventId);
			verifyNoInteractions(ticketMapper);
			verifyNoMoreInteractions(ticketInfoRepository);
		}
	}

	@Test
	@DisplayName("should pass correct parameters into repository for all methods")
	void verifyParameters() {
		// given
		String ticketId = "T-xyz";
		String eventId = "E-xyz";
		String venueId = "V-xyz";

		TicketInfo entity = new TicketInfo();
		TicketInfoDTO dto = new TicketInfoDTO();
		dto.setTicketId(ticketId);

		when(ticketInfoRepository.findById(ticketId)).thenReturn(Optional.of(entity));
		when(ticketMapper.toInfoDto(entity)).thenReturn(dto);
		when(ticketInfoRepository.countByEventId(eventId)).thenReturn(7);
		when(ticketInfoRepository.sumRevenueByVenueAndEvent(venueId, eventId))
				.thenReturn(new BigDecimal("9.99"));

		// when
		queryService.getTicket(ticketId);
		queryService.countTicketSoldByEvent(eventId);
		queryService.sumRevenueByVenueAndEvent(venueId, eventId);

		// then: 通过 captor 再次校验入参一致性（可选）
		ArgumentCaptor<String> idCap = ArgumentCaptor.forClass(String.class);
		verify(ticketInfoRepository, times(1)).findById(idCap.capture());
		assertEquals(ticketId, idCap.getValue());

		ArgumentCaptor<String> evCap = ArgumentCaptor.forClass(String.class);
		verify(ticketInfoRepository, times(1)).countByEventId(evCap.capture());
		assertEquals(eventId, evCap.getValue());

		ArgumentCaptor<String> venueCap = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> evCap2 = ArgumentCaptor.forClass(String.class);
		verify(ticketInfoRepository, times(1))
				.sumRevenueByVenueAndEvent(venueCap.capture(), evCap2.capture());
		assertEquals(venueId, venueCap.getValue());
		assertEquals(eventId, evCap2.getValue());

		verify(ticketMapper, times(1)).toInfoDto(entity);
		verifyNoMoreInteractions(ticketInfoRepository, ticketMapper);
	}
}
