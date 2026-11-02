package cz.uhk.ppro.horyavoda.infrastructure.seed;

import cz.uhk.ppro.horyavoda.domain.model.Category;
import cz.uhk.ppro.horyavoda.domain.model.Customer;
import cz.uhk.ppro.horyavoda.domain.model.EquipmentItem;
import cz.uhk.ppro.horyavoda.domain.model.ItemStatus;
import cz.uhk.ppro.horyavoda.domain.model.Rental;
import cz.uhk.ppro.horyavoda.domain.model.RentalItem;
import cz.uhk.ppro.horyavoda.domain.model.Reservation;
import cz.uhk.ppro.horyavoda.domain.model.WearCondition;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.CategoryRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.CustomerRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.EquipmentItemRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.RentalRepository;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ukázkový provoz půjčovny pro předvedení a ruční testování: další kusy, zákazníci,
 * výpůjčky (běžící, po termínu s částečným vrácením, uzavřená s penále, budoucí) a rezervace typu.
 * Všechna data jsou syntetická; časy se počítají od okamžiku startu, aby přehledy dávaly smysl.
 * Spustí se jen tehdy, když ještě neexistuje žádná výpůjčka ani rezervace.
 */
@Component
class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final CategoryRepository categoryRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    DemoDataSeeder(
            CategoryRepository categoryRepository,
            EquipmentItemRepository equipmentItemRepository,
            CustomerRepository customerRepository,
            RentalRepository rentalRepository,
            ReservationRepository reservationRepository,
            Clock clock
    ) {
        this.categoryRepository = categoryRepository;
        this.equipmentItemRepository = equipmentItemRepository;
        this.customerRepository = customerRepository;
        this.rentalRepository = rentalRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    void seed() {
        if (rentalRepository.count() > 0 || reservationRepository.count() > 0) {
            return;
        }
        log.info("Vytvářím ukázkový provoz (kusy, zákazníci, výpůjčky, rezervace)...");
        Map<String, EquipmentItem> items = seedItems();
        Map<String, Customer> customers = seedCustomers();
        LocalDateTime now = LocalDateTime.now(clock).withSecond(0).withNano(0);
        seedRentals(items, customers, now);
        seedReservations(customers, now);
    }

    // --- Kusy ---

    private Map<String, EquipmentItem> seedItems() {
        Category ski = requireCategory("SKI");
        Category snowboard = requireCategory("SNOWBOARD");
        Category boots = requireCategory("BOOTS");
        Category xcSki = requireCategory("XC_SKI");
        Category poles = requireCategory("POLES");
        Category boat = requireCategory("BOAT");
        Category vests = categoryRepository.findByCode("VEST").orElseGet(() -> categoryRepository.save(new Category(
                "Plovací vesty", "VEST", "Vesty k lodím a raftům, velikosti S až XL",
                new BigDecimal("60.00"), new BigDecimal("100.00"))));

        List<EquipmentItem> demoItems = List.of(
                item(ski, "SKI-005", "Elan Wingman 82", "170 cm", 2024, WearCondition.EXCELLENT),
                item(ski, "SKI-006", "Elan Wingman 82", "170 cm", 2024, WearCondition.GOOD),
                item(ski, "SKI-007", "Elan Wingman 82", "170 cm", 2023, WearCondition.GOOD),
                item(ski, "SKI-008", "Rossignol Experience 80", "170 cm", 2022, WearCondition.FAIR),
                item(ski, "SKI-009", "Rossignol Experience 80", "160 cm", 2023, WearCondition.GOOD),
                item(ski, "SKI-010", "Rossignol Experience 80", "160 cm", 2023, WearCondition.GOOD),
                item(ski, "SKI-011", "Völkl Deacon 76", "165 cm", 2025, WearCondition.NEW),
                item(ski, "SKI-012", "Völkl Deacon 76", "175 cm", 2025, WearCondition.NEW),
                item(snowboard, "SNB-002", "Nitro Team", "155 cm", 2023, WearCondition.GOOD),
                item(snowboard, "SNB-003", "Nitro Team", "155 cm", 2023, WearCondition.EXCELLENT),
                item(snowboard, "SNB-004", "Burton Process", "150 cm", 2021, WearCondition.FAIR),
                item(boots, "BOT-002", "Salomon S/Pro 90", "EU 42", 2024, WearCondition.GOOD),
                item(boots, "BOT-003", "Salomon S/Pro 90", "EU 42", 2024, WearCondition.GOOD),
                item(boots, "BOT-004", "Salomon S/Pro 90", "EU 41", 2023, WearCondition.EXCELLENT),
                item(boots, "BOT-005", "Head Edge 85", "EU 44", 2022, WearCondition.FAIR),
                item(boots, "BOT-006", "Head Edge 85", "EU 40", 2022, WearCondition.GOOD),
                item(xcSki, "XC-001", "Fischer Twin Skin", "190 cm", 2024, WearCondition.GOOD),
                item(xcSki, "XC-002", "Fischer Twin Skin", "200 cm", 2024, WearCondition.EXCELLENT),
                item(poles, "POL-001", "Leki Neolite", "120 cm", 2023, WearCondition.GOOD),
                item(poles, "POL-002", "Leki Neolite", "120 cm", 2023, WearCondition.GOOD),
                item(poles, "POL-003", "Leki Neolite", "125 cm", 2022, WearCondition.FAIR),
                item(poles, "POL-004", "Leki Neolite", "130 cm", 2024, WearCondition.NEW),
                item(boat, "BOA-001", "Pálava 2místná", "2 osoby", 2021, WearCondition.GOOD),
                item(boat, "BOA-002", "Raft Colorado", "6 osob", 2020, WearCondition.FAIR),
                item(vests, "VES-001", "Hiko Buffer", "M", 2024, WearCondition.GOOD),
                item(vests, "VES-002", "Hiko Buffer", "L", 2024, WearCondition.GOOD)
        );
        List<EquipmentItem> missing = demoItems.stream()
                .filter(candidate -> !equipmentItemRepository.existsByInventoryNumber(candidate.getInventoryNumber()))
                .toList();
        equipmentItemRepository.saveAll(missing);

        // Jeden dosloužilý kus pro ukázku vyřazení (zůstává v evidenci kvůli historii)
        if (!equipmentItemRepository.existsByInventoryNumber("SNB-099")) {
            equipmentItemRepository.save(new EquipmentItem(snowboard, "SNB-099", "Burton Clash", "152 cm", 2016,
                    WearCondition.DAMAGED, ItemStatus.RETIRED));
        }

        Map<String, EquipmentItem> byNumber = new HashMap<>();
        equipmentItemRepository.findAll().forEach(item -> byNumber.put(item.getInventoryNumber(), item));
        return byNumber;
    }

    private static EquipmentItem item(Category category, String number, String model, String size, int year, WearCondition wear) {
        return new EquipmentItem(category, number, model, size, year, wear, ItemStatus.AVAILABLE);
    }

    // --- Zákazníci ---

    private Map<String, Customer> seedCustomers() {
        List<Customer> demoCustomers = List.of(
                new Customer("Eva", "Kučerová", "+420601000101", "OP90000101"),
                new Customer("Pavel", "Procházka", "+420601000102", "OP90000102"),
                new Customer("Lucie", "Černá", "+420601000103", "OP90000103"),
                new Customer("Tomáš", "Dvořák", "+420601000104", "OP90000104"),
                new Customer("Klára", "Benešová", "+420601000105", "OP90000105"),
                new Customer("Ondřej", "Horák", "+420601000106", "OP90000106")
        );
        demoCustomers.stream()
                .filter(customer -> !customerRepository.existsByIdDocumentNumber(customer.getIdDocumentNumber()))
                .forEach(customerRepository::save);

        Map<String, Customer> byLastName = new HashMap<>();
        customerRepository.findAll().forEach(customer -> byLastName.put(customer.getLastName(), customer));
        return byLastName;
    }

    // --- Výpůjčky ---

    private void seedRentals(Map<String, EquipmentItem> items, Map<String, Customer> customers, LocalDateTime now) {
        LocalDate today = now.toLocalDate();

        // Běžící víkendová výpůjčka: vydáno včera, vrátit zítra večer
        Rental running = new Rental(customers.get("Kučerová"),
                today.minusDays(1).atTime(9, 0), today.plusDays(1).atTime(18, 0), BigDecimal.ZERO);
        addItems(running, items, "SKI-005", "BOT-003", "POL-002");
        running.handOverItems();
        rentalRepository.save(running);

        // Po termínu a vráceno po částech: lyže včas, boty a hole pořád venku
        Rental overdue = new Rental(customers.get("Procházka"),
                today.minusDays(3).atTime(9, 0), today.minusDays(1).atTime(18, 0), BigDecimal.ZERO);
        addItems(overdue, items, "SKI-006", "BOT-004", "POL-003");
        overdue.handOverItems();
        rentalRepository.saveAndFlush(overdue);
        returnItem(overdue, "SKI-006", today.minusDays(1).atTime(17, 30));

        // Uzavřená výpůjčka se slevou; boty se vrátily o den později → penále za 2 započaté dny
        Rental closed = new Rental(customers.get("Černá"),
                today.minusDays(10).atTime(9, 0), today.minusDays(8).atTime(18, 0), BigDecimal.ZERO);
        closed.setDiscountAmount(new BigDecimal("50.00"));
        addItems(closed, items, "SNB-002", "BOT-005");
        closed.handOverItems();
        rentalRepository.saveAndFlush(closed);
        returnItem(closed, "SNB-002", today.minusDays(8).atTime(16, 45));
        returnItem(closed, "BOT-005", today.minusDays(7).atTime(19, 10));

        // Dopředu sjednaná výpůjčka konkrétního kusu na příští víkend
        LocalDate nextFriday = today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        Rental future = new Rental(customers.get("Dvořák"),
                nextFriday.atTime(15, 0), nextFriday.plusDays(2).atTime(18, 0), BigDecimal.ZERO);
        addItems(future, items, "SKI-012");
        rentalRepository.save(future);
    }

    private static void addItems(Rental rental, Map<String, EquipmentItem> items, String... inventoryNumbers) {
        for (String number : inventoryNumbers) {
            EquipmentItem item = items.get(number);
            rental.addItem(item, item.getCategory().getBaseDailyRate());
        }
    }

    private static void returnItem(Rental rental, String inventoryNumber, LocalDateTime returnedAt) {
        RentalItem item = rental.getItems().stream()
                .filter(candidate -> candidate.getEquipmentItem().getInventoryNumber().equals(inventoryNumber))
                .findFirst()
                .orElseThrow();
        rental.returnItem(item.getId(), returnedAt);
    }

    // --- Rezervace typu ---

    private void seedReservations(Map<String, Customer> customers, LocalDateTime now) {
        Category ski = requireCategory("SKI");
        Category boots = requireCategory("BOOTS");
        Category snowboard = requireCategory("SNOWBOARD");
        LocalDate today = now.toLocalDate();

        // Vyzvednutí ještě dnes (ráno do 10:00, odpoledne nejpozději za dvě hodiny), jinak zítra ráno
        LocalDateTime pickup = now.toLocalTime().isBefore(LocalTime.of(8, 30))
                ? today.atTime(8, 30)
                : now.plusHours(2).withMinute(0);
        if (!pickup.toLocalDate().equals(today) || pickup.getHour() >= 20) {
            pickup = today.plusDays(1).atTime(8, 30);
        }
        reservationRepository.save(new Reservation(customers.get("Benešová"), ski, "160 cm", 2,
                pickup, pickup.toLocalDate().plusDays(1).atTime(18, 0), now));
        reservationRepository.save(new Reservation(customers.get("Benešová"), boots, "EU 42", 1,
                pickup, pickup.toLocalDate().plusDays(1).atTime(18, 0), now));

        // Telefonická rezervace na sobotu: „lyže sto sedmdesát“
        LocalDate saturday = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        reservationRepository.save(new Reservation(customers.get("Horák"), ski, "170 cm", 1,
                saturday.atTime(8, 0), saturday.plusDays(1).atTime(18, 0), now));
        reservationRepository.save(new Reservation(customers.get("Horák"), snowboard, "155 cm", 1,
                saturday.atTime(8, 0), saturday.plusDays(1).atTime(18, 0), now));
    }

    private Category requireCategory(String code) {
        return categoryRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Seed: chybí kategorie s kódem " + code));
    }
}
