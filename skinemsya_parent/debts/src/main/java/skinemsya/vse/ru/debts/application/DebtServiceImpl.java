package skinemsya.vse.ru.debts.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.DebtsCalculated;
import skinemsya.vse.ru.debts.domain.Debt;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.debts.domain.DebtSummary;
import skinemsya.vse.ru.debts.domain.exception.DebtNotFoundException;
import skinemsya.vse.ru.debts.domain.exception.PositionHasNoTargetsException;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtEntity;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtRepository;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.domain.exception.EventNotInDistributionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DebtServiceImpl implements DebtService {

    private static final List<DebtStatus> NON_UNPAID_STATUSES = List.of(
            DebtStatus.PENDING_CONFIRMATION,
            DebtStatus.PAID
    );

    private final DebtRepository debtRepository;
    private final ReceiptDataPort receiptDataPort;
    private final EventAccessPort eventAccessPort;
    private final ApplicationEventPublisher eventPublisher;

    public DebtServiceImpl(
            DebtRepository debtRepository,
            ReceiptDataPort receiptDataPort,
            EventAccessPort eventAccessPort,
            ApplicationEventPublisher eventPublisher
    ) {
        this.debtRepository = debtRepository;
        this.receiptDataPort = receiptDataPort;
        this.eventAccessPort = eventAccessPort;
        this.eventPublisher = eventPublisher;
    }

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
            var entity = new DebtEntity();
            entity.setEventId(eventId);
            entity.setDebtorId(entry.getKey());
            entity.setCreditorId(payerId);
            entity.setAmountKopecks(entry.getValue());
            entity.setStatus(DebtStatus.UNPAID);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            result.add(toDomain(debtRepository.save(entity)));
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
            var entity = existing.get();
            if (entity.getStatus() != DebtStatus.UNPAID) {
                return;
            }
            entity.setAmountKopecks(amount);
            entity.setUpdatedAt(now);
            debtRepository.save(entity);
            return;
        }

        var entity = new DebtEntity();
        entity.setEventId(eventId);
        entity.setDebtorId(userId);
        entity.setCreditorId(payerId);
        entity.setAmountKopecks(amount);
        entity.setStatus(DebtStatus.UNPAID);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        debtRepository.save(entity);
    }

    @Override
    public void recalculateUnpaidDebts(long eventId) {
        var status = eventAccessPort.getStatus(eventId);
        if (status != EventStatus.DISTRIBUTION && status != EventStatus.CALCULATED) {
            return;
        }
        if (debtRepository.existsByEventIdAndStatusIn(eventId, NON_UNPAID_STATUSES)) {
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
        return debtRepository.findByEventId(eventId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DebtSummary getSummary(long userId) {
        long owed = debtRepository.findByDebtorId(userId).stream()
                .filter(d -> d.getStatus() != DebtStatus.PAID)
                .mapToLong(DebtEntity::getAmountKopecks)
                .sum();
        long toReceive = debtRepository.findByCreditorId(userId).stream()
                .filter(d -> d.getStatus() != DebtStatus.PAID)
                .mapToLong(DebtEntity::getAmountKopecks)
                .sum();
        long unpaid = debtRepository.findByDebtorId(userId).stream()
                .filter(d -> d.getStatus() == DebtStatus.UNPAID)
                .count();
        long pending = debtRepository.findByDebtorId(userId).stream()
                .filter(d -> d.getStatus() == DebtStatus.PENDING_CONFIRMATION)
                .count();
        return new DebtSummary(owed, toReceive, unpaid, pending);
    }

    @Override
    public Debt markPendingConfirmation(long debtId) {
        var entity = debtRepository.findById(debtId).orElseThrow(DebtNotFoundException::new);
        entity.setStatus(DebtStatus.PENDING_CONFIRMATION);
        entity.setUpdatedAt(Instant.now());
        return toDomain(debtRepository.save(entity));
    }

    @Override
    public Debt close(long debtId) {
        var entity = debtRepository.findById(debtId).orElseThrow(DebtNotFoundException::new);
        entity.setStatus(DebtStatus.PAID);
        entity.setUpdatedAt(Instant.now());
        return toDomain(debtRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allPaidForEvent(long eventId) {
        return !debtRepository.existsByEventIdAndStatusNot(eventId, DebtStatus.PAID);
    }

    private Map<Long, Long> computeAggregatedDebts(long eventId) {
        long payerId = eventAccessPort.getPayerId(eventId);
        List<Long> participants = eventAccessPort.getParticipantUserIds(eventId);

        var positions = receiptDataPort.getPositions(eventId);
        var selections = receiptDataPort.getSelections(eventId);
        var sharedTargets = receiptDataPort.getSharedTargets(eventId);

        var selectionsByPosition = selections.stream()
                .collect(Collectors.groupingBy(ReceiptDataPort.SelectionData::positionId));
        var sharedTargetsByPosition = sharedTargets.stream()
                .collect(Collectors.groupingBy(ReceiptDataPort.SharedTargetData::positionId));

        Map<Long, Long> aggregated = new HashMap<>();

        for (var position : positions) {
            Map<Long, BigDecimal> weights = resolveWeights(
                    position,
                    participants,
                    selectionsByPosition.getOrDefault(position.id(), List.of()),
                    sharedTargetsByPosition.getOrDefault(position.id(), List.of())
            );
            if (weights.isEmpty()) {
                throw new PositionHasNoTargetsException();
            }

            var shares = splitKopecks(position.totalPriceKopecks(), weights);
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
        var positions = receiptDataPort.getPositions(eventId);
        var selections = receiptDataPort.getSelections(eventId);
        var sharedTargets = receiptDataPort.getSharedTargets(eventId);

        var selectionsByPosition = selections.stream()
                .collect(Collectors.groupingBy(ReceiptDataPort.SelectionData::positionId));
        var sharedTargetsByPosition = sharedTargets.stream()
                .collect(Collectors.groupingBy(ReceiptDataPort.SharedTargetData::positionId));

        long total = 0;
        for (var position : positions) {
            Map<Long, BigDecimal> weights = resolveWeights(
                    position,
                    participants,
                    selectionsByPosition.getOrDefault(position.id(), List.of()),
                    sharedTargetsByPosition.getOrDefault(position.id(), List.of())
            );
            if (weights.isEmpty()) {
                continue;
            }
            var shares = splitKopecks(position.totalPriceKopecks(), weights);
            total += shares.getOrDefault(userId, 0L);
        }
        return total;
    }

    private boolean isDebtLocked(long eventId, long debtorId) {
        return debtRepository.findByEventIdAndDebtorId(eventId, debtorId)
                .map(debt -> debt.getStatus() != DebtStatus.UNPAID)
                .orElse(false);
    }

    private void requireDistribution(long eventId) {
        if (eventAccessPort.getStatus(eventId) != EventStatus.DISTRIBUTION) {
            throw new EventNotInDistributionException();
        }
    }

    private static Map<Long, BigDecimal> resolveWeights(
            ReceiptDataPort.PositionData position,
            List<Long> participants,
            List<ReceiptDataPort.SelectionData> selections,
            List<ReceiptDataPort.SharedTargetData> targets
    ) {
        Map<Long, BigDecimal> weights = new LinkedHashMap<>();
        if (position.shared()) {
            if (targets.isEmpty()) {
                for (long userId : participants) {
                    weights.put(userId, BigDecimal.ONE);
                }
            } else {
                for (var target : targets) {
                    weights.put(target.userId(), BigDecimal.ONE);
                }
            }
            return weights;
        }
        for (var selection : selections) {
            weights.put(selection.userId(), selection.selectedQuantity());
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
            BigDecimal exact = BigDecimal.valueOf(amountKopecks).multiply(weight).divide(totalWeight, 10, java.math.RoundingMode.DOWN);
            long floor = exact.longValue();
            shares.put(userId, floor);
            allocated += floor;
            remainders.add(new RemainderEntry(userId, exact.subtract(BigDecimal.valueOf(floor))));
        }

        long remainder = amountKopecks - allocated;
        remainders.sort(Comparator
                .comparing(RemainderEntry::fractional).reversed()
                .thenComparing(RemainderEntry::userId));

        for (int i = 0; i < remainder && i < remainders.size(); i++) {
            long userId = remainders.get(i).userId();
            shares.put(userId, shares.get(userId) + 1);
        }
        return shares;
    }

    private Debt toDomain(DebtEntity entity) {
        return new Debt(
                entity.getId(),
                entity.getEventId(),
                entity.getDebtorId(),
                entity.getCreditorId(),
                entity.getAmountKopecks(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private record RemainderEntry(long userId, BigDecimal fractional) {
    }
}
