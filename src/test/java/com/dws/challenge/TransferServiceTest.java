package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.exception.InvalidAccountException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import com.dws.challenge.service.TransferService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private AccountsService accountsService;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private TransferService transferService;

    @Test
    public void testValidTransfer() {
        String accountFromId = "1";
        String accountToId = "2";
        Account accountFrom = new Account(accountFromId, new BigDecimal("100"));
        Account accountTo = new Account(accountToId, new BigDecimal("50"));

        when(accountsService.getAccount(accountFromId)).thenReturn(accountFrom);
        when(accountsService.getAccount(accountToId)).thenReturn(accountTo);

        BigDecimal amount = new BigDecimal("30");
        transferService.processTransferRequest(accountFromId, accountToId, amount);

        assertEquals(new BigDecimal("70"), accountFrom.getBalance());
        assertEquals(new BigDecimal("80"), accountTo.getBalance());

        verify(notificationService, times(2)).notifyAboutTransfer(any(), any());
    }

    @Test
    public void testTransferWithInsufficientFunds() {
        String accountIdFrom = "1";
        String accountIdTo = "2";
        Account accountFrom = new Account(accountIdFrom, new BigDecimal("20"));
        Account accountTo = new Account(accountIdTo, new BigDecimal("50"));

        when(accountsService.getAccount(accountIdFrom)).thenReturn(accountFrom);
        when(accountsService.getAccount(accountIdTo)).thenReturn(accountTo);

        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class, () ->
                transferService.processTransferRequest(accountIdFrom, accountIdTo, new BigDecimal("30")));

        assertEquals(new BigDecimal("20"), accountFrom.getBalance());
        assertEquals(new BigDecimal("50"), accountTo.getBalance());
        assertEquals("There's no money enough.", exception.getMessage());

        // No notification should be sent
        verifyNoInteractions(notificationService);
    }

    @Test
    public void testTransferWithInvalidAccountFrom() {
        String accountIdFrom = "1";
        String accountIdTo = "2";
        when(accountsService.getAccount(accountIdFrom)).thenReturn(null);

        InvalidAccountException exception = assertThrows(InvalidAccountException.class, () -> {
            transferService.processTransferRequest(accountIdFrom, accountIdTo, new BigDecimal("30"));
        });

        assertEquals("Invalid account from ID", exception.getMessage());

        verify(accountsService).getAccount(accountIdFrom);
        verifyNoInteractions(notificationService);
    }

    @Test
    public void testTransferWithInvalidAccountTo() {
        String accountIdFrom = "1";
        String accountIdTo = "2";
        Account accountFrom = new Account(accountIdFrom, new BigDecimal("20"));
        when(accountsService.getAccount(accountIdFrom)).thenReturn(accountFrom);
        when(accountsService.getAccount(accountIdTo)).thenReturn(null);

        InvalidAccountException exception = assertThrows(InvalidAccountException.class, () -> {
            transferService.processTransferRequest(accountIdFrom, accountIdTo, new BigDecimal("30"));
        });

        assertEquals("Invalid account to ID", exception.getMessage());

        verify(accountsService, times(2)).getAccount(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    public void testTransferWithNegativeAmount() {
        String accountIdFrom = "1";
        String accountIdTo = "2";
        Account accountFrom = new Account(accountIdFrom, new BigDecimal("100"));
        Account accountTo = new Account(accountIdTo, new BigDecimal("50"));

        when(accountsService.getAccount(accountIdFrom)).thenReturn(accountFrom);
        when(accountsService.getAccount(accountIdTo)).thenReturn(accountTo);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transferService.processTransferRequest(accountIdFrom, accountIdTo, new BigDecimal(-30));
        });

        assertEquals("Amount can not be negative.", exception.getMessage());
    }
}

