package com.helvino.ecommerce.config;

import com.helvino.ecommerce.entity.Category;
import com.helvino.ecommerce.entity.DeliveryZone;
import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.UserRole;
import com.helvino.ecommerce.repository.CategoryRepository;
import com.helvino.ecommerce.repository.DeliveryZoneRepository;
import com.helvino.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureAdminExists();
        enableAllDisabledUsers();
        seedDefaultCategories();
        seedDeliveryZones();
    }

    private void ensureAdminExists() {
        String email    = env("ADMIN_EMAIL",    "info@helvino.org");
        String password = env("ADMIN_PASSWORD", "Helvino@2026");
        String phone    = env("ADMIN_PHONE",    "0110421320");

        userRepository.findByEmail(email).ifPresentOrElse(
            existing -> {
                boolean dirty = false;
                if (!existing.isEnabled())                         { existing.setEnabled(true);                       dirty = true; }
                if (!existing.isEmailVerified())                   { existing.setEmailVerified(true);                 dirty = true; }
                if (existing.getRole() != UserRole.SUPER_ADMIN)   { existing.setRole(UserRole.SUPER_ADMIN);           dirty = true; }
                existing.setPassword(passwordEncoder.encode(password));
                dirty = true;
                if (dirty) {
                    userRepository.save(existing);
                    log.info("Admin user updated and enabled: {}", email);
                }
            },
            () -> {
                User admin = User.builder()
                        .firstName("Helvino")
                        .lastName("Admin")
                        .email(email)
                        .phone(phone)
                        .password(passwordEncoder.encode(password))
                        .role(UserRole.SUPER_ADMIN)
                        .enabled(true)
                        .emailVerified(true)
                        .phoneVerified(true)
                        .walletBalance(BigDecimal.ZERO)
                        .loyaltyPoints(0)
                        .build();
                userRepository.save(admin);
                log.info("Admin user created: {}", email);
            }
        );
    }

    private void enableAllDisabledUsers() {
        var disabled = userRepository.findAll().stream()
                .filter(u -> !u.isEnabled())
                .toList();

        if (!disabled.isEmpty()) {
            disabled.forEach(u -> u.setEnabled(true));
            userRepository.saveAll(disabled);
            log.info("Re-enabled {} user account(s) that were incorrectly disabled", disabled.size());
        }
    }

    private void seedDefaultCategories() {
        if (categoryRepository.count() > 0) return;

        List<String[]> cats = List.of(
            new String[]{"Electronics",           "electronics"},
            new String[]{"Fashion",               "fashion"},
            new String[]{"Home & Garden",         "home-garden"},
            new String[]{"Beauty & Personal Care","beauty"},
            new String[]{"Sports & Outdoors",     "sports"},
            new String[]{"Groceries & Food",      "groceries"},
            new String[]{"Books & Education",     "books"},
            new String[]{"Toys & Games",          "toys"},
            new String[]{"Automotive",            "automotive"},
            new String[]{"Health & Wellness",     "health"},
            new String[]{"Phone Accessories",     "phone-accessories"},
            new String[]{"Computers & Laptops",   "computers-laptops"}
        );

        cats.forEach(c -> {
            Category cat = Category.builder()
                    .name(c[0])
                    .slug(c[1])
                    .active(true)
                    .sortOrder(cats.indexOf(c))
                    .build();
            categoryRepository.save(cat);
        });
        log.info("Seeded {} default categories", cats.size());
    }

    private void seedDeliveryZones() {
        if (deliveryZoneRepository.count() > 0) return;

        // cost tiers
        BigDecimal NAIROBI_COST   = new BigDecimal("150");
        BigDecimal NEAR_COST      = new BigDecimal("300");
        BigDecimal STANDARD_COST  = new BigDecimal("500");
        BigDecimal REMOTE_COST    = new BigDecimal("800");

        // days tiers
        int NAIROBI_DAYS   = 1;
        int NEAR_DAYS      = 2;
        int STANDARD_DAYS  = 3;
        int REMOTE_DAYS    = 5;

        List<CountyData> counties = List.of(
            // ---- Nairobi (KSh 150, 1 day) ----
            cd("Nairobi", NAIROBI_COST, NAIROBI_DAYS,
                "CBD", "Westlands", "Karen", "Embakasi", "Kasarani",
                "Langata", "Dagoretti", "Ruaraka", "Makadara", "Starehe",
                "Roysambu", "Kibra"),

            // ---- Near Nairobi counties (KSh 300, 2 days) ----
            cd("Kiambu", NEAR_COST, NEAR_DAYS,
                "Kiambu Town", "Thika", "Ruiru", "Limuru", "Kikuyu",
                "Gatundu", "Juja", "Lari", "Kabete"),
            cd("Machakos", NEAR_COST, NEAR_DAYS,
                "Machakos Town", "Athi River", "Kangundo", "Mavoko", "Yatta",
                "Mwala", "Masinga", "Matungulu"),
            cd("Kajiado", NEAR_COST, NEAR_DAYS,
                "Kajiado Town", "Ngong", "Kitengela", "Ongata Rongai", "Loitoktok",
                "Namanga", "Magadi"),
            cd("Murang'a", NEAR_COST, NEAR_DAYS,
                "Murang'a Town", "Kangema", "Kiharu", "Kandara", "Gatanga",
                "Maragwa", "Mathioya"),
            cd("Nyeri", NEAR_COST, NEAR_DAYS,
                "Nyeri Town", "Othaya", "Mukurweini", "Kieni", "Mathira",
                "Tetu", "Gichugu"),
            cd("Nakuru", NEAR_COST, NEAR_DAYS,
                "Nakuru Town", "Naivasha", "Gilgil", "Molo", "Njoro",
                "Rongai", "Subukia", "Kuresoi"),

            // ---- Standard counties (KSh 500, 3 days) ----
            cd("Mombasa", STANDARD_COST, STANDARD_DAYS,
                "Mombasa Island", "Nyali", "Bamburi", "Likoni", "Kisauni",
                "Changamwe", "Jomvu"),
            cd("Kwale", STANDARD_COST, STANDARD_DAYS,
                "Kwale Town", "Ukunda", "Diani", "Lungalunga", "Msambweni",
                "Kinango"),
            cd("Kilifi", STANDARD_COST, STANDARD_DAYS,
                "Kilifi Town", "Malindi", "Mtwapa", "Kaloleni", "Rabai",
                "Ganze", "Magarini"),
            cd("Tana River", STANDARD_COST, STANDARD_DAYS,
                "Hola", "Garsen", "Bura", "Madogo", "Kipini",
                "Galole"),
            cd("Lamu", STANDARD_COST, STANDARD_DAYS,
                "Lamu Town", "Mpeketoni", "Mokowe", "Hindi", "Faza",
                "Witu"),
            cd("Taita Taveta", STANDARD_COST, STANDARD_DAYS,
                "Voi", "Wundanyi", "Mwatate", "Taveta", "Maktau",
                "Kasigau"),
            cd("Isiolo", STANDARD_COST, STANDARD_DAYS,
                "Isiolo Town", "Merti", "Garbatulla", "Kina", "Oldonyiro",
                "Kula Mawe"),
            cd("Meru", STANDARD_COST, STANDARD_DAYS,
                "Meru Town", "Nkubu", "Maua", "Tigania", "Igembe",
                "Timau", "Mikinduri"),
            cd("Tharaka Nithi", STANDARD_COST, STANDARD_DAYS,
                "Chuka", "Marimanti", "Kathwana", "Chiakariga", "Tharaka",
                "Nithi"),
            cd("Embu", STANDARD_COST, STANDARD_DAYS,
                "Embu Town", "Runyenjes", "Mbeere", "Ena", "Kiritiri",
                "Siakago"),
            cd("Kitui", STANDARD_COST, STANDARD_DAYS,
                "Kitui Town", "Mwingi", "Mutomo", "Tseikuru", "Kabati",
                "Nuu"),
            cd("Makueni", STANDARD_COST, STANDARD_DAYS,
                "Wote", "Sultan Hamud", "Makindu", "Kibwezi", "Emali",
                "Kathonzweni"),
            cd("Nyandarua", STANDARD_COST, STANDARD_DAYS,
                "Ol Kalou", "Engineer", "Njabini", "Ndaragwa", "Kinangop",
                "Kipipiri"),
            cd("Kirinyaga", STANDARD_COST, STANDARD_DAYS,
                "Kerugoya", "Kutus", "Kagio", "Mwea", "Kianyaga",
                "Baricho"),
            cd("West Pokot", STANDARD_COST, STANDARD_DAYS,
                "Kapenguria", "Kitale", "Ortum", "Sigor", "Kacheliba",
                "Alale"),
            cd("Samburu", STANDARD_COST, STANDARD_DAYS,
                "Maralal", "Wamba", "Baragoi", "Archer's Post", "Suguta Marmar",
                "Loosuk"),
            cd("Trans Nzoia", STANDARD_COST, STANDARD_DAYS,
                "Kitale", "Endebess", "Saboti", "Kwanza", "Cherangany",
                "Kiminini"),
            cd("Uasin Gishu", STANDARD_COST, STANDARD_DAYS,
                "Eldoret", "Turbo", "Moiben", "Ainabkoi", "Kapseret",
                "Soy"),
            cd("Elgeyo Marakwet", STANDARD_COST, STANDARD_DAYS,
                "Iten", "Kamariny", "Cherangany", "Marakwet East", "Marakwet West",
                "Keiyo South"),
            cd("Nandi", STANDARD_COST, STANDARD_DAYS,
                "Kapsabet", "Nandi Hills", "Chesumei", "Aldai", "Tindiret",
                "Emgwen"),
            cd("Baringo", STANDARD_COST, STANDARD_DAYS,
                "Kabarnet", "Eldama Ravine", "Marigat", "Mogotio", "Tiaty",
                "Baringo North"),
            cd("Laikipia", STANDARD_COST, STANDARD_DAYS,
                "Nanyuki", "Rumuruti", "Nyahururu", "Laikipia West", "Laikipia North",
                "Ol Pejeta"),
            cd("Narok", STANDARD_COST, STANDARD_DAYS,
                "Narok Town", "Kilgoris", "Emurua Dikirr", "Narok North", "Narok South",
                "Transmara"),
            cd("Kericho", STANDARD_COST, STANDARD_DAYS,
                "Kericho Town", "Litein", "Londiani", "Sigowet", "Soin",
                "Ainamoi"),
            cd("Bomet", STANDARD_COST, STANDARD_DAYS,
                "Bomet Town", "Sotik", "Chepalungu", "Konoin", "Bomet Central",
                "Longisa"),
            cd("Kakamega", STANDARD_COST, STANDARD_DAYS,
                "Kakamega Town", "Mumias", "Butere", "Matungu", "Khwisero",
                "Lugari", "Shinyalu"),
            cd("Vihiga", STANDARD_COST, STANDARD_DAYS,
                "Vihiga Town", "Sabatia", "Emuhaya", "Luanda", "Hamisi",
                "Majengo"),
            cd("Bungoma", STANDARD_COST, STANDARD_DAYS,
                "Bungoma Town", "Webuye", "Kimilili", "Chwele", "Mt. Elgon",
                "Sirisia", "Kanduyi"),
            cd("Busia", STANDARD_COST, STANDARD_DAYS,
                "Busia Town", "Malaba", "Teso North", "Teso South", "Butula",
                "Matayos"),
            cd("Siaya", STANDARD_COST, STANDARD_DAYS,
                "Siaya Town", "Yala", "Bondo", "Ugenya", "Ukwala",
                "Rarieda"),
            cd("Kisumu", STANDARD_COST, STANDARD_DAYS,
                "Kisumu CBD", "Kisumu East", "Kisumu West", "Nyando", "Muhoroni",
                "Seme", "Nyakach"),
            cd("Homa Bay", STANDARD_COST, STANDARD_DAYS,
                "Homa Bay Town", "Mbita", "Ndhiwa", "Rachuonyo", "Kasipul",
                "Kabondo Kasipul"),
            cd("Migori", STANDARD_COST, STANDARD_DAYS,
                "Migori Town", "Uriri", "Awendo", "Suna East", "Suna West",
                "Rongo", "Nyatike"),
            cd("Kisii", STANDARD_COST, STANDARD_DAYS,
                "Kisii Town", "Suneka", "Ogembo", "Keroka", "Masimba",
                "Nyamira", "Kenyenya"),
            cd("Nyamira", STANDARD_COST, STANDARD_DAYS,
                "Nyamira Town", "Keroka", "Masaba North", "Borabu", "Manga",
                "Ekerenyo"),

            // ---- Remote counties (KSh 800, 5 days) ----
            cd("Garissa", REMOTE_COST, REMOTE_DAYS,
                "Garissa Town", "Dadaab", "Fafi", "Hulugho", "Ijara",
                "Lagdera"),
            cd("Wajir", REMOTE_COST, REMOTE_DAYS,
                "Wajir Town", "Habaswein", "Buna", "Eldas", "Tarbaj",
                "Wajir East"),
            cd("Mandera", REMOTE_COST, REMOTE_DAYS,
                "Mandera Town", "Lafey", "Mandera North", "Mandera South", "Mandera West",
                "Banissa"),
            cd("Marsabit", REMOTE_COST, REMOTE_DAYS,
                "Marsabit Town", "Moyale", "Laisamis", "Saku", "North Horr",
                "Loiyangalani"),
            cd("Turkana", REMOTE_COST, REMOTE_DAYS,
                "Lodwar", "Lokichogio", "Kakuma", "Kalokol", "Loima",
                "Turkana Central")
        );

        List<DeliveryZone> zones = new ArrayList<>();
        for (CountyData c : counties) {
            for (String town : c.towns) {
                zones.add(DeliveryZone.builder()
                        .county(c.county)
                        .town(town)
                        .deliveryCost(c.cost)
                        .estimatedDays(c.days)
                        .active(true)
                        .build());
            }
        }

        deliveryZoneRepository.saveAll(zones);
        log.info("Seeded {} delivery zones across {} counties", zones.size(), counties.size());
    }

    // --- helpers ---
    private record CountyData(String county, BigDecimal cost, int days, List<String> towns) {}

    private CountyData cd(String county, BigDecimal cost, int days, String... towns) {
        return new CountyData(county, cost, days, List.of(towns));
    }

    private String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}
