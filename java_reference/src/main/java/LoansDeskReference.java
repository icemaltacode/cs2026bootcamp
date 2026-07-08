/*
 * LoansDeskReference.java
 * =======================
 *
 * CODESPRINT 2026 - Post-Secondary Category - BOOTCAMP Task
 * "Loan Desk": an equipment-loan catalogue for laptops, cameras and robotics kits.
 *
 * This is the official Java reference solution. It is a faithful port of the
 * Python reference (loansdesk_reference.py): same structure, same behaviour,
 * same on-screen look. It implements every required functionality and adds the
 * same clearly-labelled extras.
 *
 * JSON is read and written with Gson (a small, single-jar library) using its
 * tree model - JsonObject / JsonArray - which is the direct analogue of the
 * Python version reading and writing plain dicts and lists. Gson is pulled in
 * by the build tool (see build.gradle); nothing else is needed.
 *
 * Requirements coverage
 * ---------------------
 *   #1 Init & loading .... Catalogue.load, empty-on-missing/unreadable file
 *   #2 Item types ........ Item + Laptop / Camera / RoboticsKit subclasses
 *   #3 View .............. renderCatalogue (aligned, adaptive column widths)
 *   #4 Add ............... LoanDeskApp.add + per-type interactive builders
 *   #5 Borrow / return ... LoanDeskApp.borrowReturn (toggle by ID)
 *   #6 Save .............. Catalogue.save (type tag + nested lens list kept)
 *   #7 Check routines .... validated-input layer; try/catch around all I/O
 *   #8 Modular ........... classes + methods, nested data, clear naming, comments
 *
 * Extras (each labelled "extra" in the menu / code):
 *   * Search by name, type, or availability.
 *   * Catalogue statistics (totals + per-type breakdown).
 *   * Unsaved-changes guard on exit.
 *   * Optional catalogue path as a command-line argument.
 *   * Atomic save with a .bak backup, and skip-bad-record loading.
 *
 *   gradle run                       (uses ./catalogue.json)
 *   gradle run --args="other.json"   (uses a file you choose)
 */

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class LoansDeskReference {

    // =======================================================================
    //  Item model  (Functionality #2)
    // =======================================================================
    //
    // Every concrete item type extends `Item`. A type is registered once in
    // TYPES with its stored tag, its display name, a deserializer (record ->
    // Item) and an interactive builder (id, name -> Item). Loading, the add
    // menu and the type dispatch all read that registry, so no part of the
    // program carries a hard-coded list of types.

    /** Abstract base for anything the loan desk can lend out. */
    abstract static class Item {
        final int id;
        final String name;
        boolean available;

        Item(int id, String name, boolean available) {
            this.id = id;
            this.name = name;
            this.available = available;
        }

        /** The value stored in the "type" field of the file. */
        abstract String typeTag();

        /** What the user sees in the Type column. */
        abstract String displayName();

        /** The type-specific second line shown in the catalogue listing. */
        abstract String details();

        /** Add only the type-specific fields to the record being written. */
        abstract void writeExtraFields(JsonObject record);

        /** Human-readable availability (Functionality #3). */
        String status() {
            return available ? "Available" : "On loan";
        }

        /**
         * Full serialisable record: common fields + type tag + extras. Key
         * order matches the sample in the task sheet (type and id first).
         */
        JsonObject toJson() {
            JsonObject record = new JsonObject();
            record.addProperty("type", typeTag());
            record.addProperty("id", id);
            record.addProperty("name", name);
            record.addProperty("available", available);
            writeExtraFields(record);
            return record;
        }

        /**
         * Rebuild the correct Item subclass from a saved record. Throws
         * IllegalArgumentException on anything malformed, so the loader can
         * skip the one bad record instead of aborting the whole catalogue.
         */
        static Item fromJson(JsonElement element) {
            if (element == null || !element.isJsonObject()) {
                throw new IllegalArgumentException("record is not an object");
            }
            JsonObject record = element.getAsJsonObject();

            String tag = asStringOrNull(record.get("type"));
            ItemType type = tag == null ? null : BY_TAG.get(tag);
            if (type == null) {
                throw new IllegalArgumentException(
                        "unknown item type " + repr(record.get("type")));
            }

            int id = asStoredId(record.get("id"));

            String name = asStringOrNull(record.get("name"));
            if (name == null || name.strip().isEmpty()) {
                throw new IllegalArgumentException("missing or empty name");
            }

            boolean available = true;
            if (record.has("available")) {
                JsonElement flag = record.get("available");
                if (!isBoolean(flag)) {
                    throw new IllegalArgumentException(
                            "invalid 'available' flag " + repr(flag));
                }
                available = flag.getAsBoolean();
            }

            return type.deserializer.build(id, name.strip(), available, record);
        }
    }

    // --- helpers used by the loaders to validate stored primitive fields ---

    /** A whole number > 0, or IllegalArgumentException. */
    static int asPositiveInt(JsonObject record, String field) {
        JsonElement element = record.get(field);
        Long value = asWholeNumber(element);
        if (value == null) {
            throw new IllegalArgumentException(
                    field + " must be a whole number, got " + repr(element));
        }
        if (value <= 0) {
            throw new IllegalArgumentException(
                    field + " must be greater than zero, got " + value);
        }
        return value.intValue();
    }

    /** A non-empty string, or IllegalArgumentException. */
    static String asNonEmptyString(JsonObject record, String field) {
        String value = asStringOrNull(record.get(field));
        if (value == null || value.strip().isEmpty()) {
            throw new IllegalArgumentException(field + " must be a non-empty string");
        }
        return value.strip();
    }

    /** The stored id: a whole number > 0. */
    static int asStoredId(JsonElement element) {
        Long value = asWholeNumber(element);
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("invalid id " + repr(element));
        }
        return value.intValue();
    }

    /** Return the value as a whole number, or null if it is not one. */
    static Long asWholeNumber(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        double d = element.getAsDouble();
        if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.floor(d)) {
            return null;
        }
        return element.getAsLong();
    }

    static boolean isBoolean(JsonElement element) {
        return element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isBoolean();
    }

    static String asStringOrNull(JsonElement element) {
        if (element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString()) {
            return element.getAsString();
        }
        return null;
    }

    /** A short Python-repr-ish rendering used only in error messages. */
    static String repr(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "null";
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return "'" + element.getAsString() + "'";
        }
        return element.toString();
    }

    // =======================================================================
    //  Concrete item types  (Table 1 in the task sheet)
    // =======================================================================

    static final class Laptop extends Item {
        final String osName;
        final int ramGb;

        Laptop(int id, String name, String osName, int ramGb, boolean available) {
            super(id, name, available);
            this.osName = osName;
            this.ramGb = ramGb;
        }

        @Override String typeTag() { return "laptop"; }
        @Override String displayName() { return "laptop"; }
        @Override String details() { return osName + ", " + ramGb + " GB RAM"; }

        @Override void writeExtraFields(JsonObject record) {
            record.addProperty("os", osName);
            record.addProperty("ram_gb", ramGb);
        }
    }

    static final class Camera extends Item {
        final int megapixels;
        final List<String> lenses;   // nested data

        Camera(int id, String name, int megapixels, List<String> lenses,
               boolean available) {
            super(id, name, available);
            this.megapixels = megapixels;
            this.lenses = new ArrayList<>(lenses);   // defensive copy
        }

        @Override String typeTag() { return "camera"; }
        @Override String displayName() { return "camera"; }

        @Override String details() {
            String list = lenses.isEmpty() ? "no lenses" : String.join(", ", lenses);
            return megapixels + " MP; lenses: " + list;
        }

        @Override void writeExtraFields(JsonObject record) {
            record.addProperty("megapixels", megapixels);
            JsonArray array = new JsonArray();
            for (String lens : lenses) {
                array.add(lens);
            }
            record.add("lenses", array);
        }
    }

    static final class RoboticsKit extends Item {
        final int pieces;
        final String ageRange;

        RoboticsKit(int id, String name, int pieces, String ageRange,
                    boolean available) {
            super(id, name, available);
            this.pieces = pieces;
            this.ageRange = ageRange;
        }

        @Override String typeTag() { return "robotics kit"; }
        @Override String displayName() { return "robotics kit"; }
        @Override String details() { return pieces + " pieces, ages " + ageRange; }

        @Override void writeExtraFields(JsonObject record) {
            record.addProperty("pieces", pieces);
            record.addProperty("age_range", ageRange);
        }
    }

    /** Read a camera's nested lens list from a record (validated). */
    static List<String> lensesFromRecord(JsonObject record) {
        List<String> lenses = new ArrayList<>();
        if (!record.has("lenses")) {
            return lenses;
        }
        JsonElement raw = record.get("lenses");
        if (!raw.isJsonArray()) {
            throw new IllegalArgumentException("lenses must be a list");
        }
        for (JsonElement element : raw.getAsJsonArray()) {
            String lens = element.isJsonPrimitive()
                    ? element.getAsString() : element.toString();
            lens = lens.strip();
            if (!lens.isEmpty()) {
                lenses.add(lens);
            }
        }
        return lenses;
    }

    // =======================================================================
    //  Type registry  (data-driven: the one place that knows the types)
    // =======================================================================

    /** Builds an item from a stored record (used when loading). */
    interface Deserializer {
        Item build(int id, String name, boolean available, JsonObject record);
    }

    /** Builds an item by prompting the user (used by Add). */
    interface Builder {
        Item build(int id, String name);
    }

    static final class ItemType {
        final String tag;
        final String displayName;
        final List<String> aliases;
        final Deserializer deserializer;
        final Builder builder;

        ItemType(String tag, String displayName, List<String> aliases,
                 Deserializer deserializer, Builder builder) {
            this.tag = tag;
            this.displayName = displayName;
            this.aliases = aliases;
            this.deserializer = deserializer;
            this.builder = builder;
        }
    }

    // Order controls the add-item menu. BY_TAG also holds any aliases, so a
    // file that spelled a type differently still loads.
    static final List<ItemType> TYPES = new ArrayList<>();
    static final Map<String, ItemType> BY_TAG = new HashMap<>();

    static void register(ItemType type) {
        TYPES.add(type);
        BY_TAG.put(type.tag, type);
        for (String alias : type.aliases) {
            BY_TAG.put(alias, type);
        }
    }

    static {
        register(new ItemType("laptop", "laptop", List.of(),
                (id, name, available, record) -> new Laptop(id, name,
                        asNonEmptyString(record, "os"),
                        asPositiveInt(record, "ram_gb"), available),
                (id, name) -> new Laptop(id, name,
                        promptNonEmpty("Operating system"),
                        promptPositiveInt("RAM in GB"), true)));

        register(new ItemType("camera", "camera", List.of(),
                (id, name, available, record) -> new Camera(id, name,
                        asPositiveInt(record, "megapixels"),
                        lensesFromRecord(record), available),
                (id, name) -> new Camera(id, name,
                        promptPositiveInt("Megapixels"), promptLenses(), true)));

        // Canonical tag matches the official starter catalogue ("robotics
        // kit"); the short tag "robotics" is accepted as an alias when loading.
        register(new ItemType("robotics kit", "robotics kit", List.of("robotics"),
                (id, name, available, record) -> new RoboticsKit(id, name,
                        asPositiveInt(record, "pieces"),
                        asNonEmptyString(record, "age_range"), available),
                (id, name) -> new RoboticsKit(id, name,
                        promptPositiveInt("Piece count"),
                        promptNonEmpty("Recommended age range (e.g. 10-14 or 8+)"),
                        true)));
    }

    // =======================================================================
    //  Catalogue  (Functionality #1 and #6)
    // =======================================================================

    static final class Catalogue {
        private final List<Item> items;

        Catalogue(List<Item> items) {
            this.items = items;
        }

        List<Item> items() {
            return new ArrayList<>(items);
        }

        int size() {
            return items.size();
        }

        /** Smallest unused positive id (max + 1; 1 for an empty catalogue). */
        int nextId() {
            int max = 0;
            for (Item item : items) {
                max = Math.max(max, item.id);
            }
            return max + 1;
        }

        Item get(int id) {
            for (Item item : items) {
                if (item.id == id) {
                    return item;
                }
            }
            return null;
        }

        /** Case-insensitive name lookup, used by the (extra) uniqueness check. */
        boolean nameExists(String name) {
            String target = name.strip().toLowerCase();
            for (Item item : items) {
                if (item.name.toLowerCase().equals(target)) {
                    return true;
                }
            }
            return false;
        }

        void add(Item item) {
            items.add(item);
        }

        /**
         * Load a catalogue from `path`. Never throws (Functionality #7).
         * A missing/unreadable file, malformed JSON or a top-level structure
         * that is not an array all yield an empty catalogue with a message.
         * Individual records that fail to parse are skipped with a warning.
         */
        static Catalogue load(String path) {
            Path file = Paths.get(path);
            if (!Files.exists(file)) {
                sayInfo("No catalogue file at '" + path
                        + "'. Starting with an empty catalogue.");
                return new Catalogue(new ArrayList<>());
            }

            JsonElement root;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader);
            } catch (JsonParseException exc) {
                sayErr("'" + path + "' is not valid JSON. "
                        + "Starting with an empty catalogue.");
                return new Catalogue(new ArrayList<>());
            } catch (IOException exc) {
                sayErr("Could not read '" + path + "' (" + exc.getMessage()
                        + "). Starting with an empty catalogue.");
                return new Catalogue(new ArrayList<>());
            }

            if (root == null || !root.isJsonArray()) {
                sayErr("'" + path + "' is not a list of items. "
                        + "Starting with an empty catalogue.");
                return new Catalogue(new ArrayList<>());
            }

            List<Item> loaded = new ArrayList<>();
            Set<Integer> seenIds = new HashSet<>();
            int index = 0;
            for (JsonElement element : root.getAsJsonArray()) {
                index++;
                Item item;
                try {
                    item = Item.fromJson(element);
                } catch (IllegalArgumentException exc) {
                    sayErr("Skipping record #" + index + ": " + exc.getMessage());
                    continue;
                }
                if (seenIds.contains(item.id)) {
                    sayErr("Skipping record #" + index + ": duplicate id " + item.id);
                    continue;
                }
                seenIds.add(item.id);
                loaded.add(item);
            }

            sayInfo("Loaded " + loaded.size() + " item(s) from '" + path + "'.");
            return new Catalogue(loaded);
        }

        /**
         * Write the catalogue atomically (Functionality #6). Serialise to a
         * sibling temp file, then move it over the target (atomic where the OS
         * allows). The previous file is copied to '<path>.bak' first, so a
         * crash mid-write can never leave a half-written catalogue.
         */
        void save(String path) throws IOException {
            JsonArray payload = new JsonArray();
            for (Item item : items) {
                payload.add(item.toJson());
            }
            String content = PRETTY_GSON.toJson(payload) + "\n";

            Path target = Paths.get(path);
            Path directory = target.toAbsolutePath().getParent();
            if (directory == null) {
                directory = Paths.get(".");
            }

            // Roll the existing file to a backup before we touch anything.
            if (Files.exists(target)) {
                try {
                    Files.copy(target, Paths.get(path + ".bak"), REPLACE_EXISTING);
                } catch (IOException ignore) {
                    // a missing backup is not worth aborting the save for
                }
            }

            Path temp = directory.resolve("." + target.getFileName() + ".tmp");
            Files.write(temp, content.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, target, REPLACE_EXISTING, ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, REPLACE_EXISTING);
            }
        }
    }

    // Pretty printer configured to match the Python reference's file style:
    // 2-space indent, and no HTML escaping so characters like & < > and '
    // are written literally rather than as backslash-u unicode escapes.
    static final com.google.gson.Gson PRETTY_GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // =======================================================================
    //  Terminal styling  (Functionality #3)
    // =======================================================================
    //
    // One frame everywhere: an outer border with content laid out by padding
    // inside it - no interior dividers, so nothing can misalign. Colour is
    // used only for a live terminal, and switches off when output is piped or
    // NO_COLOR is set, so the program stays clean plain text for grading.

    static final boolean USE_COLOUR = detectColour();

    /**
     * Decide whether to emit ANSI colour. Colour is used for an interactive
     * terminal and switched off when output is piped/redirected (so it stays
     * clean plain text for grading).
     *
     *   NO_COLOR set     -> always off (respects the https://no-color.org convention)
     *   FORCE_COLOR set  -> always on  (handy under `gradle run`, which pipes
     *                                   stdio so a terminal can't be detected)
     *   otherwise        -> on only when attached to a real console
     */
    static boolean detectColour() {
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        String force = System.getenv("FORCE_COLOR");
        if (force != null && !force.isEmpty() && !force.equals("0")) {
            return true;
        }
        return System.console() != null;
    }

    static final String RESET = "\u001b[0m";
    static final String BOLD = "\u001b[1m";
    static final String DIM = "\u001b[2m";
    static final String GREEN = "\u001b[32m";
    static final String YELLOW = "\u001b[33m";
    static final String CYAN = "\u001b[36m";

    static final String GAP = "  ";                 // gap between listing columns
    static final String[] TOP = {"╭", "╮"};   // ╭ ╮
    static final String[] MID = {"├", "┤"};   // ├ ┤
    static final String[] BOT = {"╰", "╯"};   // ╰ ╯

    /** Wrap text in ANSI codes when colour is on, else return it unchanged. */
    static String paint(String text, String... codes) {
        if (!USE_COLOUR || codes.length == 0) {
            return text;
        }
        return String.join("", codes) + text + RESET;
    }

    /** Length of text ignoring any ANSI colour codes it may contain. */
    static int visibleLength(String text) {
        return text.replaceAll("\u001b\\[[0-9;]*m", "").length();
    }

    /** A horizontal border rule spanning `width` characters of content. */
    static String rule(int width, String[] corners) {
        return paint(corners[0] + "─".repeat(width + 2) + corners[1], DIM);
    }

    /** One framed content line, padded (or, as a last resort, trimmed) to width. */
    static String line(String text, int width) {
        int visible = visibleLength(text);
        if (visible > width && !text.contains("\u001b")) {
            text = text.substring(0, width - 1) + "…";  // …
            visible = visibleLength(text);
        }
        int pad = Math.max(width - visible, 0);
        String bar = paint("│", DIM);               // │
        return bar + " " + text + " ".repeat(pad) + " " + bar;
    }

    // --- one-line status messages, sharing a symbol vocabulary -------------

    static void sayOk(String text) {
        System.out.println(paint("  ✓", GREEN, BOLD) + " " + text);   // ✓
    }

    static void sayErr(String text) {
        System.out.println(paint("  ✗", YELLOW, BOLD) + " " + text);  // ✗
    }

    static void sayInfo(String text) {
        System.out.println(paint("  ·", CYAN) + " " + text);          // ·
    }

    // --- catalogue and panel renderers -------------------------------------

    static String columns(String id, String name, String type, String status,
                          int idWidth, int nameWidth, int typeWidth) {
        String format = "%" + idWidth + "s%s%-" + nameWidth + "s%s%-"
                + typeWidth + "s%s%s";
        return String.format(format, id, GAP, name, GAP, type, GAP, status);
    }

    static String statusToken(Item item) {
        return item.available ? paint("● Available", GREEN)   // ●
                              : paint("○ On loan", YELLOW);   // ○
    }

    /** Build the aligned, framed catalogue listing as a string. */
    static String renderCatalogue(List<Item> items, String title) {
        int idWidth = 2;
        int nameWidth = "Name".length();
        int typeWidth = "Type".length();
        for (Item item : items) {
            idWidth = Math.max(idWidth, String.valueOf(item.id).length());
            nameWidth = Math.max(nameWidth, item.name.length());
            typeWidth = Math.max(typeWidth, item.displayName().length());
        }
        nameWidth = Math.min(nameWidth, 28);
        int statusWidth = "● Available".length();   // widest status token

        int width = idWidth + GAP.length() + nameWidth + GAP.length()
                + typeWidth + GAP.length() + statusWidth;
        int detailWidth = 0;
        for (Item item : items) {
            detailWidth = Math.max(detailWidth,
                    idWidth + GAP.length() + item.details().length());
        }
        // Grow to fit the title and the longest detail line (so lens lists show
        // in full), but cap the width so a freak entry cannot run off-screen.
        width = Math.min(Math.max(Math.max(width, title.length()), detailWidth), 76);

        List<String> lines = new ArrayList<>();
        lines.add(rule(width, TOP));
        lines.add(line(paint(title, BOLD, CYAN), width));
        lines.add(rule(width, MID));
        lines.add(line(paint(columns("ID", "Name", "Type", "Status",
                idWidth, nameWidth, typeWidth), BOLD), width));
        lines.add(rule(width, MID));

        if (items.isEmpty()) {
            lines.add(line(paint("(the catalogue is empty)", DIM), width));
            lines.add(rule(width, BOT));
            return String.join("\n", lines);
        }

        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            String name = item.name.length() <= nameWidth ? item.name
                    : item.name.substring(0, nameWidth - 1) + "…";
            lines.add(line(columns(String.valueOf(item.id), name,
                    item.displayName(), statusToken(item),
                    idWidth, nameWidth, typeWidth), width));

            // Type-specific details on their own line, indented under the Name
            // column and dimmed. Trim the plain text so painting is never sliced.
            String detail = " ".repeat(idWidth + GAP.length()) + item.details();
            if (detail.length() > width) {
                detail = detail.substring(0, width - 1) + "…";
            }
            lines.add(line(paint(detail, DIM), width));
            if (index < items.size() - 1) {              // thin spacer
                lines.add(line("", width));
            }
        }

        lines.add(rule(width, BOT));
        return String.join("\n", lines);
    }

    /** A framed panel used for the menu, banners and statistics. */
    static String renderPanel(String title, List<String> body) {
        int width = Math.max(40, title.length());
        for (String text : body) {
            width = Math.max(width, visibleLength(text));
        }
        List<String> lines = new ArrayList<>();
        lines.add(rule(width, TOP));
        lines.add(line(paint(title, BOLD, CYAN), width));
        lines.add(rule(width, MID));
        for (String text : body) {
            lines.add(line(text, width));
        }
        lines.add(rule(width, BOT));
        return String.join("\n", lines);
    }

    static void printMainMenu() {
        System.out.println();
        System.out.println(renderPanel("LOAN DESK · MAIN MENU", Arrays.asList(
                paint("1", BOLD) + "  View catalogue",
                paint("2", BOLD) + "  Add item",
                paint("3", BOLD) + "  Borrow / Return item",
                paint("4", BOLD) + "  Save catalogue",
                paint("5", BOLD) + "  Search          " + paint("(extra)", DIM),
                paint("6", BOLD) + "  Statistics      " + paint("(extra)", DIM),
                paint("7", BOLD) + "  Exit")));
    }

    // =======================================================================
    //  Validated input layer  (Functionality #7)
    // =======================================================================
    //
    // Every interactive read goes through one of these helpers, each looping
    // until the value is acceptable, so no caller has to handle bad input.
    // End-of-input (Ctrl-D) becomes a clean exit rather than a stack trace.

    static final BufferedReader STDIN =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    static String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        try {
            String value = STDIN.readLine();
            if (value == null) {
                System.out.println();
                sayInfo("Input closed. Exiting.");
                System.exit(0);
            }
            return value;
        } catch (IOException exc) {
            System.out.println();
            sayInfo("Input closed. Exiting.");
            System.exit(0);
            return "";   // unreachable
        }
    }

    /**
     * Prompt for an item name. Core rule: it must not be empty. As an extra,
     * a name that already exists (case-insensitively) is rejected. Any
     * characters are allowed, so names like 'micro:bit Go Bundle' are accepted.
     */
    static String promptName(Catalogue catalogue) {
        while (true) {
            String name = readLine("  Name: ").strip();
            if (name.isEmpty()) {
                sayErr("Name cannot be empty. Try again.");
                continue;
            }
            if (catalogue.nameExists(name)) {            // extra: uniqueness
                sayErr("An item named '" + name + "' already exists. "
                        + "Please choose another.");
                continue;
            }
            return name;
        }
    }

    /** Prompt for a whole number strictly greater than zero. */
    static int promptPositiveInt(String label) {
        while (true) {
            String raw = readLine("  " + label + ": ").strip();
            int value;
            try {
                value = Integer.parseInt(raw);
            } catch (NumberFormatException exc) {
                sayErr("'" + raw + "' is not a whole number. Try again.");
                continue;
            }
            if (value <= 0) {
                sayErr("Value must be greater than zero. Try again.");
                continue;
            }
            return value;
        }
    }

    static String promptNonEmpty(String label) {
        while (true) {
            String value = readLine("  " + label + ": ").strip();
            if (!value.isEmpty()) {
                return value;
            }
            sayErr("This field cannot be empty. Try again.");
        }
    }

    /**
     * Read a comma-separated lens list (nested data). An empty entry is allowed
     * (a camera body with no lens). Blank fragments and duplicates are dropped
     * while preserving order.
     */
    static List<String> promptLenses() {
        String raw = readLine("  Lenses (comma-separated, blank for none): ").strip();
        List<String> lenses = new ArrayList<>();
        for (String fragment : raw.split(",")) {
            String lens = fragment.strip();
            if (!lens.isEmpty() && !lenses.contains(lens)) {
                lenses.add(lens);
            }
        }
        return lenses;
    }

    // =======================================================================
    //  Application  (ties the menu to the actions - Functionality #1)
    // =======================================================================

    static final class LoanDeskApp {
        final String dataPath;
        Catalogue catalogue;
        boolean dirty = false;   // true when there are unsaved changes

        LoanDeskApp(String dataPath) {
            this.dataPath = dataPath;
            this.catalogue = Catalogue.load(dataPath);
        }

        void run() {
            // Menu accepts the option number OR a keyword, case-insensitively.
            Map<String, Runnable> actions = new HashMap<>();
            actions.put("1", this::view);
            actions.put("view", this::view);
            actions.put("2", this::add);
            actions.put("add", this::add);
            actions.put("3", this::borrowReturn);
            actions.put("borrow", this::borrowReturn);
            actions.put("return", this::borrowReturn);
            actions.put("4", this::save);
            actions.put("save", this::save);
            actions.put("5", this::search);
            actions.put("search", this::search);
            actions.put("6", this::statistics);
            actions.put("stats", this::statistics);
            actions.put("statistics", this::statistics);
            Set<String> exitChoices = new HashSet<>(
                    Arrays.asList("7", "exit", "quit", "q"));

            System.out.println();
            System.out.println(renderPanel("LOAN DESK", Arrays.asList(
                    paint("Campus equipment loan catalogue", DIM),
                    catalogue.size() + " item(s) loaded from "
                            + Paths.get(dataPath).getFileName())));

            while (true) {   // only "Exit" leaves this loop (Functionality #1)
                printMainMenu();
                String choice = readLine("  Choose an option: ").strip().toLowerCase();
                if (exitChoices.contains(choice)) {
                    if (confirmExit()) {
                        System.out.println();
                        sayInfo("Goodbye.");
                        return;
                    }
                    continue;
                }
                Runnable action = actions.get(choice);
                if (action == null) {
                    sayErr("'" + choice + "' is not a valid option. Try again.");
                    continue;
                }
                action.run();
            }
        }

        // -- individual actions ---------------------------------------------

        void view() {
            System.out.println();
            System.out.println(renderCatalogue(catalogue.items(), "EQUIPMENT CATALOGUE"));
        }

        void add() {
            List<String> body = new ArrayList<>();
            for (int i = 0; i < TYPES.size(); i++) {
                body.add(paint(String.valueOf(i + 1), BOLD) + "  " + TYPES.get(i).displayName);
            }
            body.add(paint("0", BOLD) + "  Cancel");
            System.out.println();
            System.out.println(renderPanel("ADD ITEM · CHOOSE A TYPE", body));

            // Accept the number or the type name, case-insensitively.
            String choice = readLine("  Type: ").strip().toLowerCase();
            if (choice.equals("0") || choice.equals("cancel") || choice.isEmpty()) {
                sayInfo("Add cancelled.");
                return;
            }

            Builder builder = null;
            if (choice.matches("\\d+")) {
                int number = Integer.parseInt(choice);
                if (number >= 1 && number <= TYPES.size()) {
                    builder = TYPES.get(number - 1).builder;
                }
            } else {
                for (ItemType type : TYPES) {
                    if (choice.equals(type.tag) || choice.equals(type.displayName)) {
                        builder = type.builder;
                        break;
                    }
                }
            }
            if (builder == null) {
                sayErr("'" + choice + "' is not a valid type.");
                return;
            }

            String name = promptName(catalogue);
            int newId = catalogue.nextId();          // unique id, assigned for them
            Item item = builder.build(newId, name);  // new items default to Available
            catalogue.add(item);
            dirty = true;
            sayOk("Added [" + item.id + "] " + item.name + " (" + item.displayName()
                    + "). It is Available.");
        }

        void borrowReturn() {
            if (catalogue.size() == 0) {
                sayInfo("The catalogue is empty - nothing to borrow or return.");
                return;
            }
            String raw = readLine("  Item ID: ").strip();
            int id;
            try {
                id = Integer.parseInt(raw);
            } catch (NumberFormatException exc) {
                sayErr("'" + raw + "' is not a valid ID.");
                return;
            }
            Item item = catalogue.get(id);
            if (item == null) {
                sayErr("No item has ID " + id + ".");
                return;
            }
            item.available = !item.available;        // flip availability
            dirty = true;
            String change = item.available ? "returned (now Available)"
                    : "borrowed (now On loan)";
            sayOk("[" + item.id + "] " + item.name + " " + change + ".");
        }

        void save() {
            try {
                catalogue.save(dataPath);
            } catch (IOException exc) {
                sayErr("Could not save to '" + dataPath + "': " + exc.getMessage());
                return;
            }
            dirty = false;
            sayOk("Saved " + catalogue.size() + " item(s) to '" + dataPath + "'.");
        }

        // -- extra features -------------------------------------------------

        void search() {
            String term = readLine(
                    "  Search (name/type, or 'available' / 'on loan'): ")
                    .strip().toLowerCase();
            if (term.isEmpty()) {
                sayInfo("Search cancelled.");
                return;
            }
            List<Item> results = new ArrayList<>();
            for (Item item : catalogue.items()) {
                if (matches(item, term)) {
                    results.add(item);
                }
            }
            System.out.println();
            System.out.println(renderCatalogue(results,
                    "SEARCH RESULTS (" + results.size() + " match)"));
        }

        private boolean matches(Item item, String term) {
            if (term.equals("available") || term.equals("free")) {
                return item.available;
            }
            if (term.equals("on loan") || term.equals("loan")
                    || term.equals("out") || term.equals("borrowed")) {
                return !item.available;
            }
            return item.name.toLowerCase().contains(term)
                    || item.displayName().toLowerCase().contains(term);
        }

        void statistics() {
            int total = catalogue.size();
            int onLoan = 0;
            Map<String, Integer> perType = new TreeMap<>();
            for (Item item : catalogue.items()) {
                if (!item.available) {
                    onLoan++;
                }
                perType.merge(item.displayName(), 1, Integer::sum);
            }

            List<String> body = new ArrayList<>();
            body.add("Total items   " + paint(String.valueOf(total), BOLD));
            body.add("Available     " + paint(String.valueOf(total - onLoan), GREEN));
            body.add("On loan       " + paint(String.valueOf(onLoan), YELLOW));
            if (!perType.isEmpty()) {
                body.add(paint("By type", DIM));
                for (Map.Entry<String, Integer> entry : perType.entrySet()) {
                    body.add(String.format("  %-14s%d", entry.getKey(), entry.getValue()));
                }
            }
            System.out.println();
            System.out.println(renderPanel("CATALOGUE STATISTICS", body));
        }

        // -- exit -----------------------------------------------------------

        boolean confirmExit() {
            if (!dirty) {
                return true;
            }
            while (true) {
                String answer = readLine(
                        "  You have unsaved changes. Save before exiting? "
                                + "(yes/no/cancel): ").strip().toLowerCase();
                if (answer.equals("y") || answer.equals("yes")) {
                    save();
                    return true;
                }
                if (answer.equals("n") || answer.equals("no")) {
                    return true;
                }
                if (answer.equals("c") || answer.equals("cancel") || answer.isEmpty()) {
                    return false;
                }
                sayErr("Please answer yes, no or cancel.");
            }
        }
    }

    // =======================================================================
    //  Entry point
    // =======================================================================

    static final String DEFAULT_DATA_FILE = "catalogue.json";

    public static void main(String[] args) {
        // Make sure the box-drawing characters print correctly regardless of
        // the platform's default encoding.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        // Optional first argument: the catalogue file to use (extra).
        String dataPath = args.length > 0 ? args[0] : DEFAULT_DATA_FILE;
        new LoanDeskApp(dataPath).run();
    }
}
