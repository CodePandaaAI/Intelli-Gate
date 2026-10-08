import os
import django
from datetime import timedelta
from django.utils import timezone

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings')
django.setup()

from Main.models import Vehicle

def run_seed():

    today = timezone.localdate()

    vehicles = [
        # 1. Valid Student (Authorized)
        {"plate_number": "PB02AB1234", "owner_name": "Aarav Sharma", "role": "STUDENT", "start_date": today - timedelta(days=30), "expiry_date": today + timedelta(days=180)},

        # 2. Valid Faculty (Authorized)
        {"plate_number": "DL01XY9999", "owner_name": "Dr. Ramesh Verma", "role": "FACULTY", "start_date": today - timedelta(days=60), "expiry_date": today + timedelta(days=365)},

        # 3. Expired Pass (Denied)
        {"plate_number": "HR26DK5555", "owner_name": "Vikram Singh", "role": "STUDENT", "start_date": today - timedelta(days=120), "expiry_date": today - timedelta(days=5)},
    ]

    for v in vehicles:
        plate = v.pop("plate_number")
        _, created = Vehicle.objects.get_or_create(plate_number=plate, defaults=v)
        print(f"{'Created' if created else 'Kept'}: {plate}")

    print("Database seeding completed successfully.")

if __name__ == '__main__':
    run_seed()
