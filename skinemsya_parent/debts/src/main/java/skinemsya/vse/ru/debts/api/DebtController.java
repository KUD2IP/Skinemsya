package skinemsya.vse.ru.debts.api;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.debts.api.dto.DebtResponse;
import skinemsya.vse.ru.debts.api.dto.DebtSummaryResponse;
import skinemsya.vse.ru.debts.api.dto.ParticipantsStatusResponse;
import skinemsya.vse.ru.debts.application.DebtService;
import skinemsya.vse.ru.debts.application.PaymentReadPort;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantRepository;

@RestController
public class DebtController {

    private final DebtService debtService;
    private final EventAccessPort eventAccessPort;
    private final EventParticipantRepository eventParticipantRepository;
    private final PaymentReadPort paymentReadPort;

    public DebtController(
            DebtService debtService,
            EventAccessPort eventAccessPort,
            EventParticipantRepository eventParticipantRepository,
            PaymentReadPort paymentReadPort) {
        this.debtService = debtService;
        this.eventAccessPort = eventAccessPort;
        this.eventParticipantRepository = eventParticipantRepository;
        this.paymentReadPort = paymentReadPort;
    }

    @GetMapping("/api/v1/events/{eventId}/debts")
    public List<DebtResponse> listDebts(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        eventAccessPort.requireParticipant(eventId, userId);
        var debts = debtService.findByEvent(eventId);
        var paymentInfoByDebtId = paymentReadPort.findPaymentInfoByDebtIds(
                debts.stream().map(d -> d.id()).toList());
        return debts.stream()
                .map(d -> DebtResponse.from(d, paymentInfoByDebtId.get(d.id())))
                .toList();
    }

    @GetMapping("/api/v1/debts/summary")
    public DebtSummaryResponse getSummary(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return DebtSummaryResponse.from(debtService.getSummary(requireUserId(authenticatedUser)));
    }

    @GetMapping("/api/v1/events/{eventId}/participants-status")
    public ParticipantsStatusResponse getParticipantsStatus(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        eventAccessPort.requireParticipant(eventId, userId);

        var participants = eventParticipantRepository.findByEventId(eventId);
        var debts = debtService.findByEvent(eventId);
        long completed = eventParticipantRepository.countByEventIdAndSelectionCompletedAtIsNotNull(eventId);

        var items = participants.stream()
                .map(p -> {
                    String status = debts.stream()
                            .filter(d -> d.debtorId() == p.getUserId())
                            .findFirst()
                            .map(d -> d.status().name())
                            .orElse("NONE");
                    return new ParticipantsStatusResponse.ParticipantStatusItem(
                            p.getUserId(), p.getSelectionCompletedAt() != null, status);
                })
                .toList();

        return new ParticipantsStatusResponse(participants.size(), completed, items);
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
