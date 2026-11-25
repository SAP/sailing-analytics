"""ALB Lambda target resolving server + eventId + rsc to a RaceBoard redirect.

Package this file together with rsc_raceboard.py. Uses only the standard library.
Handler: rsc_raceboard_lambda.lambda_handler. See rsc_raceboard_lambda.md.
"""

import json
import logging
import os
import re
from argparse import Namespace
from functools import partial
from http import HTTPStatus
from typing import Final
from urllib.parse import unquote_plus, urlsplit
from urllib.request import Request, urlopen
from uuid import UUID

from rsc_raceboard import resolve


LOGGER: Final = logging.getLogger(__name__)
SOCKET_TIMEOUT_SECONDS: Final = 8.0
RESPONSE_RESERVE_SECONDS: Final = 2.0
DOMAIN_LABEL: Final = r"[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"


def decode_query_component(value: str) -> str:
    if not isinstance(value, str) or re.search(r"%(?![0-9a-fA-F]{2})", value):
        raise ValueError("Invalid query string encoding")
    return unquote_plus(value, errors="strict")


def query_parameters(event: dict) -> dict:
    # ALB leaves query strings URL-encoded in both event formats.
    multi: Final = "multiValueQueryStringParameters" in event
    field: Final = "multiValueQueryStringParameters" if multi else "queryStringParameters"
    parameters: Final = {}
    for raw_name, raw_value in (event.get(field) or {}).items():
        name: Final = decode_query_component(raw_name)
        if multi:
            if not isinstance(raw_value, list) or len(raw_value) != 1:
                raise ValueError(f"Expected exactly one value for {name!r}")
        value: Final = raw_value[0] if multi else raw_value
        if name in parameters:
            raise ValueError(f"Duplicate parameter {name!r}")
        parameters[name] = decode_query_component(value)
    for required_name in ("server", "eventId", "rsc"):
        if not parameters.get(required_name):
            raise ValueError(f"Missing parameter {required_name!r}")
    return parameters


def server_url(domain: str) -> str:
    host: Final = domain.lower()
    if len(host) > 253 or re.fullmatch(rf"(?:{DOMAIN_LABEL}\.)+[a-z][a-z0-9-]*", host) is None:
        raise ValueError("server must be a DNS domain name without scheme, port or path")
    if any(len(label) > 63 or label.endswith("-") for label in host.split(".")):
        raise ValueError("Invalid server domain name")
    allowed: Final = {value.strip().lower() for value in
                      os.environ.get("ALLOWED_SERVERS", "www.sapsailing.com").split(",") if value.strip()}
    if host not in allowed:
        raise ValueError(f"Server {host!r} is not listed in ALLOWED_SERVERS")
    return "https://" + host


def get_json(url: str, context):
    # Leave time to turn network failures into an ALB response. urllib's timeout
    # applies to blocking socket operations, not the complete lookup's wall time.
    timeout = SOCKET_TIMEOUT_SECONDS
    if context is not None:
        remaining: Final = context.get_remaining_time_in_millis() / 1000 - RESPONSE_RESERVE_SECONDS
        if remaining <= 0:
            raise TimeoutError("Insufficient Lambda execution time for another upstream request")
        timeout = min(timeout, remaining)
    request: Final = Request(url, headers={"Accept": "application/json"})
    with urlopen(request, timeout=timeout) as response:
        result: Final = json.load(response)
    return result


def validate_location(location: str) -> None:
    # The event's baseURL may select a different host than the queried server.
    parsed: Final = urlsplit(location)
    if (parsed.scheme not in {"http", "https"} or not parsed.hostname
            or parsed.username is not None or parsed.password is not None
            or any(ord(character) < 33 or ord(character) > 126 for character in location)):
        raise ValueError("Invalid RaceBoard redirect URL")


def lambda_handler(event: dict, context) -> dict:
    status = HTTPStatus.NOT_FOUND
    body = "Not found\n"
    headers: Final = {"Content-Type": "text/plain; charset=utf-8", "Cache-Control": "no-store"}
    try:
        if event.get("httpMethod") not in {"GET", "HEAD"}:
            raise ValueError("Expected GET or HEAD")
        parameters: Final = query_parameters(event)
        args: Final = Namespace(
            server=server_url(parameters["server"]), event_id=UUID(parameters["eventId"]),
            rsc=parameters["rsc"], tractrac_url=[], leaderboard_group=None, leaderboard=None)
        redirect_status: Final = HTTPStatus(int(os.environ.get("REDIRECT_STATUS", "302")))
        if redirect_status not in {HTTPStatus.MOVED_PERMANENTLY, HTTPStatus.FOUND}:
            raise ValueError("REDIRECT_STATUS must be 301 or 302")
        location: Final = resolve(args, fetch_json=partial(get_json, context=context))
        validate_location(location)
        headers["Location"] = location
        status = redirect_status
        body = ""
    except Exception:
        # ALB-facing boundary: details go to CloudWatch, clients receive only 404.
        LOGGER.exception("RaceBoard lookup failed")
    multi: Final = "multiValueHeaders" in event or "multiValueQueryStringParameters" in event
    result: Final = {
        "isBase64Encoded": False,
        "statusCode": status.value,
        "statusDescription": f"{status.value} {status.phrase}",
        "body": "" if event.get("httpMethod") == "HEAD" else body,
    }
    if multi:
        result["multiValueHeaders"] = {name: [value] for name, value in headers.items()}
    else:
        result["headers"] = headers
    return result
