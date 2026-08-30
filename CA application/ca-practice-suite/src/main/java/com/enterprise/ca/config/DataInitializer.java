package com.enterprise.ca.config;

import com.enterprise.ca.entity.*;
import com.enterprise.ca.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/*
 * PURPOSE: First-boot seed data so the product is usable immediately -
 * roles, an admin login, a standard chart of accounts (the ledger heads
 * every small business needs), and one sample client with a few ledger
 * entries and an upcoming GST deadline so the dashboard isn't empty on
 * first login. Controlled by app.seed-data and guarded by an emptiness
 * check so it never re-runs against real data.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final ClientRepository clientRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ComplianceTaskRepository complianceTaskRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-data:true}")
    private boolean seedData;

    @Override
    public void run(String... args) {
        if (!seedData) return;

        Role admin = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Firm partner/owner - full access").build()));
        Role accountant = roleRepository.findByName("ROLE_ACCOUNTANT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ACCOUNTANT").description("Day-to-day bookkeeping").build()));

        if (userRepository.findByUsername("admin").isEmpty()) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .email("admin@capracticesuite.local")
                    .fullName("Practice Admin")
                    .enabled(true)
                    .roles(Set.of(admin, accountant))
                    .build());
        }

        if (chartOfAccountRepository.count() == 0) {
            seedChartOfAccounts();
        }

        if (clientRepository.count() == 0) {
            seedSampleClientAndData();
        }
    }

    private void seedChartOfAccounts() {
        save("Sales Revenue", "SALES", ChartOfAccount.AccountType.INCOME);
        save("Service Income", "SERVICE", ChartOfAccount.AccountType.INCOME);
        save("Purchases / Cost of Goods", "PURCH", ChartOfAccount.AccountType.EXPENSE);
        save("Office Rent", "RENT", ChartOfAccount.AccountType.EXPENSE);
        save("Salaries & Wages", "SALARY", ChartOfAccount.AccountType.EXPENSE);
        save("Professional Fees", "PROFFEE", ChartOfAccount.AccountType.EXPENSE);
        save("Utilities", "UTIL", ChartOfAccount.AccountType.EXPENSE);
        save("Bank & Cash", "BANK", ChartOfAccount.AccountType.ASSET);
        save("Accounts Receivable", "AR", ChartOfAccount.AccountType.ASSET);
        save("Accounts Payable", "AP", ChartOfAccount.AccountType.LIABILITY);
        save("GST Payable", "GSTPAY", ChartOfAccount.AccountType.LIABILITY);
        save("Owner's Capital", "CAPITAL", ChartOfAccount.AccountType.EQUITY);
    }

    private void save(String name, String code, ChartOfAccount.AccountType type) {
        chartOfAccountRepository.save(ChartOfAccount.builder().name(name).code(code).accountType(type).active(true).build());
    }

    private void seedSampleClientAndData() {
        Client client = clientRepository.save(Client.builder()
                .name("Sunrise Traders Pvt Ltd")
                .clientType(Client.ClientType.PRIVATE_LIMITED)
                .gstin("29ABCDE1234F1Z5")
                .pan("ABCDE1234F")
                .email("accounts@sunrisetraders.example")
                .phone("9876543210")
                .city("Bengaluru")
                .state("Karnataka")
                .active(true)
                .build());

        ChartOfAccount sales = chartOfAccountRepository.findAll().stream().filter(a -> a.getCode().equals("SALES")).findFirst().orElseThrow();
        ChartOfAccount rent = chartOfAccountRepository.findAll().stream().filter(a -> a.getCode().equals("RENT")).findFirst().orElseThrow();

        ledgerEntryRepository.save(LedgerEntry.builder()
                .client(client).account(sales)
                .entryType(LedgerEntry.EntryType.CREDIT)
                .entryDate(LocalDate.now().minusDays(10))
                .amount(new BigDecimal("50000"))
                .gstRate(new BigDecimal("18"))
                .gstAmount(new BigDecimal("9000"))
                .totalAmount(new BigDecimal("59000"))
                .description("Sample sale - onboarding demo entry")
                .reconciled(false)
                .createdBy("system")
                .build());

        ledgerEntryRepository.save(LedgerEntry.builder()
                .client(client).account(rent)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .entryDate(LocalDate.now().minusDays(5))
                .amount(new BigDecimal("15000"))
                .gstRate(BigDecimal.ZERO)
                .gstAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("15000"))
                .description("Sample office rent - onboarding demo entry")
                .reconciled(false)
                .createdBy("system")
                .build());

        complianceTaskRepository.save(ComplianceTask.builder()
                .title("GSTR-3B - " + LocalDate.now().getMonth())
                .taskType(ComplianceTask.TaskType.GSTR3B)
                .client(client)
                .frequency(ComplianceTask.Frequency.MONTHLY)
                .dueDate(LocalDate.now().plusDays(7))
                .status(ComplianceTask.TaskStatus.PENDING)
                .assignedTo("admin")
                .build());
    }
}
