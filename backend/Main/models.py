from django.db import models
from django.core.exceptions import ValidationError
import re

class Vehicle(models.Model):
    ROLE_CHOICES = [
        ('STUDENT', 'Student'),
        ('FACULTY', 'Faculty'),
        ('STAFF', 'Staff'),
    ]

    plate_number = models.CharField(max_length=20, unique=True, help_text="No spaces, e.g., PB02AB1234")
    owner_name = models.CharField(max_length=100)
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, default='STUDENT')
    start_date = models.DateField()
    expiry_date = models.DateField()

    def clean(self):
        super().clean()
        self.plate_number = re.sub(r"[^A-Z0-9]", "", self.plate_number.upper())
        if not self.plate_number:
            raise ValidationError({"plate_number": "Enter a plate number."})
        if self.start_date and self.expiry_date and self.expiry_date < self.start_date:
            raise ValidationError({"expiry_date": "Expiry cannot be before the start date."})

    def save(self, *args, **kwargs):
        self.full_clean()
        return super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.plate_number} ({self.owner_name})"

class AccessLog(models.Model):
    STATUS_CHOICES = [
        ('AUTHORIZED', 'Authorized'),
        ('DENIED_EXPIRED', 'Denied - Pass Expired or Not Started'),
        ('DENIED_UNREGISTERED', 'Denied - Not in System'),
        ('ERROR', 'Error - OCR Failed')
    ]

    # Only the text is saved here! No image field.
    plate_number = models.CharField(max_length=20)
    timestamp = models.DateTimeField(auto_now_add=True)
    status = models.CharField(max_length=30, choices=STATUS_CHOICES)

    def __str__(self):
        return f"[{self.status}] {self.plate_number} at {self.timestamp.strftime('%H:%M:%S')}"