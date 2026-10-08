"""Shared image-to-decision logic for the Firebase worker and local web tester."""

import io
import logging
import os
import re

import requests
from PIL import Image, UnidentifiedImageError
from django.utils import timezone

from .models import AccessLog, Vehicle

MAX_IMAGE_BYTES = 1_000_000
# Supported standard and BH formats; ambiguous/unsupported text requires a retake.
PLATE_PATTERN = re.compile(
    r"(?<![A-Z0-9])(?:[A-Z]{2}[ -]*\d{1,2}[ -]*(?:[A-Z]{1,3}[ -]*)?\d{4}"
    r"|\d{2}[ -]*BH[ -]*\d{4}[ -]*[A-Z]{1,2})(?![A-Z0-9])"
)


class OcrServiceError(Exception):
    pass


def validate_image(image_bytes):
    if not image_bytes or len(image_bytes) > MAX_IMAGE_BYTES:
        raise ValueError("Image must be a JPEG no larger than 1 MB.")
    try:
        with Image.open(io.BytesIO(image_bytes)) as image:
            if image.format != "JPEG" or max(image.size) > 2048:
                raise ValueError("Expected a JPEG with a maximum 2048-pixel edge.")
            image.load()
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError) as error:
        raise ValueError("Invalid JPEG image.") from error


def extract_plate_candidate(text):
    # Preserve spaces between words/plates; never concatenate all OCR text or guess O/0.
    text = re.sub(r"\s+", " ", text.upper())
    plates = {re.sub(r"[ -]", "", match.group()) for match in PLATE_PATTERN.finditer(text)}
    return next(iter(plates)) if len(plates) == 1 else None


def read_image_text(image_bytes):
    api_key = os.environ.get("OCR_SPACE_API_KEY")
    if not api_key:
        raise OcrServiceError("Set OCR_SPACE_API_KEY before starting the worker.")
    try:
        response = requests.post(
            "https://api.ocr.space/parse/image",
            headers={"apikey": api_key},
            files={"file": ("plate.jpg", image_bytes, "image/jpeg")},
            data={"language": "eng", "OCREngine": 2},
            timeout=(5, 15),
        )
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, dict) or payload.get("IsErroredOnProcessing"):
            raise OcrServiceError("OCR provider could not process the image.")
        results = payload.get("ParsedResults")
        if str(payload.get("OCRExitCode")) != "1" or not isinstance(results, list) or not results:
            raise OcrServiceError("OCR provider returned an unsuccessful response.")
        texts = []
        for page in results:
            if not isinstance(page, dict) or str(page.get("FileParseExitCode")) != "1":
                raise OcrServiceError("OCR provider could not finish a page.")
            text = page.get("ParsedText")
            if not isinstance(text, str):
                raise OcrServiceError("OCR provider returned invalid text.")
            texts.append(text)
        return "\n".join(texts)
    except (requests.RequestException, ValueError) as error:
        raise OcrServiceError("Could not contact or read the OCR provider.") from error


def verify_image(image_bytes):
    validate_image(image_bytes)
    try:
        plate = extract_plate_candidate(read_image_text(image_bytes))
    except OcrServiceError:
        logging.exception("OCR service failed")
        return {"status": "error"}
    if not plate:
        return {"status": "review"}

    vehicle = Vehicle.objects.filter(plate_number=plate).first()
    today = timezone.localdate()
    valid = vehicle is not None and vehicle.start_date <= today <= vehicle.expiry_date
    response = {"status": "complete", "plate_number": plate, "is_valid": valid}
    if vehicle:
        response.update(name=vehicle.owner_name, role=vehicle.role.lower())
    AccessLog.objects.create(
        plate_number=plate,
        status="AUTHORIZED" if valid else "DENIED_EXPIRED" if vehicle else "DENIED_UNREGISTERED",
    )
    return response
