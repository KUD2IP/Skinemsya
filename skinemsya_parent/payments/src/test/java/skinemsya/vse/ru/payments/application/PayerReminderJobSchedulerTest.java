package skinemsya.vse.ru.payments.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PayerReminderJobSchedulerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PayerReminderJobScheduler scheduler;

    @Test
    void shouldNotPublishDebtorConfirmed() {
        scheduler.processDueReminders();

        verify(eventPublisher, never()).publishEvent(any());
        verifyNoInteractions(eventPublisher);
    }
}
