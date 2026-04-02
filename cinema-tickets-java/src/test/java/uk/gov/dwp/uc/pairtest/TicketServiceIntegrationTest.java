package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceIntegrationTest {

    private TicketService ticketService;

    @Mock
    private TicketPaymentService paymentService;

    @Mock
    private SeatReservationService reservationService;

    private static final long VALID_ACCOUNT_ID = 1L;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Test
    void validPurchase_CalculatesCorrectPaymentAndSeats() {
        var adultReq = new TicketTypeRequest(Type.ADULT, 2);
        var childReq = new TicketTypeRequest(Type.CHILD, 1);
        var infantReq = new TicketTypeRequest(Type.INFANT, 1);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, adultReq, childReq, infantReq);

        verify(paymentService).makePayment(VALID_ACCOUNT_ID, 65);
        verify(reservationService).reserveSeat(VALID_ACCOUNT_ID, 3);
    }

    @Test
    void validPurchase_AggregatesSameTypes() {
        var adultReq1 = new TicketTypeRequest(Type.ADULT, 1);
        var adultReq2 = new TicketTypeRequest(Type.ADULT, 1);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, adultReq1, adultReq2);

        verify(paymentService).makePayment(VALID_ACCOUNT_ID, 50);
        verify(reservationService).reserveSeat(VALID_ACCOUNT_ID, 2);
    }

    @Test
    void invalidPurchase_MoreInfantsThanAdults_ThrowsException() {
        var adultReq = new TicketTypeRequest(Type.ADULT, 1);
        var infantReq = new TicketTypeRequest(Type.INFANT, 2);

        var exception = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(VALID_ACCOUNT_ID, adultReq, infantReq)
        );

        assertEquals("Number of infants cannot exceed the number of adults.", exception.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void invalidPurchase_NoAdult_ThrowsException() {
        var childReq = new TicketTypeRequest(Type.CHILD, 1);

        var exception = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(VALID_ACCOUNT_ID, childReq)
        );

        assertEquals("Child and Infant tickets require at least one Adult ticket.", exception.getMessage());
        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void invalidPurchase_ExceedsMaxLimit_ThrowsException() {
        var massiveOrder = new TicketTypeRequest(Type.ADULT, 26);

        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(VALID_ACCOUNT_ID, massiveOrder)
        );

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void invalidPurchase_BadAccountId_ThrowsException() {
        var adultReq = new TicketTypeRequest(Type.ADULT, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(0L, adultReq));
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(-5L, adultReq));
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(null, adultReq));
    }
}