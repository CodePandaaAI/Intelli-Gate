from django.contrib import admin
from django.utils import timezone
from .models import Vehicle, AccessLog

@admin.register(Vehicle)
class VehicleAdmin(admin.ModelAdmin):
    # Removed 'is_active' and 'is_valid'
    # Added 'start_date' and custom 'check_validity'
    list_display = ('plate_number', 'owner_name', 'role', 'start_date', 'expiry_date', 'check_validity')

    # Removed 'is_active', added date filters
    list_filter = ('role', 'start_date', 'expiry_date')

    search_fields = ('plate_number', 'owner_name')

    # Recreate the valid check just for the admin display
    def check_validity(self, obj):
        today = timezone.localdate()
        return obj.start_date <= today <= obj.expiry_date

    check_validity.boolean = True # Shows a Green Check / Red X icon
    check_validity.short_description = 'Currently Valid'

@admin.register(AccessLog)
class AccessLogAdmin(admin.ModelAdmin):
    list_display = ('plate_number', 'status', 'timestamp')
    list_filter = ('status', 'timestamp')
    search_fields = ('plate_number',)