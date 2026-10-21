package cz.uhk.ppro.horyavoda.infrastructure.seed;

import cz.uhk.ppro.horyavoda.domain.model.Category;
import cz.uhk.ppro.horyavoda.domain.model.Customer;
import cz.uhk.ppro.horyavoda.domain.model.EquipmentItem;
import cz.uhk.ppro.horyavoda.domain.model.ItemStatus;
import cz.uhk.ppro.horyavoda.domain.model.User;
import cz.uhk.ppro.horyavoda.domain.model.UserRole;
import cz.uhk.ppro.horyavoda.domain.model.WearCondition;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.CategoryRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.CustomerRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.EquipmentItemRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Naplnění databáze syntetickými daty (žádná reálná osobní data).
 * Spouští se při startu aplikace; každá tabulka se plní jen tehdy, je-li prázdná.
 */
@Component
@EnableConfigurationProperties(SeedProperties.class)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SeedProperties seedProperties;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            SeedProperties seedProperties,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            EquipmentItemRepository equipmentItemRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.seedProperties = seedProperties;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.equipmentItemRepository = equipmentItemRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedProperties.enabled()) {
            return;
        }
        seedUsers();
        seedCategories();
        seedEquipment();
        seedCustomers();
        log.info("Syntetická data byla úspěšně nahrána.");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        List<User> users = new ArrayList<>();
        if (hasText(seedProperties.adminPassword())) {
            users.add(new User("admin", "admin@horyavoda.local", passwordEncoder.encode(seedProperties.adminPassword()),
                    "Martin Řehák (Správce)", UserRole.ADMIN));
        }
        if (hasText(seedProperties.staffPassword())) {
            users.add(new User("recepce", "recepce@horyavoda.local", passwordEncoder.encode(seedProperties.staffPassword()),
                    "Obsluha Pultu", UserRole.STAFF));
        }
        if (users.isEmpty()) {
            log.warn("SEED_ADMIN_PASSWORD ani SEED_STAFF_PASSWORD nejsou nastaveny – výchozí uživatelé nebudou vytvořeni.");
            return;
        }
        log.info("Vytvářím výchozí uživatele...");
        userRepository.saveAll(users);
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }
        log.info("Vytvářím výchozí kategorie vybavení...");
        categoryRepository.saveAll(List.of(
                new Category("Sjezdové lyže", "SKI", "Carvingové a all-mountain sjezdové lyže různých délek",
                        new BigDecimal("450.00"), new BigDecimal("800.00")),
                new Category("Snowboardy", "SNOWBOARD", "Freestyle a all-mountain snowboardy včetně vázání",
                        new BigDecimal("400.00"), new BigDecimal("700.00")),
                new Category("Lyžařské boty", "BOOTS", "Sjezdová obuv s nastavitelnými přezkami",
                        new BigDecimal("180.00"), new BigDecimal("300.00")),
                new Category("Běžecké lyže", "XC_SKI", "Běžky na klasiku i bruslení",
                        new BigDecimal("350.00"), new BigDecimal("600.00")),
                new Category("Hole", "POLES", "Sjezdové a běžecké hole",
                        new BigDecimal("80.00"), new BigDecimal("140.00")),
                new Category("Nafukovací lodě a rafty", "BOAT", "Letní vybavení: 2-místné Pálavy a 6-místné rafty",
                        new BigDecimal("650.00"), new BigDecimal("1150.00"))
        ));
    }

    private void seedEquipment() {
        if (equipmentItemRepository.count() > 0) {
            return;
        }
        log.info("Vytvářím ukázkové kusy vybavení...");
        Category ski = requireCategory("SKI");
        Category snowboard = requireCategory("SNOWBOARD");
        Category boots = requireCategory("BOOTS");
        equipmentItemRepository.saveAll(List.of(
                new EquipmentItem(ski, "SKI-001", "Atomic Redster G9", "175 cm", 2023, WearCondition.EXCELLENT, ItemStatus.AVAILABLE),
                new EquipmentItem(ski, "SKI-002", "Atomic Redster G9", "175 cm", 2023, WearCondition.GOOD, ItemStatus.AVAILABLE),
                new EquipmentItem(ski, "SKI-003", "Salomon S/Max 10", "165 cm", 2024, WearCondition.NEW, ItemStatus.AVAILABLE),
                // Ukázka servisní blokace
                new EquipmentItem(ski, "SKI-004", "Head Super Joy (Dámské)", "153 cm", 2022, WearCondition.FAIR, ItemStatus.IN_SERVICE),
                new EquipmentItem(snowboard, "SNB-001", "Burton Custom Flying V", "158 cm", 2023, WearCondition.GOOD, ItemStatus.AVAILABLE),
                new EquipmentItem(boots, "BOT-001", "Nordica Sportmachine 100", "MP 28.5 (EU 44)", 2023, WearCondition.GOOD, ItemStatus.AVAILABLE)
        ));
    }

    private void seedCustomers() {
        if (customerRepository.count() > 0) {
            return;
        }
        log.info("Vytvářím ukázkové zákazníky...");
        customerRepository.saveAll(List.of(
                new Customer("Jan", "Novák", "+420777111222", "OP12345678"),
                new Customer("Petra", "Svobodová", "+420777333444", "OP87654321")
        ));
    }

    private Category requireCategory(String code) {
        return categoryRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Seed: chybí kategorie s kódem " + code));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
