package uk.gov.dwp.uc.pairtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class TicketServiceImpl implements TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketServiceImpl.class);

    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;
    private static final int INFANT_TICKET_PRICE = 0;
    private static final int MAX_TICKETS_PER_PURCHASE = 25;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        // 2. Log the start of the process
        logger.info("Starting ticket purchase for Account ID: {}", accountId);

        try {
            validateInput(accountId, ticketTypeRequests);

            Map<TicketTypeRequest.Type, Integer> counts = aggregateTicketCounts(ticketTypeRequests);

            int adultCount = counts.getOrDefault(TicketTypeRequest.Type.ADULT, 0);
            int childCount = counts.getOrDefault(TicketTypeRequest.Type.CHILD, 0);
            int infantCount = counts.getOrDefault(TicketTypeRequest.Type.INFANT, 0);

            validateBusinessRules(adultCount, childCount, infantCount);

            int totalAmountToPay = (adultCount * ADULT_TICKET_PRICE) + (childCount * CHILD_TICKET_PRICE) + (childCount *  INFANT_TICKET_PRICE);
            int totalSeatsToAllocate = adultCount + childCount;

            logger.debug("Calculated totals for Account {}: £{}, {} seats", accountId, totalAmountToPay, totalSeatsToAllocate);

            ticketPaymentService.makePayment(accountId, totalAmountToPay);
            seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);

            logger.info("Purchase completed successfully for Account ID: {}", accountId);

        } catch (InvalidPurchaseException e) {
            logger.warn("Purchase rejected for Account {}: {}", accountId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during purchase for Account {}", accountId, e);
            throw e;
        }
    }

    private void validateInput(Long accountId, TicketTypeRequest[] requests) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid Account ID. Must be greater than zero.");
        }
        if (requests == null || requests.length == 0) {
            throw new InvalidPurchaseException("Purchase request must contain at least one ticket.");
        }
    }

    private Map<TicketTypeRequest.Type, Integer> aggregateTicketCounts(TicketTypeRequest[] requests) {
        return Arrays.stream(requests)
                .filter(req -> req != null && req.noOfTickets() > 0)
                .collect(Collectors.groupingBy(
                        TicketTypeRequest::type,
                        Collectors.summingInt(TicketTypeRequest::noOfTickets)
                ));
    }

    private void validateBusinessRules(int adults, int children, int infants) {
        int totalTickets = adults + children + infants;

        if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException("Total tickets cannot exceed " + MAX_TICKETS_PER_PURCHASE);
        }

        if (totalTickets == 0) {
            throw new InvalidPurchaseException("No valid tickets were requested.");
        }

        if ((children > 0 || infants > 0) && adults == 0) {
            throw new InvalidPurchaseException("Child and Infant tickets require at least one Adult ticket.");
        }

        if (infants > adults) {
            throw new InvalidPurchaseException("Number of infants cannot exceed the number of adults.");
        }
    }
}