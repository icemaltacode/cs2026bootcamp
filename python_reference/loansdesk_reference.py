#!/usr/bin/env python3
"""
loansdesk_reference.py
======================

CODESPRINT 2026 - Post-Secondary Category - BOOTCAMP Task
"Loan Desk": an equipment-loan catalogue for laptops, cameras and robotics kits.

This is the official reference solution. It implements every required
functionality (see the task sheet) and adds a few clearly-labelled extras.
It is written to be *read*: the structure below is the point as much as the
behaviour, because "Modular Programming" is itself part of the mark scheme.

Requirements coverage
--------------------
    #1 Init & loading .... load_from_file / Catalogue.load, empty-on-missing
    #2 Item types ........ Item + Laptop / Camera / RoboticsKit subclasses
    #3 View .............. render_catalogue (aligned, adaptive column widths)
    #4 Add ............... LoanDeskApp.add + per-type interactive builders
    #5 Borrow / return ... LoanDeskApp.borrow_return (toggle by ID)
    #6 Save .............. Catalogue.save (type tag + nested lens list kept)
    #7 Check routines .... validated-input layer; try/except around all I/O
    #8 Modular ........... classes + functions, nested data, PEP 8, comments

Extras (each labelled "extra" in the menu / code):
    * Search by name, type, or availability.
    * Catalogue statistics (totals + per-type breakdown).
    * Unsaved-changes guard on exit.
    * Optional catalogue path as a command-line argument.
    * Atomic save with a .bak backup, and skip-bad-record loading.

Standard library only. Runs on Python 3.9+.

    python3 loansdesk_reference.py [path-to-catalogue.json]
"""

from __future__ import annotations

import json
import os
import sys
from abc import ABC, abstractmethod
from typing import Any, Callable, Dict, List, Optional, Type


# ===========================================================================
#  Item model  (Functionality #2)
# ===========================================================================
#
# Every concrete item type subclasses `Item`. A subclass declares two class
# attributes - `type_tag` (the value stored in the file) and `display_name`
# (what the user sees) - and the registry wires it up automatically as the
# module imports. Loading, the add-item menu and the type dispatch in
# `Item.from_dict` all read from that registry, so no part of the program
# carries a hard-coded list of types.


class Item(ABC):
    """Abstract base for anything the loan desk can lend out.

    Common fields shared by every item: id, name, availability (and the type
    tag, which is a class attribute). Each subclass adds its own fields.
    """

    # Overridden by every concrete subclass.
    type_tag: str = ""
    display_name: str = ""
    # Alternative tags accepted when loading (lets us read older/variant files
    # that spelled the type differently). Saving always uses `type_tag`.
    aliases: tuple = ()

    # tag -> subclass, populated by __init_subclass__ as the module imports.
    _registry: Dict[str, Type["Item"]] = {}

    def __init_subclass__(cls, **kwargs: Any) -> None:
        super().__init_subclass__(**kwargs)
        if cls.type_tag:  # ignore any intermediate/abstract subclasses
            Item._registry[cls.type_tag] = cls
            for alias in cls.aliases:
                Item._registry[alias] = cls

    def __init__(self, item_id: int, name: str, available: bool = True) -> None:
        self.id = item_id
        self.name = name
        self.available = available

    # -- presentation -------------------------------------------------------

    @property
    def status(self) -> str:
        """Human-readable availability (Functionality #3)."""
        return "Available" if self.available else "On loan"

    @abstractmethod
    def details(self) -> str:
        """The type-specific second line shown in the catalogue listing."""

    # -- persistence --------------------------------------------------------

    @abstractmethod
    def extra_fields(self) -> Dict[str, Any]:
        """Return only the type-specific fields, ready to serialise."""

    def to_dict(self) -> Dict[str, Any]:
        """Full serialisable record: common fields + type tag + extras.

        Key order is chosen so the saved file reads naturally (type and id
        first), matching the sample in the task sheet.
        """
        record: Dict[str, Any] = {
            "type": self.type_tag,
            "id": self.id,
            "name": self.name,
            "available": self.available,
        }
        record.update(self.extra_fields())
        return record

    @classmethod
    @abstractmethod
    def _from_extra(cls, item_id: int, name: str, available: bool,
                    data: Dict[str, Any]) -> "Item":
        """Build an instance from a record's extra fields (validated)."""

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "Item":
        """Rebuild the correct Item subclass from a saved record.

        Raises ValueError on anything malformed so the loader can skip the one
        bad record instead of aborting the whole catalogue (Functionality #7).
        """
        if not isinstance(data, dict):
            raise ValueError("record is not an object")

        tag = data.get("type")
        subclass = cls._registry.get(tag)
        if subclass is None:
            raise ValueError(f"unknown item type {tag!r}")

        item_id = data.get("id")
        # bool is a subclass of int in Python, so reject it explicitly.
        if not isinstance(item_id, int) or isinstance(item_id, bool) or item_id <= 0:
            raise ValueError(f"invalid id {item_id!r}")

        name = data.get("name")
        if not isinstance(name, str) or not name.strip():
            raise ValueError("missing or empty name")

        available = data.get("available", True)
        if not isinstance(available, bool):
            raise ValueError(f"invalid 'available' flag {available!r}")

        return subclass._from_extra(item_id, name.strip(), available, data)


