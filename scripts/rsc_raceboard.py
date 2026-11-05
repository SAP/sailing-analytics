#!/usr/bin/env python3
"""Resolve an event-scoped SwissTiming RSC to a SAP Sailing RaceBoard URL.

Requires Python 3.8+; uses only the Python standard library.
See rsc_raceboard.md for usage and REST response details.
"""

import argparse
import json
import re
import sys
from typing import Callable, Final
from urllib.error import URLError
from urllib.parse import quote, urlencode, urlsplit
from urllib.request import Request, urlopen
from uuid import UUID


def decode_property(text: str) -> str:
    def replace_escape(match) -> str:
        escaped: Final = match.group("escape")
        result: str
        if escaped.startswith("u"):
            digits: Final = escaped[1:]
            if re.fullmatch(r"[0-9a-fA-F]{4}", digits) is None:
                raise ValueError(f"Invalid Unicode escape: \\{escaped}")
            result = chr(int(digits, base=16))
        else:
            result = {"t": "\t", "n": "\n", "r": "\r", "f": "\f"}.get(escaped, escaped)
        return result
    decoded: Final = re.sub(r"\\(?P<escape>u.{0,4}|.)", replace_escape, text)
    # Java Unicode escapes represent UTF-16 code units; combine surrogate pairs
    # into Python characters while preserving any isolated surrogate code units.
    return decoded.encode("utf-16-le", errors="surrogatepass").decode("utf-16-le", errors="surrogatepass")


def load_properties(text: str) -> dict:
    properties: Final = {}
    lines: Final = iter(re.split(r"\r\n|\r|\n", text))
    for physical_line in lines:
        line = physical_line.lstrip(" \t\f")
        if line and not line.startswith(("#", "!")):
            # An odd number of trailing backslashes joins the next physical line.
            # A comment marker inside a continuation is ordinary property text.
            while (len(line) - len(line.rstrip("\\"))) % 2:
                line = line[:-1] + next(lines, "").lstrip(" \t\f")
            if line:
                assignment: Final = re.fullmatch(
                    r"(?P<key>(?:\\.|[^\\:= \t\f])*)[ \t\f]*[:=]?[ \t\f]*(?P<value>.*)", line)
                properties[decode_property(assignment.group("key"))] = decode_property(assignment.group("value"))
    return properties


def get_json(url: str):
    request: Final = Request(url, headers={"Accept": "application/json"})
    try:
        with urlopen(request, timeout=60) as response:
            result: Final = json.load(response)
    except (URLError, OSError, ValueError) as error:
        raise ValueError(f"Could not read JSON from {url}: {error}") from error
    return result


def get_feed_urls(event: dict, explicit_urls: list) -> list:
    urls: Final = set(explicit_urls)
    if not urls:
        # A TracTrac event-page URL identifies the event's jsonservice.php feed.
        for connector in event.get("trackingConnectorInfos", []):
            if connector.get("trackingConnectorName") == "TracTrac":
                web_url: Final = urlsplit(connector.get("webUrl") or "")
                match: Final = re.fullmatch(r"/event-page/(event_[^/]+)/[^/]+/?", web_url.path)
                if web_url.hostname in {"tractrac.com", "www.tractrac.com"} and match:
                    urls.add(f"https://event.tractrac.com/events/{match.group(1)}/jsonservice.php")
    if not urls:
        raise ValueError("No TracTrac feed could be discovered for this event; supply --tractrac-url URL")
    return sorted(urls)


def get_race_ids(feed_urls: list, rsc: str, fetch_json: Callable = get_json) -> set:
    race_ids: Final = set()
    for feed_url in feed_urls:
        feed: Final = fetch_json(feed_url)
        for race in feed["races"]:
            # Parse actual Java Properties semantics, including escapes and continuations.
            try:
                properties: Final = load_properties(race.get("metadata") or "")
            except ValueError as error:
                raise ValueError(f"Invalid metadata for race {race.get('id')} in {feed_url}: {error}") from error
            if properties.get("ExternalId") == rsc:
                race_ids.add(str(UUID(race["id"])))
    return race_ids


