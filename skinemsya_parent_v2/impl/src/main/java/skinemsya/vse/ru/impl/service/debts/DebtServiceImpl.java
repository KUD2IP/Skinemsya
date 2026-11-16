package skinemsya.vse.ru.impl.service.debts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.event.debts.DebtsCalculated;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;
import skinemsya.vse.ru.api.internal.pojo.debts.DebtSummary;
import skinemsya.vse.ru.api.internal.pojo.receipts.PositionData;
import skinemsya.vse.ru.api.internal.pojo.receipts.SelectionData;
import skinemsya.vse.ru.api.internal.pojo.receipts.SharedTargetData;
import skinemsya.vse.ru.api.internal.repository.debts.DebtRepository;
import skinemsya.vse.ru.api.internal.service.debts.DebtService;
import skinemsya.vse.ru.api.internal.service.events.EventAccessPort;
import skinemsya.vse.ru.api.internal.service.receipts.ReceiptDataPort;

/** Расчёт долгов по позициям чека и статусам сбора. */
@Service
@Transactional
@RequiredArgsConstructor
public class DebtServiceImpl implements DebtService {

    private static final List<DebtStatus> NON_UNPAID_STATUSES =
            List.of(DebtStatus.PENDING_CONFIRMATION, DebtStatus.PAID);

    private final DebtRepository debtRepository;
    private final ReceiptDataPort receiptDataPort;
    private final EventAccessPort eventAccessPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<Debt> calculate(long eventId) {
        requireDistribution(eventId);
        Map<Long, Long> aggregated = computeAggregatedDebts(eventId);
        boolean hasNonUnpaidDebts = debtRepository.existsByEventIdAndStatusIn(eventId, NON_UNPAID_STATUSES);

        if (hasNonUnpaidDebts) {
            debtRepository.deleteByEventIdAndStatus(eventId, DebtStatus.UNPAID);
        } else {
            debtRepository.deleteByEventId(eventId);
        }

        long payerId = eventAccessPort.getPayerId(eventId);
        var now = Instant.now();
        List<Debt> result = new ArrayList<>();

        for (var entry : aggregated.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            if (hasNonUnpaidDebts && isDebtLocked(eventId, entry.getKey())) {
                continue;
            }
            result.add(debtRepository.save(Debt.builder()
                    .eventId(eventId)
                    .debtorId(entry.getKey())
                    .creditorId(payerId)
                    .amountKopecks(entry.getValue())
                    .status(DebtStatus.UNPAID)
                    .createdAt(now)
                    .updatedAt(now)
                    .build()));
        }

        eventAccessPort.markCalculated(eventId);
        eventPublisher.publishEvent(new DebtsCalculated(eventId));
        return result;
    }

