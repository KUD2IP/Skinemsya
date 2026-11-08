package skinemsya.vse.ru.subscriptions.api.dto;

import jakarta.validation.constraints.NotNull;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionProvider;

public record SubscriptionCheckoutRequest(@NotNull SubscriptionProvider method, Boolean sbp) {}
