package za.ac.richfield.cbdventures.assistant;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The student assistant's reply engine — a faithful port of the rule-based guide
 * from the CBDVentures Assistant project, minus the optional external AI provider
 * (which needed a server-side API key). Everything here runs offline against the
 * curated directory, so the demo never depends on a third party.
 */
@Service
public class AssistantService {

    /** One chat turn. */
    public record Message(String role, String content) { }

    /** What the client renders: the reply plus the place cards to offer. */
    public record Reply(String reply, List<String> suggestedPlaceIds, String mode, String warning) { }

    private static final Map<String, Pattern> PATTERNS = new LinkedHashMap<>();
    static {
        PATTERNS.put("food", Pattern.compile(
                "\\b(food|eat|hungry|restaurant|takeaway|lunch|breakfast|coffee|burger|chicken|shawarma|magwinya|bakery|fast food|pizza)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("groceries", Pattern.compile(
                "\\b(grocer|supermarket|shoprite|woolworths|spar|pick n pay|snack|bread|milk)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("health", Pattern.compile(
                "\\b(pharmacy|medicine|doctor|clinic|health|clicks|chemist|optometrist)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("printing", Pattern.compile(
                "\\b(print|photocopy|copy|stationery|assignment|binding|postnet|paper)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("study", Pattern.compile(
                "\\b(study|studying|library|books|research|quiet|reading|wifi|wi-fi)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("banking", Pattern.compile(
                "\\b(bank|atm|withdraw|cash|fnb|absa|capitec|nedbank)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("grooming", Pattern.compile(
                "\\b(barber|haircut|salon|braid|nails|beauty|hairdresser)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("transport", Pattern.compile(
                "\\b(bus|taxi|rank|transport|commute|train|ride|bus stop|gautrain)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("fun", Pattern.compile(
                "\\b(fun|entertainment|movie|theatre|theater|show|date|lounge|music|hangout)\\b", Pattern.CASE_INSENSITIVE));
    }

    private record FoodExample(Pattern match, String idea, String range) { }

    private static final List<FoodExample> FOOD_EXAMPLES = List.of(
            new FoodExample(Pattern.compile("magwinya|vetkoek|fat cake", Pattern.CASE_INSENSITIVE),
                    "a few magwinya or a magwinya-and-drink combination",
                    "R5–R20 per item (illustrative planning range, not this store’s price)"),
            new FoodExample(Pattern.compile("shawarma|asian delight", Pattern.CASE_INSENSITIVE),
                    "a small shawarma or a portion to share",
                    "R25–R65 (illustrative planning range, not this store’s price)"),
            new FoodExample(Pattern.compile("bake|coffee|espresso|expresso", Pattern.CASE_INSENSITIVE),
                    "a pastry or a hot drink",
                    "R15–R45 (illustrative planning range, not this store’s price)"),
            new FoodExample(Pattern.compile("honchos|chicken|top class|thuba|thaba", Pattern.CASE_INSENSITIVE),
                    "a snack-sized takeaway, chips or a value special if offered",
                    "R25–R65 (illustrative planning range, not this store’s price)"));

