package za.ac.richfield.cbdventures.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.*;
import za.ac.richfield.cbdventures.domain.enums.ActivityType;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.domain.enums.ZoneRadius;
import za.ac.richfield.cbdventures.repo.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Seeds the pilot zone with verified demo data: the five approved categories,
 * standardised amenities, business owners, 26 listings spread across the 1 km
 * radius around Richfield, student reviews and upcoming events.
 *
 * <p>All coordinates are generated as metric offsets from the campus anchor, so
 * the whole data set is guaranteed to sit inside the pilot zone.</p>
 *
 * <p>Seeding runs only when the database is empty, so restarts never duplicate
 * data. Disable it with {@code cbd.seed.enabled=false}.</p>
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final double METRES_PER_DEGREE_LAT = 111_320d;

    private final CategoryRepository categoryRepository;
    private final AmenityRepository amenityRepository;
    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final UserActivityRepository activityRepository;
    private final BusinessHoursRepository hoursRepository;
    private final PasswordEncoder passwordEncoder;
    private final ZoneProperties zone;
    private final boolean enabled;

    private final Map<String, Amenity> amenityIndex = new LinkedHashMap<>();
    private final Map<String, Business> businessIndex = new LinkedHashMap<>();

    public DataSeeder(CategoryRepository categoryRepository,
                      AmenityRepository amenityRepository,
                      BusinessRepository businessRepository,
                      ReviewRepository reviewRepository,
                      UserRepository userRepository,
                      EventRepository eventRepository,
                      UserActivityRepository activityRepository,
                      BusinessHoursRepository hoursRepository,
                      PasswordEncoder passwordEncoder,
                      ZoneProperties zone,
                      @Value("${cbd.seed.enabled:true}") boolean enabled) {
        this.categoryRepository = categoryRepository;
        this.amenityRepository = amenityRepository;
        this.businessRepository = businessRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.activityRepository = activityRepository;
        this.hoursRepository = hoursRepository;
        this.passwordEncoder = passwordEncoder;
        this.zone = zone;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Data seeding disabled (cbd.seed.enabled=false).");
            return;
        }
        if (businessRepository.count() > 0) {
            log.info("Database already contains {} listings - skipping seed.", businessRepository.count());
            return;
        }
        log.info("Seeding CBDVentures pilot zone data around {}", zone.anchorName());

        List<Category> categories = seedCategories();
        seedAmenities(categories.get(0));
        Map<String, User> owners = seedOwners();
        List<User> visitors = seedVisitors();
        seedBusinesses(categories, owners);
        seedReviews(visitors, owners);
        seedActivity(visitors, owners);
        seedEvents();
        seedAdministrator();

        log.info("Seed complete: {} categories, {} amenities, {} listings, {} hours rows, {} reviews, "
                        + "{} events, {} activity rows, {} users.",
                categoryRepository.count(), amenityRepository.count(), businessRepository.count(),
                hoursRepository.count(),
                reviewRepository.count(), eventRepository.count(), activityRepository.count(),
                userRepository.count());
    }

    // ------------------------------------------------------------------
    // Reference data
    // ------------------------------------------------------------------

    private List<Category> seedCategories() {
        record Definition(String name, String slug, String description, String icon) {
        }
        List<Definition> definitions = List.of(
                new Definition("Food & Drinks", "food-drinks",
                        "Kotas, shawarmas, breakfast sandwiches, coffee carts and everyday quick eats.",
                        "food"),
                new Definition("Restaurants & Sit-Down Eateries", "restaurants",
                        "Formal dining, food halls such as The Containers, and sit-down spots worth the walk.",
                        "restaurant"),
                new Definition("Salons & Personal Care", "salons",
                        "Barbershops, hair salons, nail studios and beauty bars within walking distance.",
                        "salon"),
                new Definition("Study & Social Spaces", "study-spaces",
                        "Cafés, libraries and co-working corners filtered by Wi-Fi, power and quiet.",
                        "study"),
                new Definition("Events & Entertainment", "events",
                        "State Theatre performances, markets, pop-ups and local happenings in the zone.",
                        "event"));

        List<Category> saved = new ArrayList<>();
        for (int i = 0; i < definitions.size(); i++) {
            Definition definition = definitions.get(i);
            saved.add(categoryRepository.save(new Category(definition.name(), definition.slug(),
                    definition.description(), definition.icon(), i + 1)));
        }
        return saved;
    }

    private void seedAmenities(Category food) {
        record Definition(String name, String icon) {
        }
        List<Definition> definitions = List.of(
                new Definition("Free Wi-Fi", "wifi"),
                new Definition("Backup Power", "power"),
                new Definition("Power Outlets", "plug"),
                new Definition("Quiet Atmosphere", "quiet"),
                new Definition("Student Specials", "student"),
                new Definition("Card Payments", "card"),
                new Definition("Cash Only", "cash"),
                new Definition("Takeaway", "takeaway"),
                new Definition("Outdoor Seating", "outdoor"),
                new Definition("Air Conditioning", "aircon"),
                new Definition("Wheelchair Accessible", "accessible"),
                new Definition("Parking Nearby", "parking"),
                new Definition("Generous Portions", "portion"),
                new Definition("Late Night", "late"));

        for (Definition definition : definitions) {
            Amenity amenity = amenityRepository.save(new Amenity(food, definition.name(), definition.icon()));
            amenityIndex.put(definition.name(), amenity);
        }
    }

    // ------------------------------------------------------------------
    // Accounts
    // ------------------------------------------------------------------

    private Map<String, User> seedOwners() {
        String hash = passwordEncoder.encode("Password123!");
        Map<String, User> owners = new LinkedHashMap<>();
        record Definition(String key, String name, String email, String phone) {
        }
        List<Definition> definitions = List.of(
                new Definition("thabo", "Thabo Mokoena", "thabo@kotakingdom.co.za", "072 445 1180"),
                new Definition("naledi", "Naledi Dlamini", "naledi@dailygrind.co.za", "083 771 2094"),
                new Definition("pieter", "Pieter van Wyk", "pieter@karoo-grill.co.za", "082 330 5471"),
                new Definition("zanele", "Zanele Nkosi", "zanele@nailsbyzanele.co.za", "076 918 3320"),
                new Definition("sipho", "Sipho Radebe", "sipho@fadeking.co.za", "071 204 8836"),
                new Definition("lerato", "Lerato Mahlangu", "lerato@studynook.co.za", "084 662 9015"),
                new Definition("mmusi", "Mmusi Motaung", "mmusi@statetheatre-events.co.za", "012 392 4000"),
                new Definition("refilwe", "Refilwe Mahlangu", "refilwe@churchsquaretraders.co.za", "073 558 4402"));

        for (Definition definition : definitions) {
            User owner = userRepository.save(new User(definition.name(), definition.email(), hash, Role.OWNER));
            owners.put(definition.key(), owner);
        }
        return owners;
    }

    private List<User> seedVisitors() {
        String hash = passwordEncoder.encode("Password123!");
        List<String[]> people = List.of(
                new String[]{"Keziah Chinasamy", "keziah@student.richfield.ac.za"},
                new String[]{"Murangi Rahulani", "murangi@student.richfield.ac.za"},
                new String[]{"Xitshembiso Ngwenya", "xitshembiso@student.richfield.ac.za"},
                new String[]{"Kamogelo Machaka", "kamogelo@student.richfield.ac.za"},
                new String[]{"Mbalentle Kabini", "mbalentle@student.richfield.ac.za"},
                new String[]{"Tshiamo Peta", "tshiamo@student.richfield.ac.za"},
                new String[]{"Lefa Mfulwane", "lefa@student.richfield.ac.za"},
                new String[]{"Slondiwe Xulu", "slondiwe@student.richfield.ac.za"});

        List<User> visitors = new ArrayList<>();
        for (String[] person : people) {
            visitors.add(userRepository.save(new User(person[0], person[1], hash, Role.VISITOR)));
        }
        return visitors;
    }

    private void seedAdministrator() {
        if (userRepository.existsByEmailIgnoreCase("admin@cbdventures.co.za")) {
            return;
        }
        userRepository.save(new User("Naledi Khumalo", "admin@cbdventures.co.za",
                passwordEncoder.encode("Admin123!"), Role.ADMIN));
    }

    // ------------------------------------------------------------------
    // Listings
    // ------------------------------------------------------------------

    private void seedBusinesses(List<Category> categories, Map<String, User> owners) {
        Map<String, Category> categoryBySlug = new HashMap<>();
        categories.forEach(c -> categoryBySlug.put(c.getSlug(), c));

        record Definition(String owner, String category, String name, String address,
                          int eastMetres, int northMetres, PriceLevel price,
                          String hours, String open, String close, String description,
                          String[] amenities, String phone, String website) {
        }

        List<Definition> definitions = List.of(
                // ---------------- Food & Drinks ----------------
                new Definition("thabo", "food-drinks", "Kota Kingdom", "268 Pretorius Street, Pretoria Central",
                        180, 40, PriceLevel.R, "Mon-Sat 06:30 - 20:00", "06:30", "20:00",
                        "The kota that built a campus following: quarter loaf, polony, cheese, chips and "
                                + "atchar, wrapped hot and handed over in under four minutes. Cash and card.",
                        new String[]{"Student Specials", "Takeaway", "Generous Portions", "Card Payments"},
                        "072 445 1180", null),
                new Definition("thabo", "food-drinks", "Braam's Braai Corner", "45 Schubart Street, Pretoria Central",
                        -260, -190, PriceLevel.R, "Mon-Sat 10:00 - 19:00", "10:00", "19:00",
                        "Street-side braai stand serving wors rolls, chicken dust and pap with chakalaka. "
                                + "Lunch queues move quickly between 12:00 and 14:00.",
                        new String[]{"Takeaway", "Cash Only", "Generous Portions", "Outdoor Seating"},
                        "072 445 1181", null),
                new Definition("naledi", "food-drinks", "Jacaranda Coffee Cart", "Cnr Church & van der Walt Street",
                        60, 110, PriceLevel.R, "Mon-Fri 06:00 - 16:00", "06:00", "16:00",
                        "A two-bar espresso cart parked under the jacarandas opposite Sammy Marks Square. "
                                + "R25 filter coffee, R18 refills if you bring your own cup before 9 am.",
                        new String[]{"Free Wi-Fi", "Takeaway", "Student Specials", "Card Payments"},
                        "083 771 2094", null),
                new Definition("naledi", "food-drinks", "The Daily Grind Café", "291 Helen Joseph Street, Pretoria Central",
                        -40, 25, PriceLevel.RR, "Mon-Sat 07:00 - 17:00", "07:00", "17:00",
                        "Small café 120 m from the Richfield entrance. Breakfast sandwiches, smoothies and "
                                + "the cheapest flat white within walking distance of campus.",
                        new String[]{"Free Wi-Fi", "Power Outlets", "Student Specials", "Card Payments"},
                        "083 771 2095", "https://dailygrind.example.co.za"),
                new Definition("thabo", "food-drinks", "Sunrise Sandwich Bar", "150 Vermeulen Street, Pretoria Central",
                        -300, 260, PriceLevel.R, "Mon-Fri 06:00 - 15:00", "06:00", "15:00",
                        "Grab-and-go breakfast sandwiches, vetkoek and fresh juice. Popular with commuters "
                                + "walking up from the station.",
                        new String[]{"Takeaway", "Cash Only", "Card Payments"},
                        "072 445 1182", null),
                new Definition("pieter", "food-drinks", "Shawarma House Pretoria", "190 Thabo Sehume Street, Pretoria Central",
                        480, -250, PriceLevel.R, "Daily 10:00 - 22:00", "10:00", "22:00",
                        "Charcoal-grilled chicken and lamb shawarmas with garlic sauce, plus a vegetarian "
                                + "halloumi option. Open late on weekdays.",
                        new String[]{"Takeaway", "Late Night", "Card Payments", "Generous Portions"},
                        "082 330 5471", null),

                // ---------------- Restaurants & Sit-Down Eateries ----------------
                new Definition("pieter", "restaurants", "The Containers Food Hall", "Cnr Struben & Bosman Street, Pretoria Central",
                        420, 120, PriceLevel.RR, "Tue-Sun 11:00 - 22:00", "11:00", "22:00",
                        "A cluster of shipping-container kitchens with shared seating. Nine vendors, from "
                                + "bunny chow to bao buns, one bill. Live acoustic sets on Saturdays.",
                        new String[]{"Card Payments", "Outdoor Seating", "Free Wi-Fi", "Wheelchair Accessible"},
                        "082 330 5472", "https://thecontainers.example.co.za"),
                new Definition("pieter", "restaurants", "Karoo Grill House", "210 Pretorius Street, Pretoria Central",
                        150, 180, PriceLevel.RRR, "Mon-Sat 12:00 - 22:30", "12:00", "22:30",
                        "Sit-down grill house for steak, lamb chops and a proper Sunday lunch. Student "
                                + "two-course special from R145 on weekdays.",
                        new String[]{"Air Conditioning", "Card Payments", "Parking Nearby", "Student Specials"},
                        "082 330 5473", "https://karoogrill.example.co.za"),
                new Definition("refilwe", "restaurants", "Ndlovu's Kitchen", "88 Skinner Street, Pretoria Central",
                        520, -420, PriceLevel.RR, "Mon-Sat 09:00 - 18:00", "09:00", "18:00",
                        "Home-style African cooking: tripe, chicken feet, samp and beans, and a rotating "
                                + "daily plate that sells out by 15:00.",
                        new String[]{"Takeaway", "Generous Portions", "Cash Only", "Student Specials"},
                        "073 558 4402", null),
                new Definition("pieter", "restaurants", "La Piazza Pizzeria", "320 Pretorius Street, Pretoria Central",
                        360, 60, PriceLevel.RR, "Daily 11:00 - 23:00", "11:00", "23:00",
                        "Wood-fired pizza opposite the State Theatre, ideal for a pre-show dinner. "
                                + "Two-for-one Tuesdays for students with a campus card.",
                        new String[]{"Card Payments", "Air Conditioning", "Takeaway", "Student Specials"},
                        "082 330 5474", "https://lapiazza.example.co.za"),
                new Definition("refilwe", "restaurants", "Mzansi Tshisa Nyama", "12 Bloed Street, Pretoria Central",
                        240, -640, PriceLevel.RR, "Fri-Sun 11:00 - 23:00", "11:00", "23:00",
                        "Weekend braai yard where you pick your meat and they grill it while you find a "
                                + "seat. Loud, social and busy after 18:00.",
                        new String[]{"Outdoor Seating", "Late Night", "Cash Only", "Parking Nearby"},
                        "073 558 4403", null),

                // ---------------- Salons & Personal Care ----------------
                new Definition("sipho", "salons", "Fade King Barbershop", "77 Church Street, Pretoria Central",
                        40, 150, PriceLevel.R, "Mon-Sat 08:00 - 19:00", "08:00", "19:00",
                        "Skin fades, line-ups and beard trims at student prices. Walk-ins welcome; ask for "
                                + "the R90 campus cut before 11 am.",
                        new String[]{"Student Specials", "Cash Only", "Card Payments", "Air Conditioning"},
                        "071 204 8836", null),
                new Definition("zanele", "salons", "Blessed Hands Hair Studio", "260 Pretorius Street, Pretoria Central",
                        200, 30, PriceLevel.R, "Mon-Sat 08:00 - 18:00", "08:00", "18:00",
                        "Cornrows, box braids and wash-and-set styling. Roughly two hours per appointment, "
                                + "book by WhatsApp to avoid the queue.",
                        new String[]{"Student Specials", "Card Payments", "Cash Only"},
                        "076 918 3320", null),
                new Definition("zanele", "salons", "Nails by Zanele", "143 Vermeulen Street, Pretoria Central",
                        -220, 420, PriceLevel.RR, "Tue-Sat 09:00 - 18:00", "09:00", "18:00",
                        "Gel overlays, acrylic sets and nail art in a quiet studio with two chairs. "
                                + "R180 full set for students on Tuesdays and Wednesdays.",
                        new String[]{"Student Specials", "Air Conditioning", "Card Payments"},
                        "076 918 3321", "https://nailsbyzanele.example.co.za"),
                new Definition("zanele", "salons", "Glow Up Beauty Bar", "301 Helen Joseph Street, Pretoria Central",
                        -80, 45, PriceLevel.RRR, "Mon-Sat 09:00 - 18:00", "09:00", "18:00",
                        "Facials, lashes and brow treatments a block from campus. Appointment-based, "
                                + "with a same-day student rate on facials.",
                        new String[]{"Air Conditioning", "Card Payments", "Wheelchair Accessible"},
                        "076 918 3322", null),
                new Definition("sipho", "salons", "Sibo's Braids & Weaves", "55 Struben Street, Pretoria Central",
                        360, -300, PriceLevel.R, "Mon-Sat 07:00 - 19:00", "07:00", "19:00",
                        "Braiding house with four chairs and quick turnaround on knotless braids. "
                                + "Bring a picture and they will quote on the spot.",
                        new String[]{"Cash Only", "Student Specials", "Takeaway"},
                        "071 204 8837", null),

                // ---------------- Study & Social Spaces ----------------
                new Definition("lerato", "study-spaces", "Richfield Campus Library", "291 Helen Joseph Street, Pretoria Central",
                        15, 10, PriceLevel.R, "Mon-Fri 07:30 - 21:00, Sat 08:00 - 14:00", "07:30", "21:00",
                        "The on-campus library: quiet floors, group rooms and reliable Wi-Fi. Open to "
                                + "registered students with a campus card.",
                        new String[]{"Free Wi-Fi", "Power Outlets", "Quiet Atmosphere", "Backup Power"},
                        "012 000 0001", null),
                new Definition("lerato", "study-spaces", "The Study Nook Co-Working", "118 Thabo Sehume Street, Pretoria Central",
                        380, -260, PriceLevel.RR, "Mon-Fri 07:00 - 20:00, Sat 09:00 - 15:00", "07:00", "20:00",
                        "Pay-as-you-go co-working: R45 for a half day, unlimited coffee and a silent room. "
                                + "Backup power keeps the Wi-Fi on during load shedding.",
                        new String[]{"Free Wi-Fi", "Backup Power", "Power Outlets", "Quiet Atmosphere",
                                "Student Specials", "Air Conditioning"},
                        "084 662 9015", "https://studynook.example.co.za"),
                new Definition("lerato", "study-spaces", "Quiet Corner Café & Study", "40 Bosman Street, Pretoria Central",
                        280, 220, PriceLevel.R, "Mon-Sat 08:00 - 17:00", "08:00", "17:00",
                        "Café by day with a no-music back room, plug points at every table and a "
                                + "one-coffee-per-two-hours study policy.",
                        new String[]{"Free Wi-Fi", "Power Outlets", "Quiet Atmosphere", "Card Payments"},
                        "084 662 9016", null),
                new Definition("refilwe", "study-spaces", "Tshwane Community Library", "Cnr Vermeulen & van der Walt Street",
                        -260, 380, PriceLevel.R, "Mon-Fri 09:00 - 17:00, Sat 09:00 - 13:00", "09:00", "17:00",
                        "Free public library with a reference section, printing and a quiet reading hall. "
                                + "Bring your own data - Wi-Fi is limited.",
                        new String[]{"Quiet Atmosphere", "Power Outlets", "Wheelchair Accessible"},
                        "012 358 7000", null),
                new Definition("mmusi", "study-spaces", "Grand Central Social Club", "9 Church Square, Pretoria Central",
                        -540, 240, PriceLevel.RRR, "Wed-Sun 16:00 - 00:00", "16:00", "00:00",
                        "Rooftop social space above Church Square for after-class drinks, board games and "
                                + "small gigs. Cover charge on Fridays.",
                        new String[]{"Outdoor Seating", "Late Night", "Card Payments", "Air Conditioning"},
                        "012 392 4000", "https://grandcentral.example.co.za"),

                // ---------------- Events & Entertainment ----------------
                new Definition("mmusi", "events", "South African State Theatre", "320 Pretorius Street, Pretoria Central",
                        400, 70, PriceLevel.RR, "Box office Tue-Sat 09:00 - 20:00", "09:00", "20:00",
                        "The city's main stage for ballet, opera, drama and music. Student rush tickets "
                                + "from R80 an hour before curtain.",
                        new String[]{"Card Payments", "Wheelchair Accessible", "Parking Nearby",
                                "Air Conditioning", "Student Specials"},
                        "012 392 4000", "https://statetheatre.example.co.za"),
                new Definition("refilwe", "events", "Church Square Night Market", "Church Square, Pretoria Central",
                        -600, 280, PriceLevel.R, "Fri-Sat 17:00 - 23:00", "17:00", "23:00",
                        "Weekly night market with food stalls, crafts and DJs around the square. Free entry, "
                                + "cash is easier for the smaller stalls.",
                        new String[]{"Outdoor Seating", "Cash Only", "Late Night", "Takeaway"},
                        "073 558 4404", null),
                new Definition("refilwe", "events", "Sammy Marks Square Pop-Up", "Sammy Marks Square, Church Street",
                        110, -30, PriceLevel.R, "Sat 09:00 - 16:00", "09:00", "16:00",
                        "Weekend pop-up market directly opposite campus: vintage clothing, street food and "
                                + "a rotating line-up of local makers.",
                        new String[]{"Outdoor Seating", "Takeaway", "Wheelchair Accessible"},
                        "073 558 4405", null),
                new Definition("mmusi", "events", "Jacaranda Rooftop Cinema", "250 Church Street, Pretoria Central",
                        -120, 200, PriceLevel.RR, "Thu-Sun 18:00 - 23:00", "18:00", "23:00",
                        "Open-air rooftop screenings under the jacarandas. Beanbags provided, bring a "
                                + "blanket in winter.",
                        new String[]{"Outdoor Seating", "Late Night", "Card Payments"},
                        "012 392 4001", null),
                new Definition("mmusi", "events", "Burgers Park Open Air Sessions", "Burgers Park, van der Walt Street",
                        -80, 780, PriceLevel.R, "Sat-Sun 10:00 - 18:00", "10:00", "18:00",
                        "Free weekend music and yoga sessions in the park, ten minutes' walk from campus "
                                + "through the CBD.",
                        new String[]{"Outdoor Seating", "Wheelchair Accessible", "Takeaway"},
                        "012 392 4002", null),
                // ---------------- Verified real Pretoria CBD businesses (storefront photographs) ----------------
                new Definition("thabo", "food-drinks", "Thuba Rush KJ", "230 Pretorius Street, Pretoria Central (Opera Plaza)",
                        20, -150, PriceLevel.R, "Mon-Sat 07:30 - 22:00", "07:30", "22:00",
                        "Kota joint opposite FNB Bank in Opera Plaza, serving quarter-loaf kotas with polony, cheese, chips and atchar. Quick takeaway service popular with students.",
                        new String[]{"Student Specials", "Takeaway", "Generous Portions", "Card Payments"},
                        "073 115 6260", null),
                new Definition("naledi", "food-drinks", "Magwinya Zone", "266 Pretorius Street, Pretoria Central (Centre Walk)",
                        30, -260, PriceLevel.R, "Mon-Sat 08:00 - 18:00", "08:00", "18:00",
                        "Magwinya (vetkoek / fat cakes) takeaway in the CBD, served fresh and hot. A student favourite for a quick breakfast or snack.",
                        new String[]{"Takeaway", "Cash Only", "Generous Portions"},
                        "081 301 5623", null),
                new Definition("pieter", "restaurants", "Rebels Pizza", "Middestad Mall, Cnr Thabo Sehume & Pretorius Street, Pretoria Central",
                        60, -100, PriceLevel.RR, "Daily 11:00 - 20:00", "11:00", "20:00",
                        "Pretoria-born pizza chain with fresh, hand-made pizza, takeaway and delivery. A CBD staple for students and workers.",
                        new String[]{"Card Payments", "Takeaway", "Student Specials", "Air Conditioning"},
                        "012 321 1391", null),
                new Definition("thabo", "food-drinks", "Honchos", "171 Lilian Ngoyi Street, Pretoria Central",
                        -40, -60, PriceLevel.R, "Mon-Sat 09:00 - 19:45", "09:00", "19:45",
                        "Halaal-certified fast food specialising in flame-grilled chicken, wraps and chips at student-friendly prices.",
                        new String[]{"Takeaway", "Card Payments", "Student Specials", "Cash Only"},
                        "012 321 1391", null),
                new Definition("refilwe", "food-drinks", "QUICK BAKE", "Shop 1a, Opera Plaza, 7 Bank Lane, Pretoria Central",
                        -33, -285, PriceLevel.R, "Mon-Fri 08:00 - 16:30, Sat 08:00 - 13:00", "08:00", "16:30",
                        "Confectionery and coffee shop in Opera Plaza, known for fresh scones, biscuits and home-style baking.",
                        new String[]{"Takeaway", "Card Payments", "Student Specials"},
                        "012 753 4145", null),
                new Definition("naledi", "food-drinks", "Top Class Fast Foods", "287 Pretorius Street, Pretoria Central",
                        25, -280, PriceLevel.R, "Mon-Sat 07:00 - 19:00", "07:00", "19:00",
                        "Fast-food takeaway on Pretorius Street serving kotas, chips and everyday quick eats.",
                        new String[]{"Takeaway", "Cash Only", "Generous Portions"},
                        null, null),
                new Definition("pieter", "restaurants", "Four Four Two", "399 Helen Joseph Street, Pretoria Central",
                        200, 10, PriceLevel.RR, "Mon-Sat 10:00 - 22:00", "10:00", "22:00",
                        "Sports and music lounge with cocktails, bar bites and live music. Dine-in and outdoor seating in a relaxed CBD setting.",
                        new String[]{"Card Payments", "Outdoor Seating", "Late Night"},
                        "081 527 5444", null),
                new Definition("refilwe", "restaurants", "Phola Shesa Nyama", "219 Pretorius Street, Finpark Arcade, Pretoria Central",
                        -333, -183, PriceLevel.R, "Daily 09:00 - 19:00", "09:00", "19:00",
                        "Shisa nyama grill in Finpark Arcade serving authentic braai meat with generous portions. Cash only and popular at lunch.",
                        new String[]{"Takeaway", "Cash Only", "Generous Portions"},
                        null, null),
                new Definition("lerato", "study-spaces", "Es'kia Mphahlele Community Library", "Corner Madiba & Lillian Ngoyi Street, Sammy Marks Square, Pretoria Central",
                        -80, 40, PriceLevel.R, "Mon-Fri 09:30 - 16:30", "09:30", "16:30",
                        "City of Tshwane public library in Sammy Marks Square with reading and study areas, reference books, public computers and Wi-Fi. Houses the American Corner Pretoria.",
                        new String[]{"Free Wi-Fi", "Quiet Atmosphere", "Power Outlets", "Wheelchair Accessible"},
                        "012 358 8957", null));

        int coverIndex = 0;
        for (Definition definition : definitions) {
            Category category = categoryBySlug.get(definition.category());
            User owner = owners.get(definition.owner());
            Business business = new Business();
            business.setOwner(owner);
            business.setCategory(category);
            business.setName(definition.name());
            business.setAddress(definition.address());
            business.setDescription(definition.description());
            business.setPriceLevel(definition.price());
            business.setOperatingHours(definition.hours());
            business.setOpenTime(LocalTime.parse(definition.open()));
            business.setCloseTime(LocalTime.parse(definition.close()));
            seedWeek(business, definition.hours());
            business.setPhone(definition.phone());
            business.setWebsite(definition.website());
            business.setStatus(BusinessStatus.ACTIVE);
            business.setAdminNote("Verified by the CBDVentures team.");

            double[] coordinates = offset(definition.eastMetres(), definition.northMetres());
            business.setLatitude(BigDecimal.valueOf(coordinates[0]).setScale(8, RoundingMode.HALF_UP));
            business.setLongitude(BigDecimal.valueOf(coordinates[1]).setScale(8, RoundingMode.HALF_UP));
            business.setZoneRadius(ZoneRadius.PRIMARY_1KM);

            for (String amenityName : definition.amenities()) {
                Amenity amenity = amenityIndex.get(amenityName);
                if (amenity != null) {
                    business.getAmenities().add(amenity);
                }
            }

            coverIndex++;
            int covers = 1 + (coverIndex % 3);
            for (int i = 1; i <= covers; i++) {
                String file = String.format("/images/covers/%s-%02d.svg", category.getSlug(),
                        ((coverIndex + i) % 8) + 1);
                business.addImage(new BusinessImage(file, definition.name() + " - photo " + i, i == 1));
            }

            businessIndex.put(definition.name(), businessRepository.save(business));
        }

        // Front image override: use the real storefront photograph as the primary
        // (front) image for each verified real Pretoria CBD business.
        Map<String, String> frontPhotos = Map.of(
                "Thuba Rush KJ", "/images/places/thuba-rush-kj.jpg",
                "Magwinya Zone", "/images/places/magwinya-zone.jpg",
                "Rebels Pizza", "/images/places/rebels-pizza.jpg",
                "Honchos", "/images/places/honchos.jpg",
                "QUICK BAKE", "/images/places/quick-bake.jpg",
                "Top Class Fast Foods", "/images/places/top-class-fast-foods.jpg",
                "Four Four Two", "/images/places/four-four-two.jpg",
                "Phola Shesa Nyama", "/images/places/phola-shisa-nyama.jpg",
                "Es'kia Mphahlele Community Library", "/images/places/es-kia-mphahlele-library.jpg"
        );
        for (Map.Entry<String, String> entry : frontPhotos.entrySet()) {
            Business business = businessIndex.get(entry.getKey());
            if (business != null) {
                business.getImages().clear();
                business.addImage(new BusinessImage(entry.getValue(), entry.getKey() + " - storefront photograph", true));
                businessRepository.save(business);
            }
        }

        seedVerificationQueue(categoryBySlug, owners);
    }

    /**
     * Two submissions awaiting verification and one rejected submission, so the
     * administrator queue is never empty on a fresh install.
     */
    private void seedVerificationQueue(Map<String, Category> categoryBySlug, Map<String, User> owners) {
        record Submission(String owner, String category, String name, String address, int east, int north,
                          PriceLevel price, String hours, String open, String close, String description,
                          BusinessStatus status, String note) {
        }

        List<Submission> submissions = List.of(
                new Submission("naledi", "food-drinks", "Campus Corner Coffee Cart",
                        "44 Bosman Street, Pretoria Central", 150, -260, PriceLevel.R,
                        "Mon-Fri 06:30 - 15:00", "06:30", "15:00",
                        "A mobile coffee cart parked by the Bosman Street entrance. R20 filter coffee, "
                                + "rusks and a student loyalty card.",
                        BusinessStatus.PENDING, null),
                new Submission("naledi", "salons", "Bloom Nail Studio",
                        "118 Visagie Street, Pretoria Central", -320, -140, PriceLevel.RR,
                        "Tue-Sat 09:00 - 18:00", "09:00", "18:00",
                        "Gel overlays, nail art and student Tuesday specials. Walk-ins welcome before 11:00.",
                        BusinessStatus.PENDING, null),
                new Submission("zanele", "study-spaces", "The Quiet Room Study Hub",
                        "9 Bureau Lane, Pretoria Central", 60, 420, PriceLevel.RR,
                        "Mon-Sun 07:00 - 22:00", "07:00", "22:00",
                        "Silent study desks, bookable rooms and unlimited Wi-Fi. Hourly and monthly passes.",
                        BusinessStatus.REJECTED,
                        "Please add the street number for Bureau Lane and a contact number before resubmitting."));

        int index = 0;
        for (Submission submission : submissions) {
            Category category = categoryBySlug.get(submission.category());
            User owner = owners.get(submission.owner());
            if (category == null || owner == null) {
                continue;
            }
            Business business = new Business();
            business.setOwner(owner);
            business.setCategory(category);
            business.setName(submission.name());
            business.setAddress(submission.address());
            business.setDescription(submission.description());
            business.setPriceLevel(submission.price());
            business.setOperatingHours(submission.hours());
            business.setOpenTime(LocalTime.parse(submission.open()));
            business.setCloseTime(LocalTime.parse(submission.close()));
            seedWeek(business, submission.hours());
            business.setStatus(submission.status());
            business.setAdminNote(submission.note());

            double[] coordinates = offset(submission.east(), submission.north());
            business.setLatitude(BigDecimal.valueOf(coordinates[0]).setScale(8, RoundingMode.HALF_UP));
            business.setLongitude(BigDecimal.valueOf(coordinates[1]).setScale(8, RoundingMode.HALF_UP));
            business.setZoneRadius(ZoneRadius.PRIMARY_1KM);
            business.addImage(new BusinessImage(
                    String.format("/images/covers/%s-%02d.svg", category.getSlug(), (index % 8) + 1),
                    submission.name() + " - photo 1", true));
            businessRepository.save(business);
            index++;
        }
        log.info("Verification queue seeded with {} submissions (pending and rejected).", index);
    }


    /** Converts a metric offset from the campus anchor into lat/lng degrees. */
    private double[] offset(int eastMetres, int northMetres) {
        double latitude = zone.anchorLatitude() + (northMetres / METRES_PER_DEGREE_LAT);
        double metresPerDegreeLng = 111_320d * Math.cos(Math.toRadians(zone.anchorLatitude()));
        double longitude = zone.anchorLongitude() + (eastMetres / metresPerDegreeLng);
        return new double[]{latitude, longitude};
    }

    // ------------------------------------------------------------------
    // Reviews
    // ------------------------------------------------------------------

    private void seedReviews(List<User> visitors, Map<String, User> owners) {
        record Definition(String business, int visitorIndex, int rating, String comment, int daysAgo,
                          boolean verified) {
        }

        List<Definition> definitions = List.of(
                new Definition("Kota Kingdom", 0, 5, "The R35 student kota is unreal value. Chips were fresh "
                        + "and the queue moved in three minutes.", 4, true),
                new Definition("Kota Kingdom", 3, 4, "Great taste, but go before 13:00 or the polony is finished.", 12, true),
                new Definition("Kota Kingdom", 5, 5, "Best kotas near campus, full stop. They remember my order now.", 21, true),
                new Definition("Jacaranda Coffee Cart", 1, 5, "R25 filter coffee and I can tap my card. My "
                        + "morning stop before an 08:00 lecture.", 2, true),
                new Definition("Jacaranda Coffee Cart", 4, 4, "Lovely spot under the trees. Only takes four "
                        + "people at a time though.", 9, true),
                new Definition("The Daily Grind Café", 2, 4, "Flat white is the cheapest nearby and the "
                        + "breakfast sandwich keeps me going until lunch.", 3, true),
                new Definition("The Daily Grind Café", 6, 5, "Wi-Fi actually works and there are plug points "
                        + "at the window seats. Stayed for two hours, nobody minded.", 8, true),
                new Definition("The Daily Grind Café", 0, 3, "Good coffee, but very full between 08:00 and 09:00.", 15, true),
                new Definition("Braam's Braai Corner", 7, 5, "Chicken dust and pap for R45. Loud music, quick "
                        + "service, exactly what you want on a Friday.", 5, true),
                new Definition("Shawarma House Pretoria", 3, 4, "Big shawarma, fair price, open when "
                        + "everything else is closed.", 6, true),
                new Definition("The Containers Food Hall", 1, 5, "Nine kitchens, one table. Perfect for a "
                        + "group where everyone wants something different.", 1, true),
                new Definition("The Containers Food Hall", 5, 4, "Great vibe on Saturday. Slightly pricey "
                        + "for a student but worth it once a month.", 11, true),
                new Definition("The Containers Food Hall", 6, 5, "Bao buns are ridiculous. Card only at "
                        + "some stalls, so bring a card.", 18, true),
                new Definition("Karoo Grill House", 2, 5, "Took my parents here after graduation. "
                        + "The lamb chops were excellent.", 20, true),
                new Definition("Karoo Grill House", 4, 4, "Sunday lunch special is good value, service "
                        + "slows down when it gets busy.", 26, true),
                new Definition("Ndlovu's Kitchen", 7, 5, "Tastes like home. Get there before 15:00 or the "
                        + "daily plate is gone.", 7, true),
                new Definition("La Piazza Pizzeria", 0, 4, "Two-for-one Tuesday is the move before a show "
                        + "at the State Theatre.", 10, true),
                new Definition("La Piazza Pizzeria", 3, 5, "Proper wood-fired base and they are quick "
                        + "before curtain-up.", 14, true),
                new Definition("Mzansi Tshisa Nyama", 5, 5, "You pick the meat, they braai it. Saturday "
                        + "afternoon sorted.", 9, true),
                new Definition("Mzansi Tshisa Nyama", 6, 3, "Great food, but very crowded after 18:00 and "
                        + "you wait for a table.", 22, true),
                new Definition("Fade King Barbershop", 1, 5, "R90 campus cut before 11 am and the fade "
                        + "held for three weeks. Booked again.", 3, true),
                new Definition("Fade King Barbershop", 4, 4, "Clean cut, friendly team. Saturdays are busy "
                        + "so go during the week.", 16, true),
                new Definition("Blessed Hands Hair Studio", 2, 5, "Cornrows done in two hours, neat and "
                        + "exactly the pattern I asked for.", 6, true),
                new Definition("Blessed Hands Hair Studio", 7, 4, "Good work and fair price. Bring cash "
                        + "for the deposit.", 19, true),
                new Definition("Nails by Zanele", 0, 5, "Tuesday student set for R180, lasted three weeks "
                        + "with no chips.", 4, true),
                new Definition("Nails by Zanele", 5, 5, "Quiet studio, lovely nails, and she is honest "
                        + "about what will last.", 13, true),
                new Definition("Glow Up Beauty Bar", 3, 4, "Pricey but the facial was worth it before "
                        + "our presentation.", 8, true),
                new Definition("Sibo's Braids & Weaves", 6, 4, "Knotless braids done quickly. Take a "
                        + "picture, the quote was accurate.", 17, true),
                new Definition("Sibo's Braids & Weaves", 2, 5, "Four chairs so it moves fast. Cash only, "
                        + "which is the only downside.", 24, true),
                new Definition("Richfield Campus Library", 1, 5, "Third floor is silent and there are "
                        + "plugs at every desk. My go-to before exams.", 2, true),
                new Definition("Richfield Campus Library", 6, 4, "Great for quiet work, but group rooms "
                        + "need to be booked a day ahead.", 7, true),
                new Definition("Richfield Campus Library", 4, 5, "Backup power means load shedding does not "
                        + "interrupt study sessions.", 12, true),
                new Definition("The Study Nook Co-Working", 0, 5, "R45 half day with unlimited coffee and "
                        + "a silent room. Cheaper than data and much quieter.", 5, true),
                new Definition("The Study Nook Co-Working", 7, 4, "Backup power saved my online test "
                        + "during load shedding. Will be back.", 10, true),
                new Definition("The Study Nook Co-Working", 3, 5, "The silent room really is silent. "
                        + "Perfect for assignment season.", 23, true),
                new Definition("Quiet Corner Café & Study", 5, 4, "Back room has no music and plugs at "
                        + "every table. Coffee is decent too.", 6, true),
                new Definition("Tshwane Community Library", 2, 4, "Free, quiet and close. Printing is "
                        + "cheap, but bring your own data.", 15, true),
                new Definition("Grand Central Social Club", 6, 4, "Nice rooftop for after-class drinks. "
                        + "Cover charge on Fridays is a bit steep.", 11, true),
                new Definition("South African State Theatre", 3, 5, "Student rush tickets for R80 and the "
                        + "production was world class.", 3, true),
                new Definition("South African State Theatre", 0, 5, "Saw the ballet here. Beautiful venue, "
                        + "ten minutes' walk from campus.", 18, true),
                new Definition("Church Square Night Market", 1, 4, "Good food and music. Bring cash for "
                        + "the smaller stalls.", 4, true),
                new Definition("Church Square Night Market", 5, 5, "Friday night ritual. The vetkoek stand "
                        + "in the corner is the best one.", 13, true),
                new Definition("Sammy Marks Square Pop-Up", 4, 4, "Literally opposite campus. Great for "
                        + "vintage denim.", 6, true),
                new Definition("Jacaranda Rooftop Cinema", 2, 5, "Watching a film under the jacarandas is "
                        + "worth every rand.", 9, true),
                new Definition("Burgers Park Open Air Sessions", 7, 4, "Free yoga on Saturday morning, "
                        + "then breakfast in the park.", 8, true));

        for (Definition definition : definitions) {
            Business business = businessIndex.get(definition.business());
            if (business == null) {
                continue;
            }
            User author = visitors.get(definition.visitorIndex() % visitors.size());
            Review review = new Review(author, business, definition.rating(), definition.comment());
            review.setVerified(definition.verified());
            review.setCreatedAt(LocalDateTime.now().minusDays(definition.daysAgo())
                    .atZone(java.time.ZoneId.of("Africa/Johannesburg")).toInstant());
            review.setUpdatedAt(review.getCreatedAt());
            reviewRepository.save(review);
        }
    }

    // ------------------------------------------------------------------
    // Browsing trail (user_activity) - what makes the recommendations personal
    // ------------------------------------------------------------------

    /**
     * Seeds a plausible browsing history for the demo visitors so the home
     * screen shows a personalised feed on first run instead of falling back to
     * "what's popular". Dates are spread over the last two weeks, and the
     * demo visitor (index 0) deliberately browses a category they have not
     * reviewed yet, which is what the recommendation engine should notice.
     */
    private void seedActivity(List<User> visitors, Map<String, User> owners) {
        record Definition(int visitorIndex, String business, ActivityType type, int hoursAgo) {
        }

        List<Definition> definitions = List.of(
                new Definition(0, "The Daily Grind Café", ActivityType.VIEW_PROFILE, 3),
                new Definition(0, "The Daily Grind Café", ActivityType.SAVE_BUSINESS, 3),
                new Definition(0, "Shawarma House Pretoria", ActivityType.VIEW_PROFILE, 20),
                new Definition(0, "Braam's Braai Corner", ActivityType.VIEW_PROFILE, 30),
                new Definition(0, "The Containers Food Hall", ActivityType.SEARCH, 52),
                new Definition(0, "Grand Central Social Club", ActivityType.VIEW_PROFILE, 76),
                new Definition(0, "Grand Central Social Club", ActivityType.SAVE_BUSINESS, 76),
                new Definition(1, "Jacaranda Coffee Cart", ActivityType.VIEW_PROFILE, 6),
                new Definition(1, "Kota Kingdom", ActivityType.VIEW_PROFILE, 28),
                new Definition(2, "Richfield Campus Library", ActivityType.VIEW_PROFILE, 14),
                new Definition(2, "Tshwane Community Library", ActivityType.VIEW_PROFILE, 15),
                new Definition(3, "Fade King Barbershop", ActivityType.VIEW_PROFILE, 40),
                new Definition(4, "South African State Theatre", ActivityType.SEARCH, 22),
                new Definition(5, "The Containers Food Hall", ActivityType.VIEW_PROFILE, 11),
                new Definition(6, "Kota Kingdom", ActivityType.SAVE_BUSINESS, 9));

        int seeded = 0;
        for (Definition definition : definitions) {
            if (definition.visitorIndex() >= visitors.size()) {
                continue;
            }
            Business business = businessIndex.get(definition.business());
            if (business == null) {
                continue;
            }
            UserActivity activity = new UserActivity(visitors.get(definition.visitorIndex()),
                    business, definition.type());
            activity.setCreatedAt(Instant.now().minusSeconds(definition.hoursAgo() * 3600L));
            activityRepository.save(activity);
            seeded++;
        }
        log.info("Browsing history seeded with {} activity rows for personalised suggestions.", seeded);
    }

    // ------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------

    private void seedEvents() {
        record Definition(String business, String title, String description, String venue,
                          int daysFromNow, int hour, String price) {
        }

        List<Definition> definitions = List.of(
                new Definition("South African State Theatre", "Ballet Season: 'Jacaranda Suite'",
                        "A three-act contemporary ballet performed by the State Theatre dance company, "
                                + "with a live orchestra. Student rush tickets available an hour before curtain.",
                        "Main Stage", 4, 19, "180.00"),
                new Definition("South African State Theatre", "Jazz at the State: Thursday Sessions",
                        "Rotating line-up of Pretoria jazz musicians in the intimate Arena venue.",
                        "Arena", 9, 20, "120.00"),
                new Definition("South African State Theatre", "Open Auditions: Youth Theatre Programme",
                        "Free auditions for the 2027 youth theatre intake. Bring a two-minute monologue.",
                        "Rehearsal Room 2", 16, 10, "0.00"),
                new Definition("Church Square Night Market", "Friday Night Market",
                        "Forty food and craft stalls around the square with a DJ set from 19:00. Free entry.",
                        "Church Square", 2, 17, "0.00"),
                new Definition("Church Square Night Market", "Saturday Street Food Crawl",
                        "Guided tasting crawl across eight stalls, R150 per person including all tastings.",
                        "Church Square", 10, 18, "150.00"),
                new Definition("The Containers Food Hall", "Container Sessions: Live Acoustic",
                        "Unplugged sets from local artists on the shared seating deck. No cover charge.",
                        "Deck", 3, 19, "0.00"),
                new Definition("The Containers Food Hall", "Vendor Takeover: Kota vs Bunny Chow",
                        "Two vendor teams, one menu, R120 tasting ticket covering both dishes.",
                        "Hall 1", 12, 12, "120.00"),
                new Definition("Sammy Marks Square Pop-Up", "Saturday Makers Market",
                        "Vintage clothing, prints and street food from forty local makers.",
                        "Sammy Marks Square", 5, 9, "0.00"),
                new Definition("Jacaranda Rooftop Cinema", "Rooftop Screening: South African Classics",
                        "Open-air screening of a South African classic with beanbags and a pizza stand.",
                        "Rooftop", 6, 18, "90.00"),
                new Definition("Grand Central Social Club", "Rooftop Quiz Night",
                        "Five rounds, teams of four, bar tab for the winners. R40 per player.",
                        "Rooftop", 7, 18, "40.00"),
                new Definition("Burgers Park Open Air Sessions", "Sunday Park Yoga",
                        "Free community yoga session on the lawn. Bring a mat and water.",
                        "Burgers Park lawn", 8, 9, "0.00"),
                new Definition("Karoo Grill House", "Spring Menu Tasting Evening",
                        "Five-course tasting of the new spring menu, R395 per person including a welcome drink.",
                        "Private dining room", 14, 18, "395.00"));

        for (Definition definition : definitions) {
            Business business = businessIndex.get(definition.business());
            if (business == null) {
                continue;
            }
            EventListing event = new EventListing();
            event.setBusiness(business);
            event.setTitle(definition.title());
            event.setDescription(definition.description());
            event.setVenue(definition.venue());
            event.setEventDateTime(LocalDateTime.now().plusDays(definition.daysFromNow())
                    .withHour(definition.hour()).withMinute(0).withSecond(0).withNano(0));
            event.setTicketPrice(new BigDecimal(definition.price()));
            eventRepository.save(event);
        }
    }

    /* ------------------------------------------------- per-day hours seed */

    private static final Pattern HOURS_SEGMENT = Pattern.compile(
            "([A-Za-z]{3}(?:-[A-Za-z]{3})?|Daily)\\s+(\\d{2}:\\d{2})\\s*-\\s*(\\d{2}:\\d{2})");

    private static final Map<String, DayOfWeek> DAY_TOKENS = Map.ofEntries(
            Map.entry("mon", DayOfWeek.MONDAY),
            Map.entry("tue", DayOfWeek.TUESDAY),
            Map.entry("wed", DayOfWeek.WEDNESDAY),
            Map.entry("thu", DayOfWeek.THURSDAY),
            Map.entry("fri", DayOfWeek.FRIDAY),
            Map.entry("sat", DayOfWeek.SATURDAY),
            Map.entry("sun", DayOfWeek.SUNDAY));

    /**
     * Expands a human hours string such as "Mon-Fri 07:00 - 20:00, Sat 09:00 - 15:00"
     * into one {@code business_hours} row per weekday. Days that the string does not
     * mention are seeded as explicitly closed, which is what makes the per-day model
     * better than a single window: "closed on Sundays" becomes representable.
     */
    private void seedWeek(Business business, String human) {
        Map<DayOfWeek, BusinessHours> week = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            week.put(day, BusinessHours.closed(day));
        }
        if (human != null) {
            Matcher matcher = HOURS_SEGMENT.matcher(human);
            while (matcher.find()) {
                LocalTime open = LocalTime.parse(matcher.group(2));
                LocalTime close = normaliseClose(matcher.group(3), open);
                for (DayOfWeek day : expand(matcher.group(1))) {
                    week.put(day, BusinessHours.open(day, open, close));
                }
            }
        }
        business.getHours().clear();
        week.values().forEach(business::addHours);
    }

    /** "00:00" as a closing time means midnight, which we store as 23:59. */
    private LocalTime normaliseClose(String raw, LocalTime open) {
        LocalTime close = LocalTime.parse(raw);
        return close.equals(LocalTime.MIDNIGHT) && open.isAfter(LocalTime.MIDNIGHT)
                ? LocalTime.of(23, 59)
                : close;
    }

    /** "Daily" -> all seven days; "Mon-Fri" -> the range; "Sat" -> one day. */
    private List<DayOfWeek> expand(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        if (lower.equals("daily")) {
            return List.of(DayOfWeek.values());
        }
        String[] parts = lower.split("-");
        DayOfWeek from = DAY_TOKENS.get(parts[0]);
        DayOfWeek to = parts.length > 1 ? DAY_TOKENS.get(parts[1]) : from;
        if (from == null || to == null) {
            return List.of();
        }
        List<DayOfWeek> days = new ArrayList<>();
        DayOfWeek cursor = from;
        while (true) {
            days.add(cursor);
            if (cursor == to) {
                break;
            }
            cursor = cursor.plus(1);
        }
        return days;
    }
}
