package skinemsya.vse.ru.impl.rest;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;
import skinemsya.vse.ru.api.internal.pojo.events.EventParticipant;
import skinemsya.vse.ru.api.internal.repository.events.EventParticipantRepository;
import skinemsya.vse.ru.api.internal.security.AuthenticatedUser;
import skinemsya.vse.ru.api.internal.service.debts.DebtService;
import skinemsya.vse.ru.api.internal.service.events.EventAccessPort;
import skinemsya.vse.ru.api.internal.service.payments.PaymentReadPort;
import skinemsya.vse.ru.api.rest.debts.dto.DebtResponse;
import skinemsya.vse.ru.api.rest.debts.dto.DebtSummaryResponse;
import skinemsya.vse.ru.api.rest.debts.dto.ParticipantStatusItem;
import skinemsya.vse.ru.api.rest.debts.dto.ParticipantsStatusResponse;
import skinemsya.vse.ru.impl.mapper.rest.DebtRestMapper;

/** REST API долгов сбора. */
@RestController
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;
    private final EventAccessPort eventAccessPort;
    private final EventParticipantRepository eventParticipantRepository;
    private final PaymentReadPort paymentReadPort;
    private final DebtRestMapper debtRestMapper;

    /** Возвращает долги сбора для участника. */
    @GetMapping("/api/v1/events/{eventId}/debts")
    public List<DebtResponse> listDebts(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        eventAccessPort.requireParticipant(eventId, userId);
        var debts = debtService.findByEvent(eventId);
        var paymentInfoByDebtId = paymentReadPort.findPaymentInfoByDebtIds(
                debts.stream().map(debt -> debt.getId()).toList());
        return debts.stream()
                .map(debt -> debtRestMapper.toResponse(debt, paymentInfoByDebtId.get(debt.getId())))
                .toList();
    }

    /** Возвращает сводку долгов текущего пользователя. */
    @GetMapping("/api/v1/debts/summary")
    public DebtSummaryResponse getSummary(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return debtRestMapper.toResponse(debtService.getSummary(requireUserId(authenticatedUser)));
    }

    /** Возвращает статус выбора и долга по участникам сбора. */
    @GetMapping("/api/v1/events/{eventId}/participants-status")
    public ParticipantsStatusResponse getParticipantsStatus(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        eventAccessPort.requireParticipant(eventId, userId);

        var participants = eventParticipantRepository.findByEventId(eventId);
        var debts = debtService.findByEvent(eventId);
        long completed = eventParticipantRepository.countByEventIdAndSelectionCompletedAtIsNotNull(eventId);

        var items = participants.stream()
                .map(participant -> toParticipantStatus(participant, debts))
                .toList();

        long expected = eventAccessPort.getExpectedParticipantCount(eventId);
        long joined = participants.size();
        return new ParticipantsStatusResponse()
                .totalParticipants(joined)
                .expectedParticipantCount(expected)
                .joinedCount(joined)
                .completedSelections(completed)
                .participants(items);
    }

    private ParticipantStatusItem toParticipantStatus(EventParticipant participant, List<Debt> debts) {
        String status = debts.stream()
                .filter(debt -> debt.getDebtorId() == participant.getUserId())
                .findFirst()
                .map(debt -> debt.getStatus().name())
                .orElse("NONE");
        return new ParticipantStatusItem()
                .userId(participant.getUserId())
                .selectionCompleted(participant.getSelectionCompletedAt() != null)
                .debtStatus(status);
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