# --- helpers used by the loaders to validate primitive stored fields -------

def _as_positive_int(value: Any, field: str) -> int:
    """Coerce a stored value to a whole number > 0 or raise ValueError.

    `bool` is rejected explicitly because in Python `True`/`False` are ints.
    """
    if isinstance(value, bool) or not isinstance(value, int):
        raise ValueError(f"{field} must be a whole number, got {value!r}")
    if value <= 0:
        raise ValueError(f"{field} must be greater than zero, got {value}")
    return value


def _as_nonempty_str(value: Any, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{field} must be a non-empty string")
    return value.strip()


# ===========================================================================
#  Concrete item types  (Table 1 in the task sheet)
# ===========================================================================


class Laptop(Item):
    type_tag = "laptop"
    display_name = "laptop"

    def __init__(self, item_id: int, name: str, os_name: str, ram_gb: int,
                 available: bool = True) -> None:
        super().__init__(item_id, name, available)
        self.os_name = os_name
        self.ram_gb = ram_gb

    def details(self) -> str:
        return f"{self.os_name}, {self.ram_gb} GB RAM"

    def extra_fields(self) -> Dict[str, Any]:
        return {"os": self.os_name, "ram_gb": self.ram_gb}

    @classmethod
    def _from_extra(cls, item_id, name, available, data):
        return cls(
            item_id, name,
            _as_nonempty_str(data.get("os"), "os"),
            _as_positive_int(data.get("ram_gb"), "ram_gb"),
            available,
        )


class Camera(Item):
    type_tag = "camera"
    display_name = "camera"

    def __init__(self, item_id: int, name: str, megapixels: int,
                 lenses: List[str], available: bool = True) -> None:
        super().__init__(item_id, name, available)
        self.megapixels = megapixels
        self.lenses = list(lenses)  # nested data: keep our own copy

    def details(self) -> str:
        lenses = ", ".join(self.lenses) if self.lenses else "no lenses"
        return f"{self.megapixels} MP; lenses: {lenses}"

    def extra_fields(self) -> Dict[str, Any]:
        # list(...) so the saved structure can never alias our live list.
        return {"megapixels": self.megapixels, "lenses": list(self.lenses)}

    @classmethod
    def _from_extra(cls, item_id, name, available, data):
        raw_lenses = data.get("lenses", [])
        if not isinstance(raw_lenses, list):
            raise ValueError("lenses must be a list")
        lenses = [str(x).strip() for x in raw_lenses if str(x).strip()]
        return cls(
            item_id, name,
            _as_positive_int(data.get("megapixels"), "megapixels"),
            lenses,
            available,
        )


class RoboticsKit(Item):
    type_tag = "robotics kit"
    display_name = "robotics kit"
    # Accept the short tag "robotics" too, so a file that spelled the type
    # either way still loads. Saving normalises back to "robotics kit".
    aliases = ("robotics",)

    def __init__(self, item_id: int, name: str, pieces: int, age_range: str,
                 available: bool = True) -> None:
        super().__init__(item_id, name, available)
        self.pieces = pieces
        self.age_range = age_range

    def details(self) -> str:
        return f"{self.pieces} pieces, ages {self.age_range}"

    def extra_fields(self) -> Dict[str, Any]:
        return {"pieces": self.pieces, "age_range": self.age_range}

    @classmethod
    def _from_extra(cls, item_id, name, available, data):
        return cls(
            item_id, name,
            _as_positive_int(data.get("pieces"), "pieces"),
            _as_nonempty_str(data.get("age_range"), "age_range"),
            available,
        )


# ===========================================================================
#  Catalogue  (Functionality #1 and #6)
# ===========================================================================


class Catalogue:
    """The collection of items plus the rules that operate over the whole set
    (unique IDs, lookup, load/save). Knows nothing about the user interface."""

    def __init__(self, items: Optional[List[Item]] = None) -> None:
        self._items: List[Item] = items or []

    # -- collection access --------------------------------------------------

    def __iter__(self):
        return iter(self._items)

    def __len__(self) -> int:
        return len(self._items)

    @property
    def items(self) -> List[Item]:
        return list(self._items)

    def next_id(self) -> int:
        """Smallest unused positive id (max + 1; 1 for an empty catalogue)."""
        return max((item.id for item in self._items), default=0) + 1

    def get(self, item_id: int) -> Optional[Item]:
        for item in self._items:
            if item.id == item_id:
                return item
        return None

    def name_exists(self, name: str) -> bool:
        """Case-insensitive name lookup, used by the (extra) uniqueness check."""
        target = name.strip().casefold()
        return any(item.name.casefold() == target for item in self._items)

    def add(self, item: Item) -> None:
        self._items.append(item)

    # -- persistence --------------------------------------------------------

    @classmethod
    def load(cls, path: str) -> "Catalogue":
        """Load a catalogue from `path`. Never raises (Functionality #7).

        A missing file, unreadable file, malformed JSON, or a top-level
        structure that is not a list all yield an empty catalogue with a
        message. Individual records that fail to parse are skipped with a
        warning; the good ones still load.
        """
        if not os.path.exists(path):
            say_info(f"No catalogue file at '{path}'. "
                     f"Starting with an empty catalogue.")
            return cls([])

        try:
            with open(path, "r", encoding="utf-8") as handle:
                raw = json.load(handle)
        except (OSError, json.JSONDecodeError) as exc:
            say_err(f"Could not read '{path}' ({exc}). "
                    f"Starting with an empty catalogue.")
            return cls([])

        if not isinstance(raw, list):
            say_err(f"'{path}' is not a list of items. "
                    f"Starting with an empty catalogue.")
            return cls([])

        items: List[Item] = []
        seen_ids: set = set()
        for index, record in enumerate(raw, start=1):
            try:
                item = Item.from_dict(record)
            except ValueError as exc:
                say_err(f"Skipping record #{index}: {exc}")
                continue
            if item.id in seen_ids:
                say_err(f"Skipping record #{index}: duplicate id {item.id}")
                continue
            seen_ids.add(item.id)
            items.append(item)

        say_info(f"Loaded {len(items)} item(s) from '{path}'.")
        return cls(items)

    def save(self, path: str) -> None:
        """Write the catalogue atomically (Functionality #6).

        Strategy: serialise to a sibling temp file, fsync, then os.replace()
        over the target (atomic on POSIX and Windows). The previous file is
        copied to '<path>.bak' first. A crash at any point leaves either the
        old file or the new file intact - never a half-written one.
        """
        payload = [item.to_dict() for item in self._items]
        directory = os.path.dirname(os.path.abspath(path)) or "."
        tmp_path = os.path.join(directory, f".{os.path.basename(path)}.tmp")

        # Roll the existing file to a backup before we touch anything.
        if os.path.exists(path):
            try:
                with open(path, "r", encoding="utf-8") as src:
                    previous = src.read()
                with open(path + ".bak", "w", encoding="utf-8") as bak:
                    bak.write(previous)
            except OSError:
                pass  # a missing backup is not worth aborting the save for

        with open(tmp_path, "w", encoding="utf-8") as handle:
            json.dump(payload, handle, indent=2, ensure_ascii=False)
            handle.write("\n")
            handle.flush()
            os.fsync(handle.fileno())

        os.replace(tmp_path, path)


# ===========================================================================
#  Validated input layer  (Functionality #7)
# ===========================================================================
#
# Every interactive read goes through one of these helpers. Each loops until
# the user supplies an acceptable value, so no caller ever has to handle bad
# input itself - and EOF (Ctrl-D) / interrupt (Ctrl-C) become a clean exit
# rather than a traceback.


def read_line(prompt: str) -> str:
    """input() that treats EOF/interrupt as a graceful quit."""
    try:
        return input(prompt)
    except (EOFError, KeyboardInterrupt):
        print()
        say_info("Input closed. Exiting.")
        sys.exit(0)


def prompt_name(catalogue: Catalogue) -> str:
    """Prompt for an item name.

    Core rule (task sheet): the name must not be empty. As an *extra*, we also
    reject a name that already exists (case-insensitively) - a catalogue with
    two identically-named items is almost always a mistake. Any characters are
    allowed, so real product names like 'micro:bit Go Bundle' are accepted.
    """
    while True:
        name = read_line("  Name: ").strip()
        if not name:
            say_err("Name cannot be empty. Try again.")
            continue
        if catalogue.name_exists(name):  # extra: uniqueness
            say_err(f"An item named '{name}' already exists. "
                    f"Please choose another.")
            continue
        return name


def prompt_positive_int(label: str) -> int:
    """Prompt for a whole number strictly greater than zero (Functionality #7)."""
    while True:
        raw = read_line(f"  {label}: ").strip()
        try:
            value = int(raw)
        except ValueError:
            say_err(f"'{raw}' is not a whole number. Try again.")
            continue
        if value <= 0:
            say_err("Value must be greater than zero. Try again.")
            continue
        return value


def prompt_nonempty(label: str) -> str:
    while True:
        value = read_line(f"  {label}: ").strip()
        if value:
            return value
        say_err("This field cannot be empty. Try again.")


def prompt_lenses() -> List[str]:
    """Read a comma-separated lens list (nested data). An empty entry is
    allowed (a camera body with no lens bundled). Blank fragments and
    duplicates are dropped while preserving order."""
    raw = read_line("  Lenses (comma-separated, blank for none): ").strip()
    lenses: List[str] = []
    for fragment in raw.split(","):
        lens = fragment.strip()
        if lens and lens not in lenses:
            lenses.append(lens)
    return lenses


# ===========================================================================
#  Interactive builders, one per type  (Functionality #4)
# ===========================================================================
#
# Each builder gathers a new item of its type from the user. They are listed
# against the same type tags used everywhere else, keeping the add-item menu
# data-driven: the menu is generated from ITEM_BUILDERS, not hand-written.


def build_laptop(item_id: int, name: str) -> Laptop:
    os_name = prompt_nonempty("Operating system")
    ram_gb = prompt_positive_int("RAM in GB")
    return Laptop(item_id, name, os_name, ram_gb)


def build_camera(item_id: int, name: str) -> Camera:
    megapixels = prompt_positive_int("Megapixels")
    lenses = prompt_lenses()
    return Camera(item_id, name, megapixels, lenses)


def build_robotics(item_id: int, name: str) -> RoboticsKit:
    pieces = prompt_positive_int("Piece count")
    age_range = prompt_nonempty("Recommended age range (e.g. 10-14 or 8+)")
    return RoboticsKit(item_id, name, pieces, age_range)


# (display label, type tag, builder). Ordering controls the add-item menu.
ITEM_BUILDERS: List[tuple] = [
    (Laptop.display_name, Laptop.type_tag, build_laptop),
    (Camera.display_name, Camera.type_tag, build_camera),
    (RoboticsKit.display_name, RoboticsKit.type_tag, build_robotics),
]


# ===========================================================================
#  Presentation  (Functionality #3)
# ===========================================================================
#
# All output is drawn with box-drawing characters and (when the terminal
# supports it) a little colour. Colour is turned off automatically when output
# is redirected/piped or when the NO_COLOR environment variable is set, so the
# program stays perfectly readable as plain text - which is what a marker
# running it non-interactively will see.

# ANSI colour is used only for a live terminal; never when piped or NO_COLOR set.
_USE_COLOUR = sys.stdout.isatty() and os.environ.get("NO_COLOR") is None

# ANSI SGR codes.
_RESET, _BOLD, _DIM = "\033[0m", "\033[1m", "\033[2m"
_GREEN, _YELLOW, _CYAN = "\033[32m", "\033[33m", "\033[36m"


def _paint(text: str, *codes: str) -> str:
    """Wrap `text` in ANSI codes when colour is enabled, else return it as-is.

    Colour codes add no *visible* width, so callers pad to a plain width first
    and paint afterwards - the box borders always line up either way.
    """
    if not _USE_COLOUR or not codes:
        return text
    return "".join(codes) + text + _RESET


# -- one-line status messages, sharing a symbol vocabulary ------------------

def say_ok(text: str) -> None:
    """A completed action."""
    print(_paint("  ✓", _GREEN, _BOLD), text)


def say_err(text: str) -> None:
    """A rejected input the user should correct and retry."""
    print(_paint("  ✗", _YELLOW, _BOLD), text)


def say_info(text: str) -> None:
    """A neutral note (cancellations, empty states, load results)."""
    print(_paint("  ·", _CYAN), text)


# Box-drawing pieces. The whole app uses one simple frame: an outer border
# only, with content laid out by padding *inside* it. There are no interior
# vertical dividers, so nothing can misalign - the catalogue reads like a tidy
# report rather than a spreadsheet grid.
_H = "─"
_TOP = ("╭", "╮")
_MID = ("├", "┤")
_BOT = ("╰", "╯")

_GAP = "  "                     # gap between columns in the listing


_ANSI_RE = None  # compiled lazily so the top-of-file import list stays short


def _visible_len(text: str) -> int:
    """Length of `text` ignoring any ANSI colour codes it may contain."""
    global _ANSI_RE
    if _ANSI_RE is None:
        import re
        _ANSI_RE = re.compile(r"\033\[[0-9;]*m")
    return len(_ANSI_RE.sub("", text))


def _rule(width: int, corners: tuple) -> str:
    """A horizontal border rule spanning `width` characters of content
    (plus one space of padding on each side)."""
    left, right = corners
    return _paint(left + _H * (width + 2) + right, _DIM)


def _line(text: str, width: int) -> str:
    """One framed content line, padded (or trimmed) to `width`.

    `text` may already contain colour codes, so we measure and pad on its
    *visible* length. We only trim when the text is plain, so an escape code is
    never sliced through the middle."""
    visible = _visible_len(text)
    if visible > width and "\033" not in text:
        text = text[:width - 1] + "…"
        visible = _visible_len(text)
    pad = max(width - visible, 0)
    bar = _paint("│", _DIM)
    return f"{bar} {text}{' ' * pad} {bar}"


def _status_token(item: Item) -> str:
    """Coloured availability token: green ● Available / yellow ○ On loan."""
    if item.available:
        return _paint("● Available", _GREEN)
    return _paint("○ On loan", _YELLOW)


def render_catalogue(items: List[Item],
                     title: str = "EQUIPMENT CATALOGUE") -> str:
    """Build the aligned, framed listing as a string (kept separate from
    printing so it is easy to test). Column widths adapt to the data."""
    # Column widths grow to fit the data; the name column is capped so one
    # freak entry cannot blow the layout apart.
    id_w = max(2, *(len(str(item.id)) for item in items)) if items else 2
    name_w = max(len("Name"), *(len(item.name) for item in items)) if items \
        else len("Name")
    name_w = min(name_w, 28)
    type_w = max(len("Type"), *(len(item.display_name) for item in items)) \
        if items else len("Type")
    status_w = len("● Available")               # widest status token

    # Full content width of a listing row (status sits last, so it needs no
    # trailing pad of its own here - _line pads the row to `width`).
    width = id_w + len(_GAP) + name_w + len(_GAP) + type_w + len(_GAP) + status_w
    # Grow to fit the title and the longest detail line (so lens lists show in
    # full), but cap it so an unusually long entry can't stretch the frame off
    # the screen - anything beyond the cap is trimmed with an ellipsis.
    detail_w = max((id_w + len(_GAP) + len(item.details()) for item in items),
                   default=0)
    width = min(max(width, len(title), detail_w), 76)

    def columns(ids: str, name: str, type_name: str, status: str) -> str:
        return (f"{ids:>{id_w}}{_GAP}{name:<{name_w}}{_GAP}"
                f"{type_name:<{type_w}}{_GAP}{status}")

    lines = [_rule(width, _TOP),
             _line(_paint(title, _BOLD, _CYAN), width),
             _rule(width, _MID),
             _line(_paint(columns("ID", "Name", "Type", "Status"), _BOLD), width),
             _rule(width, _MID)]

    if not items:
        lines.append(_line(_paint("(the catalogue is empty)", _DIM), width))
        lines.append(_rule(width, _BOT))
        return "\n".join(lines)

    for index, item in enumerate(items):
        name = (item.name if len(item.name) <= name_w
                else item.name[:name_w - 1] + "…")
        lines.append(_line(
            columns(str(item.id), name, item.display_name, _status_token(item)),
            width))
        # Type-specific details on their own line, indented under the Name
        # column and dimmed. Trim on the plain text so painting is never sliced.
        detail = " " * (id_w + len(_GAP)) + item.details()
        if len(detail) > width:
            detail = detail[:width - 1] + "…"
        lines.append(_line(_paint(detail, _DIM), width))
        if index < len(items) - 1:               # thin spacer between items
            lines.append(_line("", width))

    lines.append(_rule(width, _BOT))
    return "\n".join(lines)


def render_panel(title: str, body_lines: List[str], min_width: int = 40) -> str:
    """A simple framed panel used for the menu, banners and the statistics
    view, so the whole app shares one visual language. Width fits the content."""
    width = max(min_width, len(title),
                *(_visible_len(line) for line in body_lines)) \
        if body_lines else max(min_width, len(title))
    lines = [_rule(width, _TOP),
             _line(_paint(title, _BOLD, _CYAN), width),
             _rule(width, _MID)]
    lines += [_line(text, width) for text in body_lines]
    lines.append(_rule(width, _BOT))
    return "\n".join(lines)


def print_main_menu() -> None:
    print()
    print(render_panel("LOAN DESK · MAIN MENU", [
        f"{_paint('1', _BOLD)}  View catalogue",
        f"{_paint('2', _BOLD)}  Add item",
        f"{_paint('3', _BOLD)}  Borrow / Return item",
        f"{_paint('4', _BOLD)}  Save catalogue",
        f"{_paint('5', _BOLD)}  Search          {_paint('(extra)', _DIM)}",
        f"{_paint('6', _BOLD)}  Statistics      {_paint('(extra)', _DIM)}",
        f"{_paint('7', _BOLD)}  Exit",
    ]))


# ===========================================================================
#  Application  (ties the menu to the actions - Functionality #1)
# ===========================================================================
#
# The application object holds the catalogue, the data-file path and an
# "unsaved changes" flag, and dispatches menu choices to the actions below.


class LoanDeskApp:
    def __init__(self, data_path: str) -> None:
        self.data_path = data_path
        self.catalogue = Catalogue.load(data_path)
        self.dirty = False  # True when there are unsaved changes

    # -- menu dispatch ------------------------------------------------------

    def run(self) -> None:
        # Menu accepts the option number OR a keyword, case-insensitively.
        actions: Dict[str, Callable[[], None]] = {
            "1": self.view, "view": self.view,
            "2": self.add, "add": self.add,
            "3": self.borrow_return, "borrow": self.borrow_return,
            "return": self.borrow_return,
            "4": self.save, "save": self.save,
            "5": self.search, "search": self.search,
            "6": self.statistics, "stats": self.statistics,
            "statistics": self.statistics,
        }
        exit_choices = {"7", "exit", "quit", "q"}

        print()
        print(render_panel("LOAN DESK", [
            _paint("Campus equipment loan catalogue", _DIM),
            f"{len(self.catalogue)} item(s) loaded"
            f" from {os.path.basename(self.data_path)}",
        ]))
        while True:  # only "Exit" leaves this loop (Functionality #1)
            print_main_menu()
            choice = read_line("  Choose an option: ").strip().casefold()
            if choice in exit_choices:
                if self.confirm_exit():
                    print()
                    say_info("Goodbye.")
                    return
                continue
            action = actions.get(choice)
            if action is None:
                say_err(f"'{choice}' is not a valid option. Try again.")
                continue
            action()

    # -- individual actions -------------------------------------------------

    def view(self) -> None:
        print()
        print(render_catalogue(self.catalogue.items))

    def add(self) -> None:
        print()
        print(render_panel("ADD ITEM · CHOOSE A TYPE", [
            f"{_paint(str(i), _BOLD)}  {label}"
            for i, (label, _t, _b) in enumerate(ITEM_BUILDERS, start=1)
        ] + [f"{_paint('0', _BOLD)}  Cancel"]))

        # Accept the number or the type name, case-insensitively.
        choice = read_line("  Type: ").strip().casefold()
        if choice in {"0", "cancel", ""}:
            say_info("Add cancelled.")
            return

        builder: Optional[Callable[[int, str], Item]] = None
        if choice.isdigit() and 1 <= int(choice) <= len(ITEM_BUILDERS):
            builder = ITEM_BUILDERS[int(choice) - 1][2]
        else:
            for label, tag, fn in ITEM_BUILDERS:
                if choice in (tag, label):
                    builder = fn
                    break
        if builder is None:
            say_err(f"'{choice}' is not a valid type.")
            return

        name = prompt_name(self.catalogue)
        new_id = self.catalogue.next_id()      # unique id, assigned for them
        item = builder(new_id, name)           # new items default to Available
        self.catalogue.add(item)
        self.dirty = True
        say_ok(f"Added [{item.id}] {item.name} ({item.display_name}). "
               f"It is Available.")

    def borrow_return(self) -> None:
        if len(self.catalogue) == 0:
            say_info("The catalogue is empty - nothing to borrow or return.")
            return
        raw = read_line("  Item ID: ").strip()
        try:
            item_id = int(raw)
        except ValueError:
            say_err(f"'{raw}' is not a valid ID.")
            return
        item = self.catalogue.get(item_id)
        if item is None:
            say_err(f"No item has ID {item_id}.")
            return
        item.available = not item.available          # flip availability
        self.dirty = True
        change = ("returned (now Available)" if item.available
                  else "borrowed (now On loan)")
        say_ok(f"[{item.id}] {item.name} {change}.")

    def save(self) -> None:
        try:
            self.catalogue.save(self.data_path)
        except OSError as exc:
            say_err(f"Could not save to '{self.data_path}': {exc}")
            return
        self.dirty = False
        say_ok(f"Saved {len(self.catalogue)} item(s) to '{self.data_path}'.")

    # -- extra features -----------------------------------------------------

    def search(self) -> None:
        """Extra: filter the listing by a term matched against name or type,
        or by availability. A small convenience built on top of View."""
        term = read_line(
            "  Search (name/type, or 'available' / 'on loan'): "
        ).strip().casefold()
        if not term:
            say_info("Search cancelled.")
            return

        def matches(item: Item) -> bool:
            if term in ("available", "free"):
                return item.available
            if term in ("on loan", "loan", "out", "borrowed"):
                return not item.available
            return (term in item.name.casefold()
                    or term in item.display_name.casefold())

        results = [item for item in self.catalogue if matches(item)]
        print()
        print(render_catalogue(
            results, title=f"SEARCH RESULTS ({len(results)} match)"))

    def statistics(self) -> None:
        """Extra: a quick at-a-glance summary - totals and a per-type count."""
        total = len(self.catalogue)
        on_loan = sum(1 for item in self.catalogue if not item.available)
        per_type: Dict[str, int] = {}
        for item in self.catalogue:
            per_type[item.display_name] = per_type.get(item.display_name, 0) + 1

        body = [
            f"Total items   {_paint(str(total), _BOLD)}",
            f"Available     {_paint(str(total - on_loan), _GREEN)}",
            f"On loan       {_paint(str(on_loan), _YELLOW)}",
        ]
        if per_type:
            body.append(_paint("By type", _DIM))
            for type_name in sorted(per_type):
                body.append(f"  {type_name:<14}{per_type[type_name]}")
        print()
        print(render_panel("CATALOGUE STATISTICS", body))

    # -- exit ---------------------------------------------------------------

    def confirm_exit(self) -> bool:
        """Extra: guard against losing unsaved work. Returns True to exit."""
        if not self.dirty:
            return True
        while True:
            answer = read_line(
                "  You have unsaved changes. Save before exiting? "
                "(yes/no/cancel): "
            ).strip().casefold()
            if answer in ("y", "yes"):
                self.save()
                return True
            if answer in ("n", "no"):
                return True
            if answer in ("c", "cancel", ""):
                return False
            say_err("Please answer yes, no or cancel.")


# ===========================================================================
#  Entry point
# ===========================================================================

# Default to the catalogue that sits next to this script, so the program finds
# it no matter which directory you launch it from (the IDE run button and a
# terminal often use different working directories).
DEFAULT_DATA_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                 "catalogue.json")


def main(argv: List[str]) -> int:
    # Optional positional argument: the catalogue file to use (extra).
    data_path = argv[1] if len(argv) > 1 else DEFAULT_DATA_FILE
    app = LoanDeskApp(data_path)
    app.run()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
