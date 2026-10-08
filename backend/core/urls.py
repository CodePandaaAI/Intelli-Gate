from django.contrib import admin
from django.urls import path
from Main import views

urlpatterns = [
    path("", views.gate_test_ui, name="test_gate"),
    path("admin/", admin.site.urls),
    path("api/v1/verify", views.verify_vehicle, name="verify_vehicle"),
]