def get_attached_races(event: dict, api: str, group_id, leaderboard_name, fetch_json: Callable = get_json):
    # Only groups referenced by the requested event are reachable. Group details
    # already embed leaderboards, series, fleets and race-column attachments.
    groups: Final = {group["id"]: group for group in event["leaderboardGroups"]}
    for reference in groups.values():
        if group_id is None or reference["id"] == str(group_id):
            group: Final = fetch_json(f"{api}/leaderboardgroups/{quote(reference['id'], safe='')}")
            if group["id"] != reference["id"]:
                raise ValueError(f"Leaderboard group response does not match requested ID {reference['id']}")
            for leaderboard in group["leaderboards"]:
                if leaderboard_name is None or leaderboard["name"] == leaderboard_name:
                    for series in leaderboard["series"]:
                        for fleet in series["fleets"]:
                            for race in fleet["races"]:
                                if race.get("isTracked") and race.get("trackingProviderType") == "TracTrac":
                                    yield group, leaderboard, race


def raceboard_url(base_url: str, event_id: str, group: dict, leaderboard: dict, race: dict) -> str:
    # Same context as RaceBoardLinkFactory / RaceboardContextDefinition, with
    # every parameter encoded (including leaderboardName).
    parameters: Final = {
        "eventId": event_id,
        "leaderboardName": leaderboard["name"],
        "leaderboardGroupId": group["id"],
        "raceName": race["trackedRaceName"],
        "showMapControls": "true",
        "regattaName": race["regattaName"],
        "mode": "PLAYER",
    }
    if any(value is None or value == "" for value in parameters.values()):
        raise ValueError(f"Incomplete RaceBoard context for race {race.get('raceId')}")
    return base_url.rstrip("/") + "/gwt/RaceBoard.html?" + urlencode(parameters)


def resolve(args: argparse.Namespace, fetch_json: Callable = get_json) -> str:
    """Resolve one viewing context; callers may supply their own JSON transport."""
    server: Final = args.server.rstrip("/")
    api: Final = server + "/sailingserver/api/v1"
    event_id: Final = str(args.event_id)
    event: Final = fetch_json(f"{api}/events/{event_id}")
    if event["id"] != event_id:
        raise ValueError("Event response does not match requested event ID")
    feed_urls: Final = get_feed_urls(event, args.tractrac_url)
    race_ids: Final = get_race_ids(feed_urls, args.rsc, fetch_json=fetch_json)
    if not race_ids:
        raise ValueError(f"RSC {args.rsc!r} was not found in the TracTrac feeds: {', '.join(feed_urls)}")
    base_url: Final = event.get("baseURL") or server
    matches: Final = {}
    for group, leaderboard, race in get_attached_races(
            event, api, args.leaderboard_group, args.leaderboard, fetch_json=fetch_json):
        race_id: Final = str(UUID(race["raceId"]))
        if race_id in race_ids:
            # Keep different viewing contexts distinct, even for the same tracked
            # race. Repeated attachments of the same context are harmless.
            context: Final = (group["id"], leaderboard["name"], race["regattaName"], race["trackedRaceName"], race_id)
            matches[context] = raceboard_url(base_url, event_id, group, leaderboard, race)
    if not matches:
        raise ValueError(f"No attached TracTrac race matches RSC {args.rsc!r} in SAP event {event_id}"
                         " with the requested group/leaderboard filters")
    if len(matches) > 1:
        raise ValueError(f"RSC {args.rsc!r} matches {len(matches)} viewing contexts in event {event_id};"
                         " use --leaderboard-group and/or --leaderboard to select one:\n"
                         + "\n".join(sorted(matches.values())))
    return next(iter(matches.values()))


def main() -> int:
    parser: Final = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("event_id", type=UUID, help="SAP Sailing event UUID")
    parser.add_argument("rsc", help="SwissTiming RSC / TracTrac ExternalId")
    parser.add_argument("--server", default="https://www.sapsailing.com", help="SAP server base URL")
    parser.add_argument("--tractrac-url", action="append", default=[], metavar="URL",
                        help="TracTrac jsonservice.php URL; repeat for multiple feeds; overrides discovery")
    parser.add_argument("--leaderboard-group", type=UUID, metavar="UUID", help="Restrict to this event group")
    parser.add_argument("--leaderboard", metavar="NAME", help="Restrict to this exact leaderboard name")
    args: Final = parser.parse_args()
    status = 0
    try:
        print(resolve(args))
    except (ValueError, KeyError, TypeError) as error:
        print(f"Error: {error}", file=sys.stderr)
        status = 1
    return status


if __name__ == "__main__":
    sys.exit(main())