    @Override
    public void upsertDebtForParticipant(long eventId, long userId) {
        requireDistribution(eventId);
        long payerId = eventAccessPort.getPayerId(eventId);
        if (userId == payerId) {
            return;
        }
        if (isDebtLocked(eventId, userId)) {
            return;
        }

        long amount = computeParticipantDebt(eventId, userId);
        var existing = debtRepository.findByEventIdAndDebtorId(eventId, userId);
        var now = Instant.now();

        if (amount <= 0) {
            existing.ifPresent(debt -> {
                if (debt.getStatus() == DebtStatus.UNPAID) {
                    debtRepository.delete(debt);
                }
            });
            return;
        }

        if (existing.isPresent()) {
            var debt = existing.get();
            if (debt.getStatus() != DebtStatus.UNPAID) {
                return;
            }
            debt.setAmountKopecks(amount);
            debt.setUpdatedAt(now);
            debtRepository.save(debt);
            return;
        }

        debtRepository.save(Debt.builder()
                .eventId(eventId)
                .debtorId(userId)
                .creditorId(payerId)
                .amountKopecks(amount)
                .status(DebtStatus.UNPAID)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Override
    public void recalculateUnpaidDebts(long eventId) {
        var status = eventAccessPort.getStatus(eventId);
        if (status != EventStatus.DISTRIBUTION && status != EventStatus.CALCULATED) {
            return;
        }

        if (status == EventStatus.CALCULATED) {
            eventAccessPort.revertCalculatedToDistribution(eventId);
        }

        debtRepository.deleteByEventIdAndStatus(eventId, DebtStatus.UNPAID);
        for (long userId : eventAccessPort.getSelectionCompletedParticipantUserIds(eventId)) {
            upsertDebtForParticipant(eventId, userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Debt> findByEvent(long eventId) {
        return debtRepository.findByEventId(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public DebtSummary getSummary(long userId) {
        long owed = debtRepository.findByDebtorId(userId).stream()
                .filter(debt -> debt.getStatus() != DebtStatus.PAID)
                .mapToLong(Debt::getAmountKopecks)
                .sum();
        long toReceive = debtRepository.findByCreditorId(userId).stream()
                .filter(debt -> debt.getStatus() != DebtStatus.PAID)
                .mapToLong(Debt::getAmountKopecks)
                .sum();
        long unpaid = debtRepository.findByDebtorId(userId).stream()
                .filter(debt -> debt.getStatus() == DebtStatus.UNPAID)
                .count();
        long pending = debtRepository.findByDebtorId(userId).stream()
                .filter(debt -> debt.getStatus() == DebtStatus.PENDING_CONFIRMATION)
                .count();
        return new DebtSummary(owed, toReceive, unpaid, pending);
    }

    @Override
    public Debt markPendingConfirmation(long debtId) {
        var debt = debtRepository.findById(debtId).orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Debt not found"));
        debt.setStatus(DebtStatus.PENDING_CONFIRMATION);
        debt.setUpdatedAt(Instant.now());
        return debtRepository.save(debt);
    }

    @Override
    public Debt close(long debtId) {
        var debt = debtRepository.findById(debtId).orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Debt not found"));
        debt.setStatus(DebtStatus.PAID);
        debt.setUpdatedAt(Instant.now());
        return debtRepository.save(debt);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allPaidForEvent(long eventId) {
        return !debtRepository.existsByEventIdAndStatusNot(eventId, DebtStatus.PAID);
    }

    private Map<Long, Long> computeAggregatedDebts(long eventId) {
        long payerId = eventAccessPort.getPayerId(eventId);
        List<Long> participants = eventAccessPort.getParticipantUserIds(eventId);
        int expectedCount = eventAccessPort.getExpectedParticipantCount(eventId);

        var positions = receiptDataPort.getPositions(eventId);
        var selections = receiptDataPort.getSelections(eventId);
        var sharedTargets = receiptDataPort.getSharedTargets(eventId);

        var selectionsByPosition = selections.stream().collect(Collectors.groupingBy(SelectionData::getPositionId));
        var sharedTargetsByPosition =
                sharedTargets.stream().collect(Collectors.groupingBy(SharedTargetData::getPositionId));

        Map<Long, Long> aggregated = new HashMap<>();

        for (var position : positions) {
            var shares = sharesForPosition(
                    position,
                    participants,
                    selectionsByPosition.getOrDefault(position.getId(), List.of()),
                    sharedTargetsByPosition.getOrDefault(position.getId(), List.of()),
                    payerId,
                    expectedCount);
            for (var entry : shares.entrySet()) {
                long userId = entry.getKey();
                long share = entry.getValue();
                if (userId == payerId || share <= 0) {
                    continue;
                }
                aggregated.merge(userId, share, Long::sum);
            }
        }
        return aggregated;
    }

    private long computeParticipantDebt(long eventId, long userId) {
        long payerId = eventAccessPort.getPayerId(eventId);
        if (userId == payerId) {
            return 0;
        }

        List<Long> participants = eventAccessPort.getParticipantUserIds(eventId);
        int expectedCount = eventAccessPort.getExpectedParticipantCount(eventId);
        var positions = receiptDataPort.getPositions(eventId);
        var selections = receiptDataPort.getSelections(eventId);
        var sharedTargets = receiptDataPort.getSharedTargets(eventId);

        var selectionsByPosition = selections.stream().collect(Collectors.groupingBy(SelectionData::getPositionId));
        var sharedTargetsByPosition =
                sharedTargets.stream().collect(Collectors.groupingBy(SharedTargetData::getPositionId));

        long total = 0;
        for (var position : positions) {
            var shares = sharesForPosition(
                    position,
                    participants,
                    selectionsByPosition.getOrDefault(position.getId(), List.of()),
                    sharedTargetsByPosition.getOrDefault(position.getId(), List.of()),
                    payerId,
                    expectedCount);
            total += shares.getOrDefault(userId, 0L);
        }
        return total;
    }

    private Map<Long, Long> sharesForPosition(
            PositionData position,
            List<Long> participants,
            List<SelectionData> selections,
            List<SharedTargetData> targets,
            long payerId,
            int expectedCount) {
        Map<Long, BigDecimal> weights = resolveWeights(position, participants, selections, targets, payerId, expectedCount);
        if (weights.isEmpty()) {
            return Map.of();
        }
        long amount = amountToSplit(position, selections);
        if (amount <= 0) {
            return Map.of();
        }
        return splitKopecks(amount, weights);
    }

    private static long amountToSplit(PositionData position, List<SelectionData> selections) {
        if (position.isShared()) {
            return position.getTotalPriceKopecks();
        }
        int totalUnits = toIntUnits(position.getQuantity());
        int selectedUnits = 0;
        for (var selection : selections) {
            selectedUnits += toIntUnits(selection.getSelectedQuantity());
        }
        if (totalUnits <= 0 || selectedUnits <= 0) {
            return 0;
        }
        if (selectedUnits >= totalUnits) {
            return position.getTotalPriceKopecks();
        }
        return position.getTotalPriceKopecks() * selectedUnits / totalUnits;
    }

    private static int toIntUnits(BigDecimal quantity) {
        if (quantity == null) {
            return 0;
        }
        return quantity.setScale(0, RoundingMode.DOWN).intValue();
    }

    private boolean isDebtLocked(long eventId, long debtorId) {
        return debtRepository
                .findByEventIdAndDebtorId(eventId, debtorId)
                .map(debt -> debt.getStatus() != DebtStatus.UNPAID)
                .orElse(false);
    }

    private void requireDistribution(long eventId) {
        if (eventAccessPort.getStatus(eventId) != EventStatus.DISTRIBUTION) {
            throw new DomainException(ErrorCode.DOMAIN_CONFLICT, "Event is not in distribution status");
        }
    }

    private static Map<Long, BigDecimal> resolveWeights(
            PositionData position,
            List<Long> participants,
            List<SelectionData> selections,
            List<SharedTargetData> targets,
            long payerId,
            int expectedCount) {
        Map<Long, BigDecimal> weights = new LinkedHashMap<>();
        if (position.isShared()) {
            if (targets.isEmpty()) {
                for (long userId : participants) {
                    weights.put(userId, BigDecimal.ONE);
                }
                int unjoined = Math.max(0, expectedCount - participants.size());
                if (unjoined > 0) {
                    weights.merge(payerId, BigDecimal.valueOf(unjoined), BigDecimal::add);
                }
            } else {
                for (var target : targets) {
                    weights.put(target.getUserId(), BigDecimal.ONE);
                }
            }
            return weights;
        }
        for (var selection : selections) {
            weights.put(selection.getUserId(), selection.getSelectedQuantity());
        }
        return weights;
    }

    static Map<Long, Long> splitKopecks(long amountKopecks, Map<Long, BigDecimal> weights) {
        List<Long> userIds = weights.keySet().stream().sorted().toList();
        BigDecimal totalWeight = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of();
        }

        Map<Long, Long> shares = new LinkedHashMap<>();
        long allocated = 0;
        List<RemainderEntry> remainders = new ArrayList<>();

        for (long userId : userIds) {
            BigDecimal weight = weights.get(userId);
            BigDecimal exact = BigDecimal.valueOf(amountKopecks)
                    .multiply(weight)
                    .divide(totalWeight, 10, RoundingMode.DOWN);
            long floor = exact.longValue();
            shares.put(userId, floor);
            allocated += floor;
            remainders.add(new RemainderEntry(userId, exact.subtract(BigDecimal.valueOf(floor))));
        }

        long remainder = amountKopecks - allocated;
        remainders.sort(
                Comparator.comparing(RemainderEntry::fractional).reversed().thenComparing(RemainderEntry::userId));

        for (int i = 0; i < remainder && i < remainders.size(); i++) {
            long userId = remainders.get(i).userId();
            shares.put(userId, shares.get(userId) + 1);
        }
        return shares;
    }

    private record RemainderEntry(long userId, BigDecimal fractional) {}
}