    private static final Pattern GREETING = Pattern.compile(
            "^\\s*(hi|hello|hey|howzit|good morning|good afternoon|good evening|how are you)[!? .]*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern THANKS = Pattern.compile(
            "^\\s*(thanks|thank you|thank u|shot|sharp|cool|okay|ok)[!? .]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HELP = Pattern.compile(
            "^\\s*(help|options|features|what can you do|what can this bot do|how does this chatbot work)[!? .]*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FOLLOW_UP = Pattern.compile(
            "\\b(this place|that place|the place|there|it|its|their|the library|that library|this library|tell me more|more about|interesting|special|unique|features|facilities|what can i do|what does it offer|what makes|why visit|what about|is it|can i|does it|how about|what is good|what is nice|first one|second one|third one|option [123]|number [123]|[123](?:st|nd|rd) one)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERESTING = Pattern.compile(
            "\\b(interesting|special|unique|features|facilities|what can i do|what does it offer|what makes|why visit|tell me more|more about|what is good|what is nice)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAVIGATION = Pattern.compile(
            "\\b(direction|directions|route|walk|walking|navigate|navigation|where is|how do i get|how can i get|how to get|how do i reach|how can i reach|take me to|get to|go to)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICING = Pattern.compile(
            "\\b(price|prices|cost|costs|costing|menu|special|meal deal|discount|afford|affordable|budget|cheap|cheapest|spend|buy|under)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY = Pattern.compile("\\bR\\s?(\\d{1,5})\\b|\\b(\\d{1,5})\\s?(?:rand|zar)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SERVICE_QUESTION = Pattern.compile(
            "\\b(wifi|wi-fi|internet|computers?|laptop|photocopy|printing|print|books|borrow|quiet|study space|seats?|desks?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QUESTION_FORM = Pattern.compile(
            "\\b(does|can|is|are|has|have|there|it|this|that)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRODUCTS = Pattern.compile(
            "\\b(what do they sell|what does it sell|what can i get there|what food|what kind of food|do they sell)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS = Pattern.compile(
            "\\b(open|opening|close|closing|hours|weekend|saturday|sunday)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS_FORM = Pattern.compile(
            "\\b(is|does|it|there|what|when|the)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEATHER = Pattern.compile("\\b(weather|temperature|rain|forecast)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NEARBY = Pattern.compile(
            "\\b(near|nearby|closest|places|recommend|find|looking for|suggest|around campus)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDINAL = Pattern.compile(
            "\\b(?:first|second|third|1st|2nd|3rd|option 1|option 2|option 3|number 1|number 2|number 3)(?: one| place| library)?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLANET = Pattern.compile(
            "\\b(?:on|in|at|near|around|from|the planet|planet)\\s+(?:the\\s+)?(mars|venus|jupiter|saturn|uranus|neptune|pluto|mercury|moon)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OTHER_CITY = Pattern.compile(
            "\\b(?:in|at|near|around|from|to)\\s+(?:the\\s+)?(johannesburg|joburg|durban|cape town|bloemfontein|polokwane|centurion|hatfield|menlyn|sandton|soweto|london|new york|paris|tokyo)\\b",
            Pattern.CASE_INSENSITIVE);

    private final AssistantDirectory directory;

    public AssistantService(AssistantDirectory directory) {
        this.directory = directory;
    }

    /* ------------------------------------------------------------------ API */

    public Reply respond(String question, List<Message> history, String viewerName) {
        String safe = question == null ? "" : question.replaceAll("\\s+", " ").trim();
        if (safe.isEmpty()) return new Reply("Ask me about food, study spots or directions around campus.", List.of(), "local", null);

        Optional<Reply> outOfArea = outOfAreaRequest(safe);
        if (outOfArea.isPresent()) return outOfArea.get();

        List<AssistantPlace> nearby = directory.nearby();
        List<AssistantPlace> pending = directory.pending();
        List<AssistantPlace> all = directory.everything();

        AssistantPlace explicit = matchPlace(safe, all).orElse(null);
        AssistantPlace context = selectedFromHistory(safe, history, nearby, pending);
        boolean followUp = FOLLOW_UP.matcher(safe).find();
        AssistantPlace hit = explicit != null ? explicit : (followUp ? context : null);

        Matcher moneyMatcher = MONEY.matcher(safe);
        String money = moneyMatcher.find()
                ? (moneyMatcher.group(1) != null ? moneyMatcher.group(1) : moneyMatcher.group(2))
                : null;
        boolean pricing = PRICING.matcher(safe).find() || money != null;
        boolean navigation = NAVIGATION.matcher(safe).find();
        boolean interesting = INTERESTING.matcher(safe).find();
        boolean serviceQuestion = SERVICE_QUESTION.matcher(safe).find() && QUESTION_FORM.matcher(safe).find();
        boolean products = PRODUCTS.matcher(safe).find();
        boolean hoursQuestion = HOURS.matcher(safe).find() && HOURS_FORM.matcher(safe).find();
        String category = detectCategory(safe);

        if (GREETING.matcher(safe).matches()) {
            String who = viewerName == null || viewerName.isBlank()
                    ? ""
                    : " " + viewerName.trim().split("\\s+")[0] + "!";
            return new Reply("Hey" + who + " 👋 What are you looking for today — somewhere to eat, a place to study, "
                    + "or directions from Richfield?", List.of(), "local", null);
        }
        if (THANKS.matcher(safe).matches()) {
            return new Reply("You’re welcome! 😊 Anything else you want to find around campus?", List.of(), "local", null);
        }
        if (HELP.matcher(safe).matches()) {
            return new Reply("I can help you find food, study spaces, shops and services near Richfield. "
                    + "Ask me about a place, request walking directions from campus, or tell me your budget for a food idea.",
                    List.of(), "local", null);
        }

        if (hit != null) {
            if (products && "food".equals(hit.category())) {
                return new Reply(placeIntro(hit) + "\n\nI don’t have a complete current menu for " + hit.name()
                        + ". You can open its card to recognise the shop, or ask me for a student-budget idea.",
                        List.of(hit.id()), "local", null);
            }
            if (hoursQuestion) {
                return new Reply("I don’t have today’s opening hours for " + hit.name() + ". It’s listed at "
                        + hit.address() + "; check with the venue before making a special trip. "
                        + "I can still show you directions from Richfield.", List.of(hit.id()), "local", null);
            }
            if ((interesting || serviceQuestion) && money == null && !PRICING.matcher(safe).find()) {
                return new Reply(interestingAnswer(hit), List.of(hit.id()), "local", null);
            }
            if (pricing) {
                return new Reply(priceAnswer(money, hit), List.of(hit.id()), "local", null);
            }
            return new Reply(placeAnswer(hit, navigation), List.of(hit.id()), "local", null);
        }

        if (followUp && PRICING.matcher(safe).find() == false
                && Pattern.compile("\\b(this place|that place|there|it|interesting|special|tell me more|more about|what about|the library)\\b",
                        Pattern.CASE_INSENSITIVE).matcher(safe).find()) {
            List<String> studyIds = new ArrayList<>();
            nearby.stream().filter(p -> "study".equals(p.category())).limit(2).forEach(p -> studyIds.add(p.id()));
            pending.stream().filter(p -> "study".equals(p.category())).limit(2).forEach(p -> studyIds.add(p.id()));
            return new Reply("Which place would you like to know more about — Richfield Pretoria campus library or "
                    + "Es’kia Mphahlele Community Library? Tap a place or tell me its name.",
                    studyIds.stream().limit(2).toList(), "local", null);
        }

        if (pricing) {
            List<AssistantPlace> food = new ArrayList<>();
            nearby.stream().filter(p -> "food".equals(p.category())).limit(6).forEach(food::add);
            if (food.size() < 6) {
                pending.stream().filter(p -> "food".equals(p.category())).limit(6 - food.size()).forEach(food::add);
            }
            List<String> ids = food.stream().map(AssistantPlace::id).toList();
            return new Reply(priceAnswer(money, null) + "\n\nSome places to compare: "
                    + String.join(", ", food.stream().map(AssistantPlace::name).toList())
                    + ". Tap a card to see the business and directions.", ids, "local", null);
        }

        if (WEATHER.matcher(safe).find()) {
            return new Reply("I can help you find places around campus, but I’m not connected to a live weather forecast yet, "
                    + "so I don’t want to guess the weather in Pretoria. Check a weather service for the latest conditions.",
                    List.of(), "local", null);
        }

        if (category != null || navigation || NEARBY.matcher(safe).find()) {
            return new Reply(nearbyList(category, nearby, pending), listIds(category, nearby, pending), "local", null);
        }

        return new Reply("I’m best at helping with places around Richfield Pretoria — food, libraries, shopping, "
                + "transport and walking directions. Tell me what you’re looking for and I’ll help you find an option.",
                List.of(), "local", null);
    }

    /* --------------------------------------------------------------- pieces */

    /** "on campus" reads better than "0 m" for the campus library. */
    private static String distancePhrase(AssistantPlace place) {
        return place.distanceMeters() < 25
                ? "on campus"
                : "about " + place.distanceMeters() + " m from campus";
    }

    /**
     * Bullet suffix for the "nearby" list. Never repeats what the address
     * already says (the campus library's address already ends in "on campus").
     */
    private static String distanceSuffix(AssistantPlace place) {
        if (!place.withinRadius()) return "";
        String phrase = distancePhrase(place);
        if (place.address().toLowerCase().contains(phrase)) return "";
        return " (" + phrase + ")";
    }

    private String nearbyList(String category, List<AssistantPlace> nearby, List<AssistantPlace> pending) {
        List<AssistantPlace> matched = filter(category, nearby, 5);
        List<AssistantPlace> unchecked = filter(category, pending, 5);
        List<String> lines = new ArrayList<>();
        for (AssistantPlace place : matched) {
            lines.add("• " + place.name() + " — " + place.address() + distanceSuffix(place));
        }
        for (AssistantPlace place : unchecked) {
            lines.add("• " + place.name() + " — " + place.address());
        }
        String label = category == null ? "places" : AssistantDirectory.labelFor(category).toLowerCase();
        if (lines.isEmpty()) {
            return "I don’t have a " + label + " recommendation to show you just yet. "
                    + "Try another category or ask me about a specific place.";
        }
        List<String> shown = lines.subList(0, Math.min(8, lines.size()));
        String note = unchecked.isEmpty() ? ""
                : " Some listings are student-submitted, so their exact distance has not been checked.";
        return "Here are some " + label + " options to check out from Richfield:\n\n"
                + String.join("\n", shown)
                + "\n\nTap a place card for its photo and directions starting at campus." + note;
    }

    private List<String> listIds(String category, List<AssistantPlace> nearby, List<AssistantPlace> pending) {
        List<AssistantPlace> combined = new ArrayList<>(filter(category, nearby, 5));
        combined.addAll(filter(category, pending, 5));
        return combined.stream().limit(8).map(AssistantPlace::id).toList();
    }

    private List<AssistantPlace> filter(String category, List<AssistantPlace> source, int limit) {
        return source.stream()
                .filter(place -> category == null || place.category().equals(category))
                .limit(limit)
                .toList();
    }

    private String detectCategory(String question) {
        for (Map.Entry<String, Pattern> entry : PATTERNS.entrySet()) {
            if (entry.getValue().matcher(question).find()) return entry.getKey();
        }
        return null;
    }

    private Optional<Reply> outOfAreaRequest(String question) {
        String words = AssistantDirectory.normaliseWords(question);
        Matcher planet = PLANET.matcher(words);
        if (planet.find()) {
            String name = planet.group(1).equals("moon")
                    ? "the Moon"
                    : planet.group(1).substring(0, 1).toUpperCase() + planet.group(1).substring(1);
            return Optional.of(new Reply("There aren't any public restaurants or shops on " + name
                    + " that I can recommend. My directory covers places around Richfield Pretoria, so I can help you "
                    + "find real nearby options instead.", List.of(), "local", null));
        }
        Matcher city = OTHER_CITY.matcher(words);
        if (city.find()) {
            String location = titleCase(city.group(1));
            return Optional.of(new Reply("I cover places within the Richfield Pretoria campus area, so I can't reliably "
                    + "recommend businesses in " + location + " from this directory. If you want places near Richfield, "
                    + "I can help with those.", List.of(), "local", null));
        }
        return Optional.empty();
    }

    private static String titleCase(String value) {
        String[] parts = value.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    /** Exact alias containment first, then a bounded edit-distance fallback (typo tolerance). */
    Optional<AssistantPlace> matchPlace(String question, List<AssistantPlace> all) {
        String padded = " " + AssistantDirectory.normaliseWords(question) + " ";
        AssistantPlace best = null;
        int score = 0;
        for (AssistantPlace place : all) {
            for (String phrase : phrases(place)) {
                String alias = AssistantDirectory.normaliseWords(phrase);
                if (alias.length() < 4 || !padded.contains(" " + alias + " ")) continue;
                if (alias.length() > score) {
                    best = place;
                    score = alias.length();
                }
            }
        }
        if (best != null) return Optional.of(best);

        String[] words = AssistantDirectory.normaliseWords(question).split(" ");
        AssistantPlace candidate = null;
        double lowest = Double.MAX_VALUE;
        boolean tied = false;
        for (AssistantPlace place : all) {
            for (String phrase : phrases(place)) {
                String[] aliasWords = AssistantDirectory.normaliseWords(phrase).split(" ");
                String target = String.join("", aliasWords);
                if (target.length() < 7 || words.length < aliasWords.length) continue;
                int threshold = aliasWords.length == 1 ? 1 : target.length() >= 11 ? 3 : 2;
                for (int i = 0; i + aliasWords.length <= words.length; i++) {
                    String fragment = String.join("", java.util.Arrays.copyOfRange(words, i, i + aliasWords.length));
                    int distance = editDistance(fragment, target, threshold);
                    if (distance == 0 || distance > threshold) continue;
                    double adjusted = (double) distance / target.length();
                    if (adjusted < lowest - 0.001) {
                        candidate = place;
                        lowest = adjusted;
                        tied = false;
                    } else if (Math.abs(adjusted - lowest) < 0.001 && candidate != null && !candidate.id().equals(place.id())) {
                        tied = true;
                    }
                }
            }
        }
        return tied ? Optional.empty() : Optional.ofNullable(candidate);
    }

    private List<String> phrases(AssistantPlace place) {
        List<String> out = new ArrayList<>();
        out.add(place.name());
        out.addAll(place.aliases());
        return out;
    }

    private AssistantPlace selectedFromHistory(String question, List<Message> history,
                                               List<AssistantPlace> nearby, List<AssistantPlace> pending) {
        List<AssistantPlace> all = new ArrayList<>(nearby);
        all.addAll(pending);

        Matcher ordinal = ORDINAL.matcher(AssistantDirectory.normaliseWords(question));
        if (ordinal.find()) {
            String text = ordinal.group();
            int rank = (text.contains("first") || text.contains("1")) ? 0
                    : (text.contains("second") || text.contains("2")) ? 1 : 2;
            String lastCategory = null;
            for (int i = history.size() - 1; i >= 0; i--) {
                Message message = history.get(i);
                if (!"user".equals(message.role())) continue;
                String detected = detectCategory(message.content());
                if (detected != null && lastCategory == null) {
                    lastCategory = detected;
                    break;
                }
            }
            if (lastCategory != null) {
                List<AssistantPlace> pool = new ArrayList<>(filter(lastCategory, nearby, 5));
                pool.addAll(filter(lastCategory, pending, 5));
                return rank < pool.size() ? pool.get(rank) : null;
            }
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            Message message = history.get(i);
            if (!"user".equals(message.role())) continue;
            AssistantPlace found = matchPlace(message.content(), all).orElse(null);
            if (found != null) return found;
            if (Pattern.compile("\\b(where can i|places to|nearby|recommend|find|show)\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(message.content()).find() && detectCategory(message.content()) != null) {
                break;
            }
        }
        return null;
    }

    private String placeIntro(AssistantPlace place) {
        return switch (place.id()) {
            case "student:magwinya-zone" -> "Magwinya Zone is a takeaway spot for magwinya, the classic South African "
                    + "fried-dough snack. It could be a good option when you want a quick bite between classes.";
            case "student:honchos" -> "Honchos is a chicken and fast-food takeaway. It’s one of the options students "
                    + "can consider when looking for a quick meal in Pretoria Central.";
            case "student:asian-delight-shawarma" -> "Asian Delight Shawarma is a takeaway for shawarma and other food. "
                    + "You can also see its storefront photo in the guide.";
            case "student:top-class-fast-foods" -> "Top Class Fast Foods is a takeaway spot on Pretorius Street. "
                    + "Its storefront photo is included in the guide.";
            case "student:quick-bake" -> "Quick Bake is a bakery and confectionery spot. Its storefront sign also mentions coffee.";
            case "student:thuba-rush-kj" -> "Thuba Rush KJ is a takeaway on Pretorius Street. "
                    + "We have a photo of its storefront so you can recognise it.";
            case "student:four-four-two" -> "Four Four Two is a sports and music lounge. "
                    + "It’s more of a hangout option than a study spot.";
            case "local:richfield-library" -> "The Richfield Pretoria campus library is conveniently on campus, "
                    + "so it is an option to consider for studying between lectures.";
            case "student:sammy-marks-library" -> "Es’kia Mphahlele Community Library is a public library near Sammy Marks "
                    + "with reading and study areas. It also has a documented American Corner.";
            case "student:absa-building" -> "ABSA Building is listed in the student guide as a banking-related location "
                    + "in Pretoria Central. Bank and ATM services at that address still need confirmation.";
            default -> switch (place.category()) {
                case "food" -> place.name() + " is one of the food options in the student directory.";
                case "study" -> place.name() + " is a study option in the student directory.";
                case "banking" -> place.name() + " is a banking-related location in the student directory.";
                case "transport" -> place.name() + " is a transport option in the student directory.";
                case "fun" -> place.name() + " is an entertainment option in the student directory.";
                case "health" -> place.name() + " is a health-related location in the student directory.";
                case "printing" -> place.name() + " is a printing or stationery option in the student directory.";
                case "grooming" -> place.name() + " is a grooming or salon option in the student directory.";
                case "groceries" -> place.name() + " is a grocery option in the student directory.";
                default -> place.name() + " is listed in the student directory.";
            };
        };
    }

    private String priceAnswer(String amountText, AssistantPlace place) {
        Integer budget = null;
        if (amountText != null) {
            try { budget = Integer.parseInt(amountText); } catch (NumberFormatException ignored) { }
        }
        FoodExample example = place == null ? null : FOOD_EXAMPLES.stream()
                .filter(candidate -> candidate.match().matcher(place.name()).find())
                .findFirst().orElse(null);
        String name = place == null ? "a food spot near campus" : place.name();

        if (place != null && !"food".equals(place.category())) {
            return "I don’t have confirmed current charges for " + name + ". If you’re checking entry fees or services, "
                    + "it’s best to ask the venue directly. I can show you where it is and how to get there from Richfield.";
        }
        String suggestion = "student:magwinya-zone".equals(place == null ? null : place.id())
                ? "For a quick bite, magwinya is the obvious thing to ask about."
                : example != null ? "You could ask about " + example.idea() + "."
                : "A snack, a small takeaway or a drink could be worth comparing.";

        if (budget != null) {
            String plan = budget <= 20 ? "Start by asking for the cheapest single snack."
                    : budget <= 60 ? "You could aim for roughly R30 for something to eat and keep R20 for a drink or another snack."
                    : "You could reserve around R" + Math.round(budget * 0.7) + " for food and keep R"
                      + (budget - Math.round(budget * 0.7)) + " for a drink or later.";
            return "With R" + budget + ", here’s how I’d plan it at " + name + ": " + suggestion + " " + plan
                    + "\n\nThose amounts are example budgets, not confirmed prices at the shop. Check the menu before ordering.";
        }
        return "For " + name + ", I don’t have a current confirmed price list. " + suggestion
                + " As a rough planning example, a small snack might be R5–R20, while a larger takeaway could cost more. "
                + "Those are illustrative amounts, not the shop’s actual prices.\n\n"
                + "Give me a budget like R30, R50 or R100 and I’ll suggest a plan.";
    }

    private String interestingAnswer(AssistantPlace place) {
        List<String> facts = place.highlights();
        if (!facts.isEmpty()) {
            String details = facts.size() > 1
                    ? String.join("\n", facts.stream().map(fact -> "• " + fact).toList())
                    : facts.get(0);
            String access = "study".equals(place.category())
                    ? " Some services and current access may vary, so check with the library before visiting." : "";
            return place.name() + " has a few things worth knowing about:\n\n" + details + "\n\n"
                    + "These are the details currently listed in the student guide." + access;
        }
        List<String> lines = new ArrayList<>();
        lines.add(placeIntro(place));
        lines.add(addressDetail(place));
        if ("food".equals(place.category())) {
            lines.add("If you’re deciding what to buy, I can help you plan a student budget. "
                    + "The shop’s current menu prices aren’t listed here.");
        }
        if ("fun".equals(place.category())) {
            lines.add("I can show you directions, but I don’t have a confirmed current events schedule.");
        }
        lines.add("Want directions from Richfield or suggestions for a student budget?");
        return String.join("\n\n", lines);
    }

    private String addressDetail(AssistantPlace place) {
        return "📍 The guide lists it at " + place.address() + ".";
    }

    private String placeAnswer(AssistantPlace place, boolean navigation) {
        List<String> lines = new ArrayList<>();
        lines.add(placeIntro(place));
        lines.add(addressDetail(place));
        if (navigation) {
            lines.add("Tap Map on the place card to view the destination, or use the walking-directions link to start "
                    + "from Richfield, 291 Helen Joseph Street.");
            if (!place.verificationNote().isBlank()) lines.add(place.verificationNote());
        } else {
            lines.add("You can tap its card for the photo and directions from Richfield.");
        }
        if (place.withinRadius()) {
            lines.add(place.distanceMeters() < 25
                    ? "It is on the campus itself, so there is barely any walk once you are at the Richfield gate."
                    : "The map puts it about " + place.distanceMeters() + " metres from campus in a straight line. "
                      + "The walking route may be longer.");
        } else {
            lines.add("I don’t have a reliable distance from campus yet, so I won’t guess the metres. "
                    + "The map link can help you check the walking route.");
        }
        return String.join("\n\n", lines);
    }

    /** Levenshtein distance with early exit. */
    static int editDistance(String a, String b, int limit) {
        if (Math.abs(a.length() - b.length()) > limit) return limit + 1;
        int[] previous = new int[b.length() + 1];
        for (int i = 0; i <= b.length(); i++) previous[i] = i;
        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1];
            current[0] = i;
            int smallest = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int next = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
                current[j] = next;
                if (next < smallest) smallest = next;
            }
            if (smallest > limit) return limit + 1;
            previous = current;
        }
        return previous[b.length()];
    }
}
