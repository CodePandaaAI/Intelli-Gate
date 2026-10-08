from django.http import JsonResponse
from django.shortcuts import render
from django.views.decorators.csrf import ensure_csrf_cookie
from django.views.decorators.http import require_POST

from .verification import MAX_IMAGE_BYTES, verify_image


@ensure_csrf_cookie
def gate_test_ui(request):
    return render(request, "Main/gate_test.html")



@require_POST
def verify_vehicle(request):
    image = request.FILES.get("image")
    if image is None or image.size > MAX_IMAGE_BYTES:
        return JsonResponse({"status": "error", "message": "Upload a JPEG no larger than 1 MB."}, status=400)
    try:
        return JsonResponse(verify_image(image.read()))
    except ValueError as error:
        return JsonResponse({"status": "error", "message": str(error)}, status=400)
