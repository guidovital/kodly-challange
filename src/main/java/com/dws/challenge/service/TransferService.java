package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.exception.InvalidAccountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TransferService {
    private final Map<String, Lock> accountLocks = new ConcurrentHashMap<>();
    private final AccountsService accountsService;
    private final NotificationService notificationService;

    @Autowired
    public TransferService(final AccountsService accountsService, final NotificationService notificationService) {
        this.accountsService = accountsService;
        this.notificationService = notificationService;
    }

    public void processTransferRequest(final String accountFromId, final String accountToId, final BigDecimal amount) {
        if (accountFromId.equals(accountToId)) {
            throw new IllegalArgumentException("The accounts have to be different.");
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount can not be negative.");
        }

        final Account accountFrom = Optional.ofNullable(accountsService.getAccount(accountFromId))
                .orElseThrow(() -> new InvalidAccountException("Invalid account from ID"));

        if (accountFrom.getBalance().compareTo(amount) <= 0) {
            // Handle insufficient funds
            throw new InsufficientFundsException("There's no money enough.");
        }

        final Account accountTo = Optional.ofNullable(accountsService.getAccount(accountToId))
                .orElseThrow(() -> new InvalidAccountException("Invalid account to ID"));

        transferMoney(amount, accountFrom, accountTo);

        // Notify account holders
        notifyTransfer(accountFrom, accountTo, amount);
    }

    private void transferMoney(BigDecimal amount, Account accountFrom, Account accountTo) {
        Lock lock = accountLocks.computeIfAbsent(accountFrom.getAccountId(), accountFromId -> new ReentrantLock());

        lock.lock();

        try {
            // Deduct amount from the sender's account
            accountFrom.setBalance(accountFrom.getBalance().subtract(amount));

            // Add amount to the receiver's account
            accountTo.setBalance(accountTo.getBalance().add(amount));
        } finally {
            lock.unlock();
        }
    }

    private void notifyTransfer(Account accountFrom, Account accountTo, BigDecimal amount) {
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

