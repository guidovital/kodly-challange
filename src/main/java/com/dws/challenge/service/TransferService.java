package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.exception.InvalidAccountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Optional;

@Service
public class TransferService {
    private final AccountsService accountsService;
    private final NotificationService notificationService;

    @Autowired
    public TransferService(final AccountsService accountsService, final NotificationService notificationService) {
        this.accountsService = accountsService;
        this.notificationService = notificationService;
    }

    public void processTransferRequest(final String accountFromId, final String accountToId, final BigDecimal amount) {
        final Account accountFrom = Optional.ofNullable(accountsService.getAccount(accountFromId))
                .orElseThrow(() -> new InvalidAccountException("Invalid account from ID"));

        final Account accountTo = Optional.ofNullable(accountsService.getAccount(accountToId))
                .orElseThrow(() -> new InvalidAccountException("Invalid account to ID"));

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount can not be negative.");
        }

        transferMoney(amount, accountFrom, accountTo);

        // Notify account holders
        notifyTransfer(accountFrom, accountTo, amount);
    }

    private static void transferMoney(BigDecimal amount, Account accountFrom, Account accountTo) {
        synchronized (TransferService.class) {
            if (accountFrom.getBalance().compareTo(amount) >= 0) {
                // Deduct amount from the sender's account
                accountFrom.setBalance(accountFrom.getBalance().subtract(amount));

                // Add amount to the receiver's account
                accountTo.setBalance(accountTo.getBalance().add(amount));
            } else {
                // Handle insufficient funds
                throw new InsufficientFundsException("There's no money enough.");
            }
        }
    }

    private void notifyTransfer(Account accountFrom, Account accountTo, BigDecimal amount) {
        // Implement notification logic using NotificationService interface
        // You can use dependency injection to inject NotificationService into TransferService class
        final String SENDING_ACCOUNT_MESSAGE = MessageFormat.format("You sent {0} to {1}", amount, accountFrom.getAccountId());
        notificationService.notifyAboutTransfer(
                accountFrom,
                SENDING_ACCOUNT_MESSAGE
        );

        final String RECEIVING_ACCOUNT_MESSAGE = MessageFormat.format("You received {0} from {1}", amount, accountTo.getAccountId());
        notificationService.notifyAboutTransfer(
                accountTo,
                RECEIVING_ACCOUNT_MESSAGE
        );
    }
}

